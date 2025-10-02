#!/bin/bash

# Тестовый скрипт для проверки системы лайков
BASE_URL="http://localhost:8080"
STICKER_SET_ID=22

echo "🧪 Тестирование системы лайков"
echo "==============================="

# 1. Проверим текущее количество лайков
echo "1. Текущее количество лайков:"
CURRENT_LIKES=$(curl -s "$BASE_URL/api/likes/top-stickersets?size=1" | jq -r '.content[0].likesCount')
echo "   Стикерсет ID $STICKER_SET_ID имеет $CURRENT_LIKES лайков"

# 2. Проверим, что endpoint требует аутентификацию
echo "2. Проверка аутентификации..."
RESPONSE=$(curl -s -w "%{http_code}" -X PUT "$BASE_URL/api/likes/stickersets/$STICKER_SET_ID/toggle")
HTTP_CODE="${RESPONSE: -3}"
if [ "$HTTP_CODE" = "401" ]; then
    echo "   ✅ Endpoint правильно требует аутентификацию (401)"
else
    echo "   ❌ Неожиданный код ответа: $HTTP_CODE"
fi

# 3. Попробуем с невалидным initData
echo "3. Тест с невалидным initData..."
RESPONSE=$(curl -s -w "%{http_code}" -X PUT "$BASE_URL/api/likes/stickersets/$STICKER_SET_ID/toggle" \
  -H "X-Telegram-Init-Data: invalid_data" \
  -H "X-Telegram-Bot-Name: StickerGallery")
HTTP_CODE="${RESPONSE: -3}"
echo "   Код ответа с невалидными данными: $HTTP_CODE"

# 4. Проверим, что количество лайков не изменилось
echo "4. Проверка, что количество лайков не изменилось..."
NEW_LIKES=$(curl -s "$BASE_URL/api/likes/top-stickersets?size=1" | jq -r '.content[0].likesCount')
if [ "$CURRENT_LIKES" = "$NEW_LIKES" ]; then
    echo "   ✅ Количество лайков не изменилось ($NEW_LIKES)"
else
    echo "   ❌ Количество лайков изменилось: $CURRENT_LIKES -> $NEW_LIKES"
fi

echo "==============================="
echo "Тестирование завершено"
echo ""
echo "Для полного тестирования нужен валидный initData от Telegram Web App"
