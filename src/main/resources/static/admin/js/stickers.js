/**
 * Логика страницы стикерсетов
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
        field: 'id',
        label: 'ID',
        render: (row) => row.id || '-'
    },
    {
        field: 'title',
        label: 'Название',
        render: (row) => escapeHtml(row.title) || '-'
    },
    {
        field: 'name',
        label: 'System Name',
        render: (row) => escapeHtml(row.name) || '-'
    },
    {
        field: 'userId',
        label: 'Owner',
        render: (row) => row.userId ? `<a href="/admin/?search=${row.userId}" class="text-blue-600 hover:underline">${row.userId}</a>` : '-'
    },
    {
        field: 'type',
        label: 'Тип',
        render: (row) => createBadge(row.type, row.type)
    },
    {
        field: 'visibility',
        label: 'Видимость',
        render: (row) => createBadge(row.visibility, row.visibility)
    },
    {
        field: 'state',
        label: 'Состояние',
        render: (row) => createBadge(row.state, row.state)
    },
    {
        field: 'likesCount',
        label: 'Лайки',
        render: (row) => `👍 ${formatNumber(row.likesCount || 0)}`
    },
    {
        field: 'dislikesCount',
        label: 'Дизлайки',
        render: (row) => `👎 ${formatNumber(row.dislikesCount || 0)}`
    },
    {
        field: 'stickersCount',
        label: 'Стикеров',
        render: (row) => formatNumber(row.stickersCount || 0)
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
            <div class="flex flex-col space-y-1 text-xs">
                ${row.state === 'BLOCKED' ? 
                    `<button onclick="unblockStickerset(${row.id})" class="text-green-600 hover:text-green-800 text-left">✓ Разблокировать</button>` :
                    `<button onclick="blockStickerset(${row.id})" class="text-red-600 hover:text-red-800 text-left">✗ Блокировать</button>`
                }
                ${row.isPublic ?
                    `<button onclick="unpublishStickerset(${row.id})" class="text-yellow-600 hover:text-yellow-800 text-left">👁 Скрыть</button>` :
                    `<button onclick="publishStickerset(${row.id})" class="text-blue-600 hover:text-blue-800 text-left">🌐 Опубликовать</button>`
                }
                ${row.isOfficial ?
                    `<button onclick="unsetOfficialStickerset(${row.id})" class="text-purple-600 hover:text-purple-800 text-left">⭐ Снять официальный</button>` :
                    `<button onclick="setOfficialStickerset(${row.id})" class="text-purple-600 hover:text-purple-800 text-left">⭐ Сделать официальным</button>`
                }
                ${row.state === 'ACTIVE' ?
                    `<button onclick="deleteStickerset(${row.id})" class="text-gray-600 hover:text-gray-800 text-left">🗑 Удалить</button>` :
                    ''
                }
            </div>
        `
    }
];

// Фильтры
const filterConfig = [
    {
        name: 'type',
        label: 'Тип',
        type: 'select',
        options: [
            { value: 'USER', label: 'USER' },
            { value: 'OFFICIAL', label: 'OFFICIAL' }
        ]
    },
    {
        name: 'visibility',
        label: 'Видимость',
        type: 'select',
        options: [
            { value: 'PUBLIC', label: 'PUBLIC' },
            { value: 'PRIVATE', label: 'PRIVATE' }
        ]
    },
    {
        name: 'userId',
        label: 'Owner ID',
        type: 'number'
    }
];

// Инициализация
document.addEventListener('DOMContentLoaded', async function() {
    // Инициализация таблицы
    dataTable = new DataTable('stickers-table', {
        columns: tableColumns,
        pageSize: 20,
        onPageChange: (page) => {
            currentPage = page;
            loadStickers();
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
            currentFilters = filterValues;
            currentPage = 0;
            loadStickers();
        }
    });
    
    // Поиск с задержкой
    const searchInput = document.getElementById('search-input');
    if (searchInput) {
        searchInput.addEventListener('input', debounce((e) => {
            currentFilters.search = e.target.value;
            currentPage = 0;
            loadStickers();
        }, 500));
    }
    
    // Загрузить стикерсеты
    await loadStickers();
});

// Загрузить стикерсеты
async function loadStickers() {
    try {
        // Добавляем shortInfo=true для уменьшения размера ответа
        const params = {
            ...currentFilters,
            shortInfo: true
        };
        
        const response = await api.getStickersets(
            params,
            currentPage,
            20,
            currentSort,
            currentDirection
        );
        
        dataTable.setData(response);
    } catch (error) {
        console.error('Failed to load stickers:', error);
        showNotification('Ошибка загрузки стикерсетов', 'error');
    }
}

// Заблокировать стикерсет
async function blockStickerset(id) {
    const reason = prompt('Укажите причину блокировки:');
    if (!reason) return;
    
    try {
        await api.blockStickerset(id, reason);
        showNotification('Стикерсет заблокирован', 'success');
        await loadStickers();
    } catch (error) {
        console.error('Failed to block stickerset:', error);
        showNotification('Ошибка блокировки стикерсета', 'error');
    }
}

// Разблокировать стикерсет
async function unblockStickerset(id) {
    if (!confirmAction('Разблокировать стикерсет?')) return;
    
    try {
        await api.unblockStickerset(id);
        showNotification('Стикерсет разблокирован', 'success');
        await loadStickers();
    } catch (error) {
        console.error('Failed to unblock stickerset:', error);
        showNotification('Ошибка разблокировки стикерсета', 'error');
    }
}

// Удалить стикерсет
async function deleteStickerset(id) {
    if (!confirmAction('Удалить стикерсет? Это действие нельзя отменить.')) return;
    
    try {
        await api.deleteStickerset(id);
        showNotification('Стикерсет удален', 'success');
        await loadStickers();
    } catch (error) {
        console.error('Failed to delete stickerset:', error);
        showNotification('Ошибка удаления стикерсета', 'error');
    }
}

// Опубликовать стикерсет
async function publishStickerset(id) {
    if (!confirmAction('Опубликовать стикерсет в галерее?')) return;
    
    try {
        await api.publishStickerset(id);
        showNotification('Стикерсет опубликован', 'success');
        await loadStickers();
    } catch (error) {
        console.error('Failed to publish stickerset:', error);
        showNotification('Ошибка публикации стикерсета', 'error');
    }
}

// Скрыть стикерсет
async function unpublishStickerset(id) {
    if (!confirmAction('Скрыть стикерсет из галереи?')) return;
    
    try {
        await api.unpublishStickerset(id);
        showNotification('Стикерсет скрыт', 'success');
        await loadStickers();
    } catch (error) {
        console.error('Failed to unpublish stickerset:', error);
        showNotification('Ошибка скрытия стикерсета', 'error');
    }
}

// Сделать официальным
async function setOfficialStickerset(id) {
    if (!confirmAction('Сделать этот стикерсет официальным?')) return;
    
    try {
        await api.setOfficial(id);
        showNotification('Стикерсет отмечен как официальный', 'success');
        await loadStickers();
    } catch (error) {
        console.error('Failed to set official:', error);
        showNotification('Ошибка установки официального статуса', 'error');
    }
}

// Снять официальный статус
async function unsetOfficialStickerset(id) {
    if (!confirmAction('Снять официальный статус?')) return;
    
    try {
        await api.unsetOfficial(id);
        showNotification('Официальный статус снят', 'success');
        await loadStickers();
    } catch (error) {
        console.error('Failed to unset official:', error);
        showNotification('Ошибка снятия официального статуса', 'error');
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
    
    const reason = prompt('Укажите причину блокировки:');
    if (!reason) return;
    
    try {
        await api.bulkOperation(
            selectedIds,
            id => api.blockStickerset(id, reason),
            'Блокировка стикерсетов'
        );
        dataTable.clearSelection();
        await loadStickers();
    } catch (error) {
        console.error('Failed to block stickersets:', error);
        showNotification('Ошибка блокировки стикерсетов', 'error');
    }
}

// Массовая разблокировка
async function bulkUnblock() {
    const selectedIds = dataTable.getSelectedRows();
    if (selectedIds.length === 0) return;
    
    if (!confirmAction(`Разблокировать ${selectedIds.length} стикерсетов?`)) return;
    
    try {
        await api.bulkOperation(
            selectedIds,
            id => api.unblockStickerset(id),
            'Разблокировка стикерсетов'
        );
        dataTable.clearSelection();
        await loadStickers();
    } catch (error) {
        console.error('Failed to unblock stickersets:', error);
        showNotification('Ошибка разблокировки стикерсетов', 'error');
    }
}

// Массовое удаление
async function bulkDelete() {
    const selectedIds = dataTable.getSelectedRows();
    if (selectedIds.length === 0) return;
    
    if (!confirmAction(`Удалить ${selectedIds.length} стикерсетов? Это действие нельзя отменить.`)) return;
    
    try {
        await api.bulkOperation(
            selectedIds,
            id => api.deleteStickerset(id),
            'Удаление стикерсетов'
        );
        dataTable.clearSelection();
        await loadStickers();
    } catch (error) {
        console.error('Failed to delete stickersets:', error);
        showNotification('Ошибка удаления стикерсетов', 'error');
    }
}

// Массовая установка официального статуса
async function bulkSetOfficial() {
    const selectedIds = dataTable.getSelectedRows();
    if (selectedIds.length === 0) return;
    
    if (!confirmAction(`Сделать ${selectedIds.length} стикерсетов официальными?`)) return;
    
    try {
        await api.bulkOperation(
            selectedIds,
            id => api.setOfficial(id),
            'Установка официального статуса'
        );
        dataTable.clearSelection();
        await loadStickers();
    } catch (error) {
        console.error('Failed to set official:', error);
        showNotification('Ошибка установки официального статуса', 'error');
    }
}
