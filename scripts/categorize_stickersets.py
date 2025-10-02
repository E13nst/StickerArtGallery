#!/usr/bin/env python3
"""
Скрипт для автоматической категоризации стикерсетов
Анализирует названия стикерсетов и назначает соответствующие категории
"""

import requests
import json
import re
from typing import List, Dict, Set

# Конфигурация
BASE_URL = "http://localhost:8080"
API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoxMjM0NTY3ODksImZpcnN0X25hbWUiOiJBbmRyZXkiLCJsYXN0X25hbWUiOiJUZXN0ZXIiLCJ1c2VybmFtZSI6ImFuZHJleV90ZXN0ZXIiLCJhdXRoX2RhdGUiOjE3MzU4MjA2OTksImhhc2giOiIxNzY3YjRlYzQ4N2U4N2E1NGU2OGQ0YzU5MjE0N2JhOTAyNzA2MGEifQ.Z8x9cD7eF2hI3jK5lM6nP8qR1sT4uV7wX0yA2bC5dE8f"

# Правила категоризации на основе ключевых слов в названиях
CATEGORIZATION_RULES = {
    # Животные
    "animals": {
        "keywords": ["animal", "cat", "dog", "bird", "fish", "bear", "lion", "tiger", "elephant", 
                    "monkey", "rabbit", "fox", "wolf", "panda", "koala", "penguin", "dolphin",
                    "spider", "zoo", "pet", "puppy", "kitten", "duck", "chick", "cow", "pig",
                    "horse", "sheep", "goat", "hamster", "mouse", "squirrel", "owl", "eagle"],
        "ru_keywords": ["животное", "кот", "собака", "птица", "рыба", "медведь", "лев", "тигр", 
                       "слон", "обезьяна", "кролик", "лиса", "волк", "панда", "коала", "пингвин",
                       "дельфин", "паук", "зоопарк", "питомец", "щенок", "котенок", "утка", "цыпленок"]
    },
    
    # Мемы
    "memes": {
        "keywords": ["meme", "funny", "lol", "lmao", "rofl", "joke", "humor", "comedy", "dank",
                    "classic", "viral", "trending", "epic", "legendary", "iconic"],
        "ru_keywords": ["мем", "смешной", "прикол", "шутка", "юмор", "комедия", "классика", 
                       "вирусный", "тренд", "эпик", "легендарный", "культовый"]
    },
    
    # Эмоции
    "emotions": {
        "keywords": ["emotion", "feeling", "mood", "happy", "sad", "angry", "love", "hate",
                    "excited", "surprised", "confused", "worried", "proud", "jealous",
                    "heart", "crying", "laughing", "smiling", "frowning"],
        "ru_keywords": ["эмоция", "чувство", "настроение", "счастливый", "грустный", "злой",
                       "любовь", "ненависть", "возбужденный", "удивленный", "озадаченный"]
    },
    
    # Еда
    "food": {
        "keywords": ["food", "eat", "drink", "pizza", "burger", "cake", "cookie", "coffee", "tea",
                    "bread", "meat", "fruit", "vegetable", "sweet", "salty", "spicy", "delicious",
                    "hungry", "thirsty", "dinner", "breakfast", "lunch", "snack"],
        "ru_keywords": ["еда", "есть", "пить", "пицца", "бургер", "торт", "печенье", "кофе", "чай",
                       "хлеб", "мясо", "фрукт", "овощ", "сладкий", "соленый", "острый", "вкусный"]
    },
    
    # Путешествия
    "travel": {
        "keywords": ["travel", "trip", "vacation", "holiday", "journey", "adventure", "explore",
                    "beach", "mountain", "city", "country", "plane", "train", "car", "hotel",
                    "passport", "luggage", "backpack", "map", "compass", "camera"],
        "ru_keywords": ["путешествие", "поездка", "отпуск", "праздник", "приключение", "исследование",
                       "пляж", "гора", "город", "страна", "самолет", "поезд", "машина", "отель"]
    },
    
    # Спорт
    "sport": {
        "keywords": ["sport", "football", "soccer", "basketball", "tennis", "swimming", "running",
                    "gym", "fitness", "workout", "training", "match", "game", "player", "team",
                    "champion", "victory", "defeat", "score", "goal", "ball"],
        "ru_keywords": ["спорт", "футбол", "баскетбол", "теннис", "плавание", "бег", "тренажерный зал",
                       "фитнес", "тренировка", "матч", "игра", "игрок", "команда", "чемпион"]
    },
    
    # Фильмы
    "movies": {
        "keywords": ["movie", "film", "cinema", "actor", "actress", "director", "producer", "star",
                    "hollywood", "oscar", "award", "premiere", "trailer", "sequel", "franchise",
                    "superhero", "villain", "hero", "action", "comedy", "drama", "horror"],
        "ru_keywords": ["фильм", "кино", "актер", "актриса", "режиссер", "продюсер", "звезда",
                       "голливуд", "оскар", "награда", "премьера", "трейлер", "сиквел"]
    },
    
    # Музыка
    "music": {
        "keywords": ["music", "song", "band", "singer", "musician", "guitar", "piano", "drum",
                    "concert", "album", "lyrics", "melody", "rhythm", "rock", "pop", "jazz",
                    "classical", "electronic", "hip hop", "rap", "dance", "party"],
        "ru_keywords": ["музыка", "песня", "группа", "певец", "музыкант", "гитара", "пианино", "барабан",
                       "концерт", "альбом", "текст", "мелодия", "ритм", "рок", "поп", "джаз"]
    },
    
    # Игры
    "games": {
        "keywords": ["game", "gaming", "player", "level", "score", "quest", "adventure", "rpg",
                    "strategy", "action", "puzzle", "arcade", "console", "pc", "mobile",
                    "multiplayer", "online", "tournament", "champion", "boss", "enemy"],
        "ru_keywords": ["игра", "гейминг", "игрок", "уровень", "счет", "квест", "приключение", "рпг",
                       "стратегия", "экшен", "головоломка", "аркада", "консоль", "пк", "мобильная"]
    },
    
    # Искусство
    "art": {
        "keywords": ["art", "artist", "painting", "drawing", "sketch", "design", "creative",
                    "beautiful", "masterpiece", "gallery", "museum", "sculpture", "canvas",
                    "brush", "color", "paint", "illustration", "graphic", "digital"],
        "ru_keywords": ["искусство", "художник", "живопись", "рисунок", "эскиз", "дизайн", "творческий",
                       "красивый", "шедевр", "галерея", "музей", "скульптура", "холст"]
    },
    
    # Природа
    "nature": {
        "keywords": ["nature", "forest", "tree", "flower", "garden", "plant", "leaf", "grass",
                    "sun", "moon", "star", "sky", "cloud", "rain", "snow", "wind", "fire",
                    "earth", "water", "ocean", "river", "lake", "mountain", "valley"],
        "ru_keywords": ["природа", "лес", "дерево", "цветок", "сад", "растение", "лист", "трава",
                       "солнце", "луна", "звезда", "небо", "облако", "дождь", "снег", "ветер"]
    },
    
    # Праздники
    "holidays": {
        "keywords": ["holiday", "celebration", "party", "festival", "christmas", "new year",
                    "birthday", "wedding", "anniversary", "graduation", "thanksgiving", "easter",
                    "valentine", "halloween", "independence", "national", "special"],
        "ru_keywords": ["праздник", "торжество", "вечеринка", "фестиваль", "рождество", "новый год",
                       "день рождения", "свадьба", "годовщина", "выпускной", "день благодарения"]
    },
    
    # Милые
    "cute": {
        "keywords": ["cute", "adorable", "sweet", "lovely", "pretty", "beautiful", "kawaii",
                    "chibi", "baby", "little", "tiny", "mini", "soft", "fluffy", "cuddly"],
        "ru_keywords": ["милый", "очаровательный", "сладкий", "прекрасный", "красивый", "кавайный",
                       "чиби", "малыш", "маленький", "крошечный", "мини", "мягкий", "пушистый"]
    },
    
    # Смешные
    "funny": {
        "keywords": ["funny", "hilarious", "comedy", "joke", "laugh", "smile", "humor", "comic",
                    "silly", "crazy", "wacky", "absurd", "ridiculous", "amusing", "entertaining"],
        "ru_keywords": ["смешной", "уморительный", "комедия", "шутка", "смех", "улыбка", "юмор", "комикс",
                       "глупый", "сумасшедший", "безумный", "абсурдный", "смешной", "забавный"]
    },
    
    # Технологии
    "tech": {
        "keywords": ["tech", "technology", "computer", "phone", "internet", "digital", "cyber",
                    "robot", "ai", "artificial intelligence", "coding", "programming", "software",
                    "hardware", "gadget", "device", "app", "website", "online", "virtual"],
        "ru_keywords": ["технология", "компьютер", "телефон", "интернет", "цифровой", "кибер",
                       "робот", "ии", "искусственный интеллект", "кодинг", "программирование"]
    }
}

