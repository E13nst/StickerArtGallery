#!/bin/bash

# Тест валидации initData
BASE_URL="http://localhost:8080"

echo "🔍 Тестирование валидации initData"
echo "=================================="

# Тест 1: Проверка endpoint без заголовков (должен вернуть 401)
echo "1. Тест без заголовков (ожидаем 401):"
RESPONSE=$(curl -s -w "%{http_code}" -X PUT "$BASE_URL/api/likes/stickersets/22/toggle")
HTTP_CODE="${RESPONSE: -3}"
echo "   Код ответа: $HTTP_CODE"
if [ "$HTTP_CODE" = "401" ]; then
    echo "   ✅ Получена 401 ошибка (ожидаемо)"
else
    echo "   ❌ Неожиданный код: $HTTP_CODE"
fi

# Тест 2: Проверка с невалидным initData
echo "2. Тест с невалидным initData (ожидаем 401):"
RESPONSE=$(curl -s -w "%{http_code}" -X PUT "$BASE_URL/api/likes/stickersets/22/toggle" \
    -H "X-Telegram-Init-Data: invalid_data")
HTTP_CODE="${RESPONSE: -3}"
echo "   Код ответа: $HTTP_CODE"
if [ "$HTTP_CODE" = "401" ]; then
    echo "   ✅ Получена 401 ошибка (ожидаемо)"
else
    echo "   ❌ Неожиданный код: $HTTP_CODE"
fi

# Тест 3: Проверка с валидным initData (но возможно неверным hash)
echo "3. Тест с предоставленным initData:"
INIT_DATA="user=%7B%22id%22%3A141614461%2C%22first_name%22%3A%22Andrey%22%2C%22last_name%22%3A%22Mitroshin%22%2C%22username%22%3A%22E13nst%22%2C%22language_code%22%3A%22ru%22%2C%22is_premium%22%3Atrue%2C%22allows_write_to_pm%22%3Atrue%2C%22photo_url%22%3A%22https%3A%5C%2F%5C%2Ft.me%5C%2Fi%5C%2Fuserpic%5C%2F320%5C%2Ffnu0NcotQmWRV81w2TFvcn8Zc3Jph2aPIGpxz_gJMWY.svg%22%7D&chat_instance=3975449685032133164&chat_type=private&auth_date=1759439050&signature=xxtdT4jFgA-qNzdwSSNedTBiizhGEZLHLeuOACn3ZwCSYVOB_QDwGO2QUkREi-pUzLmigSJq04nuZQwmHVMoBw&hash=77d53012be453c206d8caacdcee51a117cd4f3a99a988e50bc3a8edc4fb0dd49"

RESPONSE=$(curl -s -w "%{http_code}" -X PUT "$BASE_URL/api/likes/stickersets/22/toggle" \
    -H "X-Telegram-Init-Data: $INIT_DATA")
HTTP_CODE="${RESPONSE: -3}"
echo "   Код ответа: $HTTP_CODE"

if [ "$HTTP_CODE" = "200" ]; then
    echo "   ✅ Успешно! Лайк переключен"
elif [ "$HTTP_CODE" = "400" ]; then
    echo "   ⚠️  Получена 400 ошибка - возможно проблема с hash или валидацией"
elif [ "$HTTP_CODE" = "401" ]; then
    echo "   ❌ Получена 401 ошибка - initData невалиден"
else
    echo "   ❓ Неожиданный код: $HTTP_CODE"
fi

echo "=================================="
echo "Тестирование завершено"
