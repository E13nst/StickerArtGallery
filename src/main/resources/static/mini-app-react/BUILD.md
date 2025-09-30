# Инструкция по сборке React Mini App

## 📋 Архитектура

### **Development (локальная разработка)**
```bash
npm run dev → Vite dev server (localhost:3000)
            → Использует index.dev.html с <script src="/src/main.tsx">
            → Hot reload работает
            → index.html НЕ изменяется!
```

### **Production (деплой на Amvera)**
```bash
npm run build:prod → Vite собирает в dist/
                   → Копирует dist/index.html → index.html (с новыми хешами)
                   → Коммитим index.html с хешами
                   
./gradlew bootJar → Упаковывает static/ в JAR

Docker → java -jar app.jar → Spring Boot раздает index.html
```

## 🛠️ Команды

### **Для разработки:**
```bash
npm run dev           # Vite dev server (proxy к Amvera)
npm run dev:local     # Vite dev server (proxy к localhost:8080)
```

### **Для сборки production:**
```bash
npm run build         # Собрать в dist/ (БЕЗ копирования index.html)
npm run build:prod    # Собрать + скопировать dist/index.html → index.html
```

## 🔄 Workflow для деплоя

1. **Разработка:**
   ```bash
   npm run dev
   # index.html НЕ изменяется
   ```

2. **Сборка для прода:**
   ```bash
   npm run build:prod
   # Vite создает dist/index.html с новыми хешами
   # Скрипт копирует dist/index.html → index.html
   ```

3. **Коммит:**
   ```bash
   git add index.html
   git commit -m "build: update production bundle"
   git push
   ```

4. **Деплой:**
   - GitHub webhook → Amvera
   - Amvera собирает JAR с обновленным index.html
   - Spring Boot раздает новую версию

## ⚠️ Важно!

- **`index.html`** — **PRODUCTION версия** (всегда с хешами, коммитим в git)
- **`index.dev.html`** — **DEV версия** (для Vite dev server, коммитим в git)
- **`dist/`** — игнорируется в git (`.gitignore`)
- **`npm run build`** — только проверяет сборку, НЕ обновляет `index.html`
- **`npm run build:prod`** — собирает И копирует `index.html` для прода
- **Vite dev** автоматически использует `index.dev.html` и НЕ трогает `index.html`

## 🔍 Как это работает

1. **Vite в dev** использует `index.dev.html`:
   - Видит `<script src="/src/main.tsx">`
   - Запускает dev server с этим файлом
   - `index.html` остается нетронутым

2. **Vite build** генерирует `dist/index.html`:
   - Заменяет `/src/main.tsx` на `/mini-app-react/assets/index-HASH.js`
   - Добавляет CSS: `/mini-app-react/assets/index-HASH.css`

3. **`npm run build:prod`** копирует `dist/index.html` → `index.html`:
   - Теперь `index.html` содержит prod ссылки
   - Коммитим его для деплоя

## 🚫 Избегаем проблем

### **Проблема 1: `index.html` с dev ссылками в проде**
❌ Раньше: `index.html` менялся при dev, prod ломался

✅ Решение: `index.dev.html` для dev, `index.html` только для прода

### **Проблема 2: Устаревшие хеши в `index.html`**
❌ Если после сборки `index.html` содержит старые хеши, файлы не найдутся

✅ Решение: `npm run build:prod` всегда копирует актуальный `dist/index.html`

### **Проблема 3: Забыли скопировать после сборки**
❌ После `npm run build` файл `index.html` не обновлен

✅ Решение: используйте `npm run build:prod` вместо `npm run build`
