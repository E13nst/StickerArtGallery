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
                { path: '/admin/stickers.html', icon: '🎨', label: 'Стикерсеты' },
                { path: '/admin/generation-logs.html', icon: '📋', label: 'Лог генерации' }
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
            ? 'text-gray-700 bg-gray-100 border-l-4 border-blue-500' 
            : 'text-gray-600 hover:bg-gray-100 hover:text-gray-700';
        
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
                <p class="px-4 pb-1 text-[11px] font-semibold uppercase tracking-wide text-gray-400">${section.title}</p>
                ${itemsHTML}
            </div>
        `;
    },

    // Сгенерировать полный HTML sidebar
    render() {
        const menuHTML = this.menuSections.map(section => this.renderMenuSection(section)).join('');
        
        return `
            <aside class="w-64 bg-white shadow-md sidebar">
                <div class="p-4">
                    <h1 class="text-xl font-bold text-gray-800">Admin Panel</h1>
                    <p class="text-xs text-gray-600">Sticker Gallery</p>
                </div>
                
                <nav class="mt-4">
                    ${menuHTML}
                </nav>
                
                <div class="absolute bottom-0 w-64 p-4 border-t">
                    <div>
                        <p class="text-sm font-medium text-gray-700" id="current-user-name">Admin</p>
                        <p class="text-xs text-gray-500" id="current-user-role">ADMIN</p>
                    </div>
                    <button onclick="logout()" class="mt-2 w-full px-3 py-1.5 text-xs text-white bg-red-600 rounded hover:bg-red-700">
                        Выйти
                    </button>
                </div>
            </aside>
        `;
    },

    // Инициализировать sidebar
    init() {
        // Найти контейнер для sidebar (первый элемент в flex-контейнере)
        const container = document.querySelector('.flex.h-screen.overflow-hidden');
        if (container) {
            // Вставить sidebar в начало контейнера
            container.insertAdjacentHTML('afterbegin', this.render());
        } else {
            console.error('Sidebar container not found');
        }
    }
};

// Автоматически инициализировать при загрузке DOM
document.addEventListener('DOMContentLoaded', function() {
    SidebarComponent.init();
});