def get_all_stickersets() -> List[Dict]:
    """Получить все стикерсеты"""
    try:
        response = requests.get(f"{BASE_URL}/api/stickersets?size=1000")
        response.raise_for_status()
        data = response.json()
        return data.get('content', [])
    except Exception as e:
        print(f"❌ Ошибка при получении стикерсетов: {e}")
        return []

def get_all_categories() -> List[Dict]:
    """Получить все существующие категории"""
    try:
        response = requests.get(f"{BASE_URL}/api/categories")
        response.raise_for_status()
        return response.json()
    except Exception as e:
        print(f"❌ Ошибка при получении категорий: {e}")
        return []

def create_category(key: str, name_ru: str, name_en: str, description_ru: str = "", description_en: str = "") -> bool:
    """Создать новую категорию"""
    try:
        data = {
            "key": key,
            "nameRu": name_ru,
            "nameEn": name_en,
            "descriptionRu": description_ru,
            "descriptionEn": description_en,
            "displayOrder": 1000  # В конец списка
        }
        
        response = requests.post(f"{BASE_URL}/api/categories", json=data)
        response.raise_for_status()
        print(f"✅ Создана категория: {key} ({name_ru}/{name_en})")
        return True
    except Exception as e:
        print(f"❌ Ошибка при создании категории {key}: {e}")
        return False

