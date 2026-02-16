/**
 * Логика страницы пользователей
 */

// Проверка авторизации
checkAuth();

// Глобальные переменные
let dataTable;
let filters;
let currentFilters = {};
let currentPage = 0;
let currentSort = 'createdAt';
let currentDirection = 'DESC';

// Колонки таблицы
const tableColumns = [
    {
        field: 'userId',
        label: 'User ID',
        render: (row) => `<span class="font-mono text-xs">${row.userId}</span>` || '-'
    },
    {
        field: 'user',
        label: 'Пользователь',
        render: (row) => {
            if (!row.user) return '<span class="text-gray-400 text-xs">-</span>';
            const user = row.user;
            const name = [user.firstName, user.lastName].filter(Boolean).join(' ') || '-';
            const username = user.username ? `@${user.username}` : '';
            const premium = user.isPremium ? '⭐' : '';
            return `
                <div class="text-xs">
                    <div class="font-medium">${escapeHtml(name)} ${premium}</div>
                    ${username ? `<div class="text-gray-500">${escapeHtml(username)}</div>` : ''}
                </div>
            `;
        }
    },
    {
        field: 'user.languageCode',
        label: 'Язык',
        render: (row) => row.user?.languageCode || '-'
    },
    {
        field: 'role',
        label: 'Роль',
        render: (row) => createBadge(row.role, row.role)
    },
    {
        field: 'artBalance',
        label: 'Баланс ART',
        render: (row) => formatNumber(row.artBalance)
    },
    {
        field: 'subscriptionStatus',
        label: 'Подписка',
        render: (row) => createBadge(row.subscriptionStatus, row.subscriptionStatus)
    },
    {
        field: 'isBlocked',
        label: 'Заблокирован',
        render: (row) => row.isBlocked ? '🚫 Да' : '-'
    },
    {
        field: 'ownedStickerSetsCount',
        label: 'Владелец',
        render: (row) => `<span class="font-mono text-xs">${formatNumber(row.ownedStickerSetsCount || 0)}</span>`
    },
    {
        field: 'authoredStickerSetsCount',
        label: 'Автор',
        render: (row) => `<span class="font-mono text-xs">${formatNumber(row.authoredStickerSetsCount || 0)}</span>`
    },
    {
        field: 'createdAt',
        label: 'Создан',
        render: (row) => formatDate(row.createdAt)
    },
    {
        field: 'actions',
        label: 'Действия',
        render: (row) => `
            <button onclick="editUser(${row.userId})" class="text-xs px-2 py-1 text-blue-600 hover:text-blue-800">
                Редактировать
            </button>
        `
    }
];

// Фильтры (упрощенная версия - только самые нужные)
const filterConfig = [
    {
        name: 'role',
        label: 'Роль',
        type: 'select',
        options: [
            { value: 'USER', label: 'USER' },
            { value: 'ADMIN', label: 'ADMIN' }
        ]
    },
    {
        name: 'isBlocked',
        label: 'Заблокирован',
        type: 'select',
        options: [
            { value: 'true', label: 'Да' },
            { value: 'false', label: 'Нет' }
        ]
    },
    {
        name: 'sort',
        label: 'Сортировка',
        type: 'select',
        options: [
            { value: 'createdAt', label: 'Дата создания' },
            { value: 'ownedStickerSetsCount', label: 'Кол-во владения' },
            { value: 'authoredStickerSetsCount', label: 'Кол-во авторства' }
        ]
    },
    {
        name: 'direction',
        label: 'Направление',
        type: 'select',
        options: [
            { value: 'DESC', label: 'По убыванию' },
            { value: 'ASC', label: 'По возрастанию' }
        ]
    }
];

