package com.example.dream_stream_bot.config;

import com.example.dream_stream_bot.bot.AbstractTelegramBot;
import com.example.dream_stream_bot.bot.BotFactory;
import com.example.dream_stream_bot.bot.AbstractTelegramBot;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
import com.example.dream_stream_bot.service.telegram.UserStateService;
import com.example.dream_stream_bot.service.telegram.StickerSetService;
import com.example.dream_stream_bot.service.telegram.StickerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class BotInitializer {
    private final BotService botService;
    private final MessageHandlerService messageHandlerService;
    private final UserStateService userStateService;
    private final StickerSetService stickerSetService;
    private final StickerService stickerService;
    private final Map<String, AbstractTelegramBot> botRegistry = new java.util.concurrent.ConcurrentHashMap<>();

    @Autowired
    public BotInitializer(BotService botService, MessageHandlerService messageHandlerService,
                         UserStateService userStateService, StickerSetService stickerSetService,
                         StickerService stickerService) {
        this.botService = botService;
        this.messageHandlerService = messageHandlerService;
        this.userStateService = userStateService;
        this.stickerSetService = stickerSetService;
        this.stickerService = stickerService;
    }

    @Bean
    public Map<String, AbstractTelegramBot> botRegistry() {
        return botRegistry;
    }

    @EventListener({ApplicationReadyEvent.class})
    public void init() {
        log.info("🚀 Application is ready! Starting bot initialization...");
        
        try {
            initializeBots();
        } catch (Exception e) {
            log.error("❌ Critical error during bot initialization", e);
            // Не прерываем запуск приложения, только логируем ошибку
        }
    }
    
    private void initializeBots() throws TelegramApiException {
        log.info("🤖 Initializing all Telegram bots...");
        
        // Проверяем зависимости
        log.info("🔍 Checking dependencies:");
        log.info("  - BotService: {}", botService != null ? "✅" : "❌");
        log.info("  - MessageHandlerService: {}", messageHandlerService != null ? "✅" : "❌");
        log.info("  - UserStateService: {}", userStateService != null ? "✅" : "❌");
        log.info("  - StickerSetService: {}", stickerSetService != null ? "✅" : "❌");
        log.info("  - StickerService: {}", stickerService != null ? "✅" : "❌");
        
        try {
            List<BotEntity> bots = botService.getAllBots();
            log.info("📋 Found {} bots in database", bots.size());

            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            int successCount = 0;
            int errorCount = 0;
            
            for (BotEntity bot : bots) {
                log.info("🔍 Processing bot: username='{}', type='{}', active={}, miniapp={}",
                        bot.getUsername(), bot.getType(), bot.getIsActive(), bot.getMiniapp());

                if (Boolean.TRUE.equals(bot.getIsActive())) {
                    try {
                        AbstractTelegramBot telegramBot = BotFactory.createBot(bot, messageHandlerService, userStateService, stickerSetService, stickerService);
                        telegramBotsApi.registerBot(telegramBot);
                        botRegistry.put(bot.getUsername(), telegramBot);
                        log.info("✅ Bot '{}' registered successfully (type: {})", bot.getUsername(), bot.getType());
                        successCount++;
                    } catch (Exception e) {
                        log.error("❌ Error creating bot '{}' (type: {}): {}",
                                bot.getUsername(), bot.getType(), e.getMessage(), e);
                        errorCount++;
                    }
                } else {
                    log.info("⏸️ Bot '{}' skipped (inactive)", bot.getUsername());
                }
            }
            
            log.info("🎉 Bot initialization completed: {} successful, {} errors", successCount, errorCount);
            
        } catch (Exception e) {
            log.error("❌ Error loading bots from database: {}", e.getMessage(), e);
            throw e;
        }
    }
}
