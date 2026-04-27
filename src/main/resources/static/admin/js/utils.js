/**
 * Утилиты для админ-панели
 */

/**
 * Конвертирует значение datetime-local (YYYY-MM-DDTHH:mm) или дату (YYYY-MM-DD) в ISO для API.
 */
function datetimeLocalToIso(val) {
    if (!val || typeof val !== 'string') return val;
    const trimmed = val.trim();
    if (!trimmed) return val;
    const d = new Date(trimmed);
    return isNaN(d.getTime()) ? val : d.toISOString();
}

/**
 * Конвертирует ISO строку в формат datetime-local (YYYY-MM-DDTHH:mm) для input.
 */
function isoToDatetimeLocal(iso) {
    if (!iso || typeof iso !== 'string') return '';
    const d = new Date(iso.trim());
    if (isNaN(d.getTime())) return '';
    const pad = n => String(n).padStart(2, '0');
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
        'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());
}

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
        'USER': 'bg-blue-100 text-blue-800 dark:bg-blue-950/60 dark:text-blue-200',
        'ADMIN': 'bg-purple-100 text-purple-800 dark:bg-purple-950/60 dark:text-purple-200',
        'ACTIVE': 'bg-green-100 text-green-800 dark:bg-green-950/60 dark:text-green-200',
        'BLOCKED': 'bg-red-100 text-red-800 dark:bg-red-950/60 dark:text-red-200',
        'DELETED': 'bg-slate-200 text-slate-800 dark:bg-slate-700 dark:text-slate-200',
        'PUBLIC': 'bg-green-100 text-green-800 dark:bg-green-950/60 dark:text-green-200',
        'PRIVATE': 'bg-slate-200 text-slate-800 dark:bg-slate-700 dark:text-slate-200',
        'OFFICIAL': 'bg-yellow-100 text-yellow-800 dark:bg-yellow-950/60 dark:text-yellow-200',
        'NONE': 'bg-slate-200 text-slate-800 dark:bg-slate-700 dark:text-slate-200',
        'EXPIRED': 'bg-red-100 text-red-800 dark:bg-red-950/60 dark:text-red-200',
        'CANCELLED': 'bg-slate-200 text-slate-800 dark:bg-slate-700 dark:text-slate-200'
    };
    
    const colorClass = colors[text] || 'bg-slate-200 text-slate-800 dark:bg-slate-700 dark:text-slate-200';
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
            <summary class="list-none cursor-pointer px-2 py-1 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded border border-slate-300 dark:border-slate-600 text-sm flex items-center justify-center w-8">⋮</summary>
            <div class="action-dropdown-menu absolute right-0 mt-1 z-[100] bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg shadow-lg py-1 min-w-[140px]">
                ${actions.map(a => `
                    <button type="button" class="block w-full text-left px-3 py-2 text-xs text-slate-800 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800 transition ${a.className || ''}" onclick="${a.onclick}; closeActionDropdown(this)">
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

/** Позиционирует выпадающее меню с position:fixed, чтобы не обрезалось overflow контейнера */
function positionActionDropdown(details) {
    const summary = details.querySelector('summary');
    const menu = details.querySelector('.action-dropdown-menu');
    if (!summary || !menu) return;
    const rect = summary.getBoundingClientRect();
    const menuWidth = menu.offsetWidth;
    let left = rect.right - menuWidth;
    if (left < 8) left = 8;
    if (left + menuWidth > window.innerWidth - 8) left = window.innerWidth - menuWidth - 8;
    menu.style.position = 'fixed';
    menu.style.top = (rect.bottom + 4) + 'px';
    menu.style.left = left + 'px';
    menu.style.right = 'auto';
}

document.addEventListener('toggle', function(e) {
    if (e.target.matches('.action-dropdown') && e.target.open) {
        requestAnimationFrame(() => positionActionDropdown(e.target));
    }
}, true);

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
