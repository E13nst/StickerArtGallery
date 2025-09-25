// Инициализация Telegram Web App
const tg = window.Telegram.WebApp;
tg.expand();
tg.ready();

// Настройка темы
document.documentElement.style.setProperty('--tg-theme-bg-color', tg.themeParams.bg_color || '#ffffff');
document.documentElement.style.setProperty('--tg-theme-text-color', tg.themeParams.text_color || '#000000');
document.documentElement.style.setProperty('--tg-theme-hint-color', tg.themeParams.hint_color || '#999999');
document.documentElement.style.setProperty('--tg-theme-button-color', tg.themeParams.button_color || '#2481cc');
document.documentElement.style.setProperty('--tg-theme-button-text-color', tg.themeParams.button_text_color || '#ffffff');
document.documentElement.style.setProperty('--tg-theme-secondary-bg-color', tg.themeParams.secondary_bg_color || '#f8f9fa');

// Получение информации о пользователе
let user = tg.initDataUnsafe?.user;
const userId = user?.id;
let initData = tg.initData;

// Отладочная информация
console.log('🔍 Telegram Web App данные:');
console.log('tg.initData:', initData ? 'present (' + initData.length + ' chars)' : 'null');
console.log('tg.initDataUnsafe:', tg.initDataUnsafe);
console.log('user:', user);
console.log('platform:', tg.platform);
console.log('version:', tg.version);

// Функция проверки срока действия initData
function checkInitDataExpiry(initDataString) {
    if (!initDataString) return { valid: false, reason: 'initData отсутствует' };
    
    try {
        const params = new URLSearchParams(initDataString);
        const authDate = parseInt(params.get('auth_date'));
        
        if (!authDate) {
            return { valid: false, reason: 'auth_date отсутствует' };
        }
        
        const now = Math.floor(Date.now() / 1000);
        const age = now - authDate;
        const maxAge = 600; // 10 минут (как в валидаторе)
        
        console.log('🕐 Проверка срока действия initData:');
        console.log('auth_date:', authDate, '(' + new Date(authDate * 1000).toLocaleString() + ')');
        console.log('current time:', now, '(' + new Date(now * 1000).toLocaleString() + ')');
        console.log('age:', age, 'секунд');
        console.log('max age:', maxAge, 'секунд');
        
        if (age > maxAge) {
            return { 
                valid: false, 
                reason: `initData устарел (возраст: ${age} сек, максимум: ${maxAge} сек)`,
                age: age,
                maxAge: maxAge
            };
        }
        
        return { valid: true, age: age, maxAge: maxAge };
    } catch (error) {
        console.error('❌ Ошибка при проверке срока действия initData:', error);
        return { valid: false, reason: 'Ошибка парсинга initData: ' + error.message };
    }
}

// Функция обновления initData
function refreshInitData() {
    console.log('🔄 Попытка обновления initData...');
    
    // Перезагружаем данные из Telegram Web App
    const newUser = tg.initDataUnsafe?.user;
    const newInitData = tg.initData;
    
    if (newInitData && newInitData !== initData) {
        console.log('✅ initData обновлен');
        user = newUser;
        initData = newInitData;
        return true;
    } else {
        console.log('❌ initData не изменился');
        return false;
    }
}

// Функция повторной попытки с обновлением
function retryWithRefresh() {
    console.log('🔄 Повторная попытка с обновлением данных...');
    if (refreshInitData()) {
        console.log('✅ Данные обновлены, перезагружаем стикеры...');
        loadStickers();
    } else {
        console.log('❌ Не удалось обновить данные, попробуйте перезапустить приложение из бота');
        alert('Не удалось обновить данные аутентификации. Попробуйте перезапустить приложение из бота.');
    }
}

// Проверяем срок действия initData при загрузке
const initDataCheck = checkInitDataExpiry(initData);
if (!initDataCheck.valid) {
    console.warn('⚠️ Проблема с initData:', initDataCheck.reason);
}

