package com.example.sticker_art_gallery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    private String url;
    private MiniApp miniApp = new MiniApp();
    private Telegram telegram = new Telegram();
    
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
        
        public String getBotToken() {
            return botToken;
        }
        
        public void setBotToken(String botToken) {
            this.botToken = botToken;
        }
    }
}
