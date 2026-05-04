// Stars Packages Management
// ========================================

checkAuth();

let packages = [];
let editingPackageId = null;
let tonPaymentSettings = null;

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

async function loadTonPaymentSettings() {
    try {
        tonPaymentSettings = await api.getTonPaymentSettings();
        document.getElementById('ton-merchant-wallet').value = tonPaymentSettings.merchantWalletAddress || '';
        document.getElementById('ton-payments-enabled').checked = tonPaymentSettings.isEnabled !== false;
        renderTonSettingsStatus();
    } catch (error) {
        console.error('Failed to load TON payment settings:', error);
        document.getElementById('ton-settings-status').textContent = 'Не удалось загрузить TON Pay настройки';
    }
}

function renderTonSettingsStatus() {
    const statusEl = document.getElementById('ton-settings-status');
    const sourceEl = document.getElementById('ton-settings-source');
    if (!tonPaymentSettings) {
        statusEl.textContent = 'Настройки не загружены';
        sourceEl.textContent = '—';
        return;
    }
    sourceEl.textContent = tonPaymentSettings.source === 'db'
        ? 'из админки'
        : (tonPaymentSettings.source === 'env' ? 'из env fallback' : 'не настроен');
    if (tonPaymentSettings.isConfigured) {
        statusEl.textContent = tonPaymentSettings.isEnabled === false
            ? 'Кошелёк задан, но TON-оплата глобально отключена'
            : 'Кошелёк настроен. Он будет подставляться в TON Pay message на сервере.';
    } else {
        statusEl.textContent = 'Кошелёк не настроен. TON-покупки ART будут недоступны.';
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
            <tr class="hover:bg-gray-50 dark:hover:bg-slate-800/50">
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-slate-100">${pkg.id}</td>
                <td class="px-6 py-4 text-sm">
                    <div class="font-medium text-gray-900 dark:text-slate-100">${escapeHtml(pkg.name)}</div>
                    ${pkg.description ? `<div class="text-xs text-gray-500 dark:text-slate-400">${escapeHtml(pkg.description)}</div>` : ''}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-slate-100">${pkg.artAmount}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-slate-100">${pkg.starsPrice} ⭐</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-slate-100">${formatTonPrice(pkg.tonPriceNano)}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm">
                    <span class="text-gray-400 dark:text-slate-500">—</span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-slate-100">${pkg.sortOrder}</td>
                <td class="px-6 py-4 whitespace-nowrap">
                    ${pkg.isEnabled 
                        ? '<span class="px-2 py-1 text-xs font-semibold text-green-800 bg-green-100 dark:text-green-200 dark:bg-green-950/50 rounded-full">Активен</span>'
                        : '<span class="px-2 py-1 text-xs font-semibold text-gray-800 bg-gray-100 dark:text-slate-200 dark:bg-slate-700 rounded-full">Неактивен</span>'
                    }
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm">
                    ${renderActionDropdown([
                        { label: 'Изменить', onclick: `editPackage(${pkg.id})`, className: 'text-blue-600' },
                        { label: pkg.isEnabled ? 'Деактивировать' : 'Активировать', onclick: `togglePackage(${pkg.id}, ${!pkg.isEnabled})`, className: pkg.isEnabled ? 'text-yellow-600' : 'text-green-600' },
                        { label: 'Удалить', onclick: `deletePackage(${pkg.id})`, className: 'text-red-600' }
                    ])}
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
    document.getElementById('package-ton-price').value = '';
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
    document.getElementById('package-ton-price').value = nanoToTonInput(pkg.tonPriceNano);
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
    
    const tonPriceNano = tonInputToNano(document.getElementById('package-ton-price').value);
    const data = {
        code: editingPackageId ? packages.find(p => p.id === editingPackageId)?.code : 'PKG_' + Date.now(),
        name: document.getElementById('package-name').value.trim(),
        description: document.getElementById('package-description').value.trim() || null,
        artAmount: parseInt(document.getElementById('package-art-amount').value),
        starsPrice: parseInt(document.getElementById('package-price').value),
        tonPriceNano,
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

async function saveTonPaymentSettings(event) {
    event.preventDefault();
    const data = {
        merchantWalletAddress: document.getElementById('ton-merchant-wallet').value.trim() || null,
        isEnabled: document.getElementById('ton-payments-enabled').checked
    };
    try {
        tonPaymentSettings = await api.updateTonPaymentSettings(data);
        renderTonSettingsStatus();
        showNotification('TON Pay настройки сохранены', 'success');
    } catch (error) {
        console.error('Failed to save TON payment settings:', error);
        showNotification(error.message || 'Ошибка сохранения TON Pay настроек', 'error');
    }
}

function formatTonPrice(nano) {
    if (!nano || nano <= 0) {
        return '<span class="text-gray-400 dark:text-slate-500">—</span>';
    }
    return `${nanoToTonInput(nano)} TON`;
}

function nanoToTonInput(nano) {
    if (!nano || nano <= 0) return '';
    const whole = Math.floor(nano / 1000000000);
    const fraction = String(nano % 1000000000).padStart(9, '0').replace(/0+$/, '');
    return fraction ? `${whole}.${fraction}` : String(whole);
}

function tonInputToNano(value) {
    if (!value || !value.trim()) return null;
    const normalized = value.trim().replace(',', '.');
    const [wholePart, fractionPart = ''] = normalized.split('.');
    const whole = parseInt(wholePart || '0', 10);
    const fraction = (fractionPart + '000000000').slice(0, 9);
    const nano = whole * 1000000000 + parseInt(fraction || '0', 10);
    return nano > 0 ? nano : null;
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
document.getElementById('ton-settings-form').addEventListener('submit', saveTonPaymentSettings);

// Загрузка при старте
loadPackages();
loadTonPaymentSettings();
