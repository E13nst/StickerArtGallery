/**
 * Логика страницы лога отправки сообщений (админ)
 */

checkAuth();

let dataTable;
let filters;
let currentFilters = {};
let currentPage = 0;
const pageSize = 20;

const tableColumns = [
    {
        field: 'startedAt',
        label: 'Дата',
        render: (row) => formatDate(row.startedAt)
    },
    {
        field: 'messageId',
        label: 'Message ID',
        render: (row) => {
            const id = row.messageId || '';
            return `<span class="font-mono text-xs truncate max-w-[120px] block" title="${escapeHtml(id)}">${escapeHtml(id.substring(0, 12))}…</span>`;
        }
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
            const status = row.finalStatus || '-';
            const cls = status === 'SENT'
                ? 'bg-green-100 text-green-800'
                : status === 'FAILED'
                    ? 'bg-red-100 text-red-800'
                    : 'bg-gray-100 text-gray-800';
            return `<span class="inline-flex px-1.5 py-0.5 rounded text-xs font-medium ${cls}">${status}</span>`;
        }
    },
    {
        field: 'error',
        label: 'Ошибка',
        render: (row) => {
            if (!row.errorCode && !row.errorMessage) return '-';
            const msg = (row.errorMessage || '').substring(0, 60);
            const code = row.errorCode || '';
            const suffix = msg.length >= 60 ? '…' : '';
            return `<span class="text-red-600 text-xs" title="${escapeHtml(row.errorMessage || '')}">${escapeHtml(code)} ${escapeHtml(msg)}${suffix}</span>`;
        }
    },
    {
        field: 'actions',
        label: 'Действия',
        render: (row) => renderActionDropdown([
            { label: 'Детали', onclick: `openDetail('${String(row.messageId || '').replace(/'/g, "\\'")}')`, className: 'text-blue-600' }
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
            { value: 'SENT', label: 'SENT' },
            { value: 'FAILED', label: 'FAILED' }
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

    const messageIdInput = document.getElementById('message-id-input');
    if (messageIdInput) {
        messageIdInput.addEventListener('input', debounce(function() {
            const value = messageIdInput.value.trim();
            currentFilters.messageId = value || undefined;
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
    const params = { page: currentPage, size: pageSize };
    if (currentFilters.userId) params.userId = currentFilters.userId;
    if (currentFilters.finalStatus) params.finalStatus = currentFilters.finalStatus;
    if (currentFilters.dateFrom) params.dateFrom = currentFilters.dateFrom;
    if (currentFilters.dateTo) params.dateTo = currentFilters.dateTo;
    if (currentFilters.errorOnly === 'true') params.errorOnly = true;
    let messageIdVal = currentFilters.messageId;
    const messageIdInput = document.getElementById('message-id-input');
    if (messageIdInput && messageIdInput.value.trim()) messageIdVal = messageIdInput.value.trim();
    if (messageIdVal) params.messageId = messageIdVal;
    return params;
}

function syncFiltersFromUrl() {
    const params = new URLSearchParams(window.location.search);
    if (params.has('userId')) currentFilters.userId = params.get('userId');
    if (params.has('finalStatus')) currentFilters.finalStatus = params.get('finalStatus');
    if (params.has('dateFrom')) currentFilters.dateFrom = params.get('dateFrom');
    if (params.has('dateTo')) currentFilters.dateTo = params.get('dateTo');
    if (params.has('errorOnly')) currentFilters.errorOnly = params.get('errorOnly');
    if (params.has('messageId')) {
        currentFilters.messageId = params.get('messageId');
        const input = document.getElementById('message-id-input');
        if (input) input.value = currentFilters.messageId;
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
    if (currentFilters.messageId) params.set('messageId', currentFilters.messageId);
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
        const response = await api.getMessageLogs(params);
        updateUrl();
        dataTable.setData(response);
        if (response.content && response.content.length === 0 && !errorEl.classList.contains('block')) {
            if (tableEl) tableEl.classList.add('hidden');
            if (emptyEl) emptyEl.classList.remove('hidden');
        }
    } catch (error) {
        console.error('Failed to load message logs:', error);
        showNotification('Ошибка загрузки логов сообщений', 'error');
        if (tableEl) tableEl.classList.add('hidden');
        if (errorEl) errorEl.classList.remove('hidden');
        if (retryBtn) retryBtn.classList.remove('hidden');
    }
}

function openDetail(messageId) {
    if (!messageId) return;
    const content = document.getElementById('detail-content');
    const modal = document.getElementById('detail-modal');
    content.innerHTML = '<p class="text-gray-500">Загрузка…</p>';
    modal.classList.remove('hidden');

    (async function() {
        try {
            const [session, events] = await Promise.all([
                api.getMessageLogDetail(messageId),
                api.getMessageLogEvents(messageId)
            ]);
            if (!session) {
                content.innerHTML = '<p class="text-red-600">Сессия не найдена.</p>';
                return;
            }
            const statusCls = session.finalStatus === 'SENT'
                ? 'bg-green-100 text-green-800'
                : session.finalStatus === 'FAILED'
                    ? 'bg-red-100 text-red-800'
                    : 'bg-gray-100 text-gray-800';
            let html = '<div class="space-y-4">';
            html += '<div><strong>Message ID:</strong> <code class="text-xs">' + escapeHtml(session.messageId || '') + '</code></div>';
            html += '<div><strong>User ID:</strong> ' + escapeHtml(String(session.userId || '-')) + '</div>';
            html += '<div><strong>Chat ID (request):</strong> ' + escapeHtml(String(session.chatId || '-')) + '</div>';
            html += '<div><strong>Статус:</strong> <span class="inline-flex px-1.5 py-0.5 rounded text-xs font-medium ' + statusCls + '">' + escapeHtml(session.finalStatus || '-') + '</span></div>';
            html += '<div><strong>Parse mode:</strong> ' + escapeHtml(session.parseMode || '-') + '</div>';
            html += '<div><strong>Старт:</strong> ' + formatDate(session.startedAt) + '</div>';
            html += '<div><strong>Завершение:</strong> ' + formatDate(session.completedAt) + '</div>';
            html += '<div><strong>Telegram chat_id:</strong> ' + escapeHtml(String(session.telegramChatId || '-')) + '</div>';
            html += '<div><strong>Telegram message_id:</strong> ' + escapeHtml(String(session.telegramMessageId || '-')) + '</div>';
            if (session.retryOfMessageId) html += '<div><strong>Retry исходной сессии:</strong> <code class="text-xs text-orange-600">' + escapeHtml(session.retryOfMessageId) + '</code></div>';
            if (session.errorCode) html += '<div><strong>Код ошибки:</strong> <span class="text-red-600">' + escapeHtml(session.errorCode) + '</span></div>';
            if (session.errorMessage) html += '<div><strong>Причина ошибки:</strong> <pre class="text-xs bg-gray-100 p-2 rounded overflow-x-auto">' + escapeHtml(session.errorMessage) + '</pre></div>';
            html += '<div><strong>Текст сообщения:</strong><pre class="text-xs bg-gray-100 p-2 rounded whitespace-pre-wrap">' + escapeHtml(session.messageText || '') + '</pre></div>';
            if (session.requestPayload) html += '<div><strong>Payload запроса:</strong><pre class="text-xs bg-gray-100 p-2 rounded whitespace-pre-wrap">' + escapeHtml(session.requestPayload) + '</pre></div>';
            html += '<div class="pt-2"><strong>Таймлайн событий:</strong></div><ul class="list-disc pl-6 space-y-1">';
            (events || []).forEach(function(ev) {
                html += '<li><span class="text-gray-600">' + formatDate(ev.createdAt) + '</span> ' +
                    escapeHtml(ev.stage || '') + ' / ' + escapeHtml(ev.eventStatus || '') +
                    (ev.errorMessage ? ' <span class="text-red-600">' + escapeHtml(ev.errorMessage.substring(0, 100)) + '</span>' : '') +
                    '</li>';
            });
            html += '</ul>';

            if (session.finalStatus === 'FAILED') {
                const safeId = (session.messageId || '').replace(/'/g, "\\'");
                html += '<div class="pt-4 border-t border-gray-200 mt-4">';
                html += '<button id="detail-retry-btn" onclick="retryMessage(\'' + safeId + '\')" ' +
                    'class="inline-flex items-center gap-2 bg-orange-500 hover:bg-orange-600 disabled:opacity-50 disabled:cursor-not-allowed ' +
                    'text-white text-sm font-medium px-4 py-2 rounded transition-colors">' +
                    '🔄 Повторить отправку' +
                    '</button>';
                html += '</div>';
            }

            html += '</div>';
            content.innerHTML = html;
        } catch (error) {
            console.error(error);
            content.innerHTML = '<p class="text-red-600">Ошибка загрузки деталей.</p>';
        }
    })();
}

async function retryMessage(messageId) {
    if (!messageId) return;
    if (!confirmAction('Повторить отправку сообщения пользователю?')) return;

    const btn = document.getElementById('detail-retry-btn');
    if (btn) {
        btn.disabled = true;
        btn.textContent = '⏳ Отправка…';
    }

    try {
        const result = await api.retryMessageLog(messageId);
        showNotification('✅ Повторная отправка запущена. Новый ID: ' + (result && result.retryMessageId ? result.retryMessageId.substring(0, 12) + '…' : ''), 'success');
        // Обновить список и детали с небольшой задержкой, чтобы дать время async задаче стартовать
        setTimeout(async function() {
            await loadLogs();
            if (result && result.retryMessageId) {
                openDetail(result.retryMessageId);
            }
        }, 1500);
    } catch (error) {
        const msg = (error && error.data && error.data.message) ? error.data.message : 'Ошибка запуска повторной отправки';
        const isConflict = error && error.status === 409;
        showNotification(isConflict ? '⚠️ ' + msg : '❌ ' + msg, isConflict ? 'warning' : 'error');
        if (btn) {
            btn.disabled = false;
            btn.textContent = '🔄 Повторить отправку';
        }
    }
}

function closeDetailModal() {
    document.getElementById('detail-modal').classList.add('hidden');
}
