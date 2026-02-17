/**
 * Логика страницы журнала ART транзакций (админ)
 */

checkAuth();

let dataTable;
let filters;
let currentFilters = {};
let currentPage = 0;
const pageSize = 20;

const tableColumns = [
    {
        field: 'createdAt',
        label: 'Дата',
        render: (row) => formatDate(row.createdAt)
    },
    {
        field: 'userId',
        label: 'User ID',
        render: (row) => `<span class="font-mono text-xs">${row.userId}</span>` || '-'
    },
    {
        field: 'direction',
        label: 'Направление',
        render: (row) => {
            const dir = row.direction || '-';
            const cls = dir === 'CREDIT' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800';
            return `<span class="inline-flex px-1.5 py-0.5 rounded text-xs font-medium ${cls}">${dir}</span>`;
        }
    },
    {
        field: 'delta',
        label: 'Сумма',
        render: (row) => {
            const delta = row.delta || 0;
            const cls = delta > 0 ? 'text-green-600' : delta < 0 ? 'text-red-600' : '';
            return `<span class="${cls} font-mono text-xs">${delta > 0 ? '+' : ''}${delta}</span>`;
        }
    },
    {
        field: 'ruleCode',
        label: 'Rule Code',
        render: (row) => `<span class="text-xs">${escapeHtml(row.ruleCode || '-')}</span>`
    },
    {
        field: 'externalId',
        label: 'External ID',
        render: (row) => {
            if (!row.externalId) return '-';
            const short = row.externalId.substring(0, 12);
            return `<span class="font-mono text-xs truncate max-w-[100px] block" title="${escapeHtml(row.externalId)}">${escapeHtml(short)}…</span>`;
        }
    }
];

const filterConfig = [
    { name: 'userId', label: 'User ID', type: 'text' },
    {
        name: 'direction',
        label: 'Направление',
        type: 'select',
        options: [
            { value: '', label: 'Все' },
            { value: 'CREDIT', label: 'CREDIT' },
            { value: 'DEBIT', label: 'DEBIT' }
        ]
    },
    { name: 'dateFrom', label: 'Дата от (ISO)', type: 'text' },
    { name: 'dateTo', label: 'Дата до (ISO)', type: 'text' }
];

document.addEventListener('DOMContentLoaded', async function() {
    dataTable = new DataTable('transactions-table', {
        columns: tableColumns,
        pageSize: pageSize,
        selectable: false,
        onPageChange: (page) => {
            currentPage = page;
            loadTransactions();
        }
    });

    filters = new FiltersPanel('filters-container', {
        filters: filterConfig,
        onFilterChange: (filterValues) => {
            currentFilters = filterValues;
            currentPage = 0;
            loadTransactions();
        }
    });

    const errorRetryBtn = document.getElementById('error-retry-btn');
    if (errorRetryBtn) errorRetryBtn.addEventListener('click', loadTransactions);

    syncFiltersFromUrl();
    await loadTransactions();
});

function buildQueryParams() {
    const p = { page: currentPage, size: pageSize };
    if (currentFilters.userId) p.userId = currentFilters.userId;
    if (currentFilters.direction) p.direction = currentFilters.direction;
    if (currentFilters.dateFrom) p.dateFrom = currentFilters.dateFrom;
    if (currentFilters.dateTo) p.dateTo = currentFilters.dateTo;
    return p;
}

function syncFiltersFromUrl() {
    const params = new URLSearchParams(window.location.search);
    if (params.has('userId')) currentFilters.userId = params.get('userId');
    if (params.has('direction')) currentFilters.direction = params.get('direction');
    if (params.has('dateFrom')) currentFilters.dateFrom = params.get('dateFrom');
    if (params.has('dateTo')) currentFilters.dateTo = params.get('dateTo');
    if (params.has('page')) currentPage = parseInt(params.get('page'), 10) || 0;
}

function updateUrl() {
    const params = new URLSearchParams();
    if (currentFilters.userId) params.set('userId', currentFilters.userId);
    if (currentFilters.direction) params.set('direction', currentFilters.direction);
    if (currentFilters.dateFrom) params.set('dateFrom', currentFilters.dateFrom);
    if (currentFilters.dateTo) params.set('dateTo', currentFilters.dateTo);
    if (currentPage > 0) params.set('page', currentPage);
    const qs = params.toString();
    const url = qs ? window.location.pathname + '?' + qs : window.location.pathname;
    if (window.history.replaceState) window.history.replaceState({}, '', url);
}

async function loadTransactions() {
    const tableEl = document.getElementById('transactions-table');
    const emptyEl = document.getElementById('empty-state');
    const errorEl = document.getElementById('error-state');
    if (tableEl) tableEl.classList.remove('hidden');
    if (emptyEl) emptyEl.classList.add('hidden');
    if (errorEl) errorEl.classList.add('hidden');

    try {
        const params = buildQueryParams();
        const response = await api.getArtTransactions(params);
        updateUrl();
        dataTable.setData(response);
        if (response.content && response.content.length === 0 && !errorEl.classList.contains('block')) {
            if (tableEl) tableEl.classList.add('hidden');
            if (emptyEl) emptyEl.classList.remove('hidden');
        }
    } catch (error) {
        console.error('Failed to load ART transactions:', error);
        showNotification('Ошибка загрузки ART транзакций', 'error');
        if (tableEl) tableEl.classList.add('hidden');
        if (errorEl) errorEl.classList.remove('hidden');
    }
}
