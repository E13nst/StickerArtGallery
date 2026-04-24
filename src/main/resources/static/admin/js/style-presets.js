// Global Style Presets Management
// ========================================

checkAuth();

let presets = [];
let editingPresetId = null;

// Загрузка пресетов
async function loadPresets() {
    try {
        document.getElementById('loading').classList.remove('hidden');
        document.getElementById('no-data').classList.add('hidden');
        
        presets = await api.getGlobalStylePresets();
        
        // Валидация данных
        if (!Array.isArray(presets)) {
            console.warn('Expected array, got:', presets);
            presets = [];
        }
        
        renderPresets();
        
        document.getElementById('loading').classList.add('hidden');
        if (presets.length === 0) {
            document.getElementById('no-data').classList.remove('hidden');
        }
    } catch (error) {
        console.error('Failed to load presets:', error);
        showNotification('Ошибка загрузки пресетов: ' + (error.message || 'Неизвестная ошибка'), 'error');
        document.getElementById('loading').classList.add('hidden');
    }
}

// Отрисовка таблицы
function renderPresets() {
    const tbody = document.getElementById('presets-table-body');
    
    if (presets.length === 0) {
        tbody.innerHTML = '';
        return;
    }
    
    tbody.innerHTML = presets
        .sort((a, b) => a.sortOrder - b.sortOrder)
        .map(preset => `
            <tr class="hover:bg-gray-50">
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${preset.id}</td>
                <td class="px-6 py-4 text-sm">
                    <div class="font-medium text-gray-900">${escapeHtml(preset.name)}</div>
                    ${preset.description ? `<div class="text-xs text-gray-500 mt-1">${escapeHtml(preset.description)}</div>` : ''}
                </td>
                <td class="px-6 py-4 text-sm">
                    <div class="font-mono text-xs bg-gray-50 p-2 rounded max-w-xs overflow-x-auto">
                        ${preset.promptSuffix ? escapeHtml(preset.promptSuffix.substring(0, 80)) + (preset.promptSuffix.length > 80 ? '...' : '') : '<span class="text-gray-400">—</span>'}
                    </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm">
                    ${renderRemoveBackgroundPolicy(preset.removeBackground)}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${preset.sortOrder}</td>
                <td class="px-6 py-4 whitespace-nowrap">
                    ${preset.isEnabled 
                        ? '<span class="px-2 py-1 text-xs font-semibold text-green-800 bg-green-100 rounded-full">Активен</span>'
                        : '<span class="px-2 py-1 text-xs font-semibold text-gray-800 bg-gray-100 rounded-full">Неактивен</span>'
                    }
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm">
                    ${renderActionDropdown([
                        { label: 'Изменить', onclick: `editPreset(${preset.id})`, className: 'text-blue-600' },
                        { label: preset.isEnabled ? 'Выкл' : 'Вкл', onclick: `togglePreset(${preset.id}, ${!preset.isEnabled})`, className: preset.isEnabled ? 'text-yellow-600' : 'text-green-600' },
                        { label: 'Удалить', onclick: `deletePreset(${preset.id})`, className: 'text-red-600' }
                    ])}
                </td>
            </tr>
        `).join('');
}

// Открыть форму добавления
function openAddModal() {
    editingPresetId = null;
    document.getElementById('modal-title').textContent = 'Добавить пресет';
    document.getElementById('preset-form').reset();
    document.getElementById('preset-id').value = '';
    document.getElementById('preset-remove-background').value = '';
    document.getElementById('preset-enabled').checked = true;
    document.getElementById('edit-modal').classList.remove('hidden');
}

