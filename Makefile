# Telegram Bot Dream Stream - Makefile
# Удобные команды для разработки и тестирования

.PHONY: help start stop restart build test clean logs status

# Цвета для вывода
GREEN=\033[0;32m
YELLOW=\033[1;33m
RED=\033[0;31m
NC=\033[0m # No Color

# Переменные
APP_NAME=sticker-art-gallery
PORT=8080
LOG_FILE=app_debug.log
GRADLE_CMD=./gradlew

help: ## Показать справку по командам
	@echo "$(GREEN)Telegram Bot Dream Stream - Команды:$(NC)"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(YELLOW)%-15s$(NC) %s\n", $$1, $$2}'
	@echo ""

start: ## Запустить приложение локально
	@echo "$(GREEN)🚀 Запускаем приложение...$(NC)"
	@echo "$(YELLOW)📝 Логи будут сохранены в $(LOG_FILE)$(NC)"
	@echo "$(YELLOW)🌐 Приложение будет доступно на http://localhost:$(PORT)$(NC)"
	@echo ""
	@if [ -f .env.app ]; then \
		echo "$(GREEN)✅ Найден файл .env.app$(NC)"; \
		rm -f $(LOG_FILE) && set -a && source .env.app && set +a && $(GRADLE_CMD) bootRun --args='--spring.profiles.active=dev' > $(LOG_FILE) 2>&1 & \
		echo "$(GREEN)✅ Приложение запущено в фоне (PID: $$!)$(NC)"; \
	else \
		echo "$(RED)❌ Файл .env.app не найден!$(NC)"; \
		echo "$(YELLOW)💡 Создайте файл .env.app с переменными окружения$(NC)"; \
		exit 1; \
	fi

stop: ## Остановить приложение
	@echo "$(RED)🛑 Останавливаем приложение...$(NC)"
	@pkill -f "gradlew bootRun" 2>/dev/null || true
	@pkill -f "java.*sticker_art_gallery" 2>/dev/null || true
	@lsof -ti:$(PORT) | xargs kill -9 2>/dev/null || true
	@echo "$(GREEN)✅ Приложение остановлено$(NC)"

restart: stop start ## Перезапустить приложение

status: ## Показать статус приложения
	@echo "$(YELLOW)📊 Статус приложения:$(NC)"
	@if lsof -i:$(PORT) >/dev/null 2>&1; then \
		echo "$(GREEN)✅ Приложение запущено на порту $(PORT)$(NC)"; \
		echo "$(YELLOW)🌐 URL: http://localhost:$(PORT)$(NC)"; \
		echo "$(YELLOW)📝 Логи: $(LOG_FILE)$(NC)"; \
	else \
		echo "$(RED)❌ Приложение не запущено$(NC)"; \
	fi

logs: ## Показать логи приложения
	@if [ -f $(LOG_FILE) ]; then \
		echo "$(YELLOW)📝 Последние 20 строк логов:$(NC)"; \
		echo ""; \
		tail -20 $(LOG_FILE); \
	else \
		echo "$(RED)❌ Файл логов $(LOG_FILE) не найден$(NC)"; \
		echo "$(YELLOW)💡 Запустите приложение командой: make start$(NC)"; \
	fi

logs-follow: ## Следить за логами в реальном времени
	@if [ -f $(LOG_FILE) ]; then \
		echo "$(YELLOW)📝 Следим за логами (Ctrl+C для выхода):$(NC)"; \
		tail -f $(LOG_FILE); \
	else \
		echo "$(RED)❌ Файл логов $(LOG_FILE) не найден$(NC)"; \
		echo "$(YELLOW)💡 Запустите приложение командой: make start$(NC)"; \
	fi

build: ## Собрать приложение
	@echo "$(GREEN)🔨 Собираем приложение...$(NC)"
	@$(GRADLE_CMD) build
	@echo "$(GREEN)✅ Сборка завершена$(NC)"

# UNIT тесты - быстрые, без внешних зависимостей
test-unit: ## Запустить UNIT тесты (быстрые, без внешних зависимостей)
	@echo "$(GREEN)🧪 Запускаем UNIT тесты...$(NC)"
	@$(GRADLE_CMD) test
	@echo "$(GREEN)✅ UNIT тесты завершены$(NC)"

# INTEGRATION тесты - с внешними зависимостями
test-integration: ## Запустить INTEGRATION тесты (с внешними зависимостями)
	@echo "$(GREEN)🔗 Запускаем INTEGRATION тесты...$(NC)"
	@echo "$(YELLOW)⚠️ ВНИМАНИЕ: Работает с продакшен БД!$(NC)"
	@$(GRADLE_CMD) integrationTest
	@echo "$(GREEN)✅ INTEGRATION тесты завершены$(NC)"

# BENCHMARK тесты - только локально
test-benchmark: ## Запустить BENCHMARK тесты (только локально, не в CI/CD)
	@echo "$(GREEN)⚡ Запускаем BENCHMARK тесты...$(NC)"
	@echo "$(YELLOW)⚠️ RealHttpBenchmarkTest требует запущенное приложение (make start)$(NC)"
	@$(GRADLE_CMD) benchmarkTest
	@echo "$(GREEN)✅ BENCHMARK тесты завершены$(NC)"

test-benchmark-allure: ## Запустить BENCHMARK тесты с Allure отчетом
	@echo "$(GREEN)⚡ Запускаем BENCHMARK тесты с Allure...$(NC)"
	@echo "$(YELLOW)⚠️ RealHttpBenchmarkTest требует запущенное приложение (make start)$(NC)"
	@$(GRADLE_CMD) clean benchmarkTest --no-configuration-cache
	@echo "$(GREEN)📊 Генерируем Allure отчет...$(NC)"
	@$(GRADLE_CMD) allureReport --no-configuration-cache
	@echo "$(GREEN)✅ Allure отчет сгенерирован$(NC)"
	@echo "$(YELLOW)📁 Отчет: build/reports/allure-report/allureReport/index.html$(NC)"
	@echo "$(YELLOW)💡 Просмотр отчета: allure serve build/allure-results$(NC)"

# Все тесты
test-all: ## Запустить все тесты (unit + integration, БЕЗ benchmark)
	@echo "$(GREEN)🧪 Запускаем все тесты...$(NC)"
	@echo "$(YELLOW)💡 Бенчмарки исключены (запускайте отдельно: make test-benchmark)$(NC)"
	@$(GRADLE_CMD) allTests
	@echo "$(GREEN)✅ Все тесты завершены$(NC)"

# Legacy команда для совместимости
test: test-unit

# Allure отчеты для UNIT тестов
test-unit-allure: ## Запустить UNIT тесты с Allure отчетом
	@echo "$(GREEN)🧪 Запускаем UNIT тесты с Allure...$(NC)"
	@$(GRADLE_CMD) clean test --no-configuration-cache
	@echo "$(GREEN)📊 Генерируем Allure отчет...$(NC)"
	@$(GRADLE_CMD) allureReport --no-configuration-cache
	@echo "$(GREEN)✅ Allure отчет сгенерирован$(NC)"
	@echo "$(YELLOW)📁 Отчет: build/reports/allure-report/allureReport/index.html$(NC)"

# Allure отчеты для INTEGRATION тестов
test-integration-allure: ## Запустить INTEGRATION тесты с Allure отчетом
	@echo "$(GREEN)🔗 Запускаем INTEGRATION тесты с Allure...$(NC)"
	@echo "$(YELLOW)⚠️ ВНИМАНИЕ: Работает с продакшен БД!$(NC)"
	@$(GRADLE_CMD) clean integrationTest --no-configuration-cache
	@echo "$(GREEN)📊 Генерируем Allure отчет...$(NC)"
	@$(GRADLE_CMD) allureReport --no-configuration-cache
	@echo "$(GREEN)✅ Allure отчет сгенерирован$(NC)"
	@echo "$(YELLOW)📁 Отчет: build/reports/allure-report/allureReport/index.html$(NC)"

# Allure отчеты для всех тестов
test-all-allure: ## Запустить все тесты с Allure отчетом
	@echo "$(GREEN)🧪 Запускаем все тесты с Allure...$(NC)"
	@$(GRADLE_CMD) clean allTests --no-configuration-cache
	@echo "$(GREEN)📊 Генерируем Allure отчет...$(NC)"
	@$(GRADLE_CMD) allureReport --no-configuration-cache
	@echo "$(GREEN)✅ Allure отчет сгенерирован$(NC)"
	@echo "$(YELLOW)📁 Отчет: build/reports/allure-report/allureReport/index.html$(NC)"

# Legacy команды для совместимости
test-allure: test-unit-allure

test-allure-serve: ## Запустить все тесты и открыть Allure отчет через встроенный сервер
	@echo "$(GREEN)🧪 Запускаем все тесты с Allure...$(NC)"
	@$(GRADLE_CMD) clean allTests --no-configuration-cache
	@echo "$(GREEN)📊 Запускаем Allure сервер (откроет браузер автоматически)...$(NC)"
	@allure serve build/allure-results
	@echo "$(GREEN)✅ Отчет открыт в браузере$(NC)"

allure-report: ## Сгенерировать Allure отчет из существующих результатов
	@echo "$(GREEN)📊 Генерируем Allure отчет...$(NC)"
	@$(GRADLE_CMD) allureReport --no-configuration-cache
	@echo "$(GREEN)✅ Отчет сгенерирован$(NC)"
	@echo "$(YELLOW)📁 Отчет: build/reports/allure-report/allureReport/index.html$(NC)"

allure-serve: ## Открыть существующие результаты тестов через Allure сервер
	@if [ -d build/allure-results ]; then \
		echo "$(GREEN)📊 Запускаем Allure сервер...$(NC)"; \
		allure serve build/allure-results; \
	else \
		echo "$(RED)❌ Результаты тестов не найдены!$(NC)"; \
		echo "$(YELLOW)💡 Сначала запустите: make test$(NC)"; \
	fi

allure-open: ## Открыть последний Allure отчет (устаревший метод, используйте allure-serve)
	@echo "$(YELLOW)⚠️  Рекомендуется использовать: make allure-serve$(NC)"
	@if [ -f build/reports/allure-report/allureReport/index.html ]; then \
		echo "$(GREEN)🌐 Открываем Allure отчет...$(NC)"; \
		open build/reports/allure-report/allureReport/index.html; \
	else \
		echo "$(RED)❌ Отчет не найден!$(NC)"; \
		echo "$(YELLOW)💡 Сначала запустите: make test-allure$(NC)"; \
	fi

clean: ## Очистить проект
	@echo "$(YELLOW)🧹 Очищаем проект...$(NC)"
	@$(GRADLE_CMD) clean
	@rm -f $(LOG_FILE)
	@echo "$(GREEN)✅ Очистка завершена$(NC)"

test-api: ## Тестировать API локально
	@echo "$(GREEN)🧪 Тестируем API...$(NC)"
	@if lsof -i:$(PORT) >/dev/null 2>&1; then \
		echo "$(YELLOW)📡 Тестируем /auth/status...$(NC)"; \
		curl -s -X GET "http://localhost:$(PORT)/auth/status" \
			-H "accept: application/json" \
			-H "X-Telegram-Init-Data: auth_date=1757578572&hash=19b955b385ee8336c73f8032e12bf006d9b5e2267ccec761a8f06af6af303e7c&query_id=AAF93XAIAAAAAH3dcAjRIlQI&user=%7B%22id%22%3A141614461%2C%22first_name%22%3A%22Andrey%22%2C%22last_name%22%3A%22Mitroshin%22%2C%22username%22%3A%22E13nst%22%2C%22language_code%22%3A%22ru%22%7D" \
			-H "X-Telegram-Bot-Name: StickerGallery" | jq . || echo "$(RED)❌ Ошибка тестирования API$(NC)"; \
	else \
		echo "$(RED)❌ Приложение не запущено! Запустите командой: make start$(NC)"; \
	fi

deploy: ## Развернуть на продакшен (только push в main)
	@echo "$(GREEN)🚀 Развертываем на продакшен...$(NC)"
	@git push origin main
	@echo "$(GREEN)✅ Изменения отправлены на GitHub$(NC)"
	@echo "$(YELLOW)⏳ Дождитесь автоматического развертывания (2-3 минуты)$(NC)"

# Команды для разработки
dev-start: start logs-follow ## Запустить и следить за логами

dev-restart: restart logs-follow ## Перезапустить и следить за логами

# Команды для отладки
debug-logs: ## Показать только ошибки из логов
	@if [ -f $(LOG_FILE) ]; then \
		echo "$(YELLOW)🔍 Поиск ошибок в логах:$(NC)"; \
		grep -i "error\|exception\|failed" $(LOG_FILE) || echo "$(GREEN)✅ Ошибок не найдено$(NC)"; \
	else \
		echo "$(RED)❌ Файл логов $(LOG_FILE) не найден$(NC)"; \
	fi

debug-port: ## Проверить, что порт свободен
	@echo "$(YELLOW)🔍 Проверяем порт $(PORT)...$(NC)"
	@if lsof -i:$(PORT) >/dev/null 2>&1; then \
		echo "$(RED)❌ Порт $(PORT) занят:$(NC)"; \
		lsof -i:$(PORT); \
	else \
		echo "$(GREEN)✅ Порт $(PORT) свободен$(NC)"; \
	fi

# Команды для Git
commit: ## Сделать коммит с сообщением
	@echo "$(GREEN)📝 Создаем коммит...$(NC)"
	@git add .
	@echo "$(YELLOW)💬 Введите сообщение коммита:$(NC)"
	@read -p "> " message; \
	git commit -m "$$message"
	@echo "$(GREEN)✅ Коммит создан$(NC)"

# Команды по умолчанию
.DEFAULT_GOAL := help