// Stars Packages Management
// ========================================

checkAuth();

let packages = [];
let editingPackageId = null;

// Загрузка тарифов
async function loadPackages() {
    try {
        document.getElementById('loading').classList.remove('hidden');
        document.getElementById('no-data').classList.add('hidden');
        
        packages = await api.getStarsPackages();
        
        // Валидация данных
        if (!Array.isArray(packages)) {
            console.warn('Expected array, got:', packages);
            packages = [];
        }
        
        renderPackages();
        
        document.getElementById('loading').classList.add('hidden');
        if (packages.length === 0) {
            document.getElementById('no-data').classList.remove('hidden');
        }
    } catch (error) {
        console.error('Failed to load packages:', error);
        showNotification('Ошибка загрузки тарифов: ' + (error.message || 'Неизвестная ошибка'), 'error');
        document.getElementById('loading').classList.add('hidden');
    }
}

// Отрисовка таблицы
function renderPackages() {
    const tbody = document.getElementById('packages-table-body');
    
    if (packages.length === 0) {
        tbody.innerHTML = '';
        return;
    }
    
    tbody.innerHTML = packages
        .sort((a, b) => a.sortOrder - b.sortOrder)
        .map(pkg => `
            <tr class="hover:bg-gray-50">
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${pkg.id}</td>
                <td class="px-6 py-4 text-sm">
                    <div class="font-medium text-gray-900">${escapeHtml(pkg.name)}</div>
                    ${pkg.description ? `<div class="text-xs text-gray-500">${escapeHtml(pkg.description)}</div>` : ''}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${pkg.artAmount}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${pkg.starsPrice} ⭐</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm">
                    <span class="text-gray-400">—</span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${pkg.sortOrder}</td>
                <td class="px-6 py-4 whitespace-nowrap">
                    ${pkg.isEnabled 
                        ? '<span class="px-2 py-1 text-xs font-semibold text-green-800 bg-green-100 rounded-full">Активен</span>'
                        : '<span class="px-2 py-1 text-xs font-semibold text-gray-800 bg-gray-100 rounded-full">Неактивен</span>'
                    }
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm space-x-2">
                    <button onclick="editPackage(${pkg.id})" class="text-blue-600 hover:text-blue-800 font-medium">
                        Изменить
                    </button>
                    <button onclick="togglePackage(${pkg.id}, ${!pkg.isEnabled})" class="text-${pkg.isEnabled ? 'yellow' : 'green'}-600 hover:text-${pkg.isEnabled ? 'yellow' : 'green'}-800 font-medium">
                        ${pkg.isEnabled ? 'Деактивировать' : 'Активировать'}
                    </button>
                    <button onclick="deletePackage(${pkg.id})" class="text-red-600 hover:text-red-800 font-medium">
                        Удалить
                    </button>
                </td>
            </tr>
        `).join('');
}

// Открыть форму добавления тарифа
function openAddModal() {
    editingPackageId = null;
    document.getElementById('modal-title').textContent = 'Добавить тариф';
    document.getElementById('package-form').reset();
    document.getElementById('package-id').value = '';
    document.getElementById('package-enabled').checked = true;
    document.getElementById('edit-modal').classList.remove('hidden');
}

// Открыть форму редактирования
function editPackage(id) {
    const pkg = packages.find(p => p.id === id);
    if (!pkg) return;
    
    editingPackageId = id;
    document.getElementById('modal-title').textContent = 'Редактировать тариф';
    document.getElementById('package-id').value = pkg.id;
    document.getElementById('package-name').value = pkg.name;
    document.getElementById('package-description').value = pkg.description || '';
    document.getElementById('package-art-amount').value = pkg.artAmount;
    document.getElementById('package-price').value = pkg.starsPrice;
    document.getElementById('package-bonus').value = 0;
    document.getElementById('package-display-order').value = pkg.sortOrder;
    document.getElementById('package-enabled').checked = pkg.isEnabled;
    document.getElementById('edit-modal').classList.remove('hidden');
}

// Закрыть модальное окно
function closeModal() {
    document.getElementById('edit-modal').classList.add('hidden');
    editingPackageId = null;
}

// Сохранить тариф
async function savePackage(event) {
    event.preventDefault();
    
    const data = {
        code: editingPackageId ? packages.find(p => p.id === editingPackageId)?.code : 'PKG_' + Date.now(),
        name: document.getElementById('package-name').value.trim(),
        description: document.getElementById('package-description').value.trim() || null,
        artAmount: parseInt(document.getElementById('package-art-amount').value),
        starsPrice: parseInt(document.getElementById('package-price').value),
        sortOrder: parseInt(document.getElementById('package-display-order').value),
        isEnabled: document.getElementById('package-enabled').checked
    };
    
    try {
        if (editingPackageId) {
            await api.updateStarsPackage(editingPackageId, data);
            showNotification('Тариф успешно обновлен', 'success');
        } else {
            await api.createStarsPackage(data);
            showNotification('Тариф успешно создан', 'success');
        }
        
        closeModal();
        await loadPackages();
    } catch (error) {
        console.error('Failed to save package:', error);
        showNotification(error.message || 'Ошибка сохранения тарифа', 'error');
    }
}

// Переключить статус тарифа
async function togglePackage(id, enabled) {
    try {
        await api.toggleStarsPackage(id);
        showNotification(`Тариф ${enabled ? 'активирован' : 'деактивирован'}`, 'success');
        await loadPackages();
    } catch (error) {
        console.error('Failed to toggle package:', error);
        showNotification('Ошибка изменения статуса', 'error');
    }
}

// Удалить тариф
async function deletePackage(id) {
    if (!confirmAction('Удалить этот тариф? Это действие нельзя отменить.')) {
        return;
    }
    
    try {
        await api.deleteStarsPackage(id);
        showNotification('Тариф удален', 'success');
        await loadPackages();
    } catch (error) {
        console.error('Failed to delete package:', error);
        showNotification(error.message || 'Ошибка удаления тарифа', 'error');
    }
}

// Event listeners
document.getElementById('add-package-btn').addEventListener('click', openAddModal);
document.getElementById('package-form').addEventListener('submit', savePackage);
document.getElementById('logout-btn').addEventListener('click', logout);

// Загрузка при старте
loadPackages();
