/**
 * Переиспользуемый компонент таблицы с пагинацией
 */

class DataTable {
    constructor(containerId, options = {}) {
        this.container = document.getElementById(containerId);
        this.columns = options.columns || [];
        this.data = [];
        this.currentPage = 0;
        this.pageSize = options.pageSize || 20;
        this.totalElements = 0;
        this.totalPages = 0;
        this.onPageChange = options.onPageChange || (() => {});
        this.onRowClick = options.onRowClick || null;
        this.onSelectionChange = options.onSelectionChange || null;
        this.selectedRows = new Set();
        this.selectable = options.selectable !== false;
        this.rowIdField = options.rowIdField || null;
        
        this.render();
    }
    
    getRowId(row) {
        if (this.rowIdField && row[this.rowIdField] != null) {
            return String(row[this.rowIdField]);
        }
        return String(row.id != null ? row.id : row.userId != null ? row.userId : '');
    }
    
    setData(pageResponse) {
        this.data = pageResponse.content || [];
        this.currentPage = pageResponse.page || 0;
        this.totalElements = pageResponse.totalElements || 0;
        this.totalPages = pageResponse.totalPages || 0;
        this.selectedRows.clear();
        this.render();
    }
    
    render() {
        if (!this.container) return;
        
        this.container.innerHTML = `
            <div class="admin-datatable-scroll">
                <table class="admin-datatable-table">
                    <thead>
                        <tr>
                            ${this.selectable ? '<th class="text-left w-10"><input type="checkbox" id="select-all-checkbox" class="admin-filter-checkbox rounded"></th>' : ''}
                            ${this.columns.map(col => `
                                <th class="text-left">
                                    ${col.label}
                                </th>
                            `).join('')}
                        </tr>
                    </thead>
                    <tbody>
                        ${this.data.length === 0 ? `
                            <tr>
                                <td colspan="${this.columns.length + (this.selectable ? 1 : 0)}" class="px-2 py-1.5 text-center" style="color: var(--admin-text-muted)">
                                    Нет данных
                                </td>
                            </tr>
                        ` : this.data.map(row => `
                            <tr class="admin-datatable-row ${this.onRowClick ? 'cursor-pointer' : ''}" data-row-id="${this.getRowId(row)}">
                                ${this.selectable ? `
                                    <td class="text-left" onclick="event.stopPropagation()">
                                        <input type="checkbox" class="row-checkbox admin-filter-checkbox rounded" data-row-id="${this.getRowId(row)}">
                                    </td>
                                ` : ''}
                                ${this.columns.map(col => `
                                    <td class="whitespace-nowrap text-xs ${col.className || ''}">
                                        ${this.renderCell(row, col)}
                                    </td>
                                `).join('')}
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
            
            ${this.renderPagination()}
        `;
        
        this.attachEventListeners();
    }
    
    renderCell(row, column) {
        if (column.render) {
            return column.render(row);
        }
        
        const value = this.getNestedValue(row, column.field);
        
        if (value === null || value === undefined) {
            return '-';
        }
        
        return escapeHtml(value.toString());
    }
    
    getNestedValue(obj, path) {
        return path.split('.').reduce((current, key) => current?.[key], obj);
    }
    
    renderPagination() {
        if (this.totalPages <= 1) return '';
        
        const startItem = this.currentPage * this.pageSize + 1;
        const endItem = Math.min((this.currentPage + 1) * this.pageSize, this.totalElements);
        
        return `
            <div class="admin-datatable-pagination flex items-center justify-between sm:px-3">
                <div class="flex-1 flex justify-between sm:hidden">
                    <button 
                        ${this.currentPage === 0 ? 'disabled' : ''}
                        class="admin-pager-btn relative inline-flex items-center px-2 py-1 text-xs font-medium rounded-md disabled:opacity-50 disabled:cursor-not-allowed"
                        onclick="dataTable.goToPage(${this.currentPage - 1})"
                    >
                        Назад
                    </button>
                    <button 
                        ${this.currentPage >= this.totalPages - 1 ? 'disabled' : ''}
                        class="ml-2 admin-pager-btn relative inline-flex items-center px-2 py-1 text-xs font-medium rounded-md disabled:opacity-50 disabled:cursor-not-allowed"
                        onclick="dataTable.goToPage(${this.currentPage + 1})"
                    >
                        Вперед
                    </button>
                </div>
                <div class="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between w-full">
                    <div>
                        <p class="text-xs" style="color: var(--admin-text)">
                            Показано <span class="font-medium">${startItem}</span> - <span class="font-medium">${endItem}</span> из <span class="font-medium">${this.totalElements}</span> записей
                        </p>
                    </div>
                    <div>
                        <nav class="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
                            ${this.renderPaginationButtons()}
                        </nav>
                    </div>
                </div>
            </div>
        `;
    }
    
    renderPaginationButtons() {
        const buttons = [];
        const maxButtons = 7;
        let startPage = Math.max(0, this.currentPage - Math.floor(maxButtons / 2));
        let endPage = Math.min(this.totalPages - 1, startPage + maxButtons - 1);
        
        if (endPage - startPage < maxButtons - 1) {
            startPage = Math.max(0, endPage - maxButtons + 1);
        }
        
        // Кнопка "Назад"
        buttons.push(`
            <button 
                ${this.currentPage === 0 ? 'disabled' : ''}
                class="admin-pager-btn relative inline-flex items-center px-1.5 py-1 rounded-l-md disabled:opacity-50 disabled:cursor-not-allowed"
                onclick="dataTable.goToPage(${this.currentPage - 1})"
            >
                ‹
            </button>
        `);
        
        // Первая страница
        if (startPage > 0) {
            buttons.push(this.renderPageButton(0));
            if (startPage > 1) {
                buttons.push('<span class="relative inline-flex items-center px-2 py-1 admin-pager-btn text-xs font-medium" style="color: var(--admin-text-muted)">...</span>');
            }
        }
        
        // Страницы
        for (let i = startPage; i <= endPage; i++) {
            buttons.push(this.renderPageButton(i));
        }
        
        // Последняя страница
        if (endPage < this.totalPages - 1) {
            if (endPage < this.totalPages - 2) {
                buttons.push('<span class="relative inline-flex items-center px-2 py-1 admin-pager-btn text-xs font-medium" style="color: var(--admin-text-muted)">...</span>');
            }
            buttons.push(this.renderPageButton(this.totalPages - 1));
        }
        
        // Кнопка "Вперед"
        buttons.push(`
            <button 
                ${this.currentPage >= this.totalPages - 1 ? 'disabled' : ''}
                class="admin-pager-btn relative inline-flex items-center px-1.5 py-1 rounded-r-md disabled:opacity-50 disabled:cursor-not-allowed"
                onclick="dataTable.goToPage(${this.currentPage + 1})"
            >
                ›
            </button>
        `);
        
        return buttons.join('');
    }
    
    renderPageButton(pageNum) {
        const isCurrent = pageNum === this.currentPage;
        return `
            <button 
                class="relative inline-flex items-center px-2 py-1 text-xs font-medium ${
                    isCurrent
                        ? 'border border-blue-500 z-10'
                        : 'admin-pager-btn'
                }"
                style="${isCurrent ? 'background: rgba(31, 111, 235, 0.15); color: #58a6ff' : ''}"
                onclick="dataTable.goToPage(${pageNum})"
            >
                ${pageNum + 1}
            </button>
        `;
    }
    
    goToPage(page) {
        if (page < 0 || page >= this.totalPages) return;
        this.currentPage = page;
        this.onPageChange(page);
    }
    
    attachEventListeners() {
        // Select all checkbox
        const selectAllCheckbox = this.container.querySelector('#select-all-checkbox');
        if (selectAllCheckbox) {
            selectAllCheckbox.addEventListener('change', (e) => {
                const checkboxes = this.container.querySelectorAll('.row-checkbox');
                checkboxes.forEach(cb => {
                    cb.checked = e.target.checked;
                    const rowId = cb.getAttribute('data-row-id');
                    if (e.target.checked) {
                        this.selectedRows.add(rowId);
                    } else {
                        this.selectedRows.delete(rowId);
                    }
                });
                if (this.onSelectionChange) {
                    this.onSelectionChange(Array.from(this.selectedRows));
                }
            });
        }
        
        // Row checkboxes
        const rowCheckboxes = this.container.querySelectorAll('.row-checkbox');
        rowCheckboxes.forEach(cb => {
            cb.addEventListener('change', (e) => {
                const rowId = cb.getAttribute('data-row-id');
                if (e.target.checked) {
                    this.selectedRows.add(rowId);
                } else {
                    this.selectedRows.delete(rowId);
                }
                
                // Update select all checkbox
                if (selectAllCheckbox) {
                    selectAllCheckbox.checked = rowCheckboxes.length === this.selectedRows.size;
                }
                
                if (this.onSelectionChange) {
                    this.onSelectionChange(Array.from(this.selectedRows));
                }
            });
        });
        
        // Row click
        if (this.onRowClick) {
            const rows = this.container.querySelectorAll('tbody tr[data-row-id]');
            rows.forEach(row => {
                row.addEventListener('click', (e) => {
                    const rowId = row.getAttribute('data-row-id');
                    const rowData = this.data.find(d => this.getRowId(d) === rowId);
                    if (rowData) {
                        this.onRowClick(rowData);
                    }
                });
            });
        }
    }
    
    getSelectedRows() {
        return Array.from(this.selectedRows);
    }
    
    clearSelection() {
        this.selectedRows.clear();
        const checkboxes = this.container.querySelectorAll('input[type="checkbox"]');
        checkboxes.forEach(cb => cb.checked = false);
        if (this.onSelectionChange) {
            this.onSelectionChange([]);
        }
    }
}
