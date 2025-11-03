#!/bin/bash

# Тестирование пересчета likes_count для стикерсета ID 245
# Этот скрипт проверяет, что после вызова toggle пересчитывается агрегация

STICKER_SET_ID=245

# Настройки подключения (нужно будет указать проду URL)
BASE_URL="${BASE_URL:-http://localhost:8080}"

# Пример initData (нужно будет использовать реальные данные с проды)
INIT_DATA="${INIT_DATA}"
BOT_NAME="${BOT_NAME:-StickerGallery}"

echo "🧪 Тестирование пересчета likes_count для стикерсета ID $STICKER_SET_ID"
echo "=========================================="

if [ -z "$INIT_DATA" ]; then
    echo "⚠️  Переменная INIT_DATA не установлена. Установите её перед запуском:"
    echo "   export INIT_DATA='ваш_initData'"
    echo "   export BASE_URL='https://ваш-прод-домен'"
    exit 1
fi

# 1. Получаем информацию о стикерсете до пересчета
echo "1. Проверяем текущее состояние стикерсета..."
STICKERSET_INFO=$(curl -s "$BASE_URL/api/stickersets/$STICKER_SET_ID" \
    -H "X-Telegram-Init-Data: $INIT_DATA" \
    -H "X-Telegram-Bot-Name: $BOT_NAME")

LIKES_COUNT_BEFORE=$(echo "$STICKERSET_INFO" | jq -r '.likesCount // 0')
echo "   likes_count в таблице stickersets: $LIKES_COUNT_BEFORE"

# 2. Вызываем toggle - это должно пересчитать агрегацию
echo ""
echo "2. Вызываем toggle для пересчета агрегации..."
TOGGLE_RESPONSE=$(curl -s -X PUT "$BASE_URL/api/likes/stickersets/$STICKER_SET_ID/toggle" \
    -H "Content-Type: application/json" \
    -H "X-Telegram-Init-Data: $INIT_DATA" \
    -H "X-Telegram-Bot-Name: $BOT_NAME" \
    -w "\n%{http_code}")

HTTP_CODE=$(echo "$TOGGLE_RESPONSE" | tail -n 1)
BODY=$(echo "$TOGGLE_RESPONSE" | head -n -1)

echo "   Код ответа: $HTTP_CODE"
if [ "$HTTP_CODE" -eq 200 ]; then
    TOTAL_LIKES=$(echo "$BODY" | jq -r '.totalLikes // 0')
    IS_LIKED=$(echo "$BODY" | jq -r '.isLiked // false')
    echo "   ✅ Toggle выполнен успешно"
    echo "   totalLikes из ответа: $TOTAL_LIKES"
    echo "   isLiked: $IS_LIKED"
else
    echo "   ❌ Ошибка при вызове toggle: $HTTP_CODE"
    echo "   Ответ: $BODY"
    exit 1
fi

# 3. Проверяем likes_count после пересчета
echo ""
echo "3. Проверяем likes_count после пересчета..."
STICKERSET_INFO_AFTER=$(curl -s "$BASE_URL/api/stickersets/$STICKER_SET_ID" \
    -H "X-Telegram-Init-Data: $INIT_DATA" \
    -H "X-Telegram-Bot-Name: $BOT_NAME")

LIKES_COUNT_AFTER=$(echo "$STICKERSET_INFO_AFTER" | jq -r '.likesCount // 0')
echo "   likes_count после пересчета: $LIKES_COUNT_AFTER"

# 4. Сравниваем результаты
echo ""
echo "4. Результаты:"
echo "   likes_count до: $LIKES_COUNT_BEFORE"
echo "   totalLikes из toggle: $TOTAL_LIKES"
echo "   likes_count после: $LIKES_COUNT_AFTER"

if [ "$TOTAL_LIKES" -eq "$LIKES_COUNT_AFTER" ]; then
    echo "   ✅ Пересчет работает корректно! Значения совпадают."
else
    echo "   ⚠️  Значения не совпадают. Возможно, требуется повторный вызов toggle."
fi

echo "=========================================="
echo "Тестирование завершено"
echo ""
echo "💡 Для проверки в БД выполните:"
echo "   SELECT COUNT(*) FROM likes WHERE stickerset_id = $STICKER_SET_ID;"
echo "   SELECT likes_count FROM stickersets WHERE id = $STICKER_SET_ID;"
echo "   (Оба значения должны совпадать после вызова toggle)"

