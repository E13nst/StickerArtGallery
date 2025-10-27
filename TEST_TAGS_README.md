# 🏷️ Система тегов для тестов

## Обзор

Тесты в проекте разделены на 3 категории с помощью JUnit 5 тегов:

| Тег | Количество | Описание | Когда запускаются |
|-----|------------|----------|-------------------|
| **(без тега)** | 8 файлов | **Unit тесты** - быстрые, без внешних зависимостей | ✅ Всегда (CI/CD, локально) |
| `@Tag("integration")` | 4 файла | **Integration тесты** - медленные, требуют БД/Redis | 🔧 Только явно |
| `@Tag("benchmark")` | 2 файла | **Benchmark тесты** - для измерения производительности | 🔧 Только локально |

---

## 📋 Быстрый старт

### Запуск тестов

```bash
# 1️⃣  По умолчанию (UNIT тесты) - запускается в CI/CD
./gradlew test          # или: make test
# ✅ Быстро: ~15 секунд
# ✅ Не требует внешних зависимостей
# ✅ 144 теста

# 2️⃣  INTEGRATION тесты - только когда нужно
./gradlew integrationTest   # или: make test-integration
# ⚠️  Требует: БД, Redis из .env.app
# ⏱️  Медленнее: ~30-60 секунд
# 🔗 4 теста

# 3️⃣  BENCHMARK тесты - только локально
./gradlew benchmarkTest    # или: make test-benchmark
# ⚠️  Требует: запущенное приложение (make start)
# ⏱️  Очень медленно: ~1-2 минуты
# ⚡ 2 теста

# 4️⃣  Все тесты вместе (unit + integration, БЕЗ benchmark)
./gradlew allTests      # или: make test-all
```

---

## 🧪 Unit тесты (по умолчанию)

### Характеристики
- ⚡ **Быстрые**: ~15 секунд для всех тестов
- 🚀 **Не требуют зависимостей**: H2 in-memory, моки
- ✅ **Запускаются всегда**: локально, CI/CD, прод
- 📦 **144 теста**: валидация, бизнес-логика, utils

### Файлы
Все файлы `*Test.java`, КРОМЕ:
- `*IntegrationTest.java` - интеграционные тесты
- `benchmark/*` - бенчмарк-тесты

### Примеры
```
src/test/java/.../util/TelegramInitDataGeneratorTest.java
src/test/java/.../dto/CreateStickerSetDtoTest.java
src/test/java/.../service/StickerSetNameValidatorTest.java
...
```

### Команды
```bash
# Gradle
./gradlew test

# Makefile
make test              # или make test-unit

# С Allure отчетом
make test-unit-allure
```

---

## 🔗 Integration тесты (`@Tag("integration")`)

### Характеристики
- ⏱️  **Медленные**: ~30-60 секунд
- 🔗 **Требуют внешние зависимости**: PostgreSQL, Redis
- 📝 **Работают с продакшен БД**: используют данные из `.env.app`
- 🔧 **Запускаются только явно**: `make test-integration`

### Файлы (4 штуки)
```
src/test/java/.../StickerSetApiIntegrationTest.java
src/test/java/.../controller/ImprovedStickerSetControllerIntegrationTest.java
src/test/java/.../controller/StickerSetControllerIntegrationTest.java
src/test/java/.../controller/StickerSetLikedFilterIntegrationTest.java
```

### Когда запускать
- ✅ Перед важным релизом
- ✅ При изменении БД схемы
- ✅ При изменении Redis логики
- ✅ При рефакторинге критичных компонентов
- ❌ НЕ запускать на каждом коммите (медленно!)

### Команды
```bash
# Gradle
./gradlew integrationTest

# Makefile
make test-integration

# С Allure отчетом
make test-integration-allure
```

### Требования
1. Должен существовать файл `.env.app` с:
   - `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
   - `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
2. PostgreSQL и Redis должны быть доступны

---

## ⚡ Benchmark тесты (`@Tag("benchmark")`)

### Характеристики
- 🐌 **Очень медленные**: 1-2 минуты каждый
- 🔧 **Только локальный запуск**: НЕ для CI/CD!
- 📊 **Измеряют производительность**: время загрузки, throughput, cache hit rate
- 🚀 **Требуют запущенное приложение**: для `RealHttpBenchmarkTest`

### Файлы (2 штуки)

#### 1. `GalleryLoadBenchmarkTest` 
```java
@Tag("benchmark")
class GalleryLoadBenchmarkTest
```
- **Что тестирует**: Загрузка 20 стикерсетов + 80 файлов через MockMvc
- **Время**: ~50 секунд
- **Требования**: Нет (Spring Boot тест)

#### 2. `RealHttpBenchmarkTest`
```java
@Tag("benchmark")
public class RealHttpBenchmarkTest
```
- **Что тестирует**: Real HTTP запросы к живому приложению с Redis кешем
- **Время**: ~40 секунд (2 прогона: холодный + горячий кеш)
- **Требования**: 
  - ⚠️  **Запущенное приложение**: `make start`
  - Redis кеш включен: `STICKER_CACHE_ENABLED=true`

### Когда запускать
- ✅ При оптимизации производительности
- ✅ Для сравнения до/после изменений
- ✅ При изменении кеширования
- ❌ НЕ в CI/CD (слишком долго!)
- ❌ НЕ на каждом коммите

### Команды
```bash
# Запустить приложение (для RealHttpBenchmarkTest)
make start

# В другом терминале: запустить бенчмарки
make test-benchmark

# С Allure отчетом
make test-benchmark-allure
allure serve build/allure-results   # Просмотр

# Остановить приложение
make stop
```

---

## 🏗️ Технические детали

### Конфигурация в `build.gradle`

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

## 📊 Статистика по тестам

```bash
# Проверить сколько каких тестов
grep -r "@Tag(\"integration\")" src/test | wc -l   # Integration
grep -r "@Tag(\"benchmark\")" src/test | wc -l     # Benchmark
```

### Текущее состояние

| Категория | Количество | Время | Запуск |
|-----------|------------|-------|--------|
| Unit | 144 теста | ~15 сек | Всегда |
| Integration | 4 теста | ~30-60 сек | Только явно |
| Benchmark | 2 теста | ~1-2 мин | Только локально |
| **Всего** | **150 тестов** | - | - |

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

## ❓ FAQ

### Q: Почему бенчмарки не запускаются в CI/CD?
**A:** Они слишком медленные (1-2 минуты) и требуют запущенное приложение. Бенчмарки предназначены для локальной разработки и оптимизации.

### Q: Почему интеграционные тесты не запускаются по умолчанию?
**A:** Они медленные (~30-60 сек) и требуют внешние зависимости (БД, Redis). Запускайте их явно перед важными изменениями.

### Q: Как добавить новый интеграционный тест?
**A:** 
1. Создайте класс с именем `*IntegrationTest.java`
2. Добавьте `@Tag("integration")`
3. Готово! Он автоматически исключится из обычного `./gradlew test`

### Q: Как запустить ТОЛЬКО один конкретный тест?
**A:**
```bash
# Конкретный класс
./gradlew test --tests "ClassName"

# Конкретный метод
./gradlew test --tests "ClassName.methodName"
```

---

**Автор**: AI Assistant  
**Дата**: 27 октября 2025  
**Версия**: 1.0

