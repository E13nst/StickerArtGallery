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
        
        this.render();
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
            <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-gray-200">
                    <thead class="bg-gray-50">
                        <tr>
                            ${this.selectable ? '<th class="px-6 py-3 text-left"><input type="checkbox" id="select-all-checkbox" class="rounded border-gray-300"></th>' : ''}
                            ${this.columns.map(col => `
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    ${col.label}
                                </th>
                            `).join('')}
                        </tr>
                    </thead>
                    <tbody class="bg-white divide-y divide-gray-200">
                        ${this.data.length === 0 ? `
                            <tr>
                                <td colspan="${this.columns.length + (this.selectable ? 1 : 0)}" class="px-6 py-4 text-center text-gray-500">
                                    Нет данных
                                </td>
                            </tr>
                        ` : this.data.map(row => `
                            <tr class="hover:bg-gray-50 ${this.onRowClick ? 'cursor-pointer' : ''}" data-row-id="${row.id || row.userId || ''}">
                                ${this.selectable ? `
                                    <td class="px-6 py-4" onclick="event.stopPropagation()">
                                        <input type="checkbox" class="row-checkbox rounded border-gray-300" data-row-id="${row.id || row.userId || ''}">
                                    </td>
                                ` : ''}
                                ${this.columns.map(col => `
                                    <td class="px-6 py-4 whitespace-nowrap text-sm ${col.className || ''}">
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
            <div class="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6">
                <div class="flex-1 flex justify-between sm:hidden">
                    <button 
                        ${this.currentPage === 0 ? 'disabled' : ''}
                        class="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                        onclick="dataTable.goToPage(${this.currentPage - 1})"
                    >
                        Назад
                    </button>
                    <button 
                        ${this.currentPage >= this.totalPages - 1 ? 'disabled' : ''}
                        class="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                        onclick="dataTable.goToPage(${this.currentPage + 1})"
                    >
                        Вперед
                    </button>
                </div>
                <div class="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
                    <div>
                        <p class="text-sm text-gray-700">
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
                class="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                onclick="dataTable.goToPage(${this.currentPage - 1})"
            >
                ‹
            </button>
        `);
        
        // Первая страница
        if (startPage > 0) {
            buttons.push(this.renderPageButton(0));
            if (startPage > 1) {
                buttons.push('<span class="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700">...</span>');
            }
        }
        
        // Страницы
        for (let i = startPage; i <= endPage; i++) {
            buttons.push(this.renderPageButton(i));
        }
        
        // Последняя страница
        if (endPage < this.totalPages - 1) {
            if (endPage < this.totalPages - 2) {
                buttons.push('<span class="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700">...</span>');
            }
            buttons.push(this.renderPageButton(this.totalPages - 1));
        }
        
        // Кнопка "Вперед"
        buttons.push(`
            <button 
                ${this.currentPage >= this.totalPages - 1 ? 'disabled' : ''}
                class="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
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
                class="relative inline-flex items-center px-4 py-2 border ${isCurrent ? 'border-blue-500 bg-blue-50 text-blue-600 z-10' : 'border-gray-300 bg-white text-gray-700 hover:bg-gray-50'} text-sm font-medium"
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
                    const rowData = this.data.find(d => (d.id || d.userId) == rowId);
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