// Открыть форму редактирования
function editPreset(id) {
    const preset = presets.find(p => p.id === id);
    if (!preset) return;
    
    editingPresetId = id;
    document.getElementById('modal-title').textContent = 'Редактировать пресет';
    document.getElementById('preset-id').value = preset.id;
    document.getElementById('preset-name').value = preset.name;
    document.getElementById('preset-description').value = preset.description || '';
    document.getElementById('preset-style-prompt').value = preset.promptSuffix;
    document.getElementById('preset-remove-background').value = toRemoveBackgroundFormValue(preset.removeBackground);
    document.getElementById('preset-display-order').value = preset.sortOrder;
    document.getElementById('preset-enabled').checked = preset.isEnabled;
    document.getElementById('edit-modal').classList.remove('hidden');
}

// Закрыть модальное окно
function closeModal() {
    document.getElementById('edit-modal').classList.add('hidden');
    editingPresetId = null;
}

// Сохранить пресет
async function savePreset(event) {
    event.preventDefault();
    
    const data = {
        code: editingPresetId ? presets.find(p => p.id === editingPresetId)?.code : 'preset_' + Date.now(),
        name: document.getElementById('preset-name').value.trim(),
        description: document.getElementById('preset-description').value.trim() || null,
        promptSuffix: document.getElementById('preset-style-prompt').value.trim(),
        removeBackground: parseRemoveBackgroundValue(document.getElementById('preset-remove-background').value),
        sortOrder: parseInt(document.getElementById('preset-display-order').value)
    };
    const enabled = document.getElementById('preset-enabled').checked;
    
    try {
        if (editingPresetId) {
            const updatedPreset = await api.updateGlobalStylePreset(editingPresetId, data);
            if (!!updatedPreset?.isEnabled !== enabled) {
                await api.toggleGlobalStylePreset(editingPresetId, enabled);
            }
            showNotification('Пресет успешно обновлен', 'success');
        } else {
            const createdPreset = await api.createGlobalStylePreset(data);
            if (!!createdPreset?.isEnabled !== enabled) {
                await api.toggleGlobalStylePreset(createdPreset.id, enabled);
            }
            showNotification('Пресет успешно создан', 'success');
        }
        
        closeModal();
        await loadPresets();
    } catch (error) {
        console.error('Failed to save preset:', error);
        showNotification(error.message || 'Ошибка сохранения пресета', 'error');
    }
}

// Переключить статус
async function togglePreset(id, enabled) {
    try {
        await api.toggleGlobalStylePreset(id, enabled);
        showNotification(`Пресет ${enabled ? 'активирован' : 'деактивирован'}`, 'success');
        await loadPresets();
    } catch (error) {
        console.error('Failed to toggle preset:', error);
        showNotification('Ошибка изменения статуса', 'error');
    }
}

// Удалить пресет
async function deletePreset(id) {
    if (!confirmAction('Удалить этот пресет? Это действие нельзя отменить.')) {
        return;
    }
    
    try {
        await api.deleteGlobalStylePreset(id);
        showNotification('Пресет удален', 'success');
        await loadPresets();
    } catch (error) {
        console.error('Failed to delete preset:', error);
        showNotification(error.message || 'Ошибка удаления пресета', 'error');
    }
}

// Event listeners
document.getElementById('add-preset-btn').addEventListener('click', openAddModal);
document.getElementById('preset-form').addEventListener('submit', savePreset);

// Загрузка при старте
loadPresets();

function parseRemoveBackgroundValue(value) {
    if (value === 'true') {
        return true;
    }
    if (value === 'false') {
        return false;
    }
    return null;
}

function toRemoveBackgroundFormValue(value) {
    if (value === true) {
        return 'true';
    }
    if (value === false) {
        return 'false';
    }
    return '';
}

function renderRemoveBackgroundPolicy(value) {
    if (value === true) {
        return '<span class="px-2 py-1 text-xs font-semibold text-blue-800 bg-blue-100 rounded-full">Удалять</span>';
    }
    if (value === false) {
        return '<span class="px-2 py-1 text-xs font-semibold text-orange-800 bg-orange-100 rounded-full">Не удалять</span>';
    }
    return '<span class="text-gray-400">Fallback</span>';
}
