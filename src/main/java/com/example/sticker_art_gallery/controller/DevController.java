package com.example.sticker_art_gallery.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для разработки (только в dev профиле)
 * Предоставляет тестовые данные для Swagger UI
 */
@RestController
@RequestMapping("/dev")
@Profile("dev")
@Tag(name = "Разработка", description = "Тестовые эндпоинты для разработки (только в dev профиле)")
public class DevController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DevController.class);
    
    @Value("${telegram.test.init-data:}")
    private String testInitData;
    
    @Value("${telegram.test.bot-name:}")
    private String testBotName;
    
    /**
     * Получить тестовый initData для Swagger UI
     */
    @GetMapping("/test-init-data")
    @Operation(
        summary = "Получить тестовый initData",
        description = "Возвращает тестовый initData для использования в Swagger UI (только в dev профиле)"
    )
    @ApiResponse(responseCode = "200", description = "Тестовый initData получен",
        content = @Content(schema = @Schema(implementation = Map.class),
            examples = @ExampleObject(value = """
                {
                    "initData": "query_id=AAHdF6IQAAAAAN0XohDhrOrc&user=%7B%22id%22%3A123456789%7D&auth_date=1640995200&hash=test_hash_for_development_only",
                    "botName": "StickerGallery",
                    "message": "Используйте этот initData в Swagger UI для тестирования API"
                }
                """)))
    public ResponseEntity<Map<String, Object>> getTestInitData() {
        LOGGER.info("🔧 Запрос тестового initData для разработки");
        
        Map<String, Object> response = new HashMap<>();
        response.put("initData", testInitData);
        response.put("botName", testBotName);
        response.put("message", "Используйте этот initData в Swagger UI для тестирования API");
        response.put("warning", "ВНИМАНИЕ: Используйте только для разработки!");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Диагностический эндпоинт для проверки заголовков
     */
    @GetMapping("/debug-headers")
    @Operation(
        summary = "Диагностика заголовков",
        description = "Возвращает все заголовки запроса для диагностики"
    )
    @ApiResponse(responseCode = "200", description = "Заголовки получены")
    public ResponseEntity<Map<String, Object>> debugHeaders(jakarta.servlet.http.HttpServletRequest request) {
        LOGGER.info("🔧 Запрос диагностики заголовков");
        
        Map<String, Object> response = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        
        // Получаем все заголовки
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.put(headerName, headerValue);
        }
        
        response.put("headers", headers);
        response.put("requestURI", request.getRequestURI());
        response.put("method", request.getMethod());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Получить информацию о dev профиле
     */
    @GetMapping("/info")
    @Operation(
        summary = "Информация о dev профиле",
        description = "Возвращает информацию о настройках dev профиля"
    )
    @ApiResponse(responseCode = "200", description = "Информация получена")
    public ResponseEntity<Map<String, Object>> getDevInfo() {
        LOGGER.info("🔧 Запрос информации о dev профиле");
        
        Map<String, Object> response = new HashMap<>();
        response.put("profile", "dev");
        response.put("testInitDataAvailable", testInitData != null && !testInitData.isEmpty());
        response.put("testBotName", testBotName);
        response.put("swaggerUrl", "/swagger-ui.html");
        response.put("openApiUrl", "/v3/api-docs");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Тестовый эндпоинт для мини-приложения
     */
    @GetMapping("/mini-app-test")
    @Operation(
        summary = "Тест мини-приложения",
        description = "Простой тест для проверки работы мини-приложения"
    )
    @ApiResponse(responseCode = "200", description = "Тест выполнен")
    public ResponseEntity<String> miniAppTest() {
        LOGGER.info("🔧 Тест мини-приложения");
        return ResponseEntity.ok("Мини-приложение работает!");
    }
    
    /**
     * Тестовая страница мини-приложения
     */
    @GetMapping("/mini-app-test-page")
    @Operation(
        summary = "Тестовая страница мини-приложения",
        description = "Возвращает HTML страницу для тестирования мини-приложения"
    )
    @ApiResponse(responseCode = "200", description = "Страница получена")
    public ResponseEntity<String> miniAppTestPage() {
        LOGGER.info("🔧 Запрос тестовой страницы мини-приложения");
        
        String html = """
            <!DOCTYPE html>
            <html lang="ru">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                <title>Sticker Gallery - Test</title>
                <script src="https://telegram.org/js/telegram-web-app.js"></script>
                <style>
                    body { font-family: Arial, sans-serif; padding: 16px; }
                    .container { max-width: 600px; margin: 0 auto; }
                    .header { text-align: center; margin-bottom: 24px; padding: 16px; background-color: #f8f9fa; border-radius: 12px; }
                    .status { background-color: #f8f9fa; padding: 16px; border-radius: 12px; margin-bottom: 16px; text-align: center; }
                    .btn { padding: 8px 16px; border: none; border-radius: 6px; font-size: 14px; cursor: pointer; margin: 5px; background-color: #2481cc; color: #ffffff; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎨 Галерея стикеров</h1>
                        <p>Тестовая версия мини-приложения</p>
                    </div>
                    <div class="status" id="status">Проверка инициализации...</div>
                    <div class="status" id="authStatus">Проверка авторизации...</div>
                    <div class="status" id="apiStatus">Проверка API...</div>
                    <div style="text-align: center; margin-top: 20px;">
                        <button class="btn" onclick="testAuth()">Тест авторизации</button>
                        <button class="btn" onclick="testAPI()">Тест API</button>
                    </div>
                </div>
                <script>
                    const tg = window.Telegram.WebApp;
                    if (tg) {
                        tg.expand(); tg.ready();
                        document.getElementById('status').innerHTML = '✅ Telegram Web App инициализирован';
                        const user = tg.initDataUnsafe?.user;
                        if (user) {
                            document.getElementById('authStatus').innerHTML = '✅ Пользователь: ' + user.first_name;
                        }
                    }
                    async function testAuth() {
                        try {
                            const response = await fetch('/api/auth/status', {
                                headers: { 'X-Telegram-Init-Data': tg.initData, 'X-Telegram-Bot-Name': 'StickerGallery' }
                            });
                            const data = await response.json();
                            document.getElementById('authStatus').innerHTML = 'Результат: ' + (data.authenticated ? '✅ Да' : '❌ Нет');
                        } catch (error) {
                            document.getElementById('authStatus').innerHTML = '❌ Ошибка: ' + error.message;
                        }
                    }
                    async function testAPI() {
                        try {
                            const response = await fetch('/api/stickersets', {
                                headers: { 'X-Telegram-Init-Data': tg.initData, 'X-Telegram-Bot-Name': 'StickerGallery' }
                            });
                            if (response.ok) {
                                const data = await response.json();
                                document.getElementById('apiStatus').innerHTML = '✅ API работает, стикерсетов: ' + data.length;
                            } else {
                                document.getElementById('apiStatus').innerHTML = '❌ Ошибка API: ' + response.status;
                            }
                        } catch (error) {
                            document.getElementById('apiStatus').innerHTML = '❌ Ошибка: ' + error.message;
                        }
                    }
                </script>
            </body>
            </html>
            """;
        
        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=utf-8")
                .body(html);
    }
}
