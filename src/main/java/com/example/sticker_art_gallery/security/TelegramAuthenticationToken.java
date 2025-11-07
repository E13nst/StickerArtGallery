package com.example.sticker_art_gallery.security;
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
    private AuthUserPrincipal principal;
    private boolean authenticated = false;
    
    /**
     * Конструктор для неаутентифицированного токена
     */
    public TelegramAuthenticationToken(String initData, Long telegramId) {
        super(Collections.emptyList());
        this.initData = initData;
        this.telegramId = telegramId;
        this.authenticated = false;
    }
    
    /**
     * Конструктор для аутентифицированного токена
     */
    public TelegramAuthenticationToken(AuthUserPrincipal principal, String initData, Long telegramId,
                                      Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.initData = initData;
        this.telegramId = telegramId;
        this.authenticated = true;
    }
    
    @Override
    public Object getCredentials() {
        return initData;
    }
    
    @Override
    public Object getPrincipal() {
        return principal != null ? principal : telegramId;
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
    
    public AuthUserPrincipal getAuthUser() { return principal; }
    public void setAuthUser(AuthUserPrincipal principal) { this.principal = principal; }
    
    /**
     * Создает GrantedAuthority на основе роли пользователя
     */
    public static Collection<GrantedAuthority> createAuthorities(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName));
    }
}
