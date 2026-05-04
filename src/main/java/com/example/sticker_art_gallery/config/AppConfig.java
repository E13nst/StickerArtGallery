package com.example.sticker_art_gallery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    private String url;
    private String serviceApiToken;
    private MiniApp miniApp = new MiniApp();
    private Telegram telegram = new Telegram();
    private Referral referral = new Referral();
    private StickerBot stickerbot = new StickerBot();
    private Webhook webhook = new Webhook();
    private TonPay tonpay = new TonPay();
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }

    public String getServiceApiToken() {
        return serviceApiToken;
    }

    public void setServiceApiToken(String serviceApiToken) {
        this.serviceApiToken = serviceApiToken;
    }
    
    public MiniApp getMiniApp() {
        return miniApp;
    }
    
    public void setMiniApp(MiniApp miniApp) {
        this.miniApp = miniApp;
    }
    
    public Telegram getTelegram() {
        return telegram;
    }
    
    public void setTelegram(Telegram telegram) {
        this.telegram = telegram;
    }
    
    public Referral getReferral() {
        return referral;
    }
    
    public void setReferral(Referral referral) {
        this.referral = referral;
    }
    
    public StickerBot getStickerbot() {
        return stickerbot;
    }
    
    public void setStickerbot(StickerBot stickerbot) {
        this.stickerbot = stickerbot;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    public void setWebhook(Webhook webhook) {
        this.webhook = webhook;
    }

    public TonPay getTonpay() {
        return tonpay;
    }

    public void setTonpay(TonPay tonpay) {
        this.tonpay = tonpay;
    }

    public static class MiniApp {
        private String url;
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
    }
    
    public static class Telegram {
        private String botToken;
        private String botUsername;
        private String defaultStickerSetTitle;
        private boolean channelSubscriptionRequired = false;
        private Long requiredChannelId;
        private String requiredChannelUsername;
        private String requiredChannelUrl;

        /** Java создаёт invoice напрямую через Bot API, минуя StickerBot */
        private boolean nativePaymentEnabled = false;

        /** Java отправляет сообщения напрямую через Bot API, минуя StickerBot */
        private boolean nativeMessagingEnabled = false;

        /**
         * Текущий владелец входящих Telegram updates.
         * Значения: "stickerbot" (по умолчанию) или "java" (после cutover).
         */
        private String webhookOwner = "stickerbot";

        /**
         * Secret token для Telegram webhook.
         * Проверяется по заголовку X-Telegram-Bot-Api-Secret-Token.
         */
        private String webhookSecretToken;

        /**
         * Явный URL webhook (опционально). Если пусто, строится как {app.url}/api/telegram/updates.
         */
        private String webhookUrl;

        /**
         * Автоматически регистрировать Telegram webhook на старте сервиса.
         */
        private boolean webhookAutoRegisterOnStartup = true;

        /**
         * Chat ID операторского чата поддержки.
         */
        private Long supportChatId;

        /**
         * Включает обработчики режима поддержки в inbound Telegram updates.
         */
        private boolean supportEnabled = false;

        /**
         * Использовать forum topics для маршрутизации обращений поддержки.
         */
        private boolean supportUseTopics = true;

        public String getBotToken() {
            return botToken;
        }
        
        public void setBotToken(String botToken) {
            this.botToken = botToken;
        }

        public String getBotUsername() {
            return botUsername;
        }

        public void setBotUsername(String botUsername) {
            this.botUsername = botUsername;
        }

        public String getDefaultStickerSetTitle() {
            return defaultStickerSetTitle;
        }

        public void setDefaultStickerSetTitle(String defaultStickerSetTitle) {
            this.defaultStickerSetTitle = defaultStickerSetTitle;
        }

        public boolean isChannelSubscriptionRequired() {
            return channelSubscriptionRequired;
        }

        public void setChannelSubscriptionRequired(boolean channelSubscriptionRequired) {
            this.channelSubscriptionRequired = channelSubscriptionRequired;
        }

        public Long getRequiredChannelId() {
            return requiredChannelId;
        }

        public void setRequiredChannelId(Long requiredChannelId) {
            this.requiredChannelId = requiredChannelId;
        }

        public String getRequiredChannelUsername() {
            return requiredChannelUsername;
        }

        public void setRequiredChannelUsername(String requiredChannelUsername) {
            this.requiredChannelUsername = requiredChannelUsername;
        }

        public String getRequiredChannelUrl() {
            return requiredChannelUrl;
        }

        public void setRequiredChannelUrl(String requiredChannelUrl) {
            this.requiredChannelUrl = requiredChannelUrl;
        }

        public boolean isNativePaymentEnabled() {
            return nativePaymentEnabled;
        }

        public void setNativePaymentEnabled(boolean nativePaymentEnabled) {
            this.nativePaymentEnabled = nativePaymentEnabled;
        }

        public boolean isNativeMessagingEnabled() {
            return nativeMessagingEnabled;
        }

        public void setNativeMessagingEnabled(boolean nativeMessagingEnabled) {
            this.nativeMessagingEnabled = nativeMessagingEnabled;
        }

        public String getWebhookOwner() {
            return webhookOwner;
        }

        public void setWebhookOwner(String webhookOwner) {
            this.webhookOwner = webhookOwner;
        }

        public String getWebhookSecretToken() {
            return webhookSecretToken;
        }

        public void setWebhookSecretToken(String webhookSecretToken) {
            this.webhookSecretToken = webhookSecretToken;
        }

        public String getWebhookUrl() {
            return webhookUrl;
        }

        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }

        public boolean isWebhookAutoRegisterOnStartup() {
            return webhookAutoRegisterOnStartup;
        }

        public void setWebhookAutoRegisterOnStartup(boolean webhookAutoRegisterOnStartup) {
            this.webhookAutoRegisterOnStartup = webhookAutoRegisterOnStartup;
        }

        public Long getSupportChatId() {
            return supportChatId;
        }

        public void setSupportChatId(Long supportChatId) {
            this.supportChatId = supportChatId;
        }

        public boolean isSupportEnabled() {
            return supportEnabled;
        }

        public void setSupportEnabled(boolean supportEnabled) {
            this.supportEnabled = supportEnabled;
        }

        public boolean isSupportUseTopics() {
            return supportUseTopics;
        }

        public void setSupportUseTopics(boolean supportUseTopics) {
            this.supportUseTopics = supportUseTopics;
        }
    }

    /**
     * Настройки безопасности webhook-колбэков от StickerBot → Java.
     */
    public static class Webhook {
        /** Секрет для HMAC-SHA256 проверки X-Webhook-Signature */
        private String hmacSecret;

        /** true = отклонять запросы без валидной HMAC подписи */
        private boolean hmacEnforced = false;

        public String getHmacSecret() {
            return hmacSecret;
        }

        public void setHmacSecret(String hmacSecret) {
            this.hmacSecret = hmacSecret;
        }

        public boolean isHmacEnforced() {
            return hmacEnforced;
        }

        public void setHmacEnforced(boolean hmacEnforced) {
            this.hmacEnforced = hmacEnforced;
        }
    }

    public static class TonPay {
        private String adapterUrl;
        private String adapterToken;
        private String chain = "testnet";
        private String merchantWalletAddress;
        private String webhookSecret;
        private boolean webhookSignatureEnforced = true;

        public String getAdapterUrl() {
            return adapterUrl;
        }

        public void setAdapterUrl(String adapterUrl) {
            this.adapterUrl = adapterUrl;
        }

        public String getAdapterToken() {
            return adapterToken;
        }

        public void setAdapterToken(String adapterToken) {
            this.adapterToken = adapterToken;
        }

        public String getChain() {
            return chain;
        }

        public void setChain(String chain) {
            this.chain = chain;
        }

        public String getMerchantWalletAddress() {
            return merchantWalletAddress;
        }

        public void setMerchantWalletAddress(String merchantWalletAddress) {
            this.merchantWalletAddress = merchantWalletAddress;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }

        public boolean isWebhookSignatureEnforced() {
            return webhookSignatureEnforced;
        }

        public void setWebhookSignatureEnforced(boolean webhookSignatureEnforced) {
            this.webhookSignatureEnforced = webhookSignatureEnforced;
        }
    }
    
    public static class Referral {
        private int dailyReferrerRewardCap = 50;
        private int attributionWindowDays = 7;
        private int codeLength = 12;
        
        public int getDailyReferrerRewardCap() {
            return dailyReferrerRewardCap;
        }
        
        public void setDailyReferrerRewardCap(int dailyReferrerRewardCap) {
            this.dailyReferrerRewardCap = dailyReferrerRewardCap;
        }
        
        public int getAttributionWindowDays() {
            return attributionWindowDays;
        }
        
        public void setAttributionWindowDays(int attributionWindowDays) {
            this.attributionWindowDays = attributionWindowDays;
        }
        
        public int getCodeLength() {
            return codeLength;
        }
        
        public void setCodeLength(int codeLength) {
            this.codeLength = codeLength;
        }
    }
    
    public static class StickerBot {
        private String apiUrl;
        private String serviceToken;
        private Retry retry = new Retry();
        
        public String getApiUrl() {
            return apiUrl;
        }
        
        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }
        
        public String getServiceToken() {
            return serviceToken;
        }
        
        public void setServiceToken(String serviceToken) {
            this.serviceToken = serviceToken;
        }

        public Retry getRetry() {
            return retry;
        }

        public void setRetry(Retry retry) {
            this.retry = retry;
        }
    }

    public static class Retry {
        private int maxAttempts = 3;
        private long initialDelayMs = 300L;
        private double multiplier = 3.0d;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getInitialDelayMs() {
            return initialDelayMs;
        }

        public void setInitialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }
    }
}
