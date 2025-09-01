#!/bin/bash

# Скрипт для проверки webhook Telegram бота
# Автоматически загружает токен из .env.app

echo "🔍 Проверка webhook Telegram бота..."
echo "=================================="

# Проверяем наличие файла .env.app
if [ ! -f ".env.app" ]; then
    echo "❌ Файл .env.app не найден!"
    echo "Создайте файл .env.app с переменной TELEGRAM_API_TOKEN"
    exit 1
fi

# Загружаем переменные из .env.app
source .env.app

# Проверяем наличие токена
if [ -z "$TELEGRAM_API_TOKEN" ]; then
    echo "❌ TELEGRAM_API_TOKEN не найден в .env.app"
    exit 1
fi

echo "✅ Токен загружен из .env.app"
echo "🤖 Проверяем информацию о webhook..."

# Получаем информацию о webhook
echo ""
echo "📡 Запрос к Telegram API..."
WEBHOOK_INFO=$(curl -s "https://api.telegram.org/bot${TELEGRAM_API_TOKEN}/getWebhookInfo")

# Проверяем успешность запроса
if [ $? -ne 0 ]; then
    echo "❌ Ошибка при запросе к Telegram API"
    exit 1
fi

# Выводим результат в читаемом виде
echo ""
echo "📋 Информация о webhook:"
echo "========================"
echo "$WEBHOOK_INFO" | python3 -m json.tool 2>/dev/null || echo "$WEBHOOK_INFO"

# Проверяем, установлен ли webhook
WEBHOOK_URL=$(echo "$WEBHOOK_INFO" | grep -o '"url":"[^"]*"' | cut -d'"' -f4)

if [ -n "$WEBHOOK_URL" ]; then
    echo ""
    echo "✅ Webhook установлен: $WEBHOOK_URL"
    
    # Проверяем доступность webhook URL
    echo ""
    echo "🌐 Проверяем доступность webhook URL..."
    WEBHOOK_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$WEBHOOK_URL")
    
    if [ "$WEBHOOK_RESPONSE" = "405" ]; then
        echo "✅ Webhook endpoint доступен (405 - Method Not Allowed, это нормально для GET)"
    elif [ "$WEBHOOK_RESPONSE" = "200" ]; then
        echo "✅ Webhook endpoint доступен (200 - OK)"
    else
        echo "⚠️ Webhook endpoint вернул код: $WEBHOOK_RESPONSE"
    fi
else
    echo ""
    echo "❌ Webhook не установлен"
    echo ""
    echo "💡 Для установки webhook выполните:"
    echo "curl -X POST \"https://api.telegram.org/bot${TELEGRAM_API_TOKEN}/setWebhook\" \\"
    echo "  -H \"Content-Type: application/json\" \\"
    echo "  -d '{\"url\": \"https://dalek-e13nst.amvera.io/webhook\"}'"
fi

echo ""
echo "🔍 Дополнительная диагностика:"
echo "=============================="

# Проверяем информацию о боте
echo ""
echo "🤖 Информация о боте:"
BOT_INFO=$(curl -s "https://api.telegram.org/bot${TELEGRAM_API_TOKEN}/getMe")
echo "$BOT_INFO" | python3 -m json.tool 2>/dev/null || echo "$BOT_INFO"

echo ""
echo "✅ Диагностика завершена!" 