// Функция для логирования отладочной информации с результатом запроса
function logDebugInfoWithResponse(status, statusText) {
    console.log('🌐 Последний запрос к API:', status, statusText);
    console.log('🕐 Время запроса:', new Date().toLocaleTimeString());
}

// Функция для отображения отладочной информации initData
function updateDebugInfo() {
    const debugContent = document.getElementById('debugContent');
    if (!debugContent) return;
    
    const now = new Date();
    const authDate = initData ? new URLSearchParams(initData).get('auth_date') : null;
    const authDateTime = authDate ? new Date(parseInt(authDate) * 1000) : null;
    const signature = initData ? new URLSearchParams(initData).get('signature') : null;
    const hash = initData ? new URLSearchParams(initData).get('hash') : null;
    const queryId = initData ? new URLSearchParams(initData).get('query_id') : null;
    
    const debugInfo = `
<div class="debug-item">
    <span class="debug-label">🕐 Текущее время:</span>
    <span class="debug-value">${now.toLocaleString()}</span>
</div>

<div class="debug-item">
    <span class="debug-label">📱 Telegram Platform:</span>
    <span class="debug-value">${tg.platform || 'unknown'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">📋 Telegram Version:</span>
    <span class="debug-value">${tg.version || 'unknown'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">🔐 InitData присутствует:</span>
    <span class="debug-value">${initData ? '✅ Да' : '❌ Нет'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">📏 InitData длина:</span>
    <span class="debug-value">${initData ? initData.length + ' символов' : 'N/A'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">🕒 Auth Date:</span>
    <span class="debug-value">${authDateTime ? authDateTime.toLocaleString() : 'не найден'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">⏰ Возраст InitData:</span>
    <span class="debug-value">${authDate ? Math.floor((now.getTime() - authDateTime.getTime()) / 1000) + ' секунд' : 'N/A'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">✍️ Signature:</span>
    <span class="debug-value">${signature ? '✅ Присутствует (' + signature.length + ' символов)' : '❌ Отсутствует'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">#️⃣ Hash:</span>
    <span class="debug-value">${hash ? '✅ Присутствует (' + hash.length + ' символов)' : '❌ Отсутствует'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">🆔 Query ID:</span>
    <span class="debug-value">${queryId || 'не найден'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">👤 User ID:</span>
    <span class="debug-value">${user?.id || 'не найден'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">🌐 API Endpoint:</span>
    <span class="debug-value">${window.location.origin}/auth/status</span>
</div>

<div class="debug-item">
    <span class="debug-label">✅ InitData валидация:</span>
    <span class="debug-value">${initDataCheck.valid ? '✅ Валидна' : '❌ ' + initDataCheck.reason}</span>
</div>

<div class="debug-item">
    <span class="debug-label">🔤 InitData (полная строка):</span>
    <span class="debug-value">${initData || 'отсутствует'}</span>
</div>
    `;
    
    debugContent.innerHTML = debugInfo;
}

// Отображение информации о пользователе
if (user) {
    document.getElementById('userInfo').innerHTML = `
        <p>Привет, <strong>${user.first_name}${user.last_name ? ' ' + user.last_name : ''}</strong>!</p>
        <p>ID: <strong>${user.id}</strong></p>
        ${user.username ? `<p>Username: <strong>@${user.username}</strong></p>` : ''}
    `;
} else {
    document.getElementById('userInfo').innerHTML = `
        <p>Пользователь не определен</p>
    `;
}

// API базовый URL
const API_BASE = '/api/stickersets';
const AUTH_BASE = '/auth';

