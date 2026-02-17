/**
 * Утилиты для админ-панели
 */

// Форматирование даты
function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('ru-RU', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Форматирование числа с разделителями
function formatNumber(num) {
    if (num === null || num === undefined) return '0';
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
}

// Экранирование HTML
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Показать уведомление
function showNotification(message, type = 'success') {
    const notification = document.createElement('div');
    notification.className = `fixed top-4 right-4 px-6 py-4 rounded-lg shadow-lg z-50 ${
        type === 'success' ? 'bg-green-500' :
        type === 'error' ? 'bg-red-500' :
        type === 'warning' ? 'bg-yellow-500' :
        'bg-blue-500'
    } text-white`;
    notification.textContent = message;
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.remove();
    }, 3000);
}

// Подтверждение действия
function confirmAction(message) {
    return confirm(message);
}

// Получить значение из localStorage с fallback
function getLocalStorage(key, defaultValue = null) {
    try {
        const value = localStorage.getItem(key);
        return value ? JSON.parse(value) : defaultValue;
    } catch (e) {
        return defaultValue;
    }
}

// Сохранить значение в localStorage
function setLocalStorage(key, value) {
    try {
        localStorage.setItem(key, JSON.stringify(value));
    } catch (e) {
        console.error('Failed to save to localStorage:', e);
    }
}

// Создать badge для статуса
function createBadge(text, type) {
    const colors = {
        'USER': 'bg-blue-100 text-blue-800',
        'ADMIN': 'bg-purple-100 text-purple-800',
        'ACTIVE': 'bg-green-100 text-green-800',
        'BLOCKED': 'bg-red-100 text-red-800',
        'DELETED': 'bg-gray-100 text-gray-800',
        'PUBLIC': 'bg-green-100 text-green-800',
        'PRIVATE': 'bg-gray-100 text-gray-800',
        'OFFICIAL': 'bg-yellow-100 text-yellow-800',
        'NONE': 'bg-gray-100 text-gray-800',
        'EXPIRED': 'bg-red-100 text-red-800',
        'CANCELLED': 'bg-gray-100 text-gray-800'
    };
    
    const colorClass = colors[text] || 'bg-gray-100 text-gray-800';
    return `<span class="inline-flex items-center px-1.5 py-0.5 rounded-full text-xs font-medium ${colorClass}">${text}</span>`;
}

// Дебаунс функции
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

/**
 * Рендерит выпадающий список действий для колонки таблицы.
 * @param {Array<{label: string, onclick: string, className?: string}>} actions - массив действий
 * @returns {string} HTML строка
 */
function renderActionDropdown(actions) {
    if (!actions || actions.length === 0) return '-';
    return `
        <details class="relative inline-block action-dropdown">
            <summary class="list-none cursor-pointer px-2 py-1 text-gray-600 hover:bg-gray-100 rounded border border-gray-300 text-sm flex items-center justify-center w-8">⋮</summary>
            <div class="absolute right-0 mt-1 z-20 bg-white border rounded-lg shadow-lg py-1 min-w-[140px]">
                ${actions.map(a => `
                    <button type="button" class="block w-full text-left px-3 py-2 text-xs hover:bg-gray-50 transition ${a.className || ''}" onclick="${a.onclick}; closeActionDropdown(this)">
                        ${escapeHtml(a.label)}
                    </button>
                `).join('')}
            </div>
        </details>
    `;
}

/** Закрывает выпадающий список действий после клика по пункту */
function closeActionDropdown(btn) {
    const details = btn.closest('details');
    if (details) details.removeAttribute('open');
}

// Построить query string из объекта
function buildQueryString(params) {
    const queryParams = [];
    for (const key in params) {
        if (params[key] !== null && params[key] !== undefined && params[key] !== '') {
            queryParams.push(`${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`);
        }
    }
    return queryParams.length > 0 ? '?' + queryParams.join('&') : '';
}
