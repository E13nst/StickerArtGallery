/**
 * Компонент фильтров для таблиц
 */

class FiltersPanel {
    constructor(containerId, options = {}) {
        this.container = document.getElementById(containerId);
        this.filters = options.filters || [];
        this.onFilterChange = options.onFilterChange || (() => {});
        this.values = {};
        
        this.render();
    }
    
    render() {
        if (!this.container) return;
        
        this.container.innerHTML = `
            <div class="bg-white p-2 rounded-lg shadow-md mb-4">
                <div class="flex items-center justify-between mb-2">
                    <h3 class="text-sm font-medium text-gray-900">Фильтры</h3>
                    <button 
                        id="reset-filters-btn"
                        class="text-xs text-blue-600 hover:text-blue-800"
                    >
                        Сбросить
                    </button>
                </div>
                
                <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-2">
                    ${this.filters.map(filter => this.renderFilter(filter)).join('')}
                </div>
                
                <div class="mt-2 flex justify-end">
                    <button 
                        id="apply-filters-btn"
                        class="px-2 py-1 text-xs bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                        Применить
                    </button>
                </div>
            </div>
        `;
        
        this.attachEventListeners();
    }
    
    renderFilter(filter) {
        switch (filter.type) {
            case 'text':
                return this.renderTextFilter(filter);
            case 'select':
                return this.renderSelectFilter(filter);
            case 'number':
                return this.renderNumberFilter(filter);
            case 'date':
                return this.renderDateFilter(filter);
            case 'checkbox':
                return this.renderCheckboxFilter(filter);
            default:
                return '';
        }
    }
    
    renderTextFilter(filter) {
        return `
            <div>
                <label class="block text-xs font-medium text-gray-700 mb-0.5">
                    ${filter.label}
                </label>
                <input 
                    type="text"
                    id="filter-${filter.name}"
                    name="${filter.name}"
                    placeholder="${filter.placeholder || ''}"
                    class="w-full px-2 py-1 text-xs border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                >
            </div>
        `;
    }
    
    renderSelectFilter(filter) {
        return `
            <div>
                <label class="block text-xs font-medium text-gray-700 mb-0.5">
                    ${filter.label}
                </label>
                <select 
                    id="filter-${filter.name}"
                    name="${filter.name}"
                    class="w-full px-2 py-1 text-xs border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                >
                    <option value="">Все</option>
                    ${filter.options.map(opt => `
                        <option value="${opt.value}">${opt.label}</option>
                    `).join('')}
                </select>
            </div>
        `;
    }
    
    renderNumberFilter(filter) {
        return `
            <div>
                <label class="block text-xs font-medium text-gray-700 mb-0.5">
                    ${filter.label}
                </label>
                <input 
                    type="number"
                    id="filter-${filter.name}"
                    name="${filter.name}"
                    placeholder="${filter.placeholder || ''}"
                    min="${filter.min || ''}"
                    max="${filter.max || ''}"
                    class="w-full px-2 py-1 text-xs border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                >
            </div>
        `;
    }
    
    renderDateFilter(filter) {
        return `
            <div>
                <label class="block text-xs font-medium text-gray-700 mb-0.5">
                    ${filter.label}
                </label>
                <input 
                    type="date"
                    id="filter-${filter.name}"
                    name="${filter.name}"
                    class="w-full px-2 py-1 text-xs border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                >
            </div>
        `;
    }
    
    renderCheckboxFilter(filter) {
        return `
            <div class="flex items-center h-full pt-4">
                <input 
                    type="checkbox"
                    id="filter-${filter.name}"
                    name="${filter.name}"
                    class="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                >
                <label for="filter-${filter.name}" class="ml-2 text-xs text-gray-700">
                    ${filter.label}
                </label>
            </div>
        `;
    }
    
    attachEventListeners() {
        // Apply button
        const applyBtn = this.container.querySelector('#apply-filters-btn');
        if (applyBtn) {
            applyBtn.addEventListener('click', () => {
                this.applyFilters();
            });
        }
        
        // Reset button
        const resetBtn = this.container.querySelector('#reset-filters-btn');
        if (resetBtn) {
            resetBtn.addEventListener('click', () => {
                this.resetFilters();
            });
        }
        
        // Enter key in text inputs
        const textInputs = this.container.querySelectorAll('input[type="text"], input[type="number"]');
        textInputs.forEach(input => {
            input.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.applyFilters();
                }
            });
        });
    }
    
    applyFilters() {
        this.values = {};
        
        this.filters.forEach(filter => {
            const element = this.container.querySelector(`#filter-${filter.name}`);
            if (!element) return;
            
            if (filter.type === 'checkbox') {
                if (element.checked) {
                    this.values[filter.name] = true;
                }
            } else {
                const value = element.value.trim();
                if (value) {
                    this.values[filter.name] = value;
                }
            }
        });
        
        this.onFilterChange(this.values);
    }
    
    resetFilters() {
        this.values = {};
        
        this.filters.forEach(filter => {
            const element = this.container.querySelector(`#filter-${filter.name}`);
            if (!element) return;
            
            if (filter.type === 'checkbox') {
                element.checked = false;
            } else {
                element.value = '';
            }
        });
        
        this.onFilterChange(this.values);
    }
    
    getValues() {
        return { ...this.values };
    }
    
    setValues(values) {
        this.values = { ...values };
        
        this.filters.forEach(filter => {
            const element = this.container.querySelector(`#filter-${filter.name}`);
            if (!element) return;
            
            const value = values[filter.name];
            if (value !== undefined && value !== null) {
                if (filter.type === 'checkbox') {
                    element.checked = !!value;
                } else {
                    element.value = value;
                }
            }
        });
    }
}
