#!/bin/bash

# Скрипт для включения webhook Telegram бота
# Автоматически загружает токен из .env.app

echo "🌐 Включение webhook для Telegram бота..."
echo "========================================"

# Проверяем наличие файла .env.app
if [ ! -f ".env.app" ]; then
    echo "❌ Файл .env.app не найден!"
    exit 1
fi

# Загружаем переменные из .env.app
source .env.app

# Проверяем наличие токена
if [ -z "$TELEGRAM_API_TOKEN" ]; then
    echo "❌ TELEGRAM_API_TOKEN не найден в .env.app"
    exit 1
fi

# URL webhook
WEBHOOK_URL="https://dalek-e13nst.amvera.io/webhook"

echo "✅ Токен загружен из .env.app"
echo "🌐 Включаем webhook: $WEBHOOK_URL"

# Включаем webhook
echo ""
echo "📡 Отправляем запрос к Telegram API..."
RESPONSE=$(curl -s -X POST "https://api.telegram.org/bot${TELEGRAM_API_TOKEN}/setWebhook" \
  -H "Content-Type: application/json" \
  -d "{\"url\": \"$WEBHOOK_URL\"}")

# Проверяем результат
if [ $? -eq 0 ]; then
    echo ""
    echo "📋 Ответ от Telegram API:"
    echo "========================"
    echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
    
    # Проверяем успешность
    if echo "$RESPONSE" | grep -q '"ok":true'; then
        echo ""
        echo "✅ Webhook успешно включен!"
    else
        echo ""
        echo "❌ Ошибка при включении webhook"
    fi
else
    echo "❌ Ошибка при запросе к Telegram API"
fi

echo ""
echo "🔍 Проверяем результат..."
./check_webhook.sh 