// Функция для добавления заголовков аутентификации
function getAuthHeaders() {
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    };
    
    if (initData) {
        headers['X-Telegram-Init-Data'] = initData;
        headers['X-Telegram-Bot-Name'] = 'StickerGallery';
        console.log('✅ Заголовки аутентификации добавлены');
        console.log('X-Telegram-Bot-Name: StickerGallery');
        console.log('X-Telegram-Init-Data: present');
    } else {
        console.warn('⚠️ initData отсутствует, запрос без аутентификации');
    }
    
    return headers;
}

// Проверка статуса аутентификации
async function checkAuthStatus() {
    // Проверяем, запущены ли мы в Telegram Web App
    const isInTelegramApp = window.Telegram && window.Telegram.WebApp && initData && initData.trim() !== '';
    
    if (!isInTelegramApp) {
        // В обычном браузере
        console.log('🌐 Режим браузера: авторизация не требуется');
        document.getElementById('authStatus').innerHTML = `
            <div class="auth-success">
                🌐 Режим браузера
                <br>Публичный доступ к API
            </div>
        `;
        return true; // Считаем авторизованным для публичного API
    }
    
    // В Telegram Web App - проверяем авторизацию
    try {
        const response = await fetch('/auth/status', {
            method: 'GET',
            headers: getAuthHeaders()
        });
        
        // Логируем результат запроса
        logDebugInfoWithResponse(response.status, response.statusText);
        
        if (response.ok) {
            const authData = await response.json();
            console.log('✅ Данные аутентификации:', authData);
            
            if (authData.authenticated) {
                document.getElementById('authStatus').innerHTML = `
                    <div class="auth-success">
                        ✅ Аутентификация успешна
                        <br>Роль: ${authData.role || 'не определена'}
                    </div>
                `;
                return true;
            } else {
                document.getElementById('authStatus').innerHTML = `
                    <div class="auth-error">
                        ❌ Ошибка авторизации: ${authData.message || 'Неизвестная ошибка'}
                    </div>
                `;
                return false;
            }
        } else {
            const errorText = await response.text();
            console.error('❌ Ошибка проверки аутентификации:', response.status, errorText);
            document.getElementById('authStatus').innerHTML = `
                <div class="auth-error">
                    ❌ Ошибка сервера: ${response.status} ${response.statusText}
                </div>
            `;
            return false;
        }
    } catch (error) {
        document.getElementById('authStatus').innerHTML = `
            <div class="auth-error">
                ❌ Ошибка сети: ${error.message}
            </div>
        `;
        return false;
    }
}

