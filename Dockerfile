# Многоэтапная сборка для оптимизации размера образа
FROM eclipse-temurin:17-jdk AS build

# Установка рабочей директории
WORKDIR /app

# Копирование файлов конфигурации Gradle
COPY gradle gradle/
COPY gradlew build.gradle settings.gradle gradle.properties ./

# Скачивание зависимостей (кэширование слоев)
RUN ./gradlew dependencies --no-daemon

# Копирование исходного кода
COPY src src/

# Сборка приложения
RUN ./gradlew build -x test --no-daemon

# Финальный образ
FROM eclipse-temurin:17-jre

# Установка рабочей директории
WORKDIR /app

# Копирование JAR файла из этапа сборки
COPY --from=build /app/build/libs/sticker-art-gallery-0.0.1-SNAPSHOT.jar app.jar

# Копирование конфигурационных файлов
COPY --from=build /app/src/main/resources/application.yaml /app/config/application.yaml

# Создание директории для логов
RUN mkdir -p /app/logs

# Настройка переменных окружения
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport"
ENV SPRING_PROFILES_ACTIVE=prod

# Открытие порта
EXPOSE 8080

# Команда запуска
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.config.location=file:/app/config/application.yaml"]

# Метаданные
LABEL maintainer="Sticker Art Gallery Team"
LABEL version="0.0.1-SNAPSHOT"
LABEL description="Telegram Bot for creating stickers from images"