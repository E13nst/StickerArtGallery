package com.example.sticker_art_gallery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    private String url;
    private MiniApp miniApp = new MiniApp();
    private Telegram telegram = new Telegram();
    private Referral referral = new Referral();
    private StickerBot stickerbot = new StickerBot();
    
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
    }
}