// Загрузка стикеров
async function loadStickers() {
    try {
        const loading = document.getElementById('loading');
        if (loading) {
        loading.innerHTML = '<p>Загрузка стикеров...</p>';
        }

        // Проверяем, запущены ли мы в Telegram Web App
        const isInTelegramApp = window.Telegram && window.Telegram.WebApp && initData;
        
        let headers = {};
        
        if (isInTelegramApp) {
            // В Telegram Web App - проверяем авторизацию
            const isAuthenticated = await checkAuthStatus();
            if (!isAuthenticated) {
                throw new Error('Пользователь не авторизован в Telegram Web App');
            }
            headers = getAuthHeaders();
        } else {
            // В обычном браузере - используем публичный доступ
            console.log('🌐 Работаем в обычном браузере без Telegram авторизации');
            headers = {
                'Content-Type': 'application/json'
            };
        }

        const response = await fetch('/api/stickersets', {
            method: 'GET',
            headers: headers
        });

        if (response.ok) {
            const data = await response.json();
            console.log('📡 API ответ:', data);
            displayStickers(data);
        } else {
            const errorText = await response.text();
            console.error('❌ Ошибка API:', response.status, response.statusText, errorText);
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
    } catch (error) {
        console.error('Ошибка загрузки стикеров:', error);
        document.getElementById('content').innerHTML = `
            <div class="error">
                <p>Ошибка загрузки стикеров: ${error.message}</p>
                <button class="btn btn-primary" onclick="loadStickers()">Повторить</button>
            </div>
        `;
    }
}

// Отображение стикеров
function displayStickers(response) {
    const content = document.getElementById('content');
    
    // Обрабатываем пагинированный ответ
    let stickers = [];
    if (response && response.content && Array.isArray(response.content)) {
        stickers = response.content;
    } else if (Array.isArray(response)) {
        // Fallback для прямого массива
        stickers = response;
    }
    
    console.log('📋 Полученные стикерсеты:', stickers);
    
    // Сохраняем данные стикерсетов в глобальном состоянии для детального просмотра
    window.currentStickerSets = stickers;

    if (!stickers || stickers.length === 0) {
        content.innerHTML = `
            <div class="empty-state">
                <h3>🎨 Стикеры не найдены</h3>
                <p>У вас пока нет созданных наборов стикеров</p>
                <button class="btn btn-primary" onclick="tg.openTelegramLink('https://t.me/StickerGalleryBot')">
                    Создать стикер
                </button>
            </div>
        `;
        return;
    }

    content.innerHTML = '';

    const stickersHtml = stickers.map(sticker => {
        // Извлекаем первые 4 стикера для превью
        const previewStickers = getStickerPreviews(sticker);
        const previewHtml = generatePreviewHtml(previewStickers);
        
        return `
        <div class="sticker-card" data-title="${sticker.title.toLowerCase()}">
            <div class="sticker-header">
            <h3>${sticker.title}</h3>
                <span class="sticker-count">${getStickerCount(sticker)} стикеров</span>
            </div>
            
            <!-- Превью стикеров -->
            <div class="sticker-preview-grid">
                ${previewHtml}
            </div>
            
            <div class="sticker-info">
                <p class="sticker-date">Создан: ${new Date(sticker.createdAt).toLocaleDateString()}</p>
            </div>
            
            <div class="sticker-actions">
                <button class="btn btn-primary" onclick="viewStickerSet('${sticker.id}', '${sticker.name}')">
                    📱 Просмотр
                </button>
                <button class="btn btn-secondary" onclick="shareStickerSet('${sticker.name}', '${sticker.title}')">
                    📤 Поделиться
                </button>
                <button class="btn btn-danger" onclick="deleteStickerSet('${sticker.id}', '${sticker.title}')">
                    🗑️ Удалить
                </button>
            </div>
        </div>
        `;
    }).join('');

    content.innerHTML = stickersHtml;
    
    // Инициализируем lazy loading для новых превью ПОСЛЕ добавления HTML в DOM
    initializeLazyLoading();
           
           // Добавляем кнопку для принудительной загрузки (отладка)
           const debugButton = document.createElement('button');
           debugButton.textContent = '🔄 Принудительная загрузка всех изображений';
           debugButton.style.cssText = 'margin: 10px; padding: 8px; background: #007aff; color: white; border: none; border-radius: 4px; cursor: pointer;';
           debugButton.onclick = () => {
               console.log('🔄 Принудительная загрузка всех изображений...');
               const lazyImages = document.querySelectorAll('.preview-image.lazy');
               lazyImages.forEach(img => {
                   if (img.dataset.src && !img.src) {
                       console.log('🔄 Загружаем:', img.dataset.src);
                       img.src = img.dataset.src;
                       img.classList.remove('lazy');
                   }
               });
           };
           content.appendChild(debugButton);
}

// Принудительная загрузка всех изображений (fallback)
function forceLoadAllImages() {
    const lazyImages = document.querySelectorAll('.preview-image.lazy');
    
    lazyImages.forEach(img => {
        if (img.dataset.src && !img.src) {
            img.src = img.dataset.src;
            img.classList.remove('lazy');
        }
    });
}

// Глобальный observer для lazy loading
let lazyImageObserver = null;

// Функция для инициализации lazy loading изображений
function initializeLazyLoading() {
    // Проверяем поддержку IntersectionObserver
    if ('IntersectionObserver' in window) {
        // Создаём observer только один раз
        if (!lazyImageObserver) {
            lazyImageObserver = new IntersectionObserver((entries, observer) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        const img = entry.target;
                        if (img.dataset.src && !img.src) {
                            console.log('🖼️ Загружаем изображение:', img.dataset.src);
                            img.src = img.dataset.src;
                            img.classList.remove('lazy');
                            observer.unobserve(img);
                        }
                    }
                });
            }, {
                // Загружаем изображения заранее (за 50px до появления в области видимости)
                rootMargin: '50px 0px'
            });
        }

        // Находим только новые изображения, которые ещё не отслеживаются
        const lazyImages = document.querySelectorAll('.preview-image.lazy');
        console.log(`🔍 Найдено ${lazyImages.length} изображений для lazy loading`);
        
        lazyImages.forEach(img => {
            lazyImageObserver.observe(img);
            console.log('👁️ Добавлено в observer:', img.dataset.src);
            
            // Проверяем, видно ли изображение сразу (fallback)
            const rect = img.getBoundingClientRect();
            const isVisible = rect.top < window.innerHeight && rect.bottom > 0;
            if (isVisible && img.dataset.src && !img.src) {
                console.log('⚡ Изображение видно сразу, загружаем немедленно:', img.dataset.src);
                img.src = img.dataset.src;
                img.classList.remove('lazy');
                lazyImageObserver.unobserve(img);
            }
        });
        
        console.log(`🔄 Инициализирован lazy loading для ${lazyImages.length} изображений`);
    } else {
        // Fallback для старых браузеров - загружаем все изображения сразу
        console.log('⚠️ IntersectionObserver не поддерживается, загружаем все изображения');
        forceLoadAllImages();
    }
}

