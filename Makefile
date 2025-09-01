# Имя вашего JAR файла и Docker образа
APP_NAME = telegram-bot-dream-stream
VERSION = 0.0.1-SNAPSHOT
JAR_FILE = build/libs/$(APP_NAME)-$(VERSION).jar
DOCKER_IMAGE = telegram-bot-dream-stream

# Задача по умолчанию
all: build

# Сборка JAR файла с использованием Gradle
gradle-build:
	./gradlew clean build

build: gradle-build

# Создание Docker образа
docker-build: build
	docker build -t $(DOCKER_IMAGE):latest .

# Запуск Docker контейнера
docker-run: docker-build
	docker-compose up --build -d

# Остановка и удаление Docker контейнера
docker-down:
	docker-compose down

# Очистка файлов сборки и Docker образов
clean:
	./gradlew clean
	docker rmi $(DOCKER_IMAGE):latest

.PHONY: all build gradle-build docker-build docker-run docker-down clean

# Запуск автотестов с использованием Gradle
test:
	./gradlew clean test
