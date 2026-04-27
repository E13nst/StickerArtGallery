// Navigation Component
// Переиспользуемая навигация для всех страниц админки
// ========================================

const NAVIGATION_ITEMS = [
    { href: '/admin/index.html', label: 'Пользователи', icon: '👥', id: 'users' },
    { href: '/admin/stickers.html', label: 'Стикерсеты', icon: '🎨', id: 'stickers' },
    { href: '/admin/generation-logs.html', label: 'Лог генерации', icon: '📋', id: 'generation-logs' },
    { href: '/admin/stars-packages.html', label: 'Stars Packages', icon: '⭐', id: 'stars-packages' },
    { href: '/admin/art-rules.html', label: 'ART Rules', icon: '🎭', id: 'art-rules' },
    { href: '/admin/prompt-enhancers.html', label: 'Prompt Enhancers', icon: '✨', id: 'prompt-enhancers' },
    { href: '/admin/style-presets.html', label: 'Style Presets', icon: '🎨', id: 'style-presets' },
    { href: '/admin/preset-moderation.html', label: 'Модерация пресетов', icon: '✅', id: 'preset-moderation' }
];

/**
 * Рендерит навигацию для текущей страницы
 * @param {string} currentPageId - ID текущей страницы
 */
function renderNavigation(currentPageId) {
    const nav = document.getElementById('admin-nav');
    if (!nav) {
        console.warn('Navigation element not found');
        return;
    }
    
    nav.innerHTML = NAVIGATION_ITEMS.map(item => {
        const isActive = item.id === currentPageId;
        const activeClasses = isActive 
            ? 'text-gray-700 bg-gray-100 border-l-4 border-blue-500'
            : 'text-gray-600 hover:bg-gray-100 hover:text-gray-700';
        
        return `
            <a href="${item.href}" class="flex items-center px-6 py-3 ${activeClasses}">
                <span class="mx-3">${item.icon ? item.icon + ' ' : ''}${item.label}</span>
            </a>
        `;
    }).join('');
}

/**
 * URL Query State Manager
 * Синхронизирует фильтры с URL для возможности шаринга и навигации
 */
class QueryStateManager {
    constructor(defaultState = {}) {
        this.defaultState = defaultState;
        this.listeners = [];
    }
    
    /**
     * Получить текущее состояние из URL
     */
    getState() {
        const params = new URLSearchParams(window.location.search);
        const state = { ...this.defaultState };
        
        for (const [key, value] of params.entries()) {
            // Попытка парсинга JSON для сложных значений
            try {
                state[key] = JSON.parse(value);
            } catch {
                state[key] = value;
            }
        }
        
        return state;
    }
    
    /**
     * Установить состояние и обновить URL
     * @param {Object} newState - Новое состояние
     * @param {boolean} replace - Заменить текущий history entry вместо добавления нового
     */
    setState(newState, replace = false) {
        const params = new URLSearchParams();
        
        for (const [key, value] of Object.entries(newState)) {
            if (value !== null && value !== undefined && value !== '') {
                // Сериализация сложных значений в JSON
                const serialized = typeof value === 'object' 
                    ? JSON.stringify(value) 
                    : String(value);
                params.set(key, serialized);
            }
        }
        
        const queryString = params.toString();
        const newUrl = queryString 
            ? `${window.location.pathname}?${queryString}`
            : window.location.pathname;
        
        if (replace) {
            window.history.replaceState(newState, '', newUrl);
        } else {
            window.history.pushState(newState, '', newUrl);
        }
        
        this.notifyListeners(newState);
    }
    
    /**
     * Обновить часть состояния
     * @param {Object} partialState - Частичное обновление состояния
     * @param {boolean} replace - Заменить текущий history entry
     */
    updateState(partialState, replace = true) {
        const currentState = this.getState();
        const newState = { ...currentState, ...partialState };
        this.setState(newState, replace);
    }
    
    /**
     * Очистить состояние
     */
    clearState() {
        window.history.replaceState({}, '', window.location.pathname);
        this.notifyListeners(this.defaultState);
    }
    
    /**
     * Подписаться на изменения состояния
     * @param {Function} callback - Функция обратного вызова
     */
    subscribe(callback) {
        this.listeners.push(callback);
        return () => {
            this.listeners = this.listeners.filter(l => l !== callback);
        };
    }
    
    /**
     * Уведомить подписчиков об изменении состояния
     */
    notifyListeners(state) {
        this.listeners.forEach(listener => listener(state));
    }
    
    /**
     * Получить значение конкретного параметра
     * @param {string} key - Ключ параметра
     * @param {any} defaultValue - Значение по умолчанию
     */
    get(key, defaultValue = null) {
        const state = this.getState();
        return state[key] !== undefined ? state[key] : defaultValue;
    }
    
    /**
     * Установить значение конкретного параметра
     * @param {string} key - Ключ параметра
     * @param {any} value - Значение
     * @param {boolean} replace - Заменить текущий history entry
     */
    set(key, value, replace = true) {
        this.updateState({ [key]: value }, replace);
    }
}

/**
 * Получить экземпляр QueryStateManager
 * @param {Object} defaultState - Состояние по умолчанию
 * @returns {QueryStateManager}
 */
function createQueryStateManager(defaultState = {}) {
    return new QueryStateManager(defaultState);
}

// Экспорт для использования в других модулях
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { renderNavigation, QueryStateManager, createQueryStateManager };
}
