#!/usr/bin/env python3
import redis
import os

# Загружаем переменные окружения из .env.app
def load_env():
    env_vars = {}
    try:
        with open('.env.app', 'r') as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith('#') and '=' in line:
                    key, value = line.split('=', 1)
                    env_vars[key] = value
    except FileNotFoundError:
        print("Файл .env.app не найден")
        return {}
    return env_vars

# Загружаем настройки
env = load_env()

print(f"🔍 Проверяем подключение к Redis:")
print(f"   Host: {env.get('REDIS_HOST', 'localhost')}")
print(f"   Port: {env.get('REDIS_PORT', 6379)}")
print(f"   Password: {'***' if env.get('REDIS_PASSWORD') else 'не установлен'}")

# Попробуем разные варианты подключения
configs = [
    {
        "name": "Без SSL",
        "config": {
            "host": env.get('REDIS_HOST', 'localhost'),
            "port": int(env.get('REDIS_PORT', 6379)),
            "password": env.get('REDIS_PASSWORD', ''),
            "decode_responses": True
        }
    },
    {
        "name": "С SSL (без проверки сертификата)",
        "config": {
            "host": env.get('REDIS_HOST', 'localhost'),
            "port": int(env.get('REDIS_PORT', 6379)),
            "password": env.get('REDIS_PASSWORD', ''),
            "ssl": True,
            "ssl_cert_reqs": None,
            "decode_responses": True
        }
    },
    {
        "name": "С SSL (строгая проверка)",
        "config": {
            "host": env.get('REDIS_HOST', 'localhost'),
            "port": int(env.get('REDIS_PORT', 6379)),
            "password": env.get('REDIS_PASSWORD', ''),
            "ssl": True,
            "decode_responses": True
        }
    }
]

for config_info in configs:
    print(f"\n📡 Тестируем: {config_info['name']}")
    try:
        r = redis.Redis(**config_info['config'])
        
        # Проверка подключения
        response = r.ping()
        if response:
            print(f"✅ {config_info['name']}: Подключение успешно!")
            
            # Дополнительные тесты
            print("   📊 Тестируем базовые операции:")
            
            # Тест записи
            r.set('test_key', 'test_value', ex=60)  # TTL 60 секунд
            print("   ✅ Запись в Redis: OK")
            
            # Тест чтения
            value = r.get('test_key')
            print(f"   ✅ Чтение из Redis: {value}")
            
            # Тест удаления
            r.delete('test_key')
            print("   ✅ Удаление из Redis: OK")
            
            # Информация о Redis
            info = r.info()
            print(f"   📈 Информация о Redis:")
            print(f"      Версия: {info.get('redis_version', 'неизвестно')}")
            print(f"      Использованная память: {info.get('used_memory_human', 'неизвестно')}")
            print(f"      Подключенные клиенты: {info.get('connected_clients', 'неизвестно')}")
            
            # Если дошли сюда, значит этот конфиг работает
            print(f"\n🎉 РАБОЧАЯ КОНФИГУРАЦИЯ: {config_info['name']}")
            break
            
    except redis.ConnectionError as e:
        print(f"   ❌ Ошибка подключения: {e}")
    except redis.AuthenticationError as e:
        print(f"   ❌ Ошибка аутентификации: {e}")
    except Exception as e:
        print(f"   ❌ Произошла ошибка: {e}")
else:
    print(f"\n❌ Ни одна конфигурация не сработала!")
