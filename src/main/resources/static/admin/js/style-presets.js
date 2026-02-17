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
                    <span class="text-gray-400">—</span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${preset.sortOrder}</td>
                <td class="px-6 py-4 whitespace-nowrap">
                    ${preset.isEnabled 
                        ? '<span class="px-2 py-1 text-xs font-semibold text-green-800 bg-green-100 rounded-full">Активен</span>'
                        : '<span class="px-2 py-1 text-xs font-semibold text-gray-800 bg-gray-100 rounded-full">Неактивен</span>'
                    }
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm space-x-2">
                    <button onclick="editPreset(${preset.id})" class="text-blue-600 hover:text-blue-800 font-medium">
                        Изменить
                    </button>
                    <button onclick="togglePreset(${preset.id}, ${!preset.isEnabled})" class="text-${preset.isEnabled ? 'yellow' : 'green'}-600 hover:text-${preset.isEnabled ? 'yellow' : 'green'}-800 font-medium">
                        ${preset.isEnabled ? 'Выкл' : 'Вкл'}
                    </button>
                    <button onclick="deletePreset(${preset.id})" class="text-red-600 hover:text-red-800 font-medium">
                        Удалить
                    </button>
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
    document.getElementById('preset-thumbnail').value = '';
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
        sortOrder: parseInt(document.getElementById('preset-display-order').value)
    };
    
    try {
        if (editingPresetId) {
            await api.updateGlobalStylePreset(editingPresetId, data);
            showNotification('Пресет успешно обновлен', 'success');
        } else {
            await api.createGlobalStylePreset(data);
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
