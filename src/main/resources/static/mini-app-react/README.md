# Sticker Gallery Mini App (React)

Telegram Mini App для галереи стикеров, построенный на React с TypeScript.

## 🚀 Технологии

- **React 18** с TypeScript
- **Material-UI (MUI)** для UI компонентов
- **Zustand** для управления состоянием
- **Axios** для HTTP запросов
- **Lottie React** для анимаций
- **Vite** для сборки и разработки

## 📁 Структура проекта

```
src/
├── components/          # React компоненты
│   ├── AuthStatus.tsx   # Статус авторизации
│   ├── DebugPanel.tsx   # Отладочная панель
│   ├── EmptyState.tsx   # Пустое состояние
│   ├── ErrorDisplay.tsx # Отображение ошибок
│   ├── LoadingSpinner.tsx # Индикатор загрузки
│   ├── StickerCard.tsx  # Карточка стикерсета
│   ├── StickerGrid.tsx  # Сетка стикеров
│   ├── StickerPreview.tsx # Превью стикера
│   └── UserInfo.tsx     # Информация о пользователе
├── hooks/               # React хуки
│   └── useTelegram.ts   # Хук для работы с Telegram Web App
├── store/               # Управление состоянием
│   └── useStickerStore.ts # Zustand store
├── types/               # TypeScript типы
│   ├── sticker.ts       # Типы для стикеров
│   └── telegram.ts      # Типы для Telegram Web App
├── api/                 # API клиент
│   └── client.ts        # HTTP клиент
├── App.tsx              # Главный компонент
├── main.tsx             # Точка входа
└── index.css            # Глобальные стили
```

## 🛠 Установка и запуск

### 1. Установка зависимостей

```bash
cd src/main/resources/static/mini-app-react
npm install
```

### 2. Запуск в режиме разработки

```bash
npm run dev
```

Приложение будет доступно по адресу: http://localhost:3000

### 3. Сборка для продакшена

```bash
npm run build
```

Собранные файлы будут в папке `dist/`.

## 🔧 Конфигурация

### Vite конфигурация

- **Base URL**: `/mini-app-react/`
- **Proxy**: API запросы проксируются на `http://localhost:8080`
- **Source maps**: Включены для отладки

### TypeScript

- **Строгий режим**: Включен
- **Path mapping**: Настроен для удобного импорта
- **JSX**: React JSX transform

## 📱 Интеграция с Telegram Web App

### Инициализация

```typescript
const { tg, user, initData, isInTelegramApp } = useTelegram();
```

### Основные возможности

- ✅ Автоматическая инициализация Telegram Web App
- ✅ Получение данных пользователя
- ✅ Проверка срока действия initData
- ✅ Настройка темы приложения
- ✅ Обработка кнопки "Назад"
- ✅ Открытие ссылок через Telegram

### Аутентификация

```typescript
// Проверка авторизации
const authResponse = await apiClient.checkAuthStatus();

// Установка заголовков
apiClient.setAuthHeaders(initData, 'StickerGallery');
```

## 🎨 UI компоненты

### Material-UI тема

- Адаптирована под Telegram Web App
- Поддержка светлой/темной темы
- Кастомные цвета и типографика

### Основные компоненты

- **StickerCard**: Карточка стикерсета с превью
- **StickerGrid**: Сетка стикеров для детального просмотра
- **StickerPreview**: Превью стикера с поддержкой Lottie
- **AuthStatus**: Статус авторизации
- **DebugPanel**: Отладочная информация

## 🔄 Управление состоянием

### Zustand Store

```typescript
const {
  isLoading,
  stickerSets,
  authStatus,
  error,
  setStickerSets,
  setAuthStatus,
  // ... другие действия
} = useStickerStore();
```

### Основные состояния

- **isLoading**: Загрузка стикерсетов
- **isAuthLoading**: Загрузка авторизации
- **stickerSets**: Список стикерсетов
- **authStatus**: Статус авторизации
- **error**: Ошибки приложения

## 🌐 API интеграция

### HTTP клиент

```typescript
// Получение стикерсетов
const response = await apiClient.getStickerSets();

// Удаление стикерсета
await apiClient.deleteStickerSet(id);

// Проверка авторизации
const auth = await apiClient.checkAuthStatus();
```

### Автоматические заголовки

- **X-Telegram-Init-Data**: Данные инициализации
- **X-Telegram-Bot-Name**: Имя бота
- **Content-Type**: application/json

## 🎬 Lottie анимации

### Поддержка анимированных стикеров

```typescript
<StickerPreview 
  sticker={sticker} 
  size="medium"
  showBadge={true}
/>
```

### Особенности

- Автоматическая загрузка Lottie данных
- Fallback на emoji при ошибке
- Оптимизированная производительность
- Поддержка разных размеров

## 🔍 Отладка

### Debug панель

- Информация о Telegram Web App
- Проверка initData
- Статус авторизации
- Временные метки

### Логирование

- Консольные логи для всех API запросов
- Отладочная информация о стикерах
- Ошибки загрузки изображений

## 📱 Адаптивность

### Мобильные устройства

- Responsive дизайн
- Оптимизация для touch
- Адаптивные сетки
- Мобильная навигация

### Размеры экранов

- **xs**: < 600px
- **sm**: 600px - 960px
- **md**: 960px - 1280px
- **lg**: > 1280px

## 🚀 Деплой

### Интеграция с Spring Boot

1. Соберите React приложение:
   ```bash
   npm run build
   ```

2. Скопируйте содержимое `dist/` в `src/main/resources/static/mini-app-react/`

3. Перезапустите Spring Boot приложение

### Nginx конфигурация

```nginx
location /mini-app-react/ {
    try_files $uri $uri/ /mini-app-react/index.html;
}
```

## 🧪 Тестирование

### Запуск тестов

```bash
npm test
```

### Покрытие кода

```bash
npm run test:coverage
```

## 📝 Разработка

### Добавление новых компонентов

1. Создайте компонент в `src/components/`
2. Добавьте TypeScript типы
3. Интегрируйте с store при необходимости
4. Добавьте тесты

### Работа с API

1. Добавьте методы в `api/client.ts`
2. Обновите типы в `types/`
3. Интегрируйте с store
4. Обработайте ошибки

## 🔧 Troubleshooting

### Частые проблемы

1. **Telegram Web App не инициализируется**
   - Проверьте загрузку `telegram-web-app.js`
   - Убедитесь, что приложение запущено в Telegram

2. **Ошибки авторизации**
   - Проверьте срок действия initData
   - Убедитесь в правильности заголовков

3. **Проблемы с Lottie**
   - Проверьте доступность API эндпоинта
   - Убедитесь в корректности file_id

### Логи

Все важные события логируются в консоль с эмодзи для удобства:
- 🔍 Отладочная информация
- ✅ Успешные операции
- ❌ Ошибки
- 🌐 API запросы
- 🎬 Lottie анимации
