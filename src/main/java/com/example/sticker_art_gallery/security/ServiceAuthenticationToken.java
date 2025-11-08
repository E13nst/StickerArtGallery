package com.example.sticker_art_gallery.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

/**
 * Токен аутентификации для межсервисных запросов.
 */
public class ServiceAuthenticationToken extends AbstractAuthenticationToken {

    private final String serviceName;

    public ServiceAuthenticationToken(String serviceName) {
        super(defaultAuthorities());
        this.serviceName = serviceName;
        super.setAuthenticated(true);
    }

    private ServiceAuthenticationToken(String serviceName, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.serviceName = serviceName;
        super.setAuthenticated(true);
    }

    private static Collection<? extends GrantedAuthority> defaultAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_INTERNAL"));
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (!isAuthenticated) {
            super.setAuthenticated(false);
        } else {
            throw new IllegalArgumentException("Use constructor to mark token as authenticated");
        }
    }

    public static ServiceAuthenticationToken withAuthorities(String serviceName, Collection<? extends GrantedAuthority> authorities) {
        return new ServiceAuthenticationToken(serviceName, authorities);
    }
}

