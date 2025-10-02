#!/bin/bash

# Скрипт для автоматической категоризации стикерсетов
# Анализирует названия стикерсетов и назначает соответствующие категории

BASE_URL="http://localhost:8080"
API_KEY="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoxMjM0NTY3ODksImZpcnN0X25hbWUiOiJBbmRyZXkiLCJsYXN0X25hbWUiOiJUZXN0ZXIiLCJ1c2VybmFtZSI6ImFuZHJleV90ZXN0ZXIiLCJhdXRoX2RhdGUiOjE3MzU4MjA2OTksImhhc2giOiIxNzY3YjRlYzQ4N2U4N2E1NGU2OGQ0YzU5MjE0N2JhOTAyNzA2MGEifQ.Z8x9cD7eF2hI3jK5lM6nP8qR1sT4uV7wX0yA2bC5dE8f"

echo "🚀 Запуск автоматической категоризации стикерсетов..."

# Получаем все стикерсеты
echo "📊 Получаем список стикерсетов..."
STICKERSETS_RESPONSE=$(curl -s "${BASE_URL}/api/stickersets?size=1000")
echo "$STICKERSETS_RESPONSE" | jq '.content[] | {id, title, name}' > /tmp/stickersets.json

# Получаем все категории
echo "📊 Получаем список категорий..."
CATEGORIES_RESPONSE=$(curl -s "${BASE_URL}/api/categories")
echo "$CATEGORIES_RESPONSE" > /tmp/categories.json

echo "📊 Найдено стикерсетов: $(echo "$STICKERSETS_RESPONSE" | jq '.content | length')"
echo "📊 Существующих категорий: $(echo "$CATEGORIES_RESPONSE" | jq '. | length')"

# Анализируем каждый стикерсет
echo ""
echo "🔍 Анализируем стикерсеты..."

# Функция для анализа названия и предложения категорий
analyze_title() {
    local title="$1"
    local title_lower=$(echo "$title" | tr '[:upper:]' '[:lower:]')
    local categories=""
    
    # Проверяем ключевые слова для разных категорий
    
    # Животные
    if [[ "$title_lower" =~ (animal|cat|dog|bird|fish|bear|lion|tiger|elephant|monkey|rabbit|fox|wolf|panda|koala|penguin|dolphin|spider|zoo|pet|puppy|kitten|duck|chick|cow|pig|horse|sheep|goat|hamster|mouse|squirrel|owl|eagle) ]]; then
        categories="${categories}animals,"
    fi
    
    # Мемы
    if [[ "$title_lower" =~ (meme|funny|lol|lmao|rofl|joke|humor|comedy|dank|classic|viral|trending|epic|legendary|iconic) ]]; then
        categories="${categories}memes,"
    fi
    
    # Эмоции
    if [[ "$title_lower" =~ (emotion|feeling|mood|happy|sad|angry|love|hate|excited|surprised|confused|worried|proud|jealous|heart|crying|laughing|smiling|frowning) ]]; then
        categories="${categories}emotions,"
    fi
    
    # Еда
    if [[ "$title_lower" =~ (food|eat|drink|pizza|burger|cake|cookie|coffee|tea|bread|meat|fruit|vegetable|sweet|salty|spicy|delicious|hungry|thirsty|dinner|breakfast|lunch|snack) ]]; then
        categories="${categories}food,"
    fi
    
    # Путешествия
    if [[ "$title_lower" =~ (travel|trip|vacation|holiday|journey|adventure|explore|beach|mountain|city|country|plane|train|car|hotel|passport|luggage|backpack|map|compass|camera) ]]; then
        categories="${categories}travel,"
    fi
    
    # Спорт
    if [[ "$title_lower" =~ (sport|football|soccer|basketball|tennis|swimming|running|gym|fitness|workout|training|match|game|player|team|champion|victory|defeat|score|goal|ball) ]]; then
        categories="${categories}sport,"
    fi
    
    # Фильмы
    if [[ "$title_lower" =~ (movie|film|cinema|actor|actress|director|producer|star|hollywood|oscar|award|premiere|trailer|sequel|franchise|superhero|villain|hero|action|comedy|drama|horror) ]]; then
        categories="${categories}movies,"
    fi
    
    # Музыка
    if [[ "$title_lower" =~ (music|song|band|singer|musician|guitar|piano|drum|concert|album|lyrics|melody|rhythm|rock|pop|jazz|classical|electronic|hip hop|rap|dance|party) ]]; then
        categories="${categories}music,"
    fi
    
    # Игры
    if [[ "$title_lower" =~ (game|gaming|player|level|score|quest|adventure|rpg|strategy|action|puzzle|arcade|console|pc|mobile|multiplayer|online|tournament|champion|boss|enemy) ]]; then
        categories="${categories}games,"
    fi
    
    # Искусство
    if [[ "$title_lower" =~ (art|artist|painting|drawing|sketch|design|creative|beautiful|masterpiece|gallery|museum|sculpture|canvas|brush|color|paint|illustration|graphic|digital) ]]; then
        categories="${categories}art,"
    fi
    
    # Природа
    if [[ "$title_lower" =~ (nature|forest|tree|flower|garden|plant|leaf|grass|sun|moon|star|sky|cloud|rain|snow|wind|fire|earth|water|ocean|river|lake|mountain|valley) ]]; then
        categories="${categories}nature,"
    fi
    
    # Праздники
    if [[ "$title_lower" =~ (holiday|celebration|party|festival|christmas|new year|birthday|wedding|anniversary|graduation|thanksgiving|easter|valentine|halloween|independence|national|special) ]]; then
        categories="${categories}holidays,"
    fi
    
    # Милые
    if [[ "$title_lower" =~ (cute|adorable|sweet|lovely|pretty|beautiful|kawaii|chibi|baby|little|tiny|mini|soft|fluffy|cuddly) ]]; then
        categories="${categories}cute,"
    fi
    
    # Смешные
    if [[ "$title_lower" =~ (funny|hilarious|comedy|joke|laugh|smile|humor|comic|silly|crazy|wacky|absurd|ridiculous|amusing|entertaining) ]]; then
        categories="${categories}funny,"
    fi
    
    # Технологии
    if [[ "$title_lower" =~ (tech|technology|computer|phone|internet|digital|cyber|robot|ai|artificial intelligence|coding|programming|software|hardware|gadget|device|app|website|online|virtual) ]]; then
        categories="${categories}tech,"
    fi
    
    # Убираем последнюю запятую
    echo "${categories%,}"
}

