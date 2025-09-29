# Sticker Gallery Mini App (React)

Telegram Mini App для галереи стикеров, построенный на React + TypeScript.

## 🚀 Быстрый старт

### Установка зависимостей
```bash
npm install
```

### Режимы разработки

#### 1. Разработка с продакшн API (рекомендуется)
```bash
npm run dev
```
- Запускает Vite dev server на порту 3000
- Все API запросы проксируются на продакшн: `https://stickerartgallery-e13nst.amvera.io`
- Не требует запуска локального Spring Boot приложения
- URL: http://localhost:3000/mini-app-react/

#### 2. Разработка с локальным API
```bash
npm run dev:local
```
- Запускает Vite dev server на порту 3000
- Все API запросы проксируются на локальный Spring Boot: `http://localhost:8080`
- Требует запуска локального Spring Boot приложения
- URL: http://localhost:3000/mini-app-react/

### Сборка для продакшна
```bash
npm run build
```
- Создает оптимизированную сборку в папке `dist/`
- Файлы готовы для развертывания на Spring Boot

### Предварительный просмотр продакшн сборки
```bash
npm run preview
```
- Запускает локальный сервер для тестирования продакшн сборки

## 🛠️ Технологии

- **React 18** - UI библиотека
- **TypeScript** - типизация
- **Vite** - сборщик и dev server
- **Material-UI** - компоненты UI
- **Zustand** - управление состоянием
- **Axios** - HTTP клиент
- **Lottie React** - анимации

## 📁 Структура проекта

```
src/
├── components/          # React компоненты
│   ├── StickerPreview.tsx
│   ├── StickerCard.tsx
│   ├── StickerGrid.tsx
│   └── ...
├── hooks/              # Пользовательские хуки
│   └── useTelegram.ts
├── store/              # Zustand store
│   └── useStickerStore.ts
├── api/                # API клиент
│   └── client.ts
├── types/              # TypeScript типы
│   ├── telegram.ts
│   └── sticker.ts
└── main.tsx           # Точка входа
```

## 🔧 Конфигурация

### Vite конфигурация (`vite.config.ts`)
- **Development режим**: API запросы → продакшн сервер
- **Production режим**: API запросы → локальный Spring Boot
- **Path aliases**: `@` → `src/`
- **Base path**: `/mini-app-react/`

### API клиент (`src/api/client.ts`)
- Автоматическое добавление Telegram заголовков
- Обработка ошибок аутентификации
- Проксирование запросов к стикерам

## 🎨 Адаптивные размеры стикеров

Приложение автоматически определяет платформу и адаптирует размеры:

- **В Telegram**: 120x120px (компактно)
- **В браузере**: 200x200px (крупнее)
- **Сетка**: адаптивное количество колонок

## 🚀 Развертывание

1. Соберите проект: `npm run build`
2. Скопируйте содержимое `dist/` в `src/main/resources/static/mini-app-react/`
3. Запустите Spring Boot приложение
4. Приложение будет доступно по адресу: `/mini-app-react/`

## 🐛 Отладка

### Консольные логи
Приложение выводит подробную отладочную информацию:
- 🔍 Telegram Web App данные
- 🔍 Размеры стикеров
- 🔍 API запросы
- 🔍 Состояние приложения

### Проверка в разных средах
- **Telegram**: откройте через бота
- **Браузер**: http://localhost:3000/mini-app-react/
- **Продакшн**: https://stickerartgallery-e13nst.amvera.io/mini-app-react/

## 📝 Полезные команды

```bash
# Линтинг
npm run lint

# Тестирование
npm run test

# Очистка кэша
rm -rf node_modules/.vite
npm run dev
```