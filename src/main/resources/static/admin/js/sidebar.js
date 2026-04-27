/**
 * Компонент Sidebar для админ-панели
 * Автоматически рендерит sidebar и определяет активную страницу
 */

const SidebarComponent = {
    // Конфигурация разделов меню
    menuSections: [
        {
            title: 'Пользователи и контент',
            items: [
                { path: '/admin/index.html', icon: '👥', label: 'Пользователи' },
                { path: '/admin/analytics.html', icon: '📊', label: 'Аналитика' },
                { path: '/admin/stickers.html', icon: '🎨', label: 'Стикерсеты' },
                { path: '/admin/generation-logs.html', icon: '📋', label: 'Лог генерации' },
                { path: '/admin/generation-v2.html', icon: '🧪', label: 'Генерации v2' },
                { path: '/admin/message-logs.html', icon: '📨', label: 'Лог сообщений' }
            ]
        },
        {
            title: 'Платежи и экономика',
            items: [
                { path: '/admin/art-transactions.html', icon: '💰', label: 'ART Транзакции' },
                { path: '/admin/stars-transactions.html', icon: '⭐', label: 'Stars Транзакции' },
                { path: '/admin/ton-transactions.html', icon: '💎', label: 'TON Транзакции' },
                { path: '/admin/stars-packages.html', icon: '📦', label: 'Stars Packages' },
                { path: '/admin/art-rules.html', icon: '🎭', label: 'ART Rules' }
            ]
        },
        {
            title: 'AI и стили',
            items: [
                { path: '/admin/prompt-enhancers.html', icon: '✨', label: 'Prompt Enhancers' },
                { path: '/admin/style-presets.html', icon: '🖌️', label: 'Style Presets' }
            ]
        }
    ],

    // Определить активную страницу
    getActivePath() {
        return window.location.pathname;
    },

    // Проверить, является ли пункт меню активным
    isActive(itemPath) {
        const currentPath = this.getActivePath();
        // Сравниваем пути, учитывая что index.html может быть /admin/ или /admin/index.html
        if (itemPath === '/admin/index.html' && (currentPath === '/admin/' || currentPath === '/admin/index.html')) {
            return true;
        }
        return currentPath === itemPath;
    },

    // Сгенерировать HTML для пункта меню
    renderMenuItem(item) {
        const isActive = this.isActive(item.path);
        const activeClass = isActive 
            ? 'text-slate-900 dark:text-slate-100 bg-slate-100 dark:bg-slate-800 border-l-4 border-blue-500' 
            : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-slate-200';
        
        return `
            <a href="${item.path}" class="flex items-center px-4 py-2 ${activeClass}">
                <span class="text-base">${item.icon}</span>
                <span class="ml-3 text-sm">${item.label}</span>
            </a>
        `;
    },

    // Сгенерировать HTML для секции меню
    renderMenuSection(section) {
        const itemsHTML = section.items.map(item => this.renderMenuItem(item)).join('');
        return `
            <div class="mb-3">
                <p class="px-4 pb-1 text-[11px] font-semibold uppercase tracking-wide text-slate-400 dark:text-slate-500">${section.title}</p>
                ${itemsHTML}
            </div>
        `;
    },

    // Сгенерировать полный HTML sidebar
    render() {
        const menuHTML = this.menuSections.map(section => this.renderMenuSection(section)).join('');
        
        return `
            <aside class="sidebar flex flex-col w-64 h-screen bg-white dark:bg-slate-900 shadow-md border-r border-slate-200 dark:border-slate-800">
                <div class="flex-shrink-0 p-4">
                    <h1 class="text-xl font-bold text-slate-800 dark:text-slate-100">Admin Panel</h1>
                    <p class="text-xs text-slate-600 dark:text-slate-400">Sticker Gallery</p>
                </div>
                
                <nav class="sidebar-nav flex-1 min-h-0 overflow-y-auto mt-2">
                    ${menuHTML}
                </nav>
                
                <div class="sidebar-footer flex-shrink-0 p-4 border-t border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900">
                    <button type="button" id="admin-theme-toggle" class="mb-2 w-full px-3 py-1.5 text-xs rounded border border-slate-300 dark:border-slate-600 bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-700">
                        Тема
                    </button>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-200" id="current-user-name">Admin</p>
                        <p class="text-xs text-slate-500 dark:text-slate-400" id="current-user-role">ADMIN</p>
                    </div>
                    <button onclick="logout()" class="mt-2 w-full px-3 py-1.5 text-xs text-white bg-red-600 rounded hover:bg-red-700">
                        Выйти
                    </button>
                </div>
            </aside>
        `;
    },

    // Подпись кнопки темы в sidebar
    syncThemeToggleLabel(btn) {
        if (!btn || typeof AdminTheme === 'undefined') return;
        btn.textContent = AdminTheme.isDark() ? 'Светлая тема' : 'Тёмная тема';
    },

    // Инициализировать sidebar: вставка в DOM и переключатель темы
    init() {
        // Найти контейнер для sidebar (первый элемент в flex-контейнере)
        const container = document.querySelector('.flex.h-screen.overflow-hidden');
        if (container) {
            // Вставить sidebar в начало контейнера
            container.insertAdjacentHTML('afterbegin', this.render());
            const toggle = document.getElementById('admin-theme-toggle');
            if (toggle && typeof AdminTheme !== 'undefined') {
                this.syncThemeToggleLabel(toggle);
                toggle.addEventListener('click', () => {
                    AdminTheme.toggle();
                    this.syncThemeToggleLabel(toggle);
                });
            }
        } else {
            console.error('Sidebar container not found');
        }
    }
};

// Автоматически инициализировать при загрузке DOM
document.addEventListener('DOMContentLoaded', function() {
    SidebarComponent.init();
});
