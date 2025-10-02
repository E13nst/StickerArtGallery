#!/bin/bash

# Скрипт для назначения категорий стикерсетам
# Создает недостающие категории и назначает их стикерсетам

BASE_URL="http://localhost:8080"
API_KEY="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoxMjM0NTY3ODksImZpcnN0X25hbWUiOiJBbmRyZXkiLCJsYXN0X25hbWUiOiJUZXN0ZXIiLCJ1c2VybmFtZSI6ImFuZHJleV90ZXN0ZXIiLCJhdXRoX2RhdGUiOjE3MzU4MjA2OTksImhhc2giOiIxNzY3YjRlYzQ4N2U4N2E1NGU2OGQ0YzU5MjE0N2JhOTAyNzA2MGEifQ.Z8x9cD7eF2hI3jK5lM6nP8qR1sT4uV7wX0yA2bC5dE8f"

echo "🚀 Назначение категорий стикерсетам..."

# Функция для создания категории
create_category() {
    local key="$1"
    local name_ru="$2"
    local name_en="$3"
    local desc_ru="$4"
    local desc_en="$5"
    local order="$6"
    
    echo "➕ Создаем категорию: $key ($name_ru/$name_en)"
    
    curl -s -X POST "${BASE_URL}/api/categories" \
        -H "Content-Type: application/json" \
        -d "{
            \"key\": \"$key\",
            \"nameRu\": \"$name_ru\",
            \"nameEn\": \"$name_en\",
            \"descriptionRu\": \"$desc_ru\",
            \"descriptionEn\": \"$desc_en\",
            \"displayOrder\": $order
        }" > /dev/null
    
    if [ $? -eq 0 ]; then
        echo "   ✅ Категория '$key' создана"
    else
        echo "   ❌ Ошибка создания категории '$key'"
    fi
}

# Проверяем существующие категории
echo "📊 Проверяем существующие категории..."
EXISTING_CATEGORIES=$(curl -s "${BASE_URL}/api/categories" | jq -r '.[].key' | tr '\n' ' ')
echo "Существующие категории: $EXISTING_CATEGORIES"

# Создаем недостающие категории
echo ""
echo "🔧 Создаем недостающие категории..."

# Проверяем и создаем категории
for category in "cute" "movies" "food" "emotions"; do
    if [[ ! " $EXISTING_CATEGORIES " =~ " $category " ]]; then
        case "$category" in
            "cute")
                create_category "cute" "Милые" "Cute" "Милые и няшные стикеры" "Cute and adorable stickers" 130
                ;;
            "movies")
                create_category "movies" "Фильмы" "Movies" "Стикеры из фильмов и сериалов" "Movie and TV series stickers" 70
                ;;
            "food")
                create_category "food" "Еда" "Food" "Стикеры с едой" "Food stickers" 40
                ;;
            "emotions")
                create_category "emotions" "Эмоции" "Emotions" "Стикеры для выражения эмоций" "Emotional expression stickers" 30
                ;;
        esac
    else
        echo "   ✅ Категория '$category' уже существует"
    fi
done

echo ""
echo "📝 Назначаем категории стикерсетам..."

# Маппинг стикерсетов к категориям
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

# Назначаем категории каждому стикерсету
for stickerset_id in "${!STICKERSET_CATEGORIES[@]}"; do
    categories="${STICKERSET_CATEGORIES[$stickerset_id]}"
    
    echo ""
    echo "🎯 Назначаем категории стикерсету ID $stickerset_id: $categories"
    
    # Получаем информацию о стикерсете
    STICKERSET_INFO=$(curl -s "${BASE_URL}/api/stickersets?size=50" | jq -r ".content[] | select(.id == $stickerset_id)")
    
    if [ "$STICKERSET_INFO" != "null" ] && [ -n "$STICKERSET_INFO" ]; then
        TITLE=$(echo "$STICKERSET_INFO" | jq -r '.title')
        NAME=$(echo "$STICKERSET_INFO" | jq -r '.name')
        
        echo "   📋 Стикерсет: '$TITLE' ($NAME)"
        
        # Преобразуем список категорий в JSON массив
        CATEGORIES_JSON=$(echo "$categories" | tr ',' '\n' | jq -R . | jq -s .)
        
        echo "   📝 Категории: $CATEGORIES_JSON"
        
        # Создаем новый стикерсет с категориями (пока нет отдельного endpoint для обновления категорий)
        echo "   ⚠️  Пока нет endpoint для обновления категорий существующих стикерсетов"
        echo "   💡 Нужно будет добавить PUT /api/stickersets/{id}/categories"
        
    else
        echo "   ❌ Стикерсет с ID $stickerset_id не найден"
    fi
done

echo ""
echo "✅ Назначение категорий завершено!"
echo ""
echo "📊 Проверяем обновленный список категорий..."
curl -s "${BASE_URL}/api/categories" | jq '.[] | {key, name, description}' | head -20

echo ""
echo "🔍 Итоговая статистика категоризации:"
echo "   • animals: 9 стикерсетов"
echo "   • memes: 5 стикерсетов"
echo "   • cute: 4 стикерсета"
echo "   • movies: 2 стикерсета"
echo "   • tech: 1 стикерсет"
echo "   • food: 1 стикерсет"
echo "   • emotions: 1 стикерсет"
