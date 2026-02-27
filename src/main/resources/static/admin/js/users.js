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
        field: 'verifiedStickerSetsCount',
        label: 'Верифицировано',
        render: (row) => `<span class="font-mono text-xs">${formatNumber(row.verifiedStickerSetsCount || 0)}</span>`
    },
    {
        field: 'createdAt',
        label: 'Создан',
        render: (row) => formatDate(row.createdAt)
    },
    {
        field: 'actions',
        label: 'Действия',
        render: (row) => renderActionDropdown([
            { label: 'Редактировать', onclick: `editUser(${row.userId})`, className: 'text-blue-600' },
            { label: 'Начислить/списать ART', onclick: `openCreateArtTransaction(${row.userId})`, className: 'text-green-600' }
        ])
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
        name: 'artBalanceMin',
        label: 'ART от',
        type: 'number',
        placeholder: '0',
        min: 0
    },
    {
        name: 'artBalanceMax',
        label: 'ART до',
        type: 'number',
        placeholder: '',
        min: 0
    },
    {
        name: 'sort',
        label: 'Сортировка',
        type: 'select',
        options: [
            { value: 'createdAt', label: 'Дата создания' },
            { value: 'ownedStickerSetsCount', label: 'Кол-во владения' },
            { value: 'verifiedStickerSetsCount', label: 'Кол-во верифицированных' }
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
        rowIdField: 'userId',
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

    // Форма создания ART-транзакции
    document.getElementById('art-tx-cancel').addEventListener('click', closeArtTxModal);
    document.getElementById('art-tx-form').addEventListener('submit', onSubmitArtTx);

    // Форма массовой ART-операции
    document.getElementById('bulk-art-cancel').addEventListener('click', closeBulkArtModal);
    document.getElementById('bulk-art-form').addEventListener('submit', onSubmitBulkArt);
    
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

// Открыть модалку создания ART-транзакции с предзаполненным userId
function openCreateArtTransaction(userId) {
    document.getElementById('art-tx-user-id').value = userId;
    document.getElementById('art-tx-amount').value = '';
    document.getElementById('art-tx-message').value = '';
    document.getElementById('art-tx-result').classList.add('hidden');
    document.getElementById('art-tx-result').textContent = '';
    document.getElementById('art-tx-submit').disabled = false;
    document.getElementById('art-tx-modal').classList.remove('hidden');
    setTimeout(() => document.getElementById('art-tx-amount').focus(), 50);
}

function closeArtTxModal() {
    document.getElementById('art-tx-modal').classList.add('hidden');
}

async function onSubmitArtTx(e) {
    e.preventDefault();
    const userIdEl = document.getElementById('art-tx-user-id');
    const amountEl = document.getElementById('art-tx-amount');
    const messageEl = document.getElementById('art-tx-message');
    const resultEl = document.getElementById('art-tx-result');
    const submitBtn = document.getElementById('art-tx-submit');

    const userId = parseInt(userIdEl.value, 10);
    const amount = parseInt(amountEl.value, 10);
    const message = (messageEl.value || '').trim() || null;

    if (isNaN(userId) || isNaN(amount) || amount === 0) {
        resultEl.textContent = 'Укажите корректный User ID и ненулевую сумму.';
        resultEl.className = 'text-sm text-red-600';
        resultEl.classList.remove('hidden');
        return;
    }

    submitBtn.disabled = true;
    resultEl.classList.add('hidden');
    try {
        const response = await api.createArtTransaction({ userId, amount, message });
        const delta = response.transaction?.delta ?? '';
        const balanceAfter = response.transaction?.balanceAfter ?? '';
        let msg = `Транзакция создана. Delta: ${delta}, баланс после: ${balanceAfter}.`;
        if (message) {
            msg += response.messageSent
                ? ' Сообщение пользователю отправлено.'
                : ` Сообщение не отправлено: ${response.messageError || 'неизвестная ошибка'}`;
        }
        resultEl.textContent = msg;
        resultEl.className = `text-sm ${message && !response.messageSent ? 'text-amber-600' : 'text-green-600'}`;
        resultEl.classList.remove('hidden');
        showNotification('ART-транзакция создана', 'success');
        await loadUsers();
    } catch (error) {
        resultEl.textContent = error.message || 'Ошибка создания транзакции';
        resultEl.className = 'text-sm text-red-600';
        resultEl.classList.remove('hidden');
        showNotification(error.message || 'Ошибка создания транзакции', 'error');
    } finally {
        submitBtn.disabled = false;
    }
}

function formatSelectedIds(ids, maxShow = 15) {
    if (!ids || ids.length === 0) return '';
    const list = ids.slice(0, maxShow).join(', ');
    return ids.length > maxShow ? list + ' … +' + (ids.length - maxShow) : list;
}

// Обновить панель массовых действий
function updateBulkActionsPanel(selectedIds) {
    const bulkActions = document.getElementById('bulk-actions');
    const selectedCount = document.getElementById('selected-count');
    const selectedIdsEl = document.getElementById('selected-ids');
    
    if (selectedIds.length > 0) {
        bulkActions.classList.remove('hidden');
        selectedCount.textContent = selectedIds.length;
        selectedIdsEl.textContent = '(' + formatSelectedIds(selectedIds) + ')';
        selectedIdsEl.title = selectedIds.join(', ');
    } else {
        bulkActions.classList.add('hidden');
        selectedIdsEl.textContent = '';
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
        await api.bulkOperation(
            selectedIds,
            userId => api.updateUserProfile(userId, { isBlocked: true }),
            'Блокировка пользователей'
        );
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
        await api.bulkOperation(
            selectedIds,
            userId => api.updateUserProfile(userId, { isBlocked: false }),
            'Разблокировка пользователей'
        );
        dataTable.clearSelection();
        await loadUsers();
    } catch (error) {
        console.error('Failed to unblock users:', error);
        showNotification('Ошибка разблокировки пользователей', 'error');
    }
}

// Открыть модалку массовой ART-операции
function openBulkArtModal() {
    const selectedIds = dataTable.getSelectedRows();
    if (selectedIds.length === 0) return;
    document.getElementById('bulk-art-count').textContent = selectedIds.length;
    const idsEl = document.getElementById('bulk-art-ids');
    idsEl.textContent = selectedIds.join(', ');
    idsEl.title = selectedIds.join(', ');
    document.getElementById('bulk-art-amount').value = '';
    document.getElementById('bulk-art-message').value = '';
    document.getElementById('bulk-art-result').classList.add('hidden');
    document.getElementById('bulk-art-result').textContent = '';
    document.getElementById('bulk-art-submit').disabled = false;
    document.getElementById('bulk-art-modal').classList.remove('hidden');
    setTimeout(() => document.getElementById('bulk-art-amount').focus(), 50);
}

function closeBulkArtModal() {
    document.getElementById('bulk-art-modal').classList.add('hidden');
}

async function onSubmitBulkArt(e) {
    e.preventDefault();
    const amountEl = document.getElementById('bulk-art-amount');
    const messageEl = document.getElementById('bulk-art-message');
    const resultEl = document.getElementById('bulk-art-result');
    const submitBtn = document.getElementById('bulk-art-submit');

    const amount = parseInt(amountEl.value, 10);
    const message = (messageEl.value || '').trim() || null;

    if (isNaN(amount) || amount === 0) {
        resultEl.textContent = 'Укажите ненулевую сумму (положительную для начисления, отрицательную для списания).';
        resultEl.className = 'text-sm text-red-600';
        resultEl.classList.remove('hidden');
        return;
    }

    const selectedIds = dataTable.getSelectedRows();
    const userIds = selectedIds
        .map(id => parseInt(id, 10))
        .filter(id => !isNaN(id));
    if (userIds.length === 0) {
        resultEl.textContent = 'Нет выбранных пользователей.';
        resultEl.className = 'text-sm text-red-600';
        resultEl.classList.remove('hidden');
        return;
    }

    submitBtn.disabled = true;
    resultEl.classList.add('hidden');
    try {
        const report = await api.bulkOperation(
            userIds,
            userId => api.createArtTransaction({ userId, amount, message }),
            'Массовая ART-операция'
        );
        resultEl.textContent = `Выполнено: ${report.successful} из ${report.total}.${report.failed > 0 ? ` Ошибок: ${report.failed}.` : ''}`;
        resultEl.className = `text-sm ${report.failed > 0 ? 'text-amber-600' : 'text-green-600'}`;
        resultEl.classList.remove('hidden');
        dataTable.clearSelection();
        updateBulkActionsPanel([]);
        await loadUsers();
        if (report.failed === 0) {
            closeBulkArtModal();
        }
    } catch (error) {
        resultEl.textContent = error.message || 'Ошибка массовой операции';
        resultEl.className = 'text-sm text-red-600';
        resultEl.classList.remove('hidden');
        showNotification(error.message || 'Ошибка массовой ART-операции', 'error');
    } finally {
        submitBtn.disabled = false;
    }
}
