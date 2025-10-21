# Allure Test Reports - Руководство

Проект использует Allure Framework для генерации красивых и информативных отчетов по автотестам.

## 🚀 Быстрый старт

### Запустить тесты и посмотреть отчет

```bash
# Вариант 1: Через Make (рекомендуется)
make test-allure-serve

# Вариант 2: Через Allure CLI (если установлен)
./gradlew test
allure serve build/allure-results

# Вариант 3: Через Gradle
./gradlew clean test
./gradlew allureReport
# Затем открыть build/reports/allure-report/allureReport/index.html через HTTP сервер
```

## 📦 Установка Allure

### macOS
```bash
brew install allure
```

### Linux
```bash
sudo apt-add-repository ppa:qameta/allure
sudo apt-get update 
sudo apt-get install allure
```

### Windows
```bash
scoop install allure
```

Или скачать с [официального сайта](https://github.com/allure-framework/allure2/releases).

## 📊 Структура отчетов

Allure генерирует следующие разделы:

### 1. Overview (Обзор)
- Статистика тестов (пройдено/упало/пропущено)
- Trend chart (график изменений)
- Environment info

### 2. Categories (Категории)
- Product defects
- Test defects
- Other

### 3. Suites (Наборы тестов)
- По тестовым классам
- Древовидная структура

### 4. Graphs (Графики)
- Status chart
- Severity chart
- Duration chart

### 5. Timeline (Временная шкала)
- Последовательность выполнения тестов
- Время выполнения каждого теста

### 6. Behaviors (Поведение)
- Группировка по Epic → Feature → Story

## 🏷️ Аннотации Allure

В проекте используются следующие аннотации:

### На уровне класса

```java
@Epic("Название эпика")
@Feature("Название фичи")
@DisplayName("Человекочитаемое название")
class MyTest {
    // ...
}
```

### На уровне метода

```java
@Test
@Story("Название истории")
@DisplayName("Описание теста")
@Description("Детальное описание того, что тест проверяет")
@Severity(SeverityLevel.CRITICAL)
void testMethod() {
    // ...
}
```

### Уровни критичности (Severity)

- `BLOCKER` - блокирующие тесты (основной функционал)
- `CRITICAL` - критические тесты (важный функционал)
- `NORMAL` - обычные тесты (стандартный функционал)
- `MINOR` - минорные тесты (вспомогательный функционал)
- `TRIVIAL` - тривиальные тесты (примеры, документация)

### Шаги (@Step)

```java
@Step("Создание пользователя с ID {userId}")
public User createUser(Long userId, String username) {
    // ...
}
```

### Вложения (Attachments)

```java
@Attachment(value = "Response JSON", type = "application/json")
public String attachJson(String json) {
    return json;
}
```

## 📁 Структура Epic → Feature → Story

### Epic: Высокоуровневые модули
- "API для стикерсетов"
- "Тестовые утилиты"
- "Бизнес-логика стикерсетов"

### Feature: Функциональность модуля
- "Создание и управление стикерсетами"
- "Генератор Telegram initData"
- "Сервис управления стикерсетами"

### Story: Конкретные сценарии
- "Создание стикерсета"
- "Валидация параметров"
- "Генерация валидной initData"

## 🎨 Пример оформления теста

```java
@Epic("API для стикерсетов")
@Feature("Создание и управление стикерсетами")
@DisplayName("Интеграционные тесты StickerSetController")
class StickerSetControllerTest {
    
    @Test
    @Story("Создание стикерсета")
    @DisplayName("POST /api/stickersets с валидными данными возвращает 201")
    @Description("""
        Проверяет создание нового стикерсета:
        1. Отправляем POST запрос с валидными данными
        2. Ожидаем HTTP 201 Created
        3. Проверяем наличие всех обязательных полей в ответе
        4. Проверяем корректность значений
        """)
    @Severity(SeverityLevel.BLOCKER)
    void createStickerSet_WithValidData_ShouldReturn201() {
        // Given
        CreateStickerSetDto request = new CreateStickerSetDto();
        request.setName("test_stickers");
        
        // When
        ResponseEntity<StickerSetDto> response = restTemplate.postForEntity(
            "/api/stickersets", 
            request, 
            StickerSetDto.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isNotNull();
    }
}
```

## 📈 Просмотр отчетов

### Вариант 1: Встроенный сервер (рекомендуется)

```bash
./gradlew allureServe
```

Автоматически откроет браузер с отчетом.

### Вариант 2: Статические файлы

```bash
# Сгенерировать HTML отчет
./gradlew allureReport

# Отчет будет в build/reports/allure-report/index.html
open build/reports/allure-report/index.html
```

### Вариант 3: CI/CD

В CI/CD системах (GitHub Actions, GitLab CI) можно публиковать отчеты как артефакты:

```yaml
- name: Generate Allure Report
  run: ./gradlew allureReport
  
- name: Upload Allure Report
  uses: actions/upload-artifact@v3
  with:
    name: allure-report
    path: build/reports/allure-report/
```

## 🔧 Полезные команды

```bash
# Запустить конкретный тестовый класс
./gradlew test --tests TelegramInitDataGeneratorTest

# Запустить тесты с тегом/категорией
./gradlew test --tests "*IntegrationTest"

# Очистить результаты предыдущих тестов
./gradlew clean

# Полный цикл: очистка → тесты → отчет → просмотр
./gradlew clean test allureReport allureServe
```

## 📊 История выполнения тестов

Allure сохраняет историю выполнения тестов. Для корректной работы истории:

1. После каждого запуска тестов сохраняйте `allure-results`:
```bash
cp -r build/allure-results build/allure-history
```

2. При следующем запуске Allure покажет тренды

## 🐛 Troubleshooting

### Отчет показывает "Loading..." везде

**Проблема:** Браузер блокирует загрузку данных из-за CORS политики при открытии HTML файлов через `file://`.

**Решение:** Используйте `allure serve` вместо прямого открытия HTML файла:

```bash
# Правильно ✅
make allure-serve

# Или
allure serve build/allure-results

# Неправильно ❌ (не работает в современных браузерах)
open build/reports/allure-report/allureReport/index.html
```

### Отчет не генерируется

```bash
# Проверьте, что Allure установлен
allure --version

# Переустановите зависимости
./gradlew clean build --refresh-dependencies
```

### Не все тесты попадают в отчет

Убедитесь, что тесты:
1. Используют JUnit 5 (`@Test` из `org.junit.jupiter.api`)
2. Имеют аннотации Allure
3. Успешно выполнились (проверьте логи)

### Кириллица в отчете отображается неправильно

Убедитесь, что:
```bash
# В gradle.properties
org.gradle.jvmargs=-Dfile.encoding=UTF-8

# В build.gradle
tasks.withType(JavaCompile) {
    options.encoding = 'UTF-8'
}
```

## 📚 Дополнительные ресурсы

- [Официальная документация Allure](https://docs.qameta.io/allure/)
- [Allure JUnit 5 Integration](https://docs.qameta.io/allure/#_junit_5)
- [Best Practices](https://docs.qameta.io/allure/#_best_practices)

## 🎯 Best Practices

### 1. Информативные названия

❌ Плохо:
```java
@Test
void test1() { /* ... */ }
```

✅ Хорошо:
```java
@Test
@DisplayName("Создание стикерсета с валидным именем возвращает 201")
void createStickerSet_WithValidName_Returns201() { /* ... */ }
```

### 2. Используйте Description для деталей

```java
@Description("""
    Тест проверяет:
    1. Валидацию входных данных
    2. Создание записи в БД
    3. Отправку уведомления пользователю
    """)
```

### 3. Правильная критичность

- BLOCKER/CRITICAL - для основного функционала
- NORMAL - для обычных сценариев
- MINOR/TRIVIAL - для вспомогательных проверок

### 4. Группировка по Epic/Feature/Story

Создайте логическую иерархию, чтобы в отчете было легко найти нужный тест.

### 5. Используйте @Step для сложных тестов

```java
@Step("Регистрация нового пользователя")
private User registerUser(String username) {
    // ...
}
```

## 📝 Шаблоны для новых тестов

### Unit тест

```java
@Epic("Модуль X")
@Feature("Компонент Y")
@DisplayName("Unit тесты для MyService")
class MyServiceTest {
    
    @Test
    @Story("Операция Z")
    @DisplayName("Метод должен вернуть корректный результат")
    @Description("Проверяет корректность вычислений при стандартных входных данных")
    @Severity(SeverityLevel.NORMAL)
    void methodName_WithValidInput_ReturnsExpectedResult() {
        // Arrange
        // Act
        // Assert
    }
}
```

### Integration тест

```java
@Epic("API X")
@Feature("Эндпоинт Y")
@DisplayName("Интеграционные тесты для MyController")
@SpringBootTest
@AutoConfigureMockMvc
class MyControllerIntegrationTest {
    
    @Test
    @Story("CRUD операции")
    @DisplayName("POST /api/resource с валидными данными возвращает 201")
    @Description("""
        Шаги теста:
        1. Подготовка тестовых данных
        2. Отправка POST запроса
        3. Проверка статуса ответа
        4. Валидация тела ответа
        """)
    @Severity(SeverityLevel.BLOCKER)
    void createResource_WithValidData_Returns201() {
        // Given, When, Then
    }
}
```

---

**Дата последнего обновления:** 21.10.2025  
**Версия Allure:** 2.25.0

