# Имя вашего JAR файла и Docker образа
APP_NAME = sticker-art-gallery
VERSION = 0.0.1-SNAPSHOT
JAR_FILE = build/libs/$(APP_NAME)-$(VERSION).jar
DOCKER_IMAGE = sticker-art-gallery

# Задача по умолчанию
all: build

# Сборка JAR файла с использованием Gradle
gradle-build:
	./gradlew clean build -x test

build: gradle-build

# Создание Docker образа
docker-build: build
	docker build -t $(DOCKER_IMAGE):latest .
	docker tag $(DOCKER_IMAGE):latest $(DOCKER_IMAGE):$(VERSION)

# Запуск Docker контейнера
docker-run: docker-build
	docker compose up --build -d

# Запуск только базы данных
docker-db:
	docker compose up -d db

# Остановка и удаление Docker контейнера
docker-down:
	docker compose down

# Просмотр логов
docker-logs:
	docker compose logs -f

# Просмотр логов только приложения
docker-logs-app:
	docker compose logs -f sticker-art-gallery

# Просмотр логов базы данных
docker-logs-db:
	docker compose logs -f db

# Очистка файлов сборки и Docker образов
clean:
	./gradlew clean
	docker rmi $(DOCKER_IMAGE):latest $(DOCKER_IMAGE):$(VERSION) 2>/dev/null || true

# Полная очистка (включая volumes)
clean-all: clean
	docker compose down -v
	docker system prune -f

# Проверка статуса контейнеров
status:
	docker compose ps

# Перезапуск приложения
restart:
	docker compose restart sticker-art-gallery

.PHONY: all build gradle-build docker-build docker-run docker-db docker-down docker-logs docker-logs-app docker-logs-db clean clean-all status restart

# Запуск автотестов с использованием Gradle
test:
	./gradlew clean test

# Запуск тестов с отчетом
test-report:
	./gradlew clean test allureReport