// Инициализация
document.addEventListener('DOMContentLoaded', async function() {
    // Инициализация таблицы
    dataTable = new DataTable('users-table', {
        columns: tableColumns,
        pageSize: 20,
        onPageChange: (page) => {
            currentPage = page;
            loadUsers();
        },
        onRowClick: null,
        onSelectionChange: (selectedIds) => {
            updateBulkActionsPanel(selectedIds);
        },
        selectable: true
    });
    
    // Инициализация фильтров
    filters = new FiltersPanel('filters-container', {
        filters: filterConfig,
        onFilterChange: (filterValues) => {
            // Обновляем фильтры
            currentFilters = filterValues;
            
            // Обрабатываем sort и direction отдельно
            if (filterValues.sort) {
                currentSort = filterValues.sort;
                delete currentFilters.sort; // Не передаем как фильтр, это параметр сортировки
            }
            if (filterValues.direction) {
                currentDirection = filterValues.direction;
                delete currentFilters.direction; // Не передаем как фильтр, это параметр сортировки
            }
            
            currentPage = 0;
            loadUsers();
        }
    });
    
    // Поиск с задержкой
    const searchInput = document.getElementById('search-input');
    if (searchInput) {
        searchInput.addEventListener('input', debounce((e) => {
            currentFilters.search = e.target.value;
            currentPage = 0;
            loadUsers();
        }, 500));
    }
    
    // Форма редактирования
    const editForm = document.getElementById('edit-form');
    if (editForm) {
        editForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await saveUser();
        });
    }
    
    // Загрузить пользователей
    await loadUsers();
});

// Загрузить пользователей
async function loadUsers() {
    try {
        const response = await api.getUsers(
            currentFilters,
            currentPage,
            20,
            currentSort,
            currentDirection
        );
        
        dataTable.setData(response);
    } catch (error) {
        console.error('Failed to load users:', error);
        showNotification('Ошибка загрузки пользователей', 'error');
    }
}

// Редактировать пользователя
async function editUser(userId) {
    try {
        const profile = await api.getUserProfileByUserId(userId);
        
        document.getElementById('edit-user-id').value = userId;
        document.getElementById('edit-role').value = profile.role || 'USER';
        document.getElementById('edit-balance').value = profile.artBalance || 0;
        document.getElementById('edit-subscription').value = profile.subscriptionStatus || 'NONE';
        document.getElementById('edit-blocked').checked = profile.isBlocked || false;
        
        document.getElementById('edit-modal').classList.remove('hidden');
    } catch (error) {
        console.error('Failed to load user:', error);
        showNotification('Ошибка загрузки данных пользователя', 'error');
    }
}

// Сохранить пользователя
async function saveUser() {
    try {
        const userId = document.getElementById('edit-user-id').value;
        const data = {
            role: document.getElementById('edit-role').value,
            artBalance: parseInt(document.getElementById('edit-balance').value),
            subscriptionStatus: document.getElementById('edit-subscription').value,
            isBlocked: document.getElementById('edit-blocked').checked
        };
        
        await api.updateUserProfile(userId, data);
        showNotification('Пользователь успешно обновлен', 'success');
        closeEditModal();
        await loadUsers();
    } catch (error) {
        console.error('Failed to save user:', error);
        showNotification('Ошибка сохранения данных', 'error');
    }
}

// Закрыть модальное окно
function closeEditModal() {
    document.getElementById('edit-modal').classList.add('hidden');
}

// Обновить панель массовых действий
function updateBulkActionsPanel(selectedIds) {
    const bulkActions = document.getElementById('bulk-actions');
    const selectedCount = document.getElementById('selected-count');
    
    if (selectedIds.length > 0) {
        bulkActions.classList.remove('hidden');
        selectedCount.textContent = selectedIds.length;
    } else {
        bulkActions.classList.add('hidden');
    }
}

// Массовая блокировка
async function bulkBlock() {
    const selectedIds = dataTable.getSelectedRows();
    if (selectedIds.length === 0) return;
    
    if (!confirmAction(`Заблокировать ${selectedIds.length} пользователей?`)) {
        return;
    }
    
    try {
        await api.bulkBlockUsers(selectedIds);
        showNotification(`Заблокировано ${selectedIds.length} пользователей`, 'success');
        dataTable.clearSelection();
        await loadUsers();
    } catch (error) {
        console.error('Failed to block users:', error);
        showNotification('Ошибка блокировки пользователей', 'error');
    }
}

// Массовая разблокировка
async function bulkUnblock() {
    const selectedIds = dataTable.getSelectedRows();
    if (selectedIds.length === 0) return;
    
    if (!confirmAction(`Разблокировать ${selectedIds.length} пользователей?`)) {
        return;
    }
    
    try {
        await api.bulkUnblockUsers(selectedIds);
        showNotification(`Разблокировано ${selectedIds.length} пользователей`, 'success');
        dataTable.clearSelection();
        await loadUsers();
    } catch (error) {
        console.error('Failed to unblock users:', error);
        showNotification('Ошибка разблокировки пользователей', 'error');
    }
}
