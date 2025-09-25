package com.example.sticker_art_gallery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO для Telegram Web App initData
 */
public class TelegramInitData {
    
    @JsonProperty("query_id")
    private String queryId;
    
    @JsonProperty("user")
    private TelegramUser user;
    
    @JsonProperty("receiver")
    private TelegramUser receiver;
    
    @JsonProperty("chat")
    private TelegramChat chat;
    
    @JsonProperty("chat_type")
    private String chatType;
    
    @JsonProperty("chat_instance")
    private String chatInstance;
    
    @JsonProperty("start_param")
    private String startParam;
    
    @JsonProperty("can_send_after")
    private Long canSendAfter;
    
    @JsonProperty("auth_date")
    private Long authDate;
    
    @JsonProperty("hash")
    private String hash;
    
    // Конструкторы
    public TelegramInitData() {}
    
    public TelegramInitData(String queryId, TelegramUser user, TelegramUser receiver, TelegramChat chat, 
                           String chatType, String chatInstance, String startParam, Long canSendAfter, 
                           Long authDate, String hash) {
        this.queryId = queryId;
        this.user = user;
        this.receiver = receiver;
        this.chat = chat;
        this.chatType = chatType;
        this.chatInstance = chatInstance;
        this.startParam = startParam;
        this.canSendAfter = canSendAfter;
        this.authDate = authDate;
        this.hash = hash;
    }
    
    // Геттеры и сеттеры
    public String getQueryId() { return queryId; }
    public void setQueryId(String queryId) { this.queryId = queryId; }
    
    public TelegramUser getUser() { return user; }
    public void setUser(TelegramUser user) { this.user = user; }
    
    public TelegramUser getReceiver() { return receiver; }
    public void setReceiver(TelegramUser receiver) { this.receiver = receiver; }
    
    public TelegramChat getChat() { return chat; }
    public void setChat(TelegramChat chat) { this.chat = chat; }
    
    public String getChatType() { return chatType; }
    public void setChatType(String chatType) { this.chatType = chatType; }
    
    public String getChatInstance() { return chatInstance; }
    public void setChatInstance(String chatInstance) { this.chatInstance = chatInstance; }
    
    public String getStartParam() { return startParam; }
    public void setStartParam(String startParam) { this.startParam = startParam; }
    
    public Long getCanSendAfter() { return canSendAfter; }
    public void setCanSendAfter(Long canSendAfter) { this.canSendAfter = canSendAfter; }
    
    public Long getAuthDate() { return authDate; }
    public void setAuthDate(Long authDate) { this.authDate = authDate; }
    
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
    
    /**
     * Telegram User данные
     */
    public static class TelegramUser {
        @JsonProperty("id")
        private Long id;
        
        @JsonProperty("is_bot")
        private Boolean isBot;
        
        @JsonProperty("first_name")
        private String firstName;
        
        @JsonProperty("last_name")
        private String lastName;
        
        @JsonProperty("username")
        private String username;
        
        @JsonProperty("language_code")
        private String languageCode;
        
        @JsonProperty("is_premium")
        private Boolean isPremium;
        
        @JsonProperty("added_to_attachment_menu")
        private Boolean addedToAttachmentMenu;
        
        @JsonProperty("allows_write_to_pm")
        private Boolean allowsWriteToPm;
        
        @JsonProperty("photo_url")
        private String photoUrl;
        
        // Конструкторы
        public TelegramUser() {}
        
        public TelegramUser(Long id, Boolean isBot, String firstName, String lastName, String username, 
                           String languageCode, Boolean isPremium, Boolean addedToAttachmentMenu, 
                           Boolean allowsWriteToPm, String photoUrl) {
            this.id = id;
            this.isBot = isBot;
            this.firstName = firstName;
            this.lastName = lastName;
            this.username = username;
            this.languageCode = languageCode;
            this.isPremium = isPremium;
            this.addedToAttachmentMenu = addedToAttachmentMenu;
            this.allowsWriteToPm = allowsWriteToPm;
            this.photoUrl = photoUrl;
        }
        
        // Геттеры и сеттеры
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public Boolean getIsBot() { return isBot; }
        public void setIsBot(Boolean isBot) { this.isBot = isBot; }
        
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getLanguageCode() { return languageCode; }
        public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
        
        public Boolean getIsPremium() { return isPremium; }
        public void setIsPremium(Boolean isPremium) { this.isPremium = isPremium; }
        
        public Boolean getAddedToAttachmentMenu() { return addedToAttachmentMenu; }
        public void setAddedToAttachmentMenu(Boolean addedToAttachmentMenu) { this.addedToAttachmentMenu = addedToAttachmentMenu; }
        
        public Boolean getAllowsWriteToPm() { return allowsWriteToPm; }
        public void setAllowsWriteToPm(Boolean allowsWriteToPm) { this.allowsWriteToPm = allowsWriteToPm; }
        
        public String getPhotoUrl() { return photoUrl; }
        public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    }
    
    /**
     * Telegram Chat данные
     */
    public static class TelegramChat {
        @JsonProperty("id")
        private Long id;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("username")
        private String username;
        
        @JsonProperty("first_name")
        private String firstName;
        
        @JsonProperty("last_name")
        private String lastName;
        
        @JsonProperty("photo")
        private TelegramChatPhoto photo;
        
        // Конструкторы
        public TelegramChat() {}
        
        public TelegramChat(Long id, String type, String title, String username, String firstName, 
                           String lastName, TelegramChatPhoto photo) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
            this.photo = photo;
        }
        
        // Геттеры и сеттеры
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public TelegramChatPhoto getPhoto() { return photo; }
        public void setPhoto(TelegramChatPhoto photo) { this.photo = photo; }
    }
    
    /**
     * Telegram Chat Photo данные
     */
    public static class TelegramChatPhoto {
        @JsonProperty("small_file_id")
        private String smallFileId;
        
        @JsonProperty("small_file_unique_id")
        private String smallFileUniqueId;
        
        @JsonProperty("big_file_id")
        private String bigFileId;
        
        @JsonProperty("big_file_unique_id")
        private String bigFileUniqueId;
        
        // Конструкторы
        public TelegramChatPhoto() {}
        
        public TelegramChatPhoto(String smallFileId, String smallFileUniqueId, String bigFileId, String bigFileUniqueId) {
            this.smallFileId = smallFileId;
            this.smallFileUniqueId = smallFileUniqueId;
            this.bigFileId = bigFileId;
            this.bigFileUniqueId = bigFileUniqueId;
        }
        
        // Геттеры и сеттеры
        public String getSmallFileId() { return smallFileId; }
        public void setSmallFileId(String smallFileId) { this.smallFileId = smallFileId; }
        
        public String getSmallFileUniqueId() { return smallFileUniqueId; }
        public void setSmallFileUniqueId(String smallFileUniqueId) { this.smallFileUniqueId = smallFileUniqueId; }
        
        public String getBigFileId() { return bigFileId; }
        public void setBigFileId(String bigFileId) { this.bigFileId = bigFileId; }
        
        public String getBigFileUniqueId() { return bigFileUniqueId; }
        public void setBigFileUniqueId(String bigFileUniqueId) { this.bigFileUniqueId = bigFileUniqueId; }
    }
}
