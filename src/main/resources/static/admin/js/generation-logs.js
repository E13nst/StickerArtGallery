/**
 * Логика страницы лога генерации стикеров (админ)
 */

checkAuth();

let dataTable;
let filters;
let currentFilters = {};
let currentPage = 0;
const pageSize = 20;

function parseStylePresetId(requestParams) {
    if (!requestParams) return '-';
    try {
        const p = JSON.parse(requestParams);
        return p.stylePresetId != null ? p.stylePresetId : '-';
    } catch (_) {
        return '-';
    }
}

const tableColumns = [
    {
        field: 'startedAt',
        label: 'Дата',
        render: (row) => formatDate(row.startedAt)
    },
    {
        field: 'taskId',
        label: 'Task ID',
        render: (row) => `<span class="font-mono text-xs truncate max-w-[120px] block" title="${escapeHtml(row.taskId || '')}">${escapeHtml((row.taskId || '').substring(0, 12))}…</span>`
    },
    {
        field: 'userId',
        label: 'User ID',
        render: (row) => `<span class="font-mono text-xs">${row.userId}</span>` || '-'
    },
    {
        field: 'finalStatus',
        label: 'Статус',
        render: (row) => {
            const s = row.finalStatus || '-';
            const cls = s === 'COMPLETED' ? 'bg-green-100 text-green-800' : s === 'FAILED' || s === 'TIMEOUT' ? 'bg-red-100 text-red-800' : 'bg-gray-100 text-gray-800';
            return `<span class="inline-flex px-1.5 py-0.5 rounded text-xs font-medium ${cls}">${s}</span>`;
        }
    },
    {
        field: 'stylePresetId',
        label: 'Пресет',
        render: (row) => parseStylePresetId(row.requestParams)
    },
    {
        field: 'errorCode',
        label: 'Ошибка',
        render: (row) => {
            if (!row.errorCode && !row.errorMessage) return '-';
            const msg = (row.errorMessage || '').substring(0, 40);
            const code = row.errorCode || '';
            return `<span class="text-red-600 text-xs" title="${escapeHtml(row.errorMessage || '')}">${escapeHtml(code)} ${escapeHtml(msg)}${msg.length >= 40 ? '…' : ''}</span>`;
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
        name: 'finalStatus',
        label: 'Статус',
        type: 'select',
        options: [
            { value: '', label: 'Все' },
            { value: 'COMPLETED', label: 'COMPLETED' },
            { value: 'FAILED', label: 'FAILED' },
            { value: 'TIMEOUT', label: 'TIMEOUT' }
        ]
    },
    { name: 'dateFrom', label: 'Дата от', type: 'datetime' },
    { name: 'dateTo', label: 'Дата до', type: 'datetime' },
    {
        name: 'errorOnly',
        label: 'Только с ошибками',
        type: 'select',
        options: [
            { value: '', label: 'Нет' },
            { value: 'true', label: 'Да' }
        ]
    }
];

document.addEventListener('DOMContentLoaded', async function() {
    dataTable = new DataTable('logs-table', {
        columns: tableColumns,
        pageSize: pageSize,
        selectable: false,
        onPageChange: (page) => {
            currentPage = page;
            loadLogs();
        }
    });

    filters = new FiltersPanel('filters-container', {
        filters: filterConfig,
        onFilterChange: (filterValues) => {
            currentFilters = filterValues;
            currentPage = 0;
            loadLogs();
        }
    });

    const taskIdInput = document.getElementById('taskId-input');
    if (taskIdInput) {
        taskIdInput.addEventListener('input', debounce(function() {
            const v = taskIdInput.value.trim();
            currentFilters.taskId = v || undefined;
            currentPage = 0;
            loadLogs();
        }, 400));
    }

    const applyBtn = document.getElementById('apply-filters-btn');
    if (applyBtn) applyBtn.addEventListener('click', () => { currentPage = 0; loadLogs(); });

    const retryBtn = document.getElementById('retry-btn');
    if (retryBtn) retryBtn.addEventListener('click', loadLogs);

    const errorRetryBtn = document.getElementById('error-retry-btn');
    if (errorRetryBtn) errorRetryBtn.addEventListener('click', loadLogs);

    syncFiltersFromUrl();
    filters.setValues(currentFilters);
    await loadLogs();
});

function buildLogsQueryParams() {
    const p = { page: currentPage, size: pageSize };
    if (currentFilters.userId) p.userId = currentFilters.userId;
    if (currentFilters.finalStatus) p.finalStatus = currentFilters.finalStatus;
    if (currentFilters.dateFrom) p.dateFrom = currentFilters.dateFrom;
    if (currentFilters.dateTo) p.dateTo = currentFilters.dateTo;
    if (currentFilters.errorOnly === 'true') p.errorOnly = true;
    var taskIdVal = currentFilters.taskId;
    var taskIdInput = document.getElementById('taskId-input');
    if (taskIdInput && taskIdInput.value.trim()) taskIdVal = taskIdInput.value.trim();
    if (taskIdVal) p.taskId = taskIdVal;
    return p;
}

function syncFiltersFromUrl() {
    const params = new URLSearchParams(window.location.search);
    if (params.has('userId')) currentFilters.userId = params.get('userId');
    if (params.has('finalStatus')) currentFilters.finalStatus = params.get('finalStatus');
    if (params.has('dateFrom')) currentFilters.dateFrom = params.get('dateFrom');
    if (params.has('dateTo')) currentFilters.dateTo = params.get('dateTo');
    if (params.has('errorOnly')) currentFilters.errorOnly = params.get('errorOnly');
    if (params.has('taskId')) {
        currentFilters.taskId = params.get('taskId');
        const inp = document.getElementById('taskId-input');
        if (inp) inp.value = currentFilters.taskId;
    }
    if (params.has('page')) currentPage = parseInt(params.get('page'), 10) || 0;
}

function updateUrl() {
    const params = new URLSearchParams();
    if (currentFilters.userId) params.set('userId', currentFilters.userId);
    if (currentFilters.finalStatus) params.set('finalStatus', currentFilters.finalStatus);
    if (currentFilters.dateFrom) params.set('dateFrom', currentFilters.dateFrom);
    if (currentFilters.dateTo) params.set('dateTo', currentFilters.dateTo);
    if (currentFilters.errorOnly) params.set('errorOnly', currentFilters.errorOnly);
    if (currentFilters.taskId) params.set('taskId', currentFilters.taskId);
    if (currentPage > 0) params.set('page', currentPage);
    const qs = params.toString();
    const url = qs ? window.location.pathname + '?' + qs : window.location.pathname;
    if (window.history.replaceState) window.history.replaceState({}, '', url);
}

async function loadLogs() {
    const tableEl = document.getElementById('logs-table');
    const emptyEl = document.getElementById('empty-state');
    const errorEl = document.getElementById('error-state');
    const retryBtn = document.getElementById('retry-btn');
    if (tableEl) tableEl.classList.remove('hidden');
    if (emptyEl) emptyEl.classList.add('hidden');
    if (errorEl) errorEl.classList.add('hidden');
    if (retryBtn) retryBtn.classList.add('hidden');

    try {
        const params = buildLogsQueryParams();
        const response = await api.getGenerationLogs(params);
        updateUrl();
        dataTable.setData(response);
        if (response.content && response.content.length === 0 && !errorEl.classList.contains('block')) {
            if (tableEl) tableEl.classList.add('hidden');
            if (emptyEl) emptyEl.classList.remove('hidden');
        }
    } catch (error) {
        console.error('Failed to load generation logs:', error);
        showNotification('Ошибка загрузки логов генерации', 'error');
        if (tableEl) tableEl.classList.add('hidden');
        if (errorEl) errorEl.classList.remove('hidden');
        if (retryBtn) retryBtn.classList.remove('hidden');
    }
}

function openDetail(taskId) {
    if (!taskId) return;
    const content = document.getElementById('detail-content');
    const modal = document.getElementById('detail-modal');
    content.innerHTML = '<p class="text-gray-500">Загрузка…</p>';
    modal.classList.remove('hidden');

    (async function() {
        try {
            const [session, events] = await Promise.all([
                api.getGenerationLogDetail(taskId),
                api.getGenerationLogEvents(taskId)
            ]);
            if (!session) {
                content.innerHTML = '<p class="text-red-600">Сессия не найдена.</p>';
                return;
            }
            let html = '<div class="space-y-4">';
            html += '<div><strong>Task ID:</strong> <code class="text-xs">' + escapeHtml(session.taskId) + '</code></div>';
            html += '<div><strong>User ID:</strong> ' + escapeHtml(String(session.userId)) + '</div>';
            html += '<div><strong>Статус:</strong> ' + escapeHtml(session.finalStatus || '-') + '</div>';
            html += '<div><strong>Старт:</strong> ' + formatDate(session.startedAt) + '</div>';
            html += '<div><strong>Завершение:</strong> ' + formatDate(session.completedAt) + '</div>';
            if (session.errorCode) html += '<div><strong>Код ошибки:</strong> <span class="text-red-600">' + escapeHtml(session.errorCode) + '</span></div>';
            if (session.errorMessage) html += '<div><strong>Ошибка:</strong> <pre class="text-xs bg-gray-100 p-2 rounded overflow-x-auto">' + escapeHtml(session.errorMessage) + '</pre></div>';
            html += '<div><strong>Исходный промпт:</strong><pre class="text-xs bg-gray-100 p-2 rounded whitespace-pre-wrap">' + escapeHtml(session.rawPrompt || '') + '</pre></div>';
            html += '<div><strong>Обработанный промпт:</strong><pre class="text-xs bg-gray-100 p-2 rounded whitespace-pre-wrap">' + escapeHtml(session.processedPrompt || '') + '</pre></div>';
            if (session.requestParams) html += '<div><strong>Параметры запроса:</strong><pre class="text-xs bg-gray-100 p-2 rounded">' + escapeHtml(session.requestParams) + '</pre></div>';
            html += '<div class="pt-2"><strong>Таймлайн событий:</strong></div><ul class="list-disc pl-6 space-y-1">';
            (events || []).forEach(function(ev) {
                html += '<li><span class="text-gray-600">' + formatDate(ev.createdAt) + '</span> ' + escapeHtml(ev.stage || '') + ' / ' + escapeHtml(ev.eventStatus || '') + (ev.errorMessage ? ' <span class="text-red-600">' + escapeHtml(ev.errorMessage.substring(0, 80)) + '</span>' : '') + '</li>';
            });
            html += '</ul></div>';
            content.innerHTML = html;
        } catch (e) {
            console.error(e);
            content.innerHTML = '<p class="text-red-600">Ошибка загрузки деталей.</p>';
        }
    })();
}

function closeDetailModal() {
    document.getElementById('detail-modal').classList.add('hidden');
}
