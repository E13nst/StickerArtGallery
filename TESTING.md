# 🧪 Тестирование

Полное руководство по тестированию проекта Sticker Art Gallery.

## 📋 Быстрый старт

### Запуск тестов

```bash
# Unit тесты (по умолчанию) - быстро, без внешних зависимостей
make test              # или: ./gradlew test
# ✅ ~15 секунд, 144 теста

# Integration тесты - требуют БД и Redis
make test-integration  # или: ./gradlew integrationTest
# ⚠️ Требует: БД, Redis из .env.app
# ⏱️ ~30-60 секунд, 4 теста

# Benchmark тесты - только локально, требует запущенное приложение
make test-benchmark    # или: ./gradlew benchmarkTest
# ⚠️ Требует: запущенное приложение (make start)
# ⏱️ ~1-2 минуты, 2 теста

# Все тесты (unit + integration, БЕЗ benchmark)
make test-all          # или: ./gradlew allTests
```

### Запуск с Allure отчетом

```bash
# Unit тесты с отчетом
make test-unit-allure

# Integration тесты с отчетом
make test-integration-allure

# Все тесты с отчетом и открытием в браузере
make test-allure-serve

# Benchmark тесты с отчетом
make test-benchmark-serve
```

---

## 🏷️ Система тегов

Тесты разделены на 3 категории с помощью JUnit 5 тегов:

| Тег | Описание | Когда запускаются |
|-----|----------|-------------------|
| **(без тега)** | **Unit тесты** - быстрые, без внешних зависимостей | ✅ Всегда (CI/CD, локально) |
| `@Tag("integration")` | **Integration тесты** - медленные, требуют БД/Redis | 🔧 Только явно |
| `@Tag("benchmark")` | **Benchmark тесты** - для измерения производительности | 🔧 Только локально |

### Unit тесты

**Характеристики:**
- ⚡ **Быстрые**: ~15 секунд для всех тестов
- 🚀 **Не требуют зависимостей**: H2 in-memory, моки
- ✅ **Запускаются всегда**: локально, CI/CD, прод
- 📦 **144 теста**: валидация, бизнес-логика, utils

**Команды:**
```bash
./gradlew test              # или: make test
make test-unit-allure       # с Allure отчетом
```

### Integration тесты

**Характеристики:**
- ⏱️ **Медленные**: ~30-60 секунд
- 🔗 **Требуют внешние зависимости**: PostgreSQL, Redis
- 📝 **Работают с продакшен БД**: используют данные из `.env.app`
- 🔧 **Запускаются только явно**: `make test-integration`

**Требования:**
1. Файл `.env.app` с настройками БД и Redis
2. PostgreSQL и Redis должны быть доступны

**Команды:**
```bash
./gradlew integrationTest   # или: make test-integration
make test-integration-allure # с Allure отчетом
```

**Важно:** Integration тесты работают с продакшен БД! Будьте осторожны!

### Benchmark тесты

**Характеристики:**
- 🐌 **Очень медленные**: 1-2 минуты каждый
- 🔧 **Только локальный запуск**: НЕ для CI/CD!
- 📊 **Измеряют производительность**: время загрузки, throughput, cache hit rate
- 🚀 **Требуют запущенное приложение**: для `RealHttpBenchmarkTest`

**Команды:**
```bash
# Сначала запустить приложение
make start

# В другом терминале: запустить бенчмарки
make test-benchmark
make test-benchmark-serve   # с Allure отчетом

# Остановить приложение
make stop
```

---

## 📊 Allure Test Reports

Проект использует Allure Framework v2.25.0 для генерации красивых и информативных отчетов.

### Установка Allure

**macOS:**
```bash
brew install allure
```

**Linux:**
```bash
sudo apt-add-repository ppa:qameta/allure
sudo apt-get update 
sudo apt-get install allure
```

**Windows:**
```bash
scoop install allure
```