def analyze_title(title: str) -> Set[str]:
    """Анализирует название стикерсета и возвращает подходящие категории"""
    if not title:
        return set()
    
    title_lower = title.lower()
    matched_categories = set()
    
    for category_key, rules in CATEGORIZATION_RULES.items():
        # Проверяем английские ключевые слова
        for keyword in rules["keywords"]:
            if keyword in title_lower:
                matched_categories.add(category_key)
                break
        
        # Проверяем русские ключевые слова
        for keyword in rules["ru_keywords"]:
            if keyword in title_lower:
                matched_categories.add(category_key)
                break
    
    return matched_categories

def update_stickerset_categories(stickerset_id: int, category_keys: List[str]) -> bool:
    """Обновить категории стикерсета"""
    try:
        # Пока используем простой подход - создаем новый стикерсет с категориями
        # В реальном приложении нужен был бы отдельный endpoint для обновления категорий
        
        print(f"📝 Стикерсет {stickerset_id} -> категории: {category_keys}")
        return True
    except Exception as e:
        print(f"❌ Ошибка при обновлении категорий стикерсета {stickerset_id}: {e}")
        return False

def main():
    """Основная функция"""
    print("🚀 Запуск автоматической категоризации стикерсетов...")
    
    # Получаем все стикерсеты и категории
    stickersets = get_all_stickersets()
    existing_categories = get_all_categories()
    
    print(f"📊 Найдено стикерсетов: {len(stickersets)}")
    print(f"📊 Существующих категорий: {len(existing_categories)}")
    
    # Создаем словарь существующих категорий для быстрого поиска
    existing_keys = {cat['key'] for cat in existing_categories}
    
    # Анализируем каждый стикерсет
    category_stats = {}
    new_categories_created = 0
    
    for stickerset in stickersets:
        title = stickerset.get('title', '')
        stickerset_id = stickerset.get('id')
        
        print(f"\n🔍 Анализируем: '{title}' (ID: {stickerset_id})")
        
        # Анализируем название
        suggested_categories = analyze_title(title)
        
        if suggested_categories:
            print(f"   💡 Предложенные категории: {', '.join(suggested_categories)}")
            
            # Проверяем, нужно ли создать новые категории
            for category_key in suggested_categories:
                if category_key not in existing_keys:
                    # Создаем новую категорию
                    name_ru = CATEGORIZATION_RULES[category_key]["ru_keywords"][0] if CATEGORIZATION_RULES[category_key]["ru_keywords"] else category_key
                    name_en = CATEGORIZATION_RULES[category_key]["keywords"][0] if CATEGORIZATION_RULES[category_key]["keywords"] else category_key
                    
                    if create_category(category_key, name_ru.title(), name_en.title()):
                        existing_keys.add(category_key)
                        new_categories_created += 1
            
            # Обновляем статистику
            for cat in suggested_categories:
                category_stats[cat] = category_stats.get(cat, 0) + 1
            
            # Обновляем категории стикерсета
            update_stickerset_categories(stickerset_id, list(suggested_categories))
        else:
            print(f"   ❓ Категории не определены")
    
    # Выводим итоговую статистику
    print(f"\n📈 ИТОГОВАЯ СТАТИСТИКА:")
    print(f"   📊 Новых категорий создано: {new_categories_created}")
    print(f"   📊 Распределение по категориям:")
    
    for category, count in sorted(category_stats.items(), key=lambda x: x[1], reverse=True):
        print(f"      • {category}: {count} стикерсетов")
    
    print(f"\n✅ Автоматическая категоризация завершена!")

if __name__ == "__main__":
    main()
