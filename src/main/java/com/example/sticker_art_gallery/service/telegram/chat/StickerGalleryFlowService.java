package com.example.sticker_art_gallery.service.telegram.chat;

import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.repository.StickerSetRepository;
import com.example.sticker_art_gallery.service.telegram.StickerSetService;
import com.example.sticker_art_gallery.service.telegram.TelegramBotApiService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class StickerGalleryFlowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StickerGalleryFlowService.class);

    private final StickerSetRepository stickerSetRepository;
    private final StickerSetService stickerSetService;
    private final TelegramBotApiService telegramBotApiService;

    public StickerGalleryFlowService(StickerSetRepository stickerSetRepository,
                                     StickerSetService stickerSetService,
                                     TelegramBotApiService telegramBotApiService) {
        this.stickerSetRepository = stickerSetRepository;
        this.stickerSetService = stickerSetService;
        this.telegramBotApiService = telegramBotApiService;
    }

    public void handleStart(JsonNode message) {
        long chatId = message.path("chat").path("id").asLong();
        String firstName = message.path("from").path("first_name").asText("друг");
        telegramBotApiService.sendMessage(chatId, startText(firstName), "HTML", mainMenuKeyboard(), null, null);
    }

    public void handleHelp(JsonNode message) {
        long chatId = message.path("chat").path("id").asLong();
        telegramBotApiService.sendMessage(chatId, helpText(), "HTML", null, null, null);
    }

    public void handleCancel(JsonNode message) {
        long chatId = message.path("chat").path("id").asLong();
        telegramBotApiService.sendMessage(chatId, "Диалог отменен. Используйте /start, чтобы начать заново.", null, null, null, null);
    }

    public void handleIncomingSticker(JsonNode message) {
        String setName = message.path("sticker").path("set_name").asText(null);
        long chatId = message.path("chat").path("id").asLong();
        String chatType = message.path("chat").path("type").asText("");
        long msgId = message.path("message_id").asLong();
        long userId = message.path("from").path("id").asLong();

        if (setName == null || setName.isBlank()) {
            telegramBotApiService.sendMessage(chatId,
                    "У этого стикера не удалось определить стикерпак.\nПопробуйте прислать стикер из обычного набора.",
                    null, null, null, null);
            return;
        }

        boolean exists = stickerSetRepository.findByNameIgnoreCase(setName).isPresent();
        boolean isGroup = "group".equals(chatType) || "supergroup".equals(chatType);

        if (exists) {
            try {
                telegramBotApiService.setMessageReaction(chatId, msgId, "✅");
            } catch (Exception e) {
                LOGGER.debug("Reaction set failed: {}", e.getMessage());
            }
            if (!isGroup) {
                telegramBotApiService.sendMessage(
                        chatId,
                        "Мы уже знаем этот стикерсет — он уже в Галерее.",
                        null,
                        existingSetKeyboard(setName),
                        null,
                        null
                );
            }
            return;
        }

        telegramBotApiService.sendMessage(
                chatId,
                "О! Такого набора еще нет в Stixly.\n\nДобавим этот набор в Галерею и начислим +10 ART?",
                null,
                addToGalleryKeyboard(setName, msgId),
                null,
                null
        );
        LOGGER.info("Sticker set proposal sent: userId={}, setName={}", userId, setName);
    }

    public boolean handleCallback(JsonNode callbackQuery) {
        String data = callbackQuery.path("data").asText("");
        String callbackId = callbackQuery.path("id").asText();
        if ("manage_stickers_menu".equals(data)) {
            telegramBotApiService.answerCallbackQuery(callbackId, "Этот раздел переносится во 2-ю волну.", false);
            return true;
        }
        if ("back_to_main".equals(data)) {
            long chatId = callbackQuery.path("message").path("chat").path("id").asLong();
            long messageId = callbackQuery.path("message").path("message_id").asLong();
            String firstName = callbackQuery.path("from").path("first_name").asText("друг");
            telegramBotApiService.editMessageText(chatId, messageId, startText(firstName), "HTML", mainMenuKeyboard());
            telegramBotApiService.answerCallbackQuery(callbackId, null, false);
            return true;
        }
        if (!data.startsWith("add_to_gallery:")) {
            return false;
        }

        long userId = callbackQuery.path("from").path("id").asLong();
        long chatId = callbackQuery.path("message").path("chat").path("id").asLong();
        long callbackMessageId = callbackQuery.path("message").path("message_id").asLong();
        try {
            String[] parts = data.split(":");
            String setName = parts.length > 1 ? parts[1] : "";
            long originalMessageId = parts.length > 2 ? Long.parseLong(parts[2]) : callbackMessageId;

            if (setName.isBlank()) {
                telegramBotApiService.answerCallbackQuery(callbackId, "Не удалось определить стикерсет", true);
                return true;
            }

            String url = "https://t.me/addstickers/" + setName;
            CreateStickerSetDto dto = new CreateStickerSetDto();
            dto.setName(url);
            dto.setVisibility(StickerSetVisibility.PUBLIC);

            StickerSet saved = stickerSetService.createStickerSetForUser(dto, userId, "ru", false);

            telegramBotApiService.editMessageText(
                    chatId,
                    callbackMessageId,
                    "✅ Стикерсет успешно добавлен в галерею!\n\nЗа ваш вклад начислено +10 ART.\n\nСтикерсет: " + url,
                    null,
                    successKeyboard(saved.getId())
            );
            try {
                telegramBotApiService.setMessageReaction(chatId, originalMessageId, "👍");
            } catch (Exception e) {
                LOGGER.debug("Failed to set success reaction: {}", e.getMessage());
            }
            telegramBotApiService.answerCallbackQuery(callbackId, "Стикерсет добавлен", false);
            return true;
        } catch (Exception e) {
            LOGGER.error("add_to_gallery failed: {}", e.getMessage(), e);
            telegramBotApiService.answerCallbackQuery(callbackId, "Не удалось добавить стикерсет", true);
            return true;
        }
    }

    private String startText(String name) {
        return "Йо, " + name + "!\n"
                + "Ты в зоне Stixly — комьюнити самой большой галереи стикеров.\n\n"
                + "<b>Сейчас можно:</b>\n"
                + "• Найти стикер в галерее\n"
                + "• Добавить стикерсет в галерею (+10 ART)\n\n"
                + "❓ Помощь: /help | 📞 Поддержка: /support";
    }

    private String helpText() {
        return "🤖 <b>Справка</b>\n\n"
                + "/start - главное меню\n"
                + "/help - справка\n"
                + "/support - связь с поддержкой\n"
                + "/cancel - отмена\n\n"
                + "Пришлите любой стикер в ЛС — бот проверит набор в галерее.";
    }

    private Object mainMenuKeyboard() {
        return Map.of(
                "inline_keyboard",
                List.of(
                        List.of(Map.of("text", "🔍 Найти стикер в галерее", "url", "https://stickerartgallery-e13nst.amvera.io/mini-app/")),
                        List.of(Map.of("text", "🛠 Управление стикерами", "callback_data", "manage_stickers_menu")),
                        List.of(Map.of("text", "📞 Поддержка", "callback_data", "enter_support")),
                        List.of(Map.of("text", "📢 Telegram-канал", "url", "https://t.me/stixlyofficial"))
                )
        );
    }

    private Object addToGalleryKeyboard(String setName, long originalMessageId) {
        return Map.of(
                "inline_keyboard",
                List.of(
                        List.of(Map.of("text", "Добавить в галерею", "callback_data", "add_to_gallery:" + setName + ":" + originalMessageId)),
                        List.of(Map.of("text", "Главное меню", "callback_data", "back_to_main"))
                )
        );
    }

    private Object existingSetKeyboard(String setName) {
        return Map.of(
                "inline_keyboard",
                List.of(
                        List.of(Map.of("text", "Открыть в Telegram", "url", "https://t.me/addstickers/" + setName)),
                        List.of(Map.of("text", "Главное меню", "callback_data", "back_to_main"))
                )
        );
    }

    private Object successKeyboard(Long setId) {
        if (setId == null) {
            return Map.of("inline_keyboard", List.of(List.of(Map.of("text", "Главное меню", "callback_data", "back_to_main"))));
        }
        return Map.of(
                "inline_keyboard",
                List.of(
                        List.of(Map.of("text", "Посмотреть в Stixly", "url", "https://stickerartgallery-e13nst.amvera.io/mini-app/gallery?set_id=" + setId)),
                        List.of(Map.of("text", "Главное меню", "callback_data", "back_to_main"))
                )
        );
    }
}
