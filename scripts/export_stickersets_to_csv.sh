#!/bin/bash

# Скрипт для экспорта стикерсетов в CSV файл

BASE_URL="http://localhost:8080"
OUTPUT_FILE="stickersets_export.csv"

echo "🚀 Экспорт стикерсетов в CSV..."

# Получаем все стикерсеты
echo "📊 Получаем список стикерсетов..."
STICKERSETS_RESPONSE=$(curl -s "${BASE_URL}/api/stickersets?size=1000")

# Проверяем успешность запроса
if [ $? -ne 0 ]; then
    echo "❌ Ошибка при получении данных с сервера"
    exit 1
fi

# Создаем CSV файл с заголовками
echo "id,title,name" > "$OUTPUT_FILE"

# Извлекаем данные и записываем в CSV
echo "$STICKERSETS_RESPONSE" | jq -r '.content[] | [.id, .title, .name] | @csv' >> "$OUTPUT_FILE"

# Подсчитываем количество записей
RECORD_COUNT=$(tail -n +2 "$OUTPUT_FILE" | wc -l | tr -d ' ')

echo "✅ Экспорт завершен!"
echo "📁 Файл: $OUTPUT_FILE"
echo "📊 Записей: $RECORD_COUNT"

# Показываем первые несколько строк для проверки
echo ""
echo "📋 Первые 10 записей:"
head -11 "$OUTPUT_FILE"

echo ""
echo "🔍 Полный список для анализа:"
cat "$OUTPUT_FILE"
