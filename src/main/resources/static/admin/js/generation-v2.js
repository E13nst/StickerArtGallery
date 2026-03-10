checkAuth();

let dataTable;
let filters;
let currentFilters = {};
let currentPage = 0;
const pageSize = 20;

function parseMetadataValue(metadata, key, fallback = '-') {
    if (!metadata) return fallback;
    try {
        const parsed = JSON.parse(metadata);
        const value = parsed[key];
        return value !== null && value !== undefined && value !== '' ? String(value) : fallback;
    } catch (_) {
        return fallback;
    }
}

const tableColumns = [
    { field: 'createdAt', label: 'Дата', render: (row) => formatDate(row.createdAt) },
    {
        field: 'taskId',
        label: 'Task ID',
        render: (row) => `<span class="font-mono text-xs truncate max-w-[140px] block" title="${escapeHtml(row.taskId || '')}">${escapeHtml((row.taskId || '').substring(0, 16))}${(row.taskId || '').length > 16 ? '…' : ''}</span>`
    },
    { field: 'userId', label: 'User ID', render: (row) => `<span class="font-mono text-xs">${escapeHtml(String(row.userId ?? '-'))}</span>` },
    {
        field: 'status',
        label: 'Статус',
        render: (row) => {
            const s = row.status || '-';
            const cls = s === 'COMPLETED'
                ? 'bg-green-100 text-green-800'
                : (s === 'FAILED' || s === 'TIMEOUT')
                    ? 'bg-red-100 text-red-800'
                    : 'bg-gray-100 text-gray-800';
            return `<span class="inline-flex px-1.5 py-0.5 rounded text-xs font-medium ${cls}">${escapeHtml(s)}</span>`;
        }
    },
    { field: 'model', label: 'Модель', render: (row) => escapeHtml(parseMetadataValue(row.metadata, 'model')) },
    { field: 'preset', label: 'Пресет', render: (row) => escapeHtml(parseMetadataValue(row.metadata, 'stylePresetId')) },
    {
        field: 'image',
        label: 'Изображение',
        render: (row) => {
            if (!row.imageUrl) return '-';
            return `<a href="${escapeHtml(row.imageUrl)}" target="_blank" class="text-blue-600 underline text-xs">local</a>`;
        }
    },
    {
        field: 'actions',
        label: 'Действия',
        render: (row) => renderActionDropdown([
            { label: 'Детали', onclick: `openDetail('${String(row.taskId || '').replace(/'/g, "\\'")}')`, className: 'text-blue-600' }
        ])
    }
];

const filterConfig = [
    { name: 'userId', label: 'User ID', type: 'text' },
    {
        name: 'status',
        label: 'Статус',
        type: 'select',
        options: [
            { value: '', label: 'Все' },
            { value: 'PROCESSING_PROMPT', label: 'PROCESSING_PROMPT' },
            { value: 'PENDING', label: 'PENDING' },
            { value: 'GENERATING', label: 'GENERATING' },
            { value: 'REMOVING_BACKGROUND', label: 'REMOVING_BACKGROUND' },
            { value: 'COMPLETED', label: 'COMPLETED' },
            { value: 'FAILED', label: 'FAILED' },
            { value: 'TIMEOUT', label: 'TIMEOUT' }
        ]
    },
    { name: 'taskId', label: 'Task ID', type: 'text' }
];

document.addEventListener('DOMContentLoaded', async function() {
    dataTable = new DataTable('generation-v2-table', {
        columns: tableColumns,
        pageSize: pageSize,
        selectable: false,
        onPageChange: (page) => {
            currentPage = page;
            loadGenerationV2();
        }
    });

    filters = new FiltersPanel('filters-container', {
        filters: filterConfig,
        onFilterChange: (filterValues) => {
            currentFilters = filterValues;
            currentPage = 0;
            loadGenerationV2();
        }
    });

    const errorRetryBtn = document.getElementById('error-retry-btn');
    if (errorRetryBtn) errorRetryBtn.addEventListener('click', loadGenerationV2);

    syncFiltersFromUrl();
    filters.setValues(currentFilters);
    await loadGenerationV2();
});

