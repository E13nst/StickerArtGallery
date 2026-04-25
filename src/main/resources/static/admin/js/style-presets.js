// Global Style Presets Management
// ========================================

checkAuth();

let presets = [];
let editingPresetId = null;

const DEFAULT_PROMPT_INPUT = {
    enabled: true,
    required: false,
    placeholder: 'Опиши идею',
    maxLength: 500
};

const DEFAULT_FIELDS = [
    {
        key: 'emotion',
        label: 'Эмоция',
        description: 'Добавь, какую эмоцию должен изображать персонаж',
        placeholder: 'Например: радость',
        type: 'emoji',
        required: true,
        maxLength: 40,
        options: []
    },
    {
        key: 'productName',
        label: 'Название продукта',
        description: 'Введи название продукта или объекта, если оно должно попасть в результат',
        placeholder: 'Например: Stixly',
        type: 'text',
        required: false,
        maxLength: 60,
        options: []
    }
];

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
                <td class="px-6 py-4 whitespace-nowrap">
                    ${renderPreview(preset)}
                </td>
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
    applyPromptInputToForm(DEFAULT_PROMPT_INPUT);
    renderFieldEditor(DEFAULT_FIELDS);
    document.getElementById('current-preview-wrap').classList.add('hidden');
    document.getElementById('current-preview-img').src = '';
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
    applyPromptInputToForm(preset.promptInput || DEFAULT_PROMPT_INPUT);
    renderFieldEditor(Array.isArray(preset.fields) ? preset.fields : []);
    const previewUrl = getPresetPreviewUrl(preset);
    if (previewUrl) {
        document.getElementById('current-preview-img').src = previewUrl;
        document.getElementById('current-preview-wrap').classList.remove('hidden');
    } else {
        document.getElementById('current-preview-wrap').classList.add('hidden');
        document.getElementById('current-preview-img').src = '';
    }
    document.getElementById('preset-preview').value = '';
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
    const promptInput = readPromptInputFromForm();
    const fields = readFieldsFromForm();
    const promptSuffix = document.getElementById('preset-style-prompt').value.trim();
    const validationError = validatePresetUiContract(promptSuffix, promptInput, fields);
    if (validationError) {
        showNotification(validationError, 'error');
        return;
    }
    
    const data = {
        code: editingPresetId ? presets.find(p => p.id === editingPresetId)?.code : 'preset_' + Date.now(),
        name: document.getElementById('preset-name').value.trim(),
        description: document.getElementById('preset-description').value.trim() || null,
        promptSuffix,
        removeBackground: parseRemoveBackgroundValue(document.getElementById('preset-remove-background').value),
        sortOrder: parseInt(document.getElementById('preset-display-order').value),
        uiMode: document.getElementById('preset-ui-mode').value,
        promptInput,
        fields
    };
    const enabled = document.getElementById('preset-enabled').checked;
    const previewFile = document.getElementById('preset-preview').files[0] || null;
    
    try {
        let savedPresetId = editingPresetId;
        if (editingPresetId) {
            const updatedPreset = await api.updateGlobalStylePreset(editingPresetId, data);
            if (!!updatedPreset?.isEnabled !== enabled) {
                await api.toggleGlobalStylePreset(editingPresetId, enabled);
            }
            showNotification('Пресет успешно обновлен', 'success');
        } else {
            const createdPreset = await api.createGlobalStylePreset(data);
            savedPresetId = createdPreset.id;
            if (!!createdPreset?.isEnabled !== enabled) {
                await api.toggleGlobalStylePreset(createdPreset.id, enabled);
            }
            showNotification('Пресет успешно создан', 'success');
        }

        if (previewFile && savedPresetId) {
            await api.uploadGlobalStylePresetPreview(savedPresetId, previewFile);
            showNotification('Превью пресета загружено', 'success');
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

document.getElementById('preset-prompt-enabled').addEventListener('change', function () {
    const opts = document.getElementById('prompt-options');
    if (this.checked) {
        opts.classList.remove('hidden');
    } else {
        opts.classList.add('hidden');
        document.getElementById('preset-prompt-required').checked = false;
    }
    updateModeBadge();
});

document.getElementById('preset-style-prompt').addEventListener('input', updateModeBadge);

document.getElementById('add-field-btn').addEventListener('click', () => {
    const fields = readFieldsFromForm();
    fields.push({
        key: '',
        label: '',
        description: '',
        placeholder: '',
        type: 'text',
        required: false,
        maxLength: 80,
        options: []
    });
    renderFieldEditor(fields);
});

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

function getPresetPreviewUrl(preset) {
    return preset.previewWebpUrl || preset.previewUrl || preset.thumbnailUrl || null;
}

function renderPreview(preset) {
    const previewUrl = getPresetPreviewUrl(preset);
    if (previewUrl) {
        return `<img src="${escapeHtml(previewUrl)}" alt="${escapeHtml(preset.name)}" class="h-14 w-14 rounded-lg object-cover border border-gray-200">`;
    }
    const letter = (preset.name || preset.code || '?').trim().charAt(0).toUpperCase();
    return `
        <div class="h-14 w-14 rounded-lg bg-gradient-to-br from-gray-100 to-gray-300 border border-gray-200 flex items-center justify-center text-gray-500 font-semibold">
            ${escapeHtml(letter)}
        </div>
    `;
}

function applyPromptInputToForm(promptInput) {
    const input = promptInput || DEFAULT_PROMPT_INPUT;
    const enabled = input.enabled !== false;
    document.getElementById('preset-prompt-enabled').checked = enabled;
    document.getElementById('preset-prompt-required').checked = !!input.required;
    document.getElementById('preset-prompt-placeholder').value = input.placeholder || '';
    document.getElementById('preset-prompt-max-length').value = input.maxLength || '';
    const opts = document.getElementById('prompt-options');
    if (enabled) {
        opts.classList.remove('hidden');
    } else {
        opts.classList.add('hidden');
    }
    updateModeBadge();
}

function readPromptInputFromForm() {
    const maxLengthRaw = document.getElementById('preset-prompt-max-length').value;
    const enabled = document.getElementById('preset-prompt-enabled').checked;
    return {
        enabled,
        required: enabled && document.getElementById('preset-prompt-required').checked,
        placeholder: document.getElementById('preset-prompt-placeholder').value.trim() || null,
        maxLength: maxLengthRaw ? parseInt(maxLengthRaw, 10) : null
    };
}

function renderFieldEditor(fields) {
    const list = document.getElementById('preset-fields-list');
    if (!fields || fields.length === 0) {
        list.innerHTML = '<div class="text-xs text-gray-400 py-2">Поля не заданы. Добавь поле, если в шаблоне есть плейсхолдеры вроде <code>{{emotion}}</code>.</div>';
        updateModeBadge();
        return;
    }

    list.innerHTML = fields.map((field, index) => renderFieldRow(field, index)).join('');
    updateModeBadge();
}

function renderFieldRow(field, index) {
    const options = Array.isArray(field.options) ? field.options.join(', ') : '';
    const type = field.type || 'text';
    return `
        <div class="field-row border border-gray-200 rounded-lg p-3 bg-white space-y-3" data-field-row>
            <div class="flex items-center justify-between gap-3">
                <div class="text-xs font-semibold text-gray-500">Поле #${index + 1}</div>
                <button type="button" onclick="removePresetField(${index})" class="text-xs text-red-600 hover:text-red-800">Удалить</button>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
                <input data-field-key value="${escapeHtml(field.key || '')}" class="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="key: emotion">
                <input data-field-label value="${escapeHtml(field.label || '')}" class="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="Название для UI">
                <select data-field-type class="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                    <option value="text" ${type === 'text' ? 'selected' : ''}>text</option>
                    <option value="emoji" ${type === 'emoji' ? 'selected' : ''}>emoji</option>
                    <option value="select" ${type === 'select' ? 'selected' : ''}>select</option>
                </select>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                <input data-field-placeholder value="${escapeHtml(field.placeholder || '')}" class="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="Placeholder">
                <input data-field-description value="${escapeHtml(field.description || '')}" class="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="Описание/подсказка для фронта">
            </div>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
                <label class="flex items-center gap-2 text-sm text-gray-700">
                    <input data-field-required type="checkbox" ${field.required ? 'checked' : ''} class="h-4 w-4 text-blue-600 border-gray-300 rounded">
                    Обязательное
                </label>
                <input data-field-max-length type="number" min="1" max="1000" value="${field.maxLength || ''}" class="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="Max length">
                <input data-field-options value="${escapeHtml(options)}" class="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="Options через запятую">
            </div>
        </div>
    `;
}

function readFieldsFromForm() {
    return Array.from(document.querySelectorAll('[data-field-row]'))
        .map(row => {
            const key = row.querySelector('[data-field-key]').value.trim();
            const maxLengthRaw = row.querySelector('[data-field-max-length]').value;
            const optionsRaw = row.querySelector('[data-field-options]').value.trim();
            return {
                key,
                label: row.querySelector('[data-field-label]').value.trim() || null,
                description: row.querySelector('[data-field-description]').value.trim() || null,
                placeholder: row.querySelector('[data-field-placeholder]').value.trim() || null,
                type: row.querySelector('[data-field-type]').value,
                required: row.querySelector('[data-field-required]').checked,
                maxLength: maxLengthRaw ? parseInt(maxLengthRaw, 10) : null,
                options: optionsRaw
                    ? optionsRaw.split(',').map(v => v.trim()).filter(Boolean)
                    : null
            };
        })
        .filter(field => field.key);
}

function removePresetField(index) {
    const fields = readFieldsFromForm();
    fields.splice(index, 1);
    renderFieldEditor(fields);
}

function deriveUiMode(template, promptEnabled, fields) {
    const placeholders = extractTemplatePlaceholders(template);
    const hasFields = fields.length > 0;
    const templateHasNonPromptPlaceholders = [...placeholders].some(k => k !== 'prompt');

    if (hasFields || templateHasNonPromptPlaceholders) {
        if (promptEnabled) {
            return 'STRUCTURED_FIELDS';
        }
        return 'LOCKED_TEMPLATE';
    }
    if (promptEnabled) {
        return 'STYLE_WITH_PROMPT';
    }
    return 'LOCKED_TEMPLATE';
}

function updateModeBadge() {
    const template = document.getElementById('preset-style-prompt').value;
    const promptEnabled = document.getElementById('preset-prompt-enabled').checked;
    const fields = readFieldsFromForm();
    const mode = deriveUiMode(template, promptEnabled, fields);

    document.getElementById('preset-ui-mode').value = mode;

    const badge = document.getElementById('mode-badge');
    const text = document.getElementById('mode-badge-text');
    badge.classList.remove('hidden');

    const labels = {
        STYLE_WITH_PROMPT: { label: 'Свободный текст + суффикс стиля', cls: 'bg-blue-100 text-blue-700' },
        STRUCTURED_FIELDS: { label: 'Шаблон с полями и текстом', cls: 'bg-green-100 text-green-700' },
        LOCKED_TEMPLATE: { label: 'Фиксированный шаблон без ввода', cls: 'bg-yellow-100 text-yellow-700' },
        CUSTOM_PROMPT: { label: 'Кастомный prompt', cls: 'bg-gray-100 text-gray-700' }
    };
    const l = labels[mode] || labels.STYLE_WITH_PROMPT;
    text.textContent = 'Режим: ' + l.label;
    text.className = 'text-xs px-2 py-1 rounded-full font-medium ' + l.cls;
}

function validatePresetUiContract(promptSuffix, promptInput, fields) {
    const uiMode = document.getElementById('preset-ui-mode').value;
    if (uiMode !== 'STRUCTURED_FIELDS' && uiMode !== 'LOCKED_TEMPLATE' && uiMode !== 'CUSTOM_PROMPT') {
        return null;
    }

    const placeholders = extractTemplatePlaceholders(promptSuffix);
    const fieldKeys = new Set(fields.map(field => field.key));
    for (const key of placeholders) {
        if (key === 'prompt') {
            continue;
        }
        if (!fieldKeys.has(key)) {
            return `В шаблоне есть {{${key}}}, но поле "${key}" не описано ниже`;
        }
    }

    if (placeholders.has('prompt') && !promptInput.enabled) {
        return 'В шаблоне используется {{prompt}}, но поле prompt выключено';
    }

    const duplicateKey = findDuplicateFieldKey(fields);
    if (duplicateKey) {
        return `Поле "${duplicateKey}" указано несколько раз`;
    }

    return null;
}

function extractTemplatePlaceholders(template) {
    const result = new Set();
    const regex = /\{\{\s*([a-zA-Z0-9_]+)\s*}}/g;
    let match;
    while ((match = regex.exec(template || '')) !== null) {
        result.add(match[1]);
    }
    return result;
}

function findDuplicateFieldKey(fields) {
    const seen = new Set();
    for (const field of fields) {
        if (seen.has(field.key)) {
            return field.key;
        }
        seen.add(field.key);
    }
    return null;
}