// Функции для работы с превью стикеров
function getStickerPreviews(stickerSet) {
    try {
        // Проверяем наличие telegramStickerSetInfo и stickers
        if (!stickerSet || !stickerSet.telegramStickerSetInfo || !stickerSet.telegramStickerSetInfo.stickers) {
            console.log('⚠️ Нет данных stickers для стикерсета:', stickerSet?.title || 'неизвестно');
            return [];
        }
        
        // Берем первые 4 стикера для высококачественных превью
        return stickerSet.telegramStickerSetInfo.stickers.slice(0, 4);
    } catch (error) {
        console.error('❌ Ошибка при получении превью для стикерсета:', stickerSet?.title, error);
        return [];
    }
}

function getStickerCount(stickerSet) {
    try {
        if (!stickerSet || !stickerSet.telegramStickerSetInfo || !stickerSet.telegramStickerSetInfo.stickers) {
            return 0;
        }
        return stickerSet.telegramStickerSetInfo.stickers.length;
    } catch (error) {
        console.error('❌ Ошибка при подсчете стикеров для стикерсета:', stickerSet?.title, error);
        return 0;
    }
}

function generatePreviewHtml(previewStickers) {
    if (previewStickers.length === 0) {
        return `
            <div class="preview-placeholder">
                <div class="placeholder-item">🎨</div>
                <div class="placeholder-item">🖼️</div>
                <div class="placeholder-item">✨</div>
                <div class="placeholder-item">🎭</div>
        </div>
    `;
    }
    
    let html = '';
    // Всегда показываем 4 ячейки (заполняем пустыми если меньше стикеров)
    for (let i = 0; i < 4; i++) {
        if (i < previewStickers.length) {
            const sticker = previewStickers[i];
            // Используем полный file_id для высококачественных превью
            const fileId = sticker.file_id;
            const emoji = sticker.emoji || '🎨';
            const isAnimated = sticker.is_animated;
            
            if (isAnimated) {
                // Для анимированных стикеров используем Lottie
                html += `
                    <div class="preview-item" data-file-id="${fileId}">
                        <div class="preview-placeholder">${emoji}</div>
                        <div class="preview-lottie lazy" 
                             data-src="/api/stickers/${fileId}"
                             data-emoji="${emoji}"
                             title="${emoji} (анимированный)">
                        </div>
                        <div class="animated-badge">LOTTIE</div>
                        <div class="debug-url">${window.location.origin}/api/stickers/${fileId}</div>
                    </div>
                `;
            } else {
                // Для обычных стикеров используем img
                html += `
                    <div class="preview-item" data-file-id="${fileId}">
                        <div class="preview-placeholder">${emoji}</div>
                        <img class="preview-image lazy" 
                             data-src="/api/stickers/${fileId}" 
                             alt="${emoji}"
                             title="${emoji}"
                             onerror="console.error('❌ Ошибка загрузки изображения:', this.src); this.style.display='none'; this.parentElement.querySelector('.preview-placeholder').style.display='flex'"
                             onload="console.log('✅ Изображение загружено:', this.src); this.style.display='block'; this.parentElement.querySelector('.preview-placeholder').style.display='none'">
                        <div class="debug-url">${window.location.origin}/api/stickers/${fileId}</div>
                    </div>
                `;
            }
        } else {
            // Пустая ячейка
            html += `
                <div class="preview-item empty">
                    <div class="preview-placeholder">➕</div>
                </div>
            `;
        }
    }
    
    return html;
}

