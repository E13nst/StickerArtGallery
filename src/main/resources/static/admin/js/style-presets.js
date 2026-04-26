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
        type: 'emoji',
        required: true
    },
    {
        key: 'productName',
        label: 'Название продукта',
        type: 'text',
        required: false
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
    let promptInput;
    try {
        promptInput = readPromptInputFromForm();
    } catch (e) {
        showNotification(e.message || 'Некорректные настройки референсов', 'error');
        return;
    }
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
        type: 'text',
        required: false
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
    const refs = input.referenceImages || {};
    document.getElementById('preset-ref-min-count').value = refs.minCount != null ? refs.minCount : 0;
    document.getElementById('preset-ref-max-count').value = refs.maxCount != null ? refs.maxCount : 14;
    const opts = document.getElementById('prompt-options');
    if (enabled) {
        opts.classList.remove('hidden');
    } else {
        opts.classList.add('hidden');
    }
    updateModeBadge();
}

const MAX_PRESET_REFERENCE_IMAGES = 14;

function readPromptInputFromForm() {
    const maxLengthRaw = document.getElementById('preset-prompt-max-length').value;
    const enabled = document.getElementById('preset-prompt-enabled').checked;
    const refMinRaw = document.getElementById('preset-ref-min-count').value;
    const refMaxRaw = document.getElementById('preset-ref-max-count').value;
    const refMin = refMinRaw === '' ? 0 : parseInt(refMinRaw, 10);
    let refMax = refMaxRaw === '' ? MAX_PRESET_REFERENCE_IMAGES : parseInt(refMaxRaw, 10);
    if (!Number.isFinite(refMin) || refMin < 0) {
        throw new Error('Некорректный минимум референсов');
    }
    if (!Number.isFinite(refMax)) {
        refMax = MAX_PRESET_REFERENCE_IMAGES;
    }
    refMax = Math.min(MAX_PRESET_REFERENCE_IMAGES, Math.max(0, refMax));
    const referenceImages = {
        enabled: true,
        required: false,
        minCount: refMin,
        maxCount: refMax
    };
    return {
        enabled,
        required: enabled && document.getElementById('preset-prompt-required').checked,
        placeholder: document.getElementById('preset-prompt-placeholder').value.trim() || null,
        maxLength: maxLengthRaw ? parseInt(maxLengthRaw, 10) : null,
        referenceImages
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
    list.querySelectorAll('[data-field-type]').forEach((sel) => {
        sel.addEventListener('change', () => syncRefFieldOptionsVisibility(sel.closest('[data-field-row]')));
    });
    list.querySelectorAll('[data-field-row]').forEach((row) => syncRefFieldOptionsVisibility(row));
    updateModeBadge();
}

function syncRefFieldOptionsVisibility(row) {
    if (!row) return;
    const typeEl = row.querySelector('[data-field-type]');
    const opts = row.querySelector('[data-field-ref-opts]');
    if (!typeEl || !opts) return;
    const isRef = typeEl.value === 'reference';
    opts.classList.toggle('hidden', !isRef);
}

function renderFieldRow(field, index) {
    const type = field.type || 'text';
    const minImg = field.minImages != null ? field.minImages : 0;
    const maxImg = field.maxImages != null ? field.maxImages : 1;
    const ptmpl = field.promptTemplate != null ? field.promptTemplate : '';
    return `
        <div class="field-row border border-gray-200 rounded-lg p-3 bg-white space-y-3" data-field-row>
            <div class="flex items-center justify-between gap-3">
                <div class="text-xs font-semibold text-gray-500">Поле #${index + 1}</div>
                <button type="button" onclick="removePresetField(${index})" class="text-xs text-red-600 hover:text-red-800">Удалить</button>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
                <input data-field-key value="${escapeHtml(field.key || '')}" class="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="key: emotion">
                <input data-field-label value="${escapeHtml(field.label || '')}" class="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="Как поле увидит пользователь">
                <select data-field-type class="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                    <option value="text" ${type === 'text' ? 'selected' : ''}>text</option>
                    <option value="emoji" ${type === 'emoji' ? 'selected' : ''}>emoji</option>
                    <option value="select" ${type === 'select' ? 'selected' : ''}>select</option>
                    <option value="reference" ${type === 'reference' ? 'selected' : ''}>reference</option>
                </select>
            </div>
            <div data-field-ref-opts class="grid grid-cols-1 md:grid-cols-3 gap-3 ${type === 'reference' ? '' : 'hidden'}">
                <div>
                    <label class="block text-xs text-gray-500 mb-0.5">minImages</label>
                    <input data-field-min-images type="number" min="0" max="14" value="${minImg}" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
                </div>
                <div>
                    <label class="block text-xs text-gray-500 mb-0.5">maxImages</label>
                    <input data-field-max-images type="number" min="1" max="14" value="${maxImg}" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
                </div>
                <div>
                    <label class="block text-xs text-gray-500 mb-0.5">promptTemplate</label>
                    <input data-field-prompt-template type="text" value="${escapeHtml(ptmpl)}" placeholder="Image {index}" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm font-mono">
                </div>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-1 gap-3">
                <label class="flex items-center gap-2 text-sm text-gray-700">
                    <input data-field-required type="checkbox" ${field.required ? 'checked' : ''} class="h-4 w-4 text-blue-600 border-gray-300 rounded">
                    Обязательное
                </label>
            </div>
        </div>
    `;
}

function readFieldsFromForm() {
    return Array.from(document.querySelectorAll('[data-field-row]'))
        .map(row => {
            const key = row.querySelector('[data-field-key]').value.trim();
            const type = row.querySelector('[data-field-type]').value;
            const base = {
                key,
                label: row.querySelector('[data-field-label]').value.trim() || null,
                type,
                required: row.querySelector('[data-field-required]').checked
            };
            if (type === 'reference') {
                const minEl = row.querySelector('[data-field-min-images]');
                const maxEl = row.querySelector('[data-field-max-images]');
                const tplEl = row.querySelector('[data-field-prompt-template]');
                const minV = minEl ? parseInt(minEl.value, 10) : 0;
                const maxV = maxEl ? parseInt(maxEl.value, 10) : 1;
                base.minImages = Number.isFinite(minV) ? minV : 0;
                base.maxImages = Number.isFinite(maxV) ? maxV : 1;
                const tpl = tplEl && tplEl.value.trim() ? tplEl.value.trim() : null;
                if (tpl) {
                    base.promptTemplate = tpl;
                }
            }
            return base;
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

    const presetRefMax = promptInput.referenceImages && promptInput.referenceImages.maxCount != null
        ? promptInput.referenceImages.maxCount
        : MAX_PRESET_REFERENCE_IMAGES;
    let refMaxSum = 0;
    for (const f of fields) {
        if (f.type === 'reference') {
            const mx = f.maxImages != null ? f.maxImages : 1;
            refMaxSum += mx;
        }
    }
    if (refMaxSum > presetRefMax) {
        return `Сумма maxImages по reference-полям (${refMaxSum}) больше referenceImages.maxCount (${presetRefMax})`;
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
