package com.example.sticker_art_gallery.service.telegram.chat;

import com.example.sticker_art_gallery.config.AppConfig;
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
    private static final String CHECK_CHANNEL_SUBSCRIPTION = "check_channel_subscription";
    private static final String MINI_APP_BASE_URL = "https://stixly.fun/miniapp/";

    private final AppConfig appConfig;
    private final StickerSetRepository stickerSetRepository;
    private final StickerSetService stickerSetService;
    private final TelegramBotApiService telegramBotApiService;

    public StickerGalleryFlowService(AppConfig appConfig,
                                     StickerSetRepository stickerSetRepository,
                                     StickerSetService stickerSetService,
                                     TelegramBotApiService telegramBotApiService) {
        this.appConfig = appConfig;
        this.stickerSetRepository = stickerSetRepository;
        this.stickerSetService = stickerSetService;
        this.telegramBotApiService = telegramBotApiService;
    }

    public void handleStart(JsonNode message) {
        long chatId = message.path("chat").path("id").asLong();
        long userId = message.path("from").path("id").asLong();
        String firstName = message.path("from").path("first_name").asText("друг");
        showStartScreen(chatId, userId, firstName, null);
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
        long chatId = callbackQuery.path("message").path("chat").path("id").asLong();
        long messageId = callbackQuery.path("message").path("message_id").asLong();
        long userId = callbackQuery.path("from").path("id").asLong();
        String firstName = callbackQuery.path("from").path("first_name").asText("друг");
        if (CHECK_CHANNEL_SUBSCRIPTION.equals(data)) {
            TelegramBotApiService.ChannelMembershipStatus membershipStatus = getRequiredChannelMembershipStatus(userId);
            if (membershipStatus == TelegramBotApiService.ChannelMembershipStatus.SUBSCRIBED) {
                showMainMenu(chatId, firstName, messageId);
                telegramBotApiService.answerCallbackQuery(callbackId, "Подписка подтверждена", false);
            } else if (membershipStatus == TelegramBotApiService.ChannelMembershipStatus.UNKNOWN) {
                telegramBotApiService.answerCallbackQuery(callbackId, "Не удалось проверить подписку. Попробуйте еще раз.", true);
            } else {
                telegramBotApiService.answerCallbackQuery(callbackId, "Сначала подпишитесь на канал", true);
            }
            return true;
        }
        if ("start_generation_entry".equals(data)) {
            telegramBotApiService.answerCallbackQuery(callbackId, null, false);
            telegramBotApiService.sendMessage(
                    chatId,
                    "🎨 Создание стикера — в мини-приложении: нажмите <b>Создать стикер</b> в главном меню.",
                    "HTML",
                    null,
                    null,
                    null
            );
            return true;
        }
        if ("upload_stickers_entry".equals(data)) {
            telegramBotApiService.answerCallbackQuery(callbackId, null, false);
            telegramBotApiService.sendMessage(
                    chatId,
                    "📥 Раздел добавления готовых стикеров.\n\n"
                            + "Отправьте сюда любой стикер из набора — предложу добавить стикерпак в галерею.",
                    null,
                    null,
                    null,
                    null
            );
            return true;
        }
        if ("manage_stickers_menu".equals(data)) {
            telegramBotApiService.answerCallbackQuery(callbackId, "Этот раздел переносится во 2-ю волну.", false);
            return true;
        }
        if ("back_to_main".equals(data)) {
            showStartScreen(chatId, userId, firstName, messageId);
            telegramBotApiService.answerCallbackQuery(callbackId, null, false);
            return true;
        }
        if (!data.startsWith("add_to_gallery:")) {
            return false;
        }

        long callbackMessageId = messageId;
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

    private void showStartScreen(long chatId, long userId, String firstName, Long messageId) {
        if (isChannelSubscriptionRequired()) {
            TelegramBotApiService.ChannelMembershipStatus membershipStatus = getRequiredChannelMembershipStatus(userId);
            if (membershipStatus != TelegramBotApiService.ChannelMembershipStatus.SUBSCRIBED) {
                showSubscriptionGate(
                        chatId,
                        messageId,
                        membershipStatus == TelegramBotApiService.ChannelMembershipStatus.UNKNOWN
                );
                return;
            }
        }
        showMainMenu(chatId, firstName, messageId);
    }

    private void showMainMenu(long chatId, String firstName, Long messageId) {
        if (messageId == null) {
            telegramBotApiService.sendMessage(chatId, startText(firstName), "HTML", mainMenuKeyboard(), null, null);
            return;
        }
        telegramBotApiService.editMessageText(chatId, messageId, startText(firstName), "HTML", mainMenuKeyboard());
    }

    private void showSubscriptionGate(long chatId, Long messageId, boolean unknownStatus) {
        String text = unknownStatus ? subscriptionCheckErrorText() : subscriptionRequiredText();
        if (messageId == null) {
            telegramBotApiService.sendMessage(chatId, text, "HTML", subscriptionKeyboard(), null, null);
            return;
        }
        telegramBotApiService.editMessageText(chatId, messageId, text, "HTML", subscriptionKeyboard());
    }

    private boolean isChannelSubscriptionRequired() {
        return appConfig.getTelegram() != null && appConfig.getTelegram().isChannelSubscriptionRequired();
    }

    private TelegramBotApiService.ChannelMembershipStatus getRequiredChannelMembershipStatus(long userId) {
        if (!isChannelSubscriptionRequired()) {
            return TelegramBotApiService.ChannelMembershipStatus.SUBSCRIBED;
        }
        return telegramBotApiService.getRequiredChannelMembershipStatus(userId);
    }

    private String startText(String name) {
        return "Йо, " + name + "!\n"
                + "Добро пожаловать в Stixly.\n\n"
                + "Вы можете <b>добавить стикеры в галерею</b> — за одобренный набор начисляется <b>+10 ART</b>.\n\n"
                + "Новый стикер создаётся в мини-приложении: кнопка <b>Создать стикер</b>.";
    }

    private String subscriptionRequiredText() {
        return "Чтобы открыть меню бота, сначала подпишитесь на наш канал.\n\n"
                + "После подписки нажмите <b>Проверить подписку</b>.";
    }

    private String subscriptionCheckErrorText() {
        return "Не удалось проверить подписку автоматически.\n\n"
                + "Убедитесь, что вы вступили в канал, и нажмите <b>Проверить подписку</b> еще раз.";
    }

    private String helpText() {
        return "🤖 <b>Справка</b>\n\n"
                + "/start - главное меню\n"
                + "/help - справка\n"
                + "/support - связь с поддержкой\n"
                + "/cancel - отмена\n\n"
                + "Создать стикер — кнопка <b>Создать стикер</b> (мини-приложение).\n"
                + "Добавить набор в галерею: <b>Добавить стикеры</b> или пришлите любой стикер из набора в ЛС.";
    }

    private Object mainMenuKeyboard() {
        return Map.of(
                "inline_keyboard",
                List.of(
                        List.of(Map.of("text", "🎨 Создать стикер", "web_app", Map.of("url", MINI_APP_BASE_URL))),
                        List.of(Map.of("text", "📥 Добавить стикеры", "callback_data", "upload_stickers_entry")),
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

    private Object subscriptionKeyboard() {
        return Map.of(
                "inline_keyboard",
                List.of(
                        List.of(Map.of("text", "Подписаться", "url", requiredChannelUrl())),
                        List.of(Map.of("text", "Проверить подписку", "callback_data", CHECK_CHANNEL_SUBSCRIPTION))
                )
        );
    }

    private String requiredChannelUrl() {
        String configuredUrl = appConfig.getTelegram() != null ? appConfig.getTelegram().getRequiredChannelUrl() : null;
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            return configuredUrl;
        }
        String username = appConfig.getTelegram() != null ? appConfig.getTelegram().getRequiredChannelUsername() : null;
        if (username == null || username.isBlank()) {
            return "https://t.me/stixlyofficial";
        }
        return "https://t.me/" + username.replaceFirst("^@", "");
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
                        List.of(Map.of("text", "Посмотреть в Stixly", "url", MINI_APP_BASE_URL + "gallery?set_id=" + setId)),
                        List.of(Map.of("text", "Главное меню", "callback_data", "back_to_main"))
                )
        );
    }
}