// Загрузка всех изображений и Lottie анимаций сразу (без lazy loading)
function initializeLazyLoading() {
    console.log('🚀 Загружаем все изображения и анимации сразу...');
    
    const lazyImages = document.querySelectorAll('.preview-image.lazy, .sticker-image.lazy');
    const lazyLotties = document.querySelectorAll('.preview-lottie.lazy, .sticker-lottie.lazy');
    
    console.log(`🖼️ Найдено ${lazyImages.length} изображений для загрузки`);
    console.log(`🎬 Найдено ${lazyLotties.length} Lottie анимаций для загрузки`);
    
    // Загружаем все изображения сразу
    lazyImages.forEach((img, index) => {
        console.log(`🖼️ Загружаем изображение ${index + 1}/${lazyImages.length}:`, img.dataset.src);
        img.src = img.dataset.src;
        img.classList.remove('lazy');
    });
    
    // Загружаем все Lottie анимации сразу
    lazyLotties.forEach((lottie, index) => {
        console.log(`🎬 Загружаем Lottie анимацию ${index + 1}/${lazyLotties.length}:`, lottie.dataset.src);
        loadLottieAnimation(lottie);
        lottie.classList.remove('lazy');
    });
}

// Функция для загрузки Lottie анимации
async function loadLottieAnimation(element) {
    try {
        const url = element.dataset.src;
        console.log('🎬 Загружаем Lottie данные с:', url);
        console.log('🎬 Элемент:', element);
        console.log('🎬 Lottie библиотека доступна:', typeof lottie !== 'undefined');
        
        if (typeof lottie === 'undefined') {
            throw new Error('Lottie библиотека не загружена');
        }
        
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        const animationData = await response.json();
        console.log('✅ Lottie данные загружены:', animationData);
        
        // Создаем Lottie анимацию
        const animation = lottie.loadAnimation({
            container: element,
            renderer: 'svg',
            loop: true,
            autoplay: true,
            animationData: animationData
        });
        
        // Скрываем placeholder и показываем анимацию
        const placeholder = element.parentElement.querySelector('.preview-placeholder');
        if (placeholder) {
            placeholder.style.display = 'none';
        }
        element.style.display = 'block';
        
        console.log('🎬 Lottie анимация запущена');
        
    } catch (error) {
        console.error('❌ Ошибка загрузки Lottie анимации:', error);
        // Показываем placeholder при ошибке
        const placeholder = element.parentElement.querySelector('.preview-placeholder');
        if (placeholder) {
            placeholder.style.display = 'flex';
        }
        element.style.display = 'none';
    }
}