function buildQueryParams() {
    const p = { page: currentPage, size: pageSize };
    if (currentFilters.userId) p.userId = currentFilters.userId;
    if (currentFilters.status) p.status = currentFilters.status;
    if (currentFilters.taskId) p.taskId = currentFilters.taskId;
    return p;
}

function syncFiltersFromUrl() {
    const params = new URLSearchParams(window.location.search);
    if (params.has('userId')) currentFilters.userId = params.get('userId');
    if (params.has('status')) currentFilters.status = params.get('status');
    if (params.has('taskId')) currentFilters.taskId = params.get('taskId');
    if (params.has('page')) currentPage = parseInt(params.get('page'), 10) || 0;
}

function updateUrl() {
    const params = new URLSearchParams();
    if (currentFilters.userId) params.set('userId', currentFilters.userId);
    if (currentFilters.status) params.set('status', currentFilters.status);
    if (currentFilters.taskId) params.set('taskId', currentFilters.taskId);
    if (currentPage > 0) params.set('page', currentPage);
    const query = params.toString();
    const url = query ? `${window.location.pathname}?${query}` : window.location.pathname;
    window.history.replaceState({}, '', url);
}

async function loadGenerationV2() {
    const tableEl = document.getElementById('generation-v2-table');
    const emptyEl = document.getElementById('empty-state');
    const errorEl = document.getElementById('error-state');
    if (tableEl) tableEl.classList.remove('hidden');
    if (emptyEl) emptyEl.classList.add('hidden');
    if (errorEl) errorEl.classList.add('hidden');

    try {
        const response = await api.getGenerationV2History(buildQueryParams());
        updateUrl();
        dataTable.setData(response);
        if (response.content && response.content.length === 0) {
            if (tableEl) tableEl.classList.add('hidden');
            if (emptyEl) emptyEl.classList.remove('hidden');
        }
    } catch (error) {
        console.error('Failed to load generation v2 history:', error);
        showNotification('Ошибка загрузки истории генераций v2', 'error');
        if (tableEl) tableEl.classList.add('hidden');
        if (errorEl) errorEl.classList.remove('hidden');
    }
}

function openDetail(taskId) {
    const row = (dataTable?.data || []).find(item => item.taskId === taskId);
    const content = document.getElementById('detail-content');
    const modal = document.getElementById('detail-modal');
    if (!row || !content || !modal) return;

    let details = '<div class="space-y-3">';
    details += `<div><strong>Task ID:</strong> <code class="text-xs">${escapeHtml(row.taskId || '-')}</code></div>`;
    details += `<div><strong>User ID:</strong> ${escapeHtml(String(row.userId ?? '-'))}</div>`;
    details += `<div><strong>Статус:</strong> ${escapeHtml(row.status || '-')}</div>`;
    details += `<div><strong>Создана:</strong> ${formatDate(row.createdAt)}</div>`;
    details += `<div><strong>Завершена:</strong> ${formatDate(row.completedAt)}</div>`;
    if (row.imageUrl) details += `<div><strong>imageUrl:</strong> <a class="text-blue-600 underline" target="_blank" href="${escapeHtml(row.imageUrl)}">${escapeHtml(row.imageUrl)}</a></div>`;
    if (row.originalImageUrl) details += `<div><strong>originalImageUrl:</strong> <a class="text-blue-600 underline" target="_blank" href="${escapeHtml(row.originalImageUrl)}">${escapeHtml(row.originalImageUrl)}</a></div>`;
    if (row.errorMessage) details += `<div><strong>Ошибка:</strong> <pre class="text-xs bg-gray-100 p-2 rounded whitespace-pre-wrap">${escapeHtml(row.errorMessage)}</pre></div>`;
    details += `<div><strong>Метаданные:</strong><pre class="text-xs bg-gray-100 p-2 rounded whitespace-pre-wrap">${escapeHtml(row.metadata || '{}')}</pre></div>`;
    details += '</div>';
    content.innerHTML = details;
    modal.classList.remove('hidden');
}

function closeDetailModal() {
    const modal = document.getElementById('detail-modal');
    if (modal) modal.classList.add('hidden');
}
