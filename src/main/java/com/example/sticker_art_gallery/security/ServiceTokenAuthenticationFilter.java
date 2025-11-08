package com.example.sticker_art_gallery.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Фильтр для аутентификации внутренних сервисов по токену.
 * Ожидает заголовок X-Service-Token для всех запросов к /internal/**
 */
@Component
public class ServiceTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTokenAuthenticationFilter.class);
    private static final String SERVICE_TOKEN_HEADER = "X-Service-Token";
    private static final String INTERNAL_PATH_PREFIX = "/internal/";

    private final ServiceTokenService serviceTokenService;

    public ServiceTokenAuthenticationFilter(ServiceTokenService serviceTokenService) {
        this.serviceTokenService = serviceTokenService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!shouldFilterPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader(SERVICE_TOKEN_HEADER);
        if (token == null || token.trim().isEmpty()) {
            LOGGER.warn("❌ Межсервисный запрос без токена: {}", path);
            writeUnauthorized(response, "Missing service token");
            return;
        }

        Optional<String> serviceName = serviceTokenService.authenticate(token);
        if (serviceName.isEmpty()) {
            LOGGER.warn("❌ Межсервисный запрос с неверным токеном: {}", path);
            writeUnauthorized(response, "Invalid service token");
            return;
        }

        ServiceAuthenticationToken authentication = new ServiceAuthenticationToken(serviceName.get());
        authentication.setDetails(request);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        LOGGER.debug("✅ Межсервисная аутентификация успешна: сервис {} для {}", serviceName.get(), path);

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !shouldFilterPath(request.getRequestURI());
    }

    private boolean shouldFilterPath(String path) {
        return path != null && path.startsWith(INTERNAL_PATH_PREFIX);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }
}

