#!/bin/bash

# Тестирование лайков с валидным initData
BASE_URL="http://localhost:8080"
STICKER_SET_ID=22

# Новый валидный initData
INIT_DATA="user=%7B%22id%22%3A141614461%2C%22first_name%22%3A%22Andrey%22%2C%22last_name%22%3A%22Mitroshin%22%2C%22username%22%3A%22E13nst%22%2C%22language_code%22%3A%22ru%22%2C%22is_premium%22%3Atrue%2C%22allows_write_to_pm%22%3Atrue%2C%22photo_url%22%3A%22https%3A%5C%2F%5C%2Ft.me%5C%2Fi%5C%2Fuserpic%5C%2F320%5C%2Ffnu0NcotQmWRV81w2TFvcn8Zc3Jph2aPIGpxz_gJMWY.svg%22%7D&chat_instance=3975449685032133164&chat_type=private&auth_date=1759439050&signature=xxtdT4jFgA-qNzdwSSNedTBiizhGEZLHLeuOACn3ZwCSYVOB_QDwGO2QUkREi-pUzLmigSJq04nuZQwmHVMoBw&hash=77d53012be453c206d8caacdcee51a117cd4f3a99a988e50bc3a8edc4fb0dd49"
BOT_NAME="StickerGallery"

echo "🧪 Тестирование лайков с валидным initData"
echo "=========================================="

# 1. Проверим текущее количество лайков
echo "1. Текущее количество лайков:"
CURRENT_LIKES=$(curl -s "$BASE_URL/api/likes/top-stickersets?size=1" | jq -r '.content[0].likesCount')
echo "   Стикерсет ID $STICKER_SET_ID имеет $CURRENT_LIKES лайков"

# 2. Тест с валидным initData
echo "2. Тест с валидным initData..."
echo "   Отправляем PUT запрос на /api/likes/stickersets/$STICKER_SET_ID/toggle"

RESPONSE=$(curl -s -X PUT "$BASE_URL/api/likes/stickersets/$STICKER_SET_ID/toggle" \
    -H "Content-Type: application/json" \
    -H "X-Telegram-Init-Data: $INIT_DATA" \
    -H "X-Telegram-Bot-Name: $BOT_NAME" \
    -w "%{http_code}")

HTTP_CODE="${RESPONSE: -3}"
BODY="${RESPONSE%???}"

echo "   Код ответа: $HTTP_CODE"

if [ "$HTTP_CODE" -eq 200 ]; then
    echo "   ✅ Лайк успешно переключен!"
    echo "   Тело ответа: $BODY"
    
    # 3. Проверим новое количество лайков
    echo "3. Проверяем новое количество лайков..."
    NEW_LIKES=$(curl -s "$BASE_URL/api/likes/top-stickersets?size=1" | jq -r '.content[0].likesCount')
    echo "   Стикерсет ID $STICKER_SET_ID теперь имеет $NEW_LIKES лайков"
    
    if [ "$NEW_LIKES" -ne "$CURRENT_LIKES" ]; then
        echo "   ✅ Количество лайков изменилось с $CURRENT_LIKES на $NEW_LIKES"
    else
        echo "   ⚠️  Количество лайков не изменилось (возможно, лайк уже был поставлен)"
    fi
    
elif [ "$HTTP_CODE" -eq 400 ]; then
    echo "   ⚠️  Получена 400 ошибка (Bad Request)"
    echo "   Тело ответа: $BODY"
elif [ "$HTTP_CODE" -eq 401 ]; then
    echo "   ❌ Получена 401 ошибка (Не авторизован)"
    echo "   Тело ответа: $BODY"
elif [ "$HTTP_CODE" -eq 500 ]; then
    echo "   ❌ Получена 500 ошибка (Внутренняя ошибка сервера)"
    echo "   Тело ответа: $BODY"
else
    echo "   ❌ Неожиданный код ответа: $HTTP_CODE"
    echo "   Тело ответа: $BODY"
fi

echo "=========================================="
echo "Тестирование завершено"
