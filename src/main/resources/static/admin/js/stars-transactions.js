/**
 * Логика страницы журнала Stars транзакций (админ)
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
        field: 'packageCode',
        label: 'Package',
        render: (row) => {
            const name = row.packageName || row.packageCode || '-';
            return `<span class="text-xs" title="${escapeHtml(row.packageName || '')}">${escapeHtml(name)}</span>`;
        }
    },
    {
        field: 'starsPaid',
        label: 'Stars Paid',
        render: (row) => `<span class="font-mono text-xs text-yellow-600">⭐ ${row.starsPaid || 0}</span>`
    },
    {
        field: 'artCredited',
        label: 'ART Credited',
        render: (row) => `<span class="font-mono text-xs text-green-600">+${row.artCredited || 0}</span>`
    }
];

const filterConfig = [
    { name: 'userId', label: 'User ID', type: 'text' },
    { name: 'dateFrom', label: 'Дата от', type: 'datetime' },
    { name: 'dateTo', label: 'Дата до', type: 'datetime' }
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
    filters.setValues(currentFilters);
    await loadTransactions();
});

function buildQueryParams() {
    const p = { page: currentPage, size: pageSize };
    if (currentFilters.userId) p.userId = currentFilters.userId;
    if (currentFilters.dateFrom) p.dateFrom = currentFilters.dateFrom;
    if (currentFilters.dateTo) p.dateTo = currentFilters.dateTo;
    return p;
}

function syncFiltersFromUrl() {
    const params = new URLSearchParams(window.location.search);
    if (params.has('userId')) currentFilters.userId = params.get('userId');
    if (params.has('dateFrom')) currentFilters.dateFrom = params.get('dateFrom');
    if (params.has('dateTo')) currentFilters.dateTo = params.get('dateTo');
    if (params.has('page')) currentPage = parseInt(params.get('page'), 10) || 0;
}

function updateUrl() {
    const params = new URLSearchParams();
    if (currentFilters.userId) params.set('userId', currentFilters.userId);
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
        const response = await api.getStarsTransactions(params);
        updateUrl();
        dataTable.setData(response);
        if (response.content && response.content.length === 0 && !errorEl.classList.contains('block')) {
            if (tableEl) tableEl.classList.add('hidden');
            if (emptyEl) emptyEl.classList.remove('hidden');
        }
    } catch (error) {
        console.error('Failed to load Stars transactions:', error);
        showNotification('Ошибка загрузки Stars транзакций', 'error');
        if (tableEl) tableEl.classList.add('hidden');
        if (errorEl) errorEl.classList.remove('hidden');
    }
}
