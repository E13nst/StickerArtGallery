package com.example.dream_stream_bot.security;

import com.example.dream_stream_bot.model.user.UserEntity;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

/**
 * Кастомный токен аутентификации для Telegram
 */
public class TelegramAuthenticationToken extends AbstractAuthenticationToken {
    
    private final String initData;
    private final Long telegramId;
    private final String botName;
    private UserEntity user;
    private boolean authenticated = false;
    
    /**
     * Конструктор для неаутентифицированного токена
     */
    public TelegramAuthenticationToken(String initData, Long telegramId, String botName) {
        super(Collections.emptyList());
        this.initData = initData;
        this.telegramId = telegramId;
        this.botName = botName;
        this.authenticated = false;
    }
    
    /**
     * Конструктор для аутентифицированного токена
     */
    public TelegramAuthenticationToken(UserEntity user, String initData, Long telegramId, 
                                      String botName, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.user = user;
        this.initData = initData;
        this.telegramId = telegramId;
        this.botName = botName;
        this.authenticated = true;
    }
    
    @Override
    public Object getCredentials() {
        return initData;
    }
    
    @Override
    public Object getPrincipal() {
        return user != null ? user : telegramId;
    }
    
    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (isAuthenticated) {
            throw new IllegalArgumentException(
                    "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        }
        super.setAuthenticated(false);
    }
    
    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    public String getInitData() {
        return initData;
    }
    
    public Long getTelegramId() {
        return telegramId;
    }
    
    public String getBotName() {
        return botName;
    }
    
    public UserEntity getUser() {
        return user;
    }
    
    public void setUser(UserEntity user) {
        this.user = user;
    }
    
    /**
     * Создает GrantedAuthority на основе роли пользователя
     */
    public static Collection<GrantedAuthority> createAuthorities(UserEntity user) {
        if (user == null || user.getRole() == null) {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }
        
        String roleName = "ROLE_" + user.getRole().name();
        return Collections.singletonList(new SimpleGrantedAuthority(roleName));
    }
}