// Просмотр детального стикерсета
function viewStickerSet(stickerSetId, stickerSetName) {
    console.log('🔍 Просмотр стикерсета:', stickerSetId, stickerSetName);
    
    // Находим данные стикерсета
    const stickerSet = findStickerSetById(stickerSetId);
    if (!stickerSet) {
        console.error('❌ Стикерсет не найден:', stickerSetId);
        return;
    }
    
    // Отображаем детальную страницу
    displayStickerSetDetail(stickerSet);
}

// Поиск стикерсета по ID
function findStickerSetById(stickerSetId) {
    // Получаем все стикерсеты из текущего состояния
    const content = document.getElementById('content');
    const stickerCards = content.querySelectorAll('.sticker-card');
    
    for (let card of stickerCards) {
        const buttons = card.querySelectorAll('button[onclick*="viewStickerSet"]');
        for (let button of buttons) {
            const onclick = button.getAttribute('onclick');
            const match = onclick.match(/viewStickerSet\('([^']+)',/);
            if (match && match[1] === stickerSetId.toString()) {
                // Находим данные стикерсета из глобального состояния
                return window.currentStickerSets?.find(s => s.id.toString() === stickerSetId);
            }
        }
    }
    return null;
}

// Отображение детальной страницы стикерсета
function displayStickerSetDetail(stickerSet) {
    const content = document.getElementById('content');
    
    // Получаем все стикеры из набора
    const allStickers = stickerSet.telegramStickerSetInfo?.stickers || [];
    
    // Создаем HTML для детального просмотра
    const detailHtml = `
        <div class="sticker-set-detail">
            <div class="detail-header">
                <button class="btn btn-secondary back-btn" onclick="loadStickers()">
                    ← Назад к списку
                </button>
                <h2>${stickerSet.title}</h2>
                <p class="sticker-count">${allStickers.length} стикеров</p>
            </div>
            
            <div class="sticker-grid">
                ${generateStickerGridHtml(allStickers)}
            </div>
            
            <div class="detail-actions">
                <button class="btn btn-primary" onclick="shareStickerSet('${stickerSet.name}', '${stickerSet.title}')">
                    📤 Поделиться
                </button>
                <button class="btn btn-danger" onclick="deleteStickerSet('${stickerSet.id}', '${stickerSet.title}')">
                    🗑️ Удалить
                </button>
                <button class="btn btn-secondary" onclick="forceLoadAllLottieAnimations()">
                    🎬 Загрузить все анимации
                </button>
            </div>
        </div>
    `;
    
    content.innerHTML = detailHtml;
    
    // Инициализируем lazy loading для новых изображений
    initializeLazyLoading();
}

// Генерация сетки стикеров для детального просмотра
function generateStickerGridHtml(stickers) {
    if (!stickers || stickers.length === 0) {
        return '<div class="empty-state"><p>Стикеры не найдены</p></div>';
    }
    
    let html = '';
    stickers.forEach((sticker, index) => {
        const fileId = sticker.file_id;
        const emoji = sticker.emoji || '🎨';
        const isAnimated = sticker.is_animated;
        
        // Отладочная информация
        console.log(`🎨 Стикер ${index + 1}:`, {
            fileId: fileId.substring(0, 20) + '...',
            emoji,
            isAnimated,
            stickerType: isAnimated ? 'Lottie' : 'WebP',
            fullSticker: sticker
        });
        
        if (isAnimated) {
            // Для анимированных стикеров используем Lottie
            html += `
                <div class="sticker-grid-item" data-file-id="${fileId}">
                    <div class="sticker-placeholder">${emoji}</div>
                    <div class="sticker-lottie lazy" 
                         data-src="/api/stickers/${fileId}"
                         data-emoji="${emoji}"
                         title="${emoji} (анимированный)">
                    </div>
                    <div class="animated-badge">LOTTIE</div>
                </div>
            `;
        } else {
            // Для обычных стикеров используем img
            html += `
                <div class="sticker-grid-item" data-file-id="${fileId}">
                    <div class="sticker-placeholder">${emoji}</div>
                    <img class="sticker-image lazy" 
                         data-src="/api/stickers/${fileId}" 
                         alt="${emoji}"
                         title="${emoji}"
                         onerror="console.error('❌ Ошибка загрузки изображения:', this.src); this.style.display='none'; this.parentElement.querySelector('.sticker-placeholder').style.display='flex'"
                         onload="console.log('✅ Изображение загружено:', this.src); this.style.display='block'; this.parentElement.querySelector('.sticker-placeholder').style.display='none'">
                </div>
            `;
        }
    });
    
    return html;
}