Или скачать с [официального сайта](https://github.com/allure-framework/allure2/releases).

### Просмотр отчетов

**Важно:** Не открывайте `index.html` напрямую! Используйте только `allure serve`:

```bash
# Правильно ✅
make allure-serve
# или
allure serve build/allure-results

# Неправильно ❌ (не работает в современных браузерах)
open build/reports/allure-report/allureReport/index.html
```

### Структура отчетов

Allure генерирует следующие разделы:

1. **Overview** - статистика тестов, графики трендов
2. **Categories** - группировка по типам дефектов
3. **Suites** - группировка по тестовым классам
4. **Graphs** - графики по статусу, критичности, времени выполнения
5. **Timeline** - временная шкала выполнения тестов
6. **Behaviors** - группировка по Epic → Feature → Story

---

## 🏷️ Allure аннотации

### На уровне класса

```java
@Epic("API для стикерсетов")
@Feature("Создание и управление стикерсетами")
@DisplayName("Интеграционные тесты StickerSetController")
class StickerSetControllerTest {
    // ...
}
```

### На уровне метода

```java
@Test
@Story("Создание стикерсета")
@DisplayName("POST /api/stickersets с валидными данными возвращает 201")
@Description("""
    Проверяет регистрацию существующего стикерсета Telegram в галерее:
    1. Формируем запрос POST /api/stickersets с JSON-телом.
    2. Передаём заголовок X-Telegram-Init-Data с валидной подписью.
    3. Ожидаем HTTP 201 Created и полный StickerSetDto в ответе.
    """)
@Severity(SeverityLevel.BLOCKER)
void createStickerSet_WithValidData_ShouldReturn201() {
    // Given-When-Then структура
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

### Структура Epic → Feature → Story

**Epic:** Высокоуровневые модули
- "API для стикерсетов"
- "Тестовые утилиты"
- "Бизнес-логика стикерсетов"

**Feature:** Функциональность модуля
- "Создание и управление стикерсетами"
- "Генератор Telegram initData"
- "Сервис управления стикерсетами"

**Story:** Конкретные сценарии
- "Создание стикерсета"
- "Валидация параметров"
- "Генерация валидной initData"

---

## 🔧 Технические детали

### Конфигурация в build.gradle

```gradle
// UNIT тесты (по умолчанию)
test {
    useJUnitPlatform {
        excludeTags 'benchmark', 'integration'  // Исключаем!
    }
    include '**/*Test.class'
    exclude '**/*IntegrationTest.class'
    exclude '**/benchmark/**'
}

// INTEGRATION тесты
task integrationTest(type: Test) {
    useJUnitPlatform {
        includeTags 'integration'     // Только integration!
        excludeTags 'benchmark'
    }
}

// BENCHMARK тесты
task benchmarkTest(type: Test) {
    useJUnitPlatform {
        includeTags 'benchmark'       // Только benchmark!
    }
}
```

### Как добавить тег к тесту

```java
import org.junit.jupiter.api.Tag;

@Tag("integration")  // или "benchmark"
class MyIntegrationTest {
    @Test
    void myTest() {
        // ...
    }
}
```

---

## 🚀 Best Practices

### 1. Перед коммитом
```bash
make test           # Только unit тесты (быстро!)
```
✅ Должно проходить всегда

### 2. Перед merge request
```bash
make test-all       # unit + integration (без benchmark)
```
✅ Убедитесь что всё работает с реальной БД

### 3. При оптимизации производительности
```bash
# Запустить приложение
make start

# В другом терминале
make test-benchmark-allure
allure serve build/allure-results

# Остановить
make stop
```
📊 Сравните результаты до/после оптимизации

### 4. На CI/CD (продакшен)
```bash
./gradlew test      # Только unit (без integration и benchmark)
```
✅ Быстро, надежно, без внешних зависимостей

---

---

## ⚡ Нагрузочное тестирование (Gatling)

**Требование:** проект и нагрузочный тест используют **Java 17**. Gradle toolchain и Makefile настроены на 17.

Gatling — инструмент для нагрузочного тестирования. В отличие от обычных JUnit-тестов, он не проверяет корректность, а **измеряет, как система ведёт себя под нагрузкой**.

### Как это работает (простыми словами)

| Понятие | Объяснение |
|---------|------------|
| **RPS** | Запросов в секунду — главный «рычаг» нагрузки для GET-эндпоинтов |
| **Virtual user (VU)** | Не реальный человек, а скрипт. При открытой модели каждый VU делает 1 запрос и завершается — поэтому **users/sec ≈ RPS** |
| **Ступенька (staircase)** | Нагрузка растёт не сразу, а поэтапно: 1 RPS → 2 RPS → … → N RPS. Видно, на какой ступеньке всё сломалось |
| **OK / KO** | OK = HTTP 200 + тело ответа валидно. KO = любой другой статус, таймаут, сетевая ошибка |
| **p95 / p99** | 95/99 процентиль времени ответа. «p95 = 300 мс» означает: 95% запросов отвечали быстрее 300 мс |

### Симуляция: `StickerSetsProdStaircaseSimulation`

Повторяет этот curl на продакшен:
```bash
curl -X GET 'https://stickerartgallery-e13nst.amvera.io/api/stickersets?page=0&size=20&sort=likesCount&direction=DESC&officialOnly=false&isVerified=false&likedOnly=false&shortInfo=false&preview=false' \
  -H 'accept: application/json'
```

**Файл:** `src/gatling/java/.../gatling/StickerSetsProdStaircaseSimulation.java`

**Дефолтный профиль нагрузки:**
- 20 ступеней, шаг +1 RPS, каждая ступень 30 сек + 10 сек разгон
- Итог: 1 RPS → 20 RPS, ~13 минут общего времени

### Команды запуска

```bash
# Стандартный прогон (1→20 RPS, ~13 минут)
make load-test-stickersets

# Быстрый тест для проверки (1→5 RPS, ~1.5 минуты)
make load-test-quick

# С кастомными параметрами
make load-test-stickersets GATLING_LOAD_OPTS="-DstartRps=1 -DstepRps=2 -Dsteps=10 -DstepDurationSeconds=60"
```

Или напрямую через Gradle:
```bash
# Дефолты
./gradlew gatlingRun --non-interactive

# Свои параметры (5 ступеней по 10 сек)
./gradlew gatlingRun --non-interactive \
  -DstartRps=1 -DstepRps=1 -Dsteps=5 -DstepDurationSeconds=10 -DrampSeconds=5
```

### Все параметры

| Параметр | Дефолт | Описание |
|----------|--------|----------|
| `baseUrl` | `https://stickerartgallery-e13nst.amvera.io` | Целевой хост |
| `startRps` | `1` | RPS первой ступени |
| `stepRps` | `1` | Прирост RPS на каждой ступени |
| `steps` | `20` | Количество ступеней |
| `stepDurationSeconds` | `30` | Длительность каждой ступени (сек) |
| `rampSeconds` | `10` | Разгон между ступенями (сек) |
| `requestTimeoutMs` | `5000` | Таймаут запроса (мс) — после него Gatling пишет KO |

### Как читать HTML-отчёт

Отчёт генерируется в `build/reports/gatling/<имя-симуляции>-<timestamp>/index.html`.

**Открыть отчёт:**
```bash
open $(ls -dt build/reports/gatling/*/index.html | head -1)
```

**Что смотреть:**

| Раздел отчёта | На что обратить внимание |
|---------------|--------------------------|
| **Global** → Requests | Где начинают появляться `KO` (ошибки). Какие статусы: 429 (rate-limit), 500/502 (сервер падает), timeout |
| **Global** → Response time percentiles | Где p95/p99 резко растут — это часто происходит **раньше**, чем явные 5xx |
| **Global** → Throughput | Если запрашиваешь 10 RPS, но фактический throughput не растёт — сервер достиг предела |
| **Details** по конкретному запросу | Детали по каждой ступени — видно на какой именно RPS всё сломалось |

**Типичная картина при деградации:**
```
RPS 1–8:  OK 100%,  p95 < 200ms  ← норма
RPS 9–11: OK 100%,  p95 резко растёт до 1–2 сек ← сервер начинает "задыхаться"
RPS 12+:  KO появляются, p95 > 5 сек            ← предел пройден
```

### Правила безопасного запуска на проде

1. **Начинай с малого** — дефолтные параметры (1 RPS → 20 RPS) безопасны для сервиса
2. **Запускай в нерабочее время** — вечером или ночью, чтобы не затронуть реальных пользователей
3. **Следи за логами** параллельно с тестом: `make logs-follow` или в панели Amvera
4. **Не превышай N RPS** где начались KO — сервер уже перегружен, дальше только хуже
5. **Ошибка 429** = rate-limit хостинга или приложения, а не падение сервиса — это нормальная защита

### Структура файлов Gatling

```
src/
└── gatling/
    ├── java/
    │   └── com/example/sticker_art_gallery/gatling/
    │       └── StickerSetsProdStaircaseSimulation.java  ← симуляция
    └── resources/
        └── logback.xml   ← тихое логирование во время нагрузки
```

---

## 📚 Полезные ссылки

- [Allure Framework Documentation](https://docs.qameta.io/allure/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [Gatling Documentation](https://docs.gatling.io/)
- [Gatling Java DSL Reference](https://docs.gatling.io/reference/script/core/injection/)

---

**Дата последнего обновления:** 2026-03-04
**Версия Allure:** 2.25.0
**Версия Gatling:** 3.13.5