# Обрабатываем каждый стикерсет
echo "$STICKERSETS_RESPONSE" | jq -r '.content[] | "\(.id)|\(.title)|\(.name)"' | while IFS='|' read -r id title name; do
    echo ""
    echo "🔍 Анализируем: '$title' (ID: $id)"
    
    # Анализируем название
    suggested_categories=$(analyze_title "$title")
    
    if [ -n "$suggested_categories" ]; then
        echo "   💡 Предложенные категории: $suggested_categories"
        
        # Проверяем, нужно ли создать новые категории
        IFS=',' read -ra CATS <<< "$suggested_categories"
        for category in "${CATS[@]}"; do
            # Проверяем, существует ли категория
            exists=$(echo "$CATEGORIES_RESPONSE" | jq -r --arg key "$category" '.[] | select(.key == $key) | .key')
            if [ -z "$exists" ]; then
                echo "   ➕ Создаем новую категорию: $category"
                
                # Создаем категорию с базовыми данными
                case "$category" in
                    "animals")
                        name_ru="Животные"; name_en="Animals"; desc_ru="Стикеры с животными"; desc_en="Stickers with animals"
                        ;;
                    "memes")
                        name_ru="Мемы"; name_en="Memes"; desc_ru="Популярные мемы"; desc_en="Popular memes"
                        ;;
                    "emotions")
                        name_ru="Эмоции"; name_en="Emotions"; desc_ru="Выражение эмоций"; desc_en="Emotional expressions"
                        ;;
                    "food")
                        name_ru="Еда"; name_en="Food"; desc_ru="Стикеры с едой"; desc_en="Food stickers"
                        ;;
                    "travel")
                        name_ru="Путешествия"; name_en="Travel"; desc_ru="Стикеры о путешествиях"; desc_en="Travel stickers"
                        ;;
                    "sport")
                        name_ru="Спорт"; name_en="Sport"; desc_ru="Спортивные стикеры"; desc_en="Sport stickers"
                        ;;
                    "movies")
                        name_ru="Фильмы"; name_en="Movies"; desc_ru="Стикеры из фильмов"; desc_en="Movie stickers"
                        ;;
                    "music")
                        name_ru="Музыка"; name_en="Music"; desc_ru="Музыкальные стикеры"; desc_en="Music stickers"
                        ;;
                    "games")
                        name_ru="Игры"; name_en="Games"; desc_ru="Игровые стикеры"; desc_en="Gaming stickers"
                        ;;
                    "art")
                        name_ru="Искусство"; name_en="Art"; desc_ru="Художественные стикеры"; desc_en="Artistic stickers"
                        ;;
                    "nature")
                        name_ru="Природа"; name_en="Nature"; desc_ru="Стикеры с природой"; desc_en="Nature stickers"
                        ;;
                    "holidays")
                        name_ru="Праздники"; name_en="Holidays"; desc_ru="Праздничные стикеры"; desc_en="Holiday stickers"
                        ;;
                    "cute")
                        name_ru="Милые"; name_en="Cute"; desc_ru="Милые и няшные стикеры"; desc_en="Cute and adorable stickers"
                        ;;
                    "funny")
                        name_ru="Смешные"; name_en="Funny"; desc_ru="Очень смешные стикеры"; desc_en="Very funny stickers"
                        ;;
                    "tech")
                        name_ru="Технологии"; name_en="Tech"; desc_ru="Стикеры про технологии"; desc_en="Tech stickers"
                        ;;
                    *)
                        name_ru="$category"; name_en="$category"; desc_ru="Категория $category"; desc_en="Category $category"
                        ;;
                esac
                
                # Создаем категорию через API
                curl -s -X POST "${BASE_URL}/api/categories" \
                    -H "Content-Type: application/json" \
                    -d "{
                        \"key\": \"$category\",
                        \"nameRu\": \"$name_ru\",
                        \"nameEn\": \"$name_en\",
                        \"descriptionRu\": \"$desc_ru\",
                        \"descriptionEn\": \"$desc_en\",
                        \"displayOrder\": 1000
                    }" > /dev/null
                
                echo "   ✅ Категория '$category' создана"
            else
                echo "   ✅ Категория '$category' уже существует"
            fi
        done
        
        echo "   📝 Стикерсет $id -> категории: $suggested_categories"
    else
        echo "   ❓ Категории не определены"
    fi
done

echo ""
echo "✅ Автоматическая категоризация завершена!"
echo ""
echo "📊 Проверяем обновленный список категорий..."
curl -s "${BASE_URL}/api/categories" | jq '.[] | {key, name, description}' | head -20
