# 🚀 Деплой React Mini App

## ✅ Что уже сделано

1. **Установлены зависимости** - `npm install` выполнен успешно
2. **Исправлены ошибки TypeScript** - все компиляционные ошибки устранены
3. **Сборка завершена** - `npm run build` создал production файлы
4. **Файлы скопированы** - готовые файлы находятся в `src/main/resources/static/mini-app-react/`

## 📁 Структура после сборки

```
src/main/resources/static/mini-app-react/
├── assets/
│   ├── index-DtPa4RFO.css     # Стили (2.94 kB)
│   ├── index-okDxx1nW.js      # JavaScript (808.46 kB)
│   └── index-okDxx1nW.js.map  # Source map
├── index.html                 # Главная страница
├── package.json              # Зависимости
├── node_modules/             # Установленные пакеты
└── src/                      # Исходный код (для разработки)
```

## 🌐 Как это работает в продакшене

### 1. Spring Boot автоматически обслуживает статические файлы

Когда пользователь заходит на `http://your-domain.com/mini-app-react/`, Spring Boot:
- Ищет файлы в `src/main/resources/static/mini-app-react/`
- Отдает `index.html` как главную страницу
- Обслуживает CSS и JS файлы из папки `assets/`

### 2. URL маршрутизация

- **Главная страница**: `/mini-app-react/` → `index.html`
- **CSS стили**: `/mini-app-react/assets/index-DtPa4RFO.css`
- **JavaScript**: `/mini-app-react/assets/index-okDxx1nW.js`

### 3. API интеграция

React приложение автоматически обращается к вашему Spring Boot API:
- **Стикерсеты**: `/api/stickersets`
- **Авторизация**: `/auth/status`
- **Стикеры**: `/api/stickers/{file_id}`

## 🔧 Запуск в разработке

### Локальная разработка React

```bash
cd src/main/resources/static/mini-app-react
npm run dev
```

Приложение будет доступно на: http://localhost:3000

### Локальная разработка с Spring Boot

1. Запустите Spring Boot приложение:
   ```bash
   set -a; source .env.app; set +a; ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

2. React приложение будет доступно на: http://localhost:8080/mini-app-react/

## 🚀 Деплой на продакшен

### 1. Сборка для продакшена

```bash
cd src/main/resources/static/mini-app-react
npm run build
```

### 2. Копирование файлов (уже сделано)

```bash
cp -r dist/* /path/to/spring-boot/static/mini-app-react/
```

### 3. Перезапуск Spring Boot

После копирования файлов перезапустите Spring Boot приложение.

## 📱 Telegram Bot интеграция

### Обновление URL в боте

Убедитесь, что в настройках бота указан правильный URL:

```
https://your-domain.com/mini-app-react/
```

### Webhook настройка

В файле `.env.app` обновите URL:

```bash
MINI_APP_URL=https://your-domain.com/mini-app-react/
```

## 🔍 Проверка работы

### 1. Проверьте доступность файлов

- `https://your-domain.com/mini-app-react/` - главная страница
- `https://your-domain.com/mini-app-react/assets/index-DtPa4RFO.css` - стили
- `https://your-domain.com/mini-app-react/assets/index-okDxx1nW.js` - JavaScript

### 2. Проверьте в Telegram

1. Откройте бота в Telegram
2. Нажмите кнопку для запуска Mini App
3. Убедитесь, что приложение загружается

### 3. Проверьте консоль браузера

Откройте Developer Tools (F12) и проверьте:
- ✅ Нет ошибок загрузки
- ✅ API запросы работают
- ✅ Telegram Web App инициализируется

## 🛠 Обновление приложения

### Для обновления React приложения:

1. **Внесите изменения** в файлы в папке `src/`
2. **Пересоберите**:
   ```bash
   npm run build
   ```
3. **Скопируйте файлы**:
   ```bash
   cp -r dist/* /Users/andrey/Documents/Projects/sticker-art-gallery/src/main/resources/static/mini-app-react/
   ```
4. **Перезапустите Spring Boot** (если нужно)

### Автоматизация (опционально)

Можно создать скрипт для автоматического обновления:

```bash
#!/bin/bash
cd src/main/resources/static/mini-app-react
npm run build
cp -r dist/* ./
echo "React app updated successfully!"
```

## 🔧 Troubleshooting

### Проблема: 404 ошибка при загрузке

**Решение**: Проверьте, что файлы скопированы в правильную папку:
```
src/main/resources/static/mini-app-react/
├── index.html
└── assets/
    ├── index-*.css
    └── index-*.js
```

### Проблема: API запросы не работают

**Решение**: Убедитесь, что Spring Boot приложение запущено и API эндпоинты доступны.

### Проблема: Telegram Web App не инициализируется

**Решение**: Проверьте, что приложение открывается через Telegram, а не напрямую в браузере.

## 📊 Производительность

### Размер бандла
- **JavaScript**: 808.46 kB (226.64 kB gzipped)
- **CSS**: 2.94 kB (0.94 kB gzipped)
- **Общий размер**: ~230 kB gzipped

### Оптимизации
- ✅ Минификация включена
- ✅ Gzip сжатие
- ✅ Source maps для отладки
- ⚠️ Большой размер JS (можно оптимизировать code splitting)

## 🎯 Следующие шаги

1. **Протестируйте** приложение в Telegram
2. **Настройте мониторинг** ошибок
3. **Оптимизируйте** размер бандла при необходимости
4. **Добавьте** PWA функциональность (опционально)

---

**Готово!** 🎉 React Mini App готов к использованию в продакшене.
