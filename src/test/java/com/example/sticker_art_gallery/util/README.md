# Telegram InitData Generator

Утилита для генерации валидной Telegram Web App initData для использования в автотестах.

## Зачем это нужно?

При тестировании API, которое использует Telegram Web App авторизацию, нужно передавать валидную `initData` с правильной HMAC-SHA256 подписью. Вручную генерировать такие данные сложно и неудобно. Этот генератор автоматически создает корректную `initData`, которая проходит валидацию `TelegramInitDataValidator`.

## Быстрый старт

### Минимальный пример

```java
String initData = TelegramInitDataGenerator.builder()
    .botToken("123456789:ABCdefGHIjklMNOpqrsTUVwxyz")
    .userId(123456789L)
    .build();
```

### Полный пример

```java
String initData = TelegramInitDataGenerator.builder()
    .botToken("your_bot_token")
    .userId(987654321L)
    .username("testuser")
    .firstName("Андрей")
    .lastName("Тестов")
    .languageCode("ru")
    .queryId("AAHdF6IQAAAAAN0XohDhrOrc")
    .additionalParam("chat_type", "sender")
    .build();
```

## Использование в API тестах

### RestTemplate/TestRestTemplate

```java
@Test
void testGetMyProfile() throws Exception {
    // Генерируем валидную initData
    String initData = TelegramInitDataGenerator.builder()
        .botToken(botToken)
        .userId(123456789L)
        .username("testuser")
        .build();
    
    // Настраиваем заголовки
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Telegram-Init-Data", initData);
    
    HttpEntity<Void> request = new HttpEntity<>(headers);
    
    // Выполняем запрос
    ResponseEntity<UserProfileDto> response = restTemplate.exchange(
        "/api/profiles/me",
        HttpMethod.GET,
        request,
        UserProfileDto.class
    );
    
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
}
```

### MockMvc

```java
@Test
void testCreateStickerSet() throws Exception {
    String initData = TelegramInitDataGenerator.builder()
        .botToken(botToken)
        .userId(123456789L)
        .build();
    
    mockMvc.perform(post("/api/stickersets")
            .header("X-Telegram-Init-Data", initData)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"test_stickers\"}"))
        .andExpect(status().isOk());
}
```

### WebClient

```java
@Test
void testAsyncRequest() throws Exception {
    String initData = TelegramInitDataGenerator.builder()
        .botToken(botToken)
        .userId(123456789L)
        .build();
    
    StickerSetDto result = webClient.get()
        .uri("/api/stickersets/1")
        .header("X-Telegram-Init-Data", initData)
        .retrieve()
        .bodyToMono(StickerSetDto.class)
        .block();
    
    assertNotNull(result);
}
```

## Параметры

### Обязательные

- **botToken** - токен бота (должен совпадать с токеном в конфигурации)
- **userId** - Telegram ID пользователя

### Опциональные

- **username** - username пользователя (без @)
- **firstName** - имя пользователя
- **lastName** - фамилия пользователя
- **languageCode** - код языка (по умолчанию "ru")
- **authDate** - Unix timestamp авторизации (по умолчанию текущее время)
- **queryId** - ID запроса от Telegram
- **additionalParam(key, value)** - дополнительные параметры

## Тестирование разных ролей

### Админ

```java
String adminInitData = TelegramInitDataGenerator.builder()
    .botToken(botToken)
    .userId(111111111L)  // ID админа из базы
    .username("admin")
    .build();
```

### Обычный пользователь

```java
String userInitData = TelegramInitDataGenerator.builder()
    .botToken(botToken)
    .userId(222222222L)
    .username("regular_user")
    .build();
```

## Тестирование истечения срока

```java
// Устаревшая initData (более 24 часов)
long expiredAuthDate = Instant.now().getEpochSecond() - 86401;

String expiredInitData = TelegramInitDataGenerator.builder()
    .botToken(botToken)
    .userId(123456789L)
    .authDate(expiredAuthDate)
    .build();

// Эта initData НЕ пройдет валидацию
assertFalse(validator.validateInitData(expiredInitData, "StickerGallery"));
```

## Как это работает?

Генератор реализует алгоритм валидации Telegram Web Apps:

1. **Создание данных пользователя** - формируется JSON с информацией о пользователе
2. **Формирование dataCheckString** - параметры сортируются лексикографически и объединяются через `\n`
3. **Вычисление секретного ключа** - `HMAC-SHA256("WebAppData", botToken)`
4. **Вычисление подписи** - `HMAC-SHA256(dataCheckString, secretKey)` → hex
5. **Сборка initData** - все параметры + hash объединяются через `&` с URL-кодированием

Подробнее: [Telegram Web Apps Documentation](https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app)

## Best Practices

### Создание тестовых пользователей

```java
@BeforeEach
void setUp() throws Exception {
    // Админ
    adminInitData = TelegramInitDataGenerator.builder()
        .botToken(botToken)
        .userId(ADMIN_ID)
        .username("admin")
        .build();
    
    // Обычный пользователь
    userInitData = TelegramInitDataGenerator.builder()
        .botToken(botToken)
        .userId(USER_ID)
        .username("user")
        .build();
    
    // Гость (для создания профиля)
    guestInitData = TelegramInitDataGenerator.builder()
        .botToken(botToken)
        .userId(GUEST_ID)
        .username("guest")
        .build();
}
```

### Константы в тестах

```java
public class TestConstants {
    public static final String BOT_TOKEN = "test_bot_token";
    
    public static final Long ADMIN_USER_ID = 111111111L;
    public static final Long REGULAR_USER_ID = 222222222L;
    public static final Long GUEST_USER_ID = 333333333L;
    
    public static String createInitData(Long userId) throws Exception {
        return TelegramInitDataGenerator.builder()
            .botToken(BOT_TOKEN)
            .userId(userId)
            .build();
    }
}
```

### Извлечение ID из initData

```java
String initData = TelegramInitDataGenerator.builder()
    .botToken(botToken)
    .userId(123456789L)
    .build();

// Извлекаем telegram_id для использования в тестах
Long telegramId = validator.extractTelegramId(initData);
assertEquals(123456789L, telegramId);
```

## Связанные классы

- **TelegramInitDataValidator** - валидатор initData (production)
- **TelegramAuthenticationFilter** - фильтр аутентификации
- **TelegramAuthenticationProvider** - провайдер аутентификации

## Примеры тестов

Полные примеры использования можно найти в:
- `TelegramInitDataGeneratorTest.java` - unit-тесты самого генератора
- `UserProfileControllerTest.java` - integration-тесты API с авторизацией

## Troubleshooting

### Ошибка: "Bot token is required"

Не забудьте указать `botToken()` - это обязательный параметр.

### Ошибка: "User ID is required"

Не забудьте указать `userId()` - это обязательный параметр.

### InitData не проходит валидацию

1. Убедитесь, что используете тот же `botToken`, что настроен в `application-test.yaml`
2. Проверьте, что `authDate` не старше 24 часов
3. Убедитесь, что `botName` в заголовке совпадает с конфигурацией

### Спецсимволы в имени пользователя

Генератор автоматически экранирует спецсимволы в JSON (`"`, `\`, `\n`, и т.д.):

```java
String initData = TelegramInitDataGenerator.builder()
    .botToken(botToken)
    .userId(123L)
    .firstName("Test\"User")  // Будет корректно экранировано
    .build();
```

