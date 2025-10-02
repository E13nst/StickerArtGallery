#!/bin/bash

# Тестовый скрипт для проверки API лайков
BASE_URL="http://localhost:8080"
STICKER_SET_ID=22

echo "🧪 Тестирование API лайков"
echo "=========================="

# 1. Проверим, что приложение работает
echo "1. Проверка доступности приложения..."
curl -s "$BASE_URL/api/stickersets?size=1" > /dev/null
if [ $? -eq 0 ]; then
    echo "✅ Приложение доступно"
else
    echo "❌ Приложение недоступно"
    exit 1
fi

# 2. Проверим топ стикерсетов по лайкам
echo "2. Проверка топа стикерсетов по лайкам..."
curl -s "$BASE_URL/api/likes/top-stickersets?size=1" | jq -r '.content[0].stickerSet.title'
if [ $? -eq 0 ]; then
    echo "✅ Топ стикерсетов работает"
else
    echo "❌ Топ стикерсетов не работает"
fi

# 3. Попробуем поставить лайк без аутентификации (должен вернуть 401)
echo "3. Тест лайка без аутентификации..."
RESPONSE=$(curl -s -w "%{http_code}" -X PUT "$BASE_URL/api/likes/stickersets/$STICKER_SET_ID/toggle")
HTTP_CODE="${RESPONSE: -3}"
if [ "$HTTP_CODE" = "401" ]; then
    echo "✅ Аутентификация требуется (ожидаемо)"
elif [ "$HTTP_CODE" = "500" ]; then
    echo "⚠️  Получена 500 ошибка (проблема на сервере)"
else
    echo "⚠️  Неожиданный код ответа: $HTTP_CODE"
fi

# 4. Попробуем с простыми заголовками
echo "4. Тест с простыми заголовками..."
RESPONSE=$(curl -s -w "%{http_code}" -X PUT "$BASE_URL/api/likes/stickersets/$STICKER_SET_ID/toggle" \
  -H "X-Telegram-Init-Data: test" \
  -H "X-Telegram-Bot-Name: StickerGallery")
HTTP_CODE="${RESPONSE: -3}"
echo "Код ответа: $HTTP_CODE"

echo "=========================="
echo "Тестирование завершено"