// Принудительная загрузка всех Lottie анимаций (для отладки)
function forceLoadAllLottieAnimations() {
    console.log('🎬 Принудительная загрузка всех Lottie анимаций...');
    
    const lazyLotties = document.querySelectorAll('.sticker-lottie.lazy');
    console.log(`🎬 Найдено ${lazyLotties.length} Lottie элементов для загрузки`);
    
    lazyLotties.forEach((lottie, index) => {
        console.log(`🎬 Загружаем Lottie ${index + 1}/${lazyLotties.length}:`, lottie.dataset.src);
        loadLottieAnimation(lottie);
        lottie.classList.remove('lazy');
    });
}

// Фильтрация стикеров
function filterStickers() {
    const searchInput = document.getElementById('searchInput');
    const searchTerm = searchInput.value.toLowerCase();
    const stickerCards = document.querySelectorAll('.sticker-card');

    stickerCards.forEach(card => {
        const title = card.getAttribute('data-title');
        if (title.includes(searchTerm)) {
            card.style.display = 'block';
        } else {
            card.style.display = 'none';
        }
    });
}

// Открытие набора стикеров
function openStickerSet(stickerSetName) {
    tg.openTelegramLink(`https://t.me/addstickers/${stickerSetName}`);
}

// Поделиться набором стикеров
function shareStickerSet(stickerSetName, title) {
    tg.openTelegramLink(`https://t.me/addstickers/${stickerSetName}`);
}

// Удаление набора стикеров
async function deleteStickerSet(id, title) {
    if (!confirm(`Вы уверены, что хотите удалить набор стикеров "${title}"?`)) {
        return;
    }

    try {
        const response = await fetch(`/api/stickersets/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        if (response.ok) {
            loadStickers(); // Перезагружаем список
        } else {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
    } catch (error) {
        console.error('Ошибка удаления стикера:', error);
        alert(`Ошибка удаления стикера: ${error.message}`);
    }
}

// Обработка кнопки "Назад" (только для поддерживаемых версий)
if (tg.BackButton && typeof tg.BackButton.onClick === 'function') {
tg.BackButton.onClick(() => {
    tg.close();
});

// Показываем кнопку "Назад"
    if (typeof tg.BackButton.show === 'function') {
tg.BackButton.show();
    }
} else {
    console.log('BackButton не поддерживается в этой версии Telegram Web App');
}

// Отображение информации о пользователе
if (user) {
    document.getElementById('userInfo').innerHTML = `
        <p>Привет, <strong>${user.first_name}${user.last_name ? ' ' + user.last_name : ''}</strong>!</p>
        <p>ID: <strong>${user.id}</strong></p>
        ${user.username ? `<p>Username: <strong>@${user.username}</strong></p>` : ''}
    `;
} else {
    document.getElementById('userInfo').innerHTML = `
        <p>Пользователь не определен</p>
    `;
}

// Загружаем стикеры при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    // Обновляем отладочную информацию при загрузке
    updateDebugInfo();
    loadStickers();
});

// Обработка ошибок
window.addEventListener('error', (event) => {
    console.error('Ошибка:', event.error);
});