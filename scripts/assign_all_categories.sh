#!/bin/bash

# Финальный скрипт для назначения категорий всем стикерсетам

BASE_URL="http://localhost:8080"

echo "🚀 Назначение категорий всем стикерсетам..."

# Маппинг стикерсетов к категориям (ID -> категории)
declare -A STICKERSET_CATEGORIES

STICKERSET_CATEGORIES[21]="animals,memes"        # Spidermeme
STICKERSET_CATEGORIES[20]="memes"                # Classic Memes  
STICKERSET_CATEGORIES[19]="animals"              # Just zoo it!
STICKERSET_CATEGORIES[18]="animals,memes"        # Shaitan Chick
STICKERSET_CATEGORIES[17]="animals,memes"        # Shaitan Chick
STICKERSET_CATEGORIES[15]="tech"                 # Home Electronics
STICKERSET_CATEGORIES[13]="memes"                # Flog Sticks
STICKERSET_CATEGORIES[9]="animals,cute"          # Little Catto
STICKERSET_CATEGORIES[1]="animals"               # Resistance Dog
STICKERSET_CATEGORIES[8]="animals,cute"          # Chummy Alien
STICKERSET_CATEGORIES[4]="memes"                 # People Memes
STICKERSET_CATEGORIES[2]="movies"                # Butler Alfred
STICKERSET_CATEGORIES[7]="animals,cute,emotions" # Froggo In Love
STICKERSET_CATEGORIES[6]="movies"                # Rick and Morty
STICKERSET_CATEGORIES[5]="food,cute"             # Hot Cherry
STICKERSET_CATEGORIES[3]="animals"               # Duck

# Функция для обновления категорий стикерсета
update_stickerset_categories() {
    local stickerset_id="$1"
    local categories="$2"
    
    echo "🎯 Обновляем категории стикерсета ID $stickerset_id: $categories"
    
    # Преобразуем список категорий в JSON массив
    local categories_json=$(echo "$categories" | tr ',' '\n' | jq -R . | jq -s .)
    
    # Отправляем запрос на обновление
    local response=$(curl -s -X PUT "${BASE_URL}/api/stickersets/${stickerset_id}/categories" \
        -H "Content-Type: application/json" \
        -d "$categories_json")
    
    # Проверяем успешность
    if echo "$response" | jq -e '.id' > /dev/null 2>&1; then
        local title=$(echo "$response" | jq -r '.title')
        local category_count=$(echo "$response" | jq '.categories | length')
        echo "   ✅ '$title' -> $category_count категорий"
    else
        echo "   ❌ Ошибка обновления стикерсета $stickerset_id"
        echo "   📄 Ответ: $response"
    fi
}

# Обновляем категории для каждого стикерсета
for stickerset_id in "${!STICKERSET_CATEGORIES[@]}"; do
    categories="${STICKERSET_CATEGORIES[$stickerset_id]}"
    update_stickerset_categories "$stickerset_id" "$categories"
    echo ""
done

echo "✅ Назначение категорий завершено!"
echo ""
echo "📊 Проверяем результаты..."

# Получаем обновленный список стикерсетов с категориями
curl -s "${BASE_URL}/api/stickersets?size=50" | jq '.content[] | {id, title, categories: [.categories[]?.key]}' | head -20
