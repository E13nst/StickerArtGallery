package com.example.sticker_art_gallery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    private String url;
    private MiniApp miniApp = new MiniApp();
    private Telegram telegram = new Telegram();
    private Internal internal = new Internal();
    private Referral referral = new Referral();
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
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

    public Internal getInternal() {
        return internal;
    }

    public void setInternal(Internal internal) {
        this.internal = internal;
    }
    
    public Referral getReferral() {
        return referral;
    }
    
    public void setReferral(Referral referral) {
        this.referral = referral;
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
    }

    public static class Internal {
        private ServiceTokens serviceTokens = new ServiceTokens();

        public ServiceTokens getServiceTokens() {
            return serviceTokens;
        }

        public void setServiceTokens(ServiceTokens serviceTokens) {
            this.serviceTokens = serviceTokens;
        }
    }

    public static class ServiceTokens {
        private String stickerBot;

        public String getStickerBot() {
            return stickerBot;
        }

        public void setStickerBot(String stickerBot) {
            this.stickerBot = stickerBot;
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
}
