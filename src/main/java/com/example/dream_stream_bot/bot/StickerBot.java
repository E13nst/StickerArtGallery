package com.example.dream_stream_bot.bot;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
import com.example.dream_stream_bot.service.telegram.StickerService;

import com.example.dream_stream_bot.service.telegram.UserStateService;
import com.example.dream_stream_bot.service.telegram.StickerSetService;
import com.example.dream_stream_bot.model.keyboard.InlineKeyboardMarkupBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.dream_stream_bot.model.telegram.StickerSet;

import java.util.List;
import java.util.ArrayList;

public class StickerBot extends AbstractTelegramBot {
    
    private final StickerService stickerService;
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerBot.class);

    private final UserStateService userStateService;
    private final StickerSetService stickerSetService;
    
    public StickerBot(BotEntity botEntity, MessageHandlerService messageHandlerService, 
                     UserStateService userStateService, StickerSetService stickerSetService,
                     StickerService stickerService) {
        super(botEntity, messageHandlerService);
        this.stickerService = stickerService;
        this.userStateService = userStateService;
        this.stickerSetService = stickerSetService;
    }
    
    @Override
    public void onUpdateReceived(Update update) {
        // Добавляем логирование всех входящих сообщений
        if (update.hasMessage()) {
            Message msg = update.getMessage();
            LOGGER.info("📨 Получено сообщение | ChatId: {} | Тип: {} | HasPhoto: {} | HasDocument: {} | HasText: {} | HasSticker: {} | HasVideo: {} | HasAudio: {} | Text: '{}'",
                    msg.getChatId(),
                    msg.hasPhoto() ? "PHOTO" : msg.hasDocument() ? "DOCUMENT" : msg.hasText() ? "TEXT" : msg.hasSticker() ? "STICKER" : msg.hasVideo() ? "VIDEO" : msg.hasAudio() ? "AUDIO" : "OTHER",
                    msg.hasPhoto(),
                    msg.hasDocument(),
                    msg.hasText(),
                    msg.hasSticker(),
                    msg.hasVideo(),
                    msg.hasAudio(),
                    msg.hasText() ? msg.getText() : "N/A");
        }

        // Обработка callback-ов от inline кнопок
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            LOGGER.info("🔘 Получен callback | ChatId: {} | Data: {}", chatId, callbackData);

            if ("создать_новый_набор".equals(callbackData)) {
                // Начинаем процесс создания стикерпака
                userStateService.setUserState(chatId, UserStateService.UserState.WAITING_FOR_PACK_TITLE);

                SendMessage infoMessage = SendMessage.builder()
                        .chatId(chatId)
                        .text("Введи имя для нового стикерпака:")
                        .build();
                sendWithLogging(infoMessage);
                return;
            } else if ("редактировать_набор".equals(callbackData)) {
                // Показываем список наборов пользователя
                showUserStickerPacks(chatId, 0);
                return;
            } else if (callbackData.startsWith("pack_")) {
                // Обработка выбора конкретного набора
                handlePackSelection(chatId, callbackData);
                return;
            } else if (callbackData.matches("\\d+")) {
                // Обработка пагинации (кнопки с номерами страниц)
                int page = Integer.parseInt(callbackData) - 1; // Нумерация страниц с 0
                showUserStickerPacks(chatId, page);
                return;
            } else if ("back_to_main".equals(callbackData)) {
                // Возврат в главное меню
                showMainMenu(chatId);
                return;
            } else if (callbackData.startsWith("info_")) {
                // Игнорируем клик по информационным строкам
                return;
            }
        }

        if (update.hasMessage()) {
            Message msg = update.getMessage();
            String conversationId = getConversationId(msg.getChatId());

            // Проверяем, нужно ли отвечать (для групповых чатов)
            boolean isGroup = msg.isGroupMessage() || msg.isSuperGroupMessage();
            if (isGroup) {
                boolean isReplyToBot = msg.getReplyToMessage() != null &&
                        msg.getReplyToMessage().getFrom() != null &&
                        msg.getReplyToMessage().getFrom().getUserName() != null &&
                        msg.getReplyToMessage().getFrom().getUserName().equalsIgnoreCase(getBotUsername());
                boolean isMention = msg.hasText() && msg.getText().toLowerCase().contains("@" + getBotUsername().toLowerCase());
                boolean isName = msg.hasText() && msg.getText().toLowerCase().contains(botEntity.getName().toLowerCase());
                boolean isAlias = msg.hasText() && botEntity.getBotTriggersList().stream().anyMatch(alias -> !alias.isEmpty() && msg.getText().toLowerCase().contains(alias.toLowerCase()));
                boolean isTrigger = msg.hasText() && botEntity.getBotTriggersList().stream().anyMatch(trigger -> !trigger.isEmpty() && msg.getText().toLowerCase().contains(trigger.toLowerCase()));

                if (!(isReplyToBot || isMention || isName || isAlias || isTrigger)) {
                    return; // Игнорируем сообщение, если не обращение к боту
                }
            }

            // Обрабатываем изображения
            if (msg.hasPhoto() || msg.hasDocument()) {
                LOGGER.info("🖼️ Обрабатываем изображение | ChatId: {} | Тип: {}", msg.getChatId(), msg.hasPhoto() ? "PHOTO" : "DOCUMENT");

                // Получаем name стикерпака для этого пользователя
                String stickerPackName = null;
                try {
                    // Сначала проверяем, есть ли выбранный набор
                    Long selectedPackId = userStateService.getSelectedSetId(msg.getChatId());
                    if (selectedPackId != null) {
                        StickerSet selectedPack = stickerSetService.findById(selectedPackId);
                        if (selectedPack != null && selectedPack.getUserId().equals(msg.getChatId())) {
                            stickerPackName = selectedPack.getName();
                            LOGGER.info("📦 Используем выбранный стикерпак для пользователя {}: Name='{}', ID={}",
                                    msg.getChatId(), stickerPackName, selectedPackId);
                        } else {
                            // Выбранный набор не найден или не принадлежит пользователю, очищаем
                            userStateService.clearSelectedSetId(msg.getChatId());
                            LOGGER.warn("⚠️ Выбранный стикерпак не найден или не принадлежит пользователю {}, очищаем выбор", msg.getChatId());
                        }
                    }
                    
                    // Если выбранный набор не найден, берем последний созданный
                    if (stickerPackName == null) {
                        List<StickerSet> userPacks = stickerSetService.findByUserId(msg.getChatId());
                        if (!userPacks.isEmpty()) {
                            // Берем последний созданный стикерпак (самый новый)
                            StickerSet latestPack = userPacks.stream()
                                    .max((p1, p2) -> p1.getCreatedAt().compareTo(p2.getCreatedAt()))
                                    .orElse(userPacks.get(0));
                            stickerPackName = latestPack.getName();
                            LOGGER.info("📦 Используем последний созданный стикерпак для пользователя {}: Name='{}', Created={}", 
                                    msg.getChatId(), stickerPackName, latestPack.getCreatedAt());
                        } else {
                            LOGGER.warn("⚠️ Стикерпак для пользователя {} не найден в базе данных", msg.getChatId());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("❌ Ошибка при поиске стикерпака для пользователя {}: {}", msg.getChatId(), e.getMessage());
                }

                SendMessage response = stickerService.handleImageMessage(msg, this, stickerPackName);
                if (response != null) {
                    sendWithLogging(response);
                }
                // Если response == null, значит стикер уже был отправлен в StickerService
            } else if (msg.hasText()) {
                // Проверяем состояние пользователя
                UserStateService.UserState currentState = userStateService.getUserState(msg.getChatId());
                
                if (currentState == UserStateService.UserState.WAITING_FOR_PACK_TITLE) {
                    // Пользователь ввел название стикерпака
                    String title = msg.getText();
                    if (title.length() > 64) {
                        SendMessage errorMessage = SendMessage.builder()
                                .chatId(msg.getChatId())
                                .text("❌ Название слишком длинное! Максимум 64 символа. Попробуйте еще раз:")
                                .build();
                        sendWithLogging(errorMessage);
                        return;
                    }
                    
                    // Сохраняем название и переходим к следующему шагу
                    UserStateService.StickerSetData setData = new UserStateService.StickerSetData();
                    setData.setTitle(title);
                    userStateService.setStickerSetData(msg.getChatId(), setData);
                    userStateService.setUserState(msg.getChatId(), UserStateService.UserState.WAITING_FOR_PACK_NAME);
                    
                    SendMessage nextStepMessage = SendMessage.builder()
                            .chatId(msg.getChatId())
                            .text("Введите имя ссылки для нового пакета стикеров:\n\n" +
                                    "💡 Подсказка: окончание '_by_StickerGalleryBot' добавится автоматически\n" +
                                    "Пример: если введете 'my_stickers', получится 'my_stickers_by_StickerGalleryBot'")
                            .build();
                    sendWithLogging(nextStepMessage);
                    return;
                    
                } else if (currentState == UserStateService.UserState.WAITING_FOR_PACK_NAME) {
                    // Пользователь ввел имя ссылки стикерпака
                    String userInput = msg.getText();
                    
                    // Автоматически добавляем корректное окончание
                    String name = userInput;
                    if (!name.endsWith("_by_StickerGalleryBot")) {
                        name = userInput + "_by_StickerGalleryBot";
                    }
                    
                    // Проверяем уникальность имени
                    StickerSet existingPack = stickerSetService.findByName(name);
                    if (existingPack != null) {
                        SendMessage errorMessage = SendMessage.builder()
                                .chatId(msg.getChatId())
                                .text("❌ Стикерпак с таким именем уже существует!\n\n" +
                                        "Попробуйте другое имя ссылки:")
                                .build();
                        sendWithLogging(errorMessage);
                        return;
                    }
                    
                    // Сохраняем имя и завершаем создание
                    UserStateService.StickerSetData packData = userStateService.getStickerSetData(msg.getChatId());
                    packData.setName(name);
                    
                    // Сохраняем в базу данных
                    StickerSet savedPack = stickerSetService.createStickerSet(
                        msg.getChatId(), packData.getTitle(), packData.getName());
                    LOGGER.info("📦 Создан стикерпак: Title='{}', Name='{}', UserId={}, DB_ID={}", 
                            packData.getTitle(), packData.getName(), msg.getChatId(), savedPack.getId());
                    
                    // Очищаем состояние пользователя
                    userStateService.clearUserState(msg.getChatId());
                    
                    SendMessage successMessage = SendMessage.builder()
                            .chatId(msg.getChatId())
                            .text("✅ Стикерпак создан!\n\n" +
                                    "📝 Название: " + packData.getTitle() + "\n" +
                                    "🔗 Ссылка: https://t.me/addstickers/" + packData.getName() + "\n\n" +
                                    "Отправьте изображение для создания стикера!")
                            .build();
                    sendWithLogging(successMessage);
                    return;
                }
                
                // Обрабатываем текстовые сообщения
                String text = msg.getText().toLowerCase();
                
                // Обработка команды /start
                if (text.equals("/start")) {
                    InlineKeyboardMarkup keyboard = new InlineKeyboardMarkupBuilder()
                            .addRow("Создать новый набор", "создать_новый_набор")
                            .addRow("Редактировать набор", "редактировать_набор")
                            .build();
                    
                    SendMessage welcomeMessage = SendMessage.builder()
                            .chatId(msg.getChatId())
                            .text("🎯 **Добро пожаловать в StickerBot!**\n\n" +
                                    "Я помогу вам создавать стикеры из изображений.\n\n" +
                                    "📸 **Как использовать:**\n" +
                                    "1. Нажмите 'Создать новый набор'\n" +
                                    "2. Введите название для набора\n" +
                                    "3. Введите короткую ссылку\n" +
                                    "4. Отправьте изображение\n\n" +
                                    "**Выберите действие:**")
                            .parseMode("Markdown")
                            .replyMarkup(keyboard)
                            .build();
                    sendWithLogging(welcomeMessage);
                    return;
                }
                
                // Обработка кнопки "Создать новый набор"
                if (text.equals("создать новый набор")) {
                    SendMessage infoMessage = SendMessage.builder()
                            .chatId(msg.getChatId())
                            .text("📸 **Отправьте изображение для создания стикера!**\n\n" +
                                    "Поддерживаемые форматы: JPG, PNG, GIF")
                            .parseMode("Markdown")
                            .build();
                    sendWithLogging(infoMessage);
                    return;
                }
                

                
                if (text.contains("стикер") || text.contains("sticker") || text.contains("help")) {
                    SendMessage helpMessage = SendMessage.builder()
                            .chatId(msg.getChatId())
                            .text("🎯 **StickerBot** - создание стикеров из изображений\n\n" +
                                    "📸 **Как использовать:**\n" +
                                    "• Отправьте фото или изображение (JPG, PNG, GIF)\n" +
                                    "• Бот автоматически создаст стикер\n" +
                                    "• Готовый стикер будет отправлен в чат\n\n" +
                                    "⚠️ **Требования к изображению:**\n" +
                                    "• Формат: PNG, WebP\n" +
                                    "• Размер: до 512KB\n" +
                                    "• Разрешение: квадратное (512x512 пикселей)\n\n" +
                                    "🚀 **Начните прямо сейчас** - отправьте изображение!")
                            .parseMode("Markdown")
                            .build();
                    sendWithLogging(helpMessage);
                } else {
                    SendMessage infoMessage = SendMessage.builder()
                            .chatId(msg.getChatId())
                            .text("🎯 Отправьте изображение для создания стикера!\n\n" +
                                    "Поддерживаемые форматы: JPG, PNG, GIF")
                            .build();
                    sendWithLogging(infoMessage);
                }
            } else if (msg.hasSticker()) {
                // Обрабатываем стикеры
                LOGGER.info("🎭 Получен стикер | ChatId: {} | Emoji: {}", msg.getChatId(), msg.getSticker().getEmoji());
                SendMessage response = SendMessage.builder()
                        .chatId(msg.getChatId())
                        .text("🎭 Вы отправили стикер! Для создания нового стикера отправьте изображение (фото или документ).")
                        .build();
                sendWithLogging(response);
            } else if (msg.hasVideo() || msg.hasAudio()) {
                // Обрабатываем видео и аудио
                LOGGER.info("🎬 Получено видео/аудио | ChatId: {} | Тип: {}", msg.getChatId(), msg.hasVideo() ? "VIDEO" : "AUDIO");
                SendMessage response = SendMessage.builder()
                        .chatId(msg.getChatId())
                        .text("🎬 Для создания стикера отправьте изображение (фото или документ), а не видео/аудио.")
                        .build();
                sendWithLogging(response);
            } else {
                // Обрабатываем другие типы сообщений
                LOGGER.info("❓ Неизвестный тип сообщения | ChatId: {} | Тип: OTHER", msg.getChatId());
                SendMessage response = SendMessage.builder()
                        .chatId(msg.getChatId())
                        .text("📸 Для создания стикера отправьте изображение (фото или документ).\n\n" +
                                "Поддерживаемые форматы: JPG, PNG, GIF")
                        .build();
                sendWithLogging(response);
            }
        }
    }
    
    /**
     * Показывает главное меню
     */
    private void showMainMenu(Long chatId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkupBuilder()
                .addRow("Создать новый набор", "создать_новый_набор")
                .addRow("Редактировать набор", "редактировать_набор")
                .build();

        SendMessage mainMenuMessage = SendMessage.builder()
                .chatId(chatId)
                .text("🎯 **Главное меню StickerBot**\n\n" +
                        "**Выберите действие:**")
                .parseMode("Markdown")
                .replyMarkup(keyboard)
                .build();

        sendWithLogging(mainMenuMessage);
    }
    
    /**
     * Показывает список стикерпаков пользователя с пагинацией
     */
    private void showUserStickerPacks(Long chatId, int page) {
        try {
            List<StickerSet> userPacks = stickerSetService.findByUserId(chatId);
            
            if (userPacks.isEmpty()) {
                SendMessage noPacksMessage = SendMessage.builder()
                        .chatId(chatId)
                        .text("📭 У вас пока нет созданных наборов стикеров.\n\n" +
                                "Создайте первый набор, нажав кнопку 'Создать новый набор'!")
                        .build();
                sendWithLogging(noPacksMessage);
                return;
            }
            
            // Сортируем по дате создания (новые сначала)
            userPacks.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));
            
            int itemsPerPage = 10;
            int totalPages = (userPacks.size() + itemsPerPage - 1) / itemsPerPage;
            int startIndex = page * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, userPacks.size());
            
            // Получаем наборы для текущей страницы
            List<StickerSet> pagePacks = userPacks.subList(startIndex, endIndex);
            
            // Создаем клавиатуру с наборами
            InlineKeyboardMarkupBuilder keyboardBuilder = new InlineKeyboardMarkupBuilder();
            
            // Добавляем кнопки для каждого набора (по одной на строку)
            for (StickerSet pack : pagePacks) {
                String buttonText = String.format("📦 %s", 
                    pack.getTitle().length() > 40 ? pack.getTitle().substring(0, 37) + "..." : pack.getTitle());
                LOGGER.info("🔘 Создаем кнопку для набора: '{}' с callback '{}'", buttonText, "pack_" + pack.getId());
                keyboardBuilder.addButtonOnNewRow(buttonText, "pack_" + pack.getId());
            }
            
            // Добавляем навигацию по страницам
            if (totalPages > 1) {
                // Создаем кнопки страниц в одну строку
                keyboardBuilder.addPageNavigation(page, totalPages);

                // Добавляем информацию о странице
                keyboardBuilder.addInfoRow("📄 Страница " + (page + 1) + " из " + totalPages);
            }
            
            // Добавляем кнопку возврата
            keyboardBuilder.addButtonOnNewRow("🔙 Назад", "back_to_main");
            
            String messageText = String.format("📋 **Ваши наборы стикеров**\n\n" +
                    "Всего наборов: %d\n" +
                    "Страница: %d из %d\n\n" +
                    "**Выберите набор для редактирования:**",
                    userPacks.size(), page + 1, totalPages);
            
            SendMessage packsListMessage = SendMessage.builder()
                    .chatId(chatId)
                    .text(messageText)
                    .parseMode("Markdown")
                    .replyMarkup(keyboardBuilder.build())
                    .build();
            
            sendWithLogging(packsListMessage);
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении списка наборов для пользователя {}: {}", chatId, e.getMessage());
            SendMessage errorMessage = SendMessage.builder()
                    .chatId(chatId)
                    .text("❌ Произошла ошибка при получении списка наборов. Попробуйте еще раз.")
                    .build();
            sendWithLogging(errorMessage);
        }
    }
    
    /**
     * Обрабатывает выбор конкретного набора
     */
    private void handlePackSelection(Long chatId, String callbackData) {
        try {
            Long packId = Long.parseLong(callbackData.substring(5));
            LOGGER.info("🔍 Отладка: Ищем набор с ID: {}", packId);

            StickerSet pack = stickerSetService.findById(packId);

            if (pack == null) {
                LOGGER.error("❌ Набор с ID {} не найден в базе данных", packId);
                SendMessage errorMessage = SendMessage.builder()
                        .chatId(chatId)
                        .text("❌ Набор не найден. Возможно, он был удален.")
                        .build();
                sendWithLogging(errorMessage);
                return;
            }

            // Проверяем, что набор принадлежит пользователю
            if (!pack.getUserId().equals(chatId)) {
                LOGGER.error("❌ Набор {} не принадлежит пользователю {}", packId, chatId);
                SendMessage errorMessage = SendMessage.builder()
                        .chatId(chatId)
                        .text("❌ У вас нет доступа к этому набору.")
                        .build();
                sendWithLogging(errorMessage);
                return;
            }

            // Устанавливаем состояние для добавления стикера в выбранный набор
            userStateService.setSelectedSetId(chatId, packId);

            String messageText = String.format("✅ **Выбран набор:** %s\n\n" +
                            "📝 Название: %s\n" +
                            "🔗 Ссылка: https://t.me/addstickers/%s\n\n" +
                            "Отправьте изображение для добавления стикера!",
                            pack.getTitle(), pack.getTitle(), pack.getName());

            SendMessage setSelectedMessage = SendMessage.builder()
                    .chatId(chatId)
                    .text(messageText)
                    .parseMode("Markdown")
                    .build();

            sendWithLogging(setSelectedMessage);

        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при выборе набора {} для пользователя {}: {}", callbackData, chatId, e.getMessage());
            SendMessage errorMessage = SendMessage.builder()
                    .chatId(chatId)
                    .text("❌ Произошла ошибка при выборе набора. Попробуйте еще раз.")
                    .build();
            sendWithLogging(errorMessage);
        }
    }
} 