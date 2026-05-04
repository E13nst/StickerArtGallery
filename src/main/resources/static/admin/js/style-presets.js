// Global Style Presets Management
// ========================================

checkAuth();

let presets = [];
let globalPresets = [];
let approvedUserPresets = [];
let categories = [];
let editingPresetId = null;
let editingCategoryId = null;
/** В модалке: загружено ли референсное фото пресета (для валидации {{preset_ref}}) */
let editingHasPresetReference = false;

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

async function loadCategories() {
    try {
        const list = await api.getStylePresetCategories();
        categories = Array.isArray(list) ? list : [];
        categories.sort((a, b) => (a.sortOrder - b.sortOrder) || (a.name || '').localeCompare(b.name || ''));
    } catch (e) {
        console.error('Failed to load categories', e);
        categories = [];
    }
    renderCategoriesTable();
    fillCategorySelect();
}

function fillCategorySelect() {
    const sel = document.getElementById('preset-category-id');
    if (!sel) return;
    const current = sel.value;
    sel.innerHTML = categories.map(c => `<option value="${c.id}">#${c.id} · ${escapeHtml(c.code)} · ${escapeHtml(c.name)}</option>`).join('');
    if (current && categories.some(c => String(c.id) === String(current))) {
        sel.value = current;
    }
}

function renderCategoriesTable() {
    const tbody = document.getElementById('categories-table-body');
    if (!tbody) return;
    if (categories.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="px-4 py-2 text-xs text-gray-400 dark:text-slate-500">Нет категорий</td></tr>';
        return;
    }
    tbody.innerHTML = categories.map(c => `
        <tr class="hover:bg-gray-50 dark:hover:bg-slate-800/50">
            <td class="px-4 py-2 text-gray-700 dark:text-slate-300">${c.id}</td>
            <td class="px-4 py-2 font-mono text-xs text-slate-800 dark:text-slate-200">${escapeHtml(c.code)}</td>
            <td class="px-4 py-2 text-slate-800 dark:text-slate-200">${escapeHtml(c.name)}</td>
            <td class="px-4 py-2 text-gray-600 dark:text-slate-400">${c.sortOrder}</td>
            <td class="px-4 py-2 whitespace-nowrap text-sm">
                ${renderActionDropdown([
                    { label: 'Изменить', onclick: `editCategoryModal(${c.id})`, className: 'text-blue-600' },
                    { label: 'Удалить', onclick: `deleteCategory(${c.id})`, className: 'text-red-600' }
                ])}
            </td>
        </tr>
    `).join('');
}

function sortPresetsForDisplay(list) {
    return [...list].sort((a, b) => {
        const ca = a.category ? a.category.sortOrder : 9999;
        const cb = b.category ? b.category.sortOrder : 9999;
        if (ca !== cb) return ca - cb;
        const so = (a.sortOrder || 0) - (b.sortOrder || 0);
        if (so !== 0) return so;
        return (a.name || '').localeCompare(b.name || '');
    });
}

// Загрузка пресетов
async function loadPresets() {
    try {
        document.getElementById('loading').classList.remove('hidden');
        document.getElementById('no-data').classList.add('hidden');

        await loadCategories();

        const [globalsRaw, approvedRaw] = await Promise.all([
            api.getGlobalStylePresets(),
            api.getUserPresetsForModeration('APPROVED')
        ]);

        globalPresets = Array.isArray(globalsRaw) ? globalsRaw : [];
        approvedUserPresets = Array.isArray(approvedRaw) ? approvedRaw : [];

        presets = [
            ...globalPresets.map(p => ({ ...p, _sourceType: 'GLOBAL' })),
            ...approvedUserPresets.map(p => ({ ...p, _sourceType: 'USER_APPROVED' }))
        ];

        renderPresets();
        
        document.getElementById('loading').classList.add('hidden');
        if (applyPresetFilters(presets).length === 0) {
            document.getElementById('no-data').classList.remove('hidden');
        }
    } catch (error) {
        console.error('Failed to load presets:', error);
        showNotification('Ошибка загрузки пресетов: ' + (error.message || 'Неизвестная ошибка'), 'error');
        document.getElementById('loading').classList.add('hidden');
    }
}

function applyPresetFilters(items) {
    const source = document.getElementById('filter-source')?.value || 'ALL';
    const catalog = document.getElementById('filter-catalog')?.value || 'ALL';
    return (items || []).filter(p => {
        if (source === 'GLOBAL' && p._sourceType !== 'GLOBAL') return false;
        if (source === 'USER_APPROVED' && p._sourceType !== 'USER_APPROVED') return false;
        if (catalog === 'ON' && p.publishedToCatalog !== true) return false;
        if (catalog === 'OFF' && p.publishedToCatalog !== false) return false;
        return true;
    });
}

// Отрисовка таблицы
function renderPresets() {
    const tbody = document.getElementById('presets-table-body');
    const filtered = applyPresetFilters(presets);

    if (filtered.length === 0) {
        tbody.innerHTML = '';
        document.getElementById('no-data').classList.remove('hidden');
        return;
    }
    document.getElementById('no-data').classList.add('hidden');

    tbody.innerHTML = sortPresetsForDisplay(filtered)
        .map(preset => `
            <tr class="hover:bg-gray-50 dark:hover:bg-slate-800/50">
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-slate-100">${preset.id}</td>
                <td class="px-6 py-4 whitespace-nowrap">
                    ${renderPreview(preset)}
                </td>
                <td class="px-6 py-4 text-sm text-gray-800 dark:text-slate-200">
                    ${preset.category ? `<span class="text-sm">${escapeHtml(preset.category.name)}</span><div class="text-xs text-gray-500 dark:text-slate-400 font-mono">${escapeHtml(preset.category.code)}</div>` : '—'}
                </td>
                <td class="px-6 py-4 text-sm">
                    <div class="font-medium text-gray-900 dark:text-slate-100">${escapeHtml(preset.name)}</div>
                    ${preset.description ? `<div class="text-xs text-gray-500 dark:text-slate-400 mt-1">${escapeHtml(preset.description)}</div>` : ''}
                </td>
                <td class="px-6 py-4 text-sm">
                    <div class="font-mono text-xs bg-gray-50 dark:bg-slate-800 p-2 rounded max-w-xs overflow-x-auto text-slate-800 dark:text-slate-200">
                        ${preset.promptSuffix ? escapeHtml(preset.promptSuffix.substring(0, 80)) + (preset.promptSuffix.length > 80 ? '...' : '') : '<span class="text-gray-400 dark:text-slate-500">—</span>'}
                    </div>
                    <div class="mt-1 text-[11px]">
                        ${preset._sourceType === 'GLOBAL'
                            ? '<span class="px-1.5 py-0.5 rounded bg-slate-100 text-slate-700 dark:bg-slate-700 dark:text-slate-200">GLOBAL</span>'
                            : '<span class="px-1.5 py-0.5 rounded bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-200">USER APPROVED</span>'}
                    </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm">
                    ${renderRemoveBackgroundPolicy(preset.removeBackground)}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-slate-100" title="Порядок внутри категории">${preset.sortOrder}</td>
                <td class="px-6 py-4 whitespace-nowrap">
                    <div class="flex flex-col gap-1">
                        ${preset.isEnabled
                            ? '<span class="px-2 py-1 text-xs font-semibold text-green-800 bg-green-100 dark:text-green-200 dark:bg-green-950/50 rounded-full">Активен</span>'
                            : '<span class="px-2 py-1 text-xs font-semibold text-gray-800 bg-gray-100 dark:text-slate-200 dark:bg-slate-700 rounded-full">Неактивен</span>'}
                        ${preset._sourceType === 'USER_APPROVED'
                            ? `<span class="px-2 py-1 text-xs font-semibold ${preset.publishedToCatalog ? 'text-blue-800 bg-blue-100 dark:text-blue-200 dark:bg-blue-950/50' : 'text-orange-800 bg-orange-100 dark:text-orange-200 dark:bg-orange-950/50'} rounded-full">${preset.publishedToCatalog ? 'На витрине' : 'Скрыт с витрины'}</span>`
                            : ''}
                    </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm">
                    ${renderActionDropdown(getPresetActions(preset))}
                </td>
            </tr>
        `).join('');
}

function getPresetActions(preset) {
    if (preset._sourceType === 'USER_APPROVED') {
        const actions = [
            {
                label: 'Открыть модерацию',
                onclick: `openModerationPageForPreset(${preset.id}, ${JSON.stringify(preset.moderationStatus || 'APPROVED')})`,
                className: 'text-blue-600'
            }
        ];
        if (preset.publishedToCatalog) {
            actions.push({ label: 'Скрыть с витрины', onclick: `takedownUserApprovedPreset(${preset.id})`, className: 'text-orange-600' });
        } else {
            actions.push({ label: 'Вернуть на витрину', onclick: `republishUserApprovedPreset(${preset.id})`, className: 'text-emerald-600' });
        }
        return actions;
    }
    return [
        { label: 'Изменить', onclick: `editPreset(${preset.id})`, className: 'text-blue-600' },
        { label: preset.isEnabled ? 'Выкл' : 'Вкл', onclick: `togglePreset(${preset.id}, ${!preset.isEnabled})`, className: preset.isEnabled ? 'text-yellow-600' : 'text-green-600' },
        { label: 'Удалить', onclick: `deletePreset(${preset.id})`, className: 'text-red-600' }
    ];
}

function openModerationPageForPreset(presetId, moderationStatus) {
    const st = (moderationStatus != null && String(moderationStatus).trim()) ? String(moderationStatus).trim() : 'APPROVED';
    window.location.href = `/admin/preset-moderation.html?status=${encodeURIComponent(st)}&presetId=${encodeURIComponent(presetId)}`;
}

async function takedownUserApprovedPreset(presetId) {
    if (!confirmAction('Снять пользовательский пресет с публичной витрины?')) {
        return;
    }
    try {
        await api.takedownPresetModeration(presetId);
        showNotification('Пресет снят с витрины', 'success');
        await loadPresets();
    } catch (e) {
        console.error(e);
        showNotification(e.message || 'Ошибка снятия с витрины', 'error');
    }
}

async function republishUserApprovedPreset(presetId) {
    if (!confirmAction('Вернуть пользовательский пресет на публичную витрину?')) {
        return;
    }
    try {
        await api.republishPresetModeration(presetId);
        showNotification('Пресет возвращён на витрину', 'success');
        await loadPresets();
    } catch (e) {
        console.error(e);
        showNotification(e.message || 'Ошибка возврата на витрину', 'error');
    }
}

// Открыть форму добавления
function openAddModal() {
    editingPresetId = null;
    document.getElementById('modal-title').textContent = 'Добавить пресет';
    document.getElementById('preset-form').reset();
    document.getElementById('preset-id').value = '';
    fillCategorySelect();
    const general = categories.find(c => c.code === 'general');
    if (general) {
        document.getElementById('preset-category-id').value = String(general.id);
    }
    document.getElementById('preset-remove-background').value = '';
    document.getElementById('preset-enabled').checked = true;
    applyPromptInputToForm(DEFAULT_PROMPT_INPUT);
    renderFieldEditor(DEFAULT_FIELDS);
    document.getElementById('current-preview-wrap').classList.add('hidden');
    document.getElementById('current-preview-img').src = '';
    document.getElementById('preset-reference').value = '';
    document.getElementById('current-reference-wrap').classList.add('hidden');
    document.getElementById('current-reference-img').src = '';
    document.getElementById('clear-reference-btn').classList.add('hidden');
    editingHasPresetReference = false;
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
    fillCategorySelect();
    if (preset.category && preset.category.id) {
        document.getElementById('preset-category-id').value = String(preset.category.id);
    }
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
    const refUrl = preset.presetReferenceImageUrl || null;
    editingHasPresetReference = Boolean(preset.presetReferenceSourceImageId || preset.presetReferenceImageUrl);
    if (refUrl) {
        document.getElementById('current-reference-img').src = refUrl;
        document.getElementById('current-reference-wrap').classList.remove('hidden');
        document.getElementById('clear-reference-btn').classList.remove('hidden');
    } else {
        document.getElementById('current-reference-wrap').classList.add('hidden');
        document.getElementById('current-reference-img').src = '';
        document.getElementById('clear-reference-btn').classList.add('hidden');
    }
    document.getElementById('preset-preview').value = '';
    document.getElementById('preset-reference').value = '';
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
    
    const categoryIdRaw = document.getElementById('preset-category-id').value;
    const categoryId = categoryIdRaw ? parseInt(categoryIdRaw, 10) : null;
    if (!categoryId || Number.isNaN(categoryId)) {
        showNotification('Выбери категорию', 'error');
        return;
    }

    const data = {
        code: editingPresetId ? presets.find(p => p.id === editingPresetId)?.code : 'preset_' + Date.now(),
        name: document.getElementById('preset-name').value.trim(),
        description: document.getElementById('preset-description').value.trim() || null,
        promptSuffix,
        removeBackground: parseRemoveBackgroundValue(document.getElementById('preset-remove-background').value),
        sortOrder: parseInt(document.getElementById('preset-display-order').value),
        categoryId,
        uiMode: document.getElementById('preset-ui-mode').value,
        promptInput,
        fields
    };
    const enabled = document.getElementById('preset-enabled').checked;
    const previewFile = document.getElementById('preset-preview').files[0] || null;
    const referenceFile = document.getElementById('preset-reference').files[0] || null;
    
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
        if (referenceFile && savedPresetId) {
            await api.uploadGlobalStylePresetReference(savedPresetId, referenceFile);
            editingHasPresetReference = true;
            showNotification('Референс пресета загружен', 'success');
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
document.getElementById('refresh-presets-btn').addEventListener('click', loadPresets);
document.getElementById('filter-source').addEventListener('change', renderPresets);
document.getElementById('filter-catalog').addEventListener('change', renderPresets);

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

function openCategoryModal() {
    editingCategoryId = null;
    document.getElementById('category-modal-title').textContent = 'Новая категория';
    document.getElementById('category-form').reset();
    document.getElementById('category-edit-id').value = '';
    document.getElementById('category-code').removeAttribute('readonly');
    document.getElementById('category-modal').classList.remove('hidden');
}

function closeCategoryModal() {
    document.getElementById('category-modal').classList.add('hidden');
    editingCategoryId = null;
}

function editCategoryModal(id) {
    const c = categories.find(x => x.id === id);
    if (!c) return;
    editingCategoryId = id;
    document.getElementById('category-modal-title').textContent = 'Редактировать категорию';
    document.getElementById('category-edit-id').value = String(id);
    document.getElementById('category-code').value = c.code;
    document.getElementById('category-code').setAttribute('readonly', 'readonly');
    document.getElementById('category-name').value = c.name;
    document.getElementById('category-sort-order').value = c.sortOrder != null ? c.sortOrder : 0;
    document.getElementById('category-modal').classList.remove('hidden');
}

async function deleteCategory(id) {
    const c = categories.find(x => x.id === id);
    if (!c) return;
    if (c.code === 'general') {
        showNotification('Категорию general удалить нельзя', 'error');
        return;
    }
    if (!confirmAction(`Удалить категорию «${c.name}»? Пресеты будут перенесены в «Общее».`)) {
        return;
    }
    try {
        await api.deleteStylePresetCategory(id);
        showNotification('Категория удалена', 'success');
        await loadPresets();
    } catch (e) {
        console.error(e);
        showNotification(e.message || 'Ошибка удаления категории', 'error');
    }
}

document.getElementById('add-category-btn').addEventListener('click', openCategoryModal);

document.getElementById('category-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const editId = document.getElementById('category-edit-id').value;
    const name = document.getElementById('category-name').value.trim();
    const sortOrder = parseInt(document.getElementById('category-sort-order').value, 10) || 0;
    try {
        if (editId) {
            await api.updateStylePresetCategory(parseInt(editId, 10), { name, sortOrder });
            showNotification('Категория обновлена', 'success');
        } else {
            const code = document.getElementById('category-code').value.trim();
            if (!code) {
                showNotification('Укажи код категории', 'error');
                return;
            }
            await api.createStylePresetCategory({ code, name, sortOrder });
            showNotification('Категория создана', 'success');
        }
        closeCategoryModal();
        await loadPresets();
    } catch (err) {
        console.error(err);
        showNotification(err.message || 'Ошибка сохранения категории', 'error');
    }
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
        return '<span class="px-2 py-1 text-xs font-semibold text-blue-800 bg-blue-100 dark:text-blue-200 dark:bg-blue-950/50 rounded-full">Удалять</span>';
    }
    if (value === false) {
        return '<span class="px-2 py-1 text-xs font-semibold text-orange-800 bg-orange-100 dark:text-orange-200 dark:bg-orange-950/50 rounded-full">Не удалять</span>';
    }
    return '<span class="text-gray-400 dark:text-slate-500">Fallback</span>';
}

function getPresetPreviewUrl(preset) {
    return preset.previewWebpUrl || preset.previewUrl || preset.thumbnailUrl || null;
}

function renderPreview(preset) {
    const previewUrl = getPresetPreviewUrl(preset);
    if (previewUrl) {
        return `<img src="${escapeHtml(previewUrl)}" alt="${escapeHtml(preset.name)}" class="h-14 w-14 rounded-lg object-cover border border-gray-200 dark:border-slate-600">`;
    }
    const letter = (preset.name || preset.code || '?').trim().charAt(0).toUpperCase();
    return `
        <div class="h-14 w-14 rounded-lg bg-gradient-to-br from-gray-100 to-gray-300 dark:from-slate-700 dark:to-slate-800 border border-gray-200 dark:border-slate-600 flex items-center justify-center text-gray-500 dark:text-slate-300 font-semibold">
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
        list.innerHTML = '<div class="text-xs text-gray-400 dark:text-slate-500 py-2">Поля не заданы. Добавь поле, если в шаблоне есть плейсхолдеры вроде <code>{{emotion}}</code>. Плейсхолдер <code>{{preset_ref}}</code> — только при загруженном референсе выше (поле создаётся автоматически).</div>';
        updateModeBadge();
        return;
    }

    list.innerHTML = fields.map((field, index) => (field.system || field.key === 'preset_ref')
        ? renderSystemPresetRefRow(field, index)
        : renderFieldRow(field, index)).join('');
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

function renderSystemPresetRefRow(field, index) {
    return `
        <div class="field-row border border-indigo-100 dark:border-indigo-900/50 rounded-lg p-3 bg-indigo-50/60 dark:bg-indigo-950/40 space-y-2" data-field-row data-field-system="true">
            <div class="text-xs font-semibold text-indigo-800 dark:text-indigo-200">Поле #${index + 1} — референс пресета (системное)</div>
            <p class="text-xs text-indigo-900 dark:text-indigo-100">
                Ключ <code class="bg-white/80 dark:bg-slate-800/90 px-1 rounded">preset_ref</code> — в шаблоне: <code class="bg-white/80 dark:bg-slate-800/90 px-1 rounded">{{preset_ref}}</code>.
                Настраивается только загрузкой «Референсного фото пресета» выше; в JSON пресета не сохраняется.
            </p>
            <div class="text-xs text-gray-600 dark:text-slate-400">Подпись: ${escapeHtml(field.label || 'Референс пресета')}</div>
        </div>
    `;
}

function renderFieldRow(field, index) {
    const type = field.type || 'text';
    const minImg = field.minImages != null ? field.minImages : 0;
    const maxImg = field.maxImages != null ? field.maxImages : 1;
    return `
        <div class="field-row border border-gray-200 dark:border-slate-600 rounded-lg p-3 bg-white dark:bg-slate-900 space-y-3" data-field-row>
            <div class="flex items-center justify-between gap-3">
                <div class="text-xs font-semibold text-gray-500 dark:text-slate-400">Поле #${index + 1}</div>
                <button type="button" onclick="removePresetField(${index})" class="text-xs text-red-600 dark:text-red-400 hover:text-red-800 dark:hover:text-red-300">Удалить</button>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
                <input data-field-key value="${escapeHtml(field.key || '')}" class="px-3 py-2 border border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="key: emotion">
                <input data-field-label value="${escapeHtml(field.label || '')}" class="px-3 py-2 border border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="Как поле увидит пользователь">
                <select data-field-type class="px-3 py-2 border border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                    <option value="text" ${type === 'text' ? 'selected' : ''}>text</option>
                    <option value="emoji" ${type === 'emoji' ? 'selected' : ''}>emoji</option>
                    <option value="select" ${type === 'select' ? 'selected' : ''}>select</option>
                    <option value="reference" ${type === 'reference' ? 'selected' : ''}>reference (фото пользователя)</option>
                </select>
            </div>
            <div data-field-ref-opts class="grid grid-cols-1 md:grid-cols-2 gap-3 ${type === 'reference' ? '' : 'hidden'}">
                <div>
                    <label class="block text-xs text-gray-500 dark:text-slate-400 mb-0.5">minImages</label>
                    <input data-field-min-images type="number" min="0" max="14" value="${minImg}" class="w-full px-3 py-2 border border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 rounded-lg text-sm">
                </div>
                <div>
                    <label class="block text-xs text-gray-500 dark:text-slate-400 mb-0.5">maxImages</label>
                    <input data-field-max-images type="number" min="1" max="14" value="${maxImg}" class="w-full px-3 py-2 border border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 rounded-lg text-sm">
                </div>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-1 gap-3">
                <label class="flex items-center gap-2 text-sm text-gray-700 dark:text-slate-300">
                    <input data-field-required type="checkbox" ${field.required ? 'checked' : ''} class="h-4 w-4 text-blue-600 border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-800 rounded">
                    Обязательное
                </label>
            </div>
        </div>
    `;
}

function readFieldsFromForm() {
    return Array.from(document.querySelectorAll('[data-field-row]'))
        .filter(row => row.getAttribute('data-field-system') !== 'true')
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
                const minV = minEl ? parseInt(minEl.value, 10) : 0;
                const maxV = maxEl ? parseInt(maxEl.value, 10) : 1;
                base.minImages = Number.isFinite(minV) ? minV : 0;
                base.maxImages = Number.isFinite(maxV) ? maxV : 1;
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
        STYLE_WITH_PROMPT: { label: 'Свободный текст + суффикс стиля', cls: 'bg-blue-100 text-blue-700 dark:bg-blue-950/50 dark:text-blue-200' },
        STRUCTURED_FIELDS: { label: 'Шаблон с полями и текстом', cls: 'bg-green-100 text-green-700 dark:bg-green-950/50 dark:text-green-200' },
        LOCKED_TEMPLATE: { label: 'Фиксированный шаблон без ввода', cls: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-950/50 dark:text-yellow-200' },
        CUSTOM_PROMPT: { label: 'Кастомный prompt', cls: 'bg-gray-100 text-gray-700 dark:bg-slate-700 dark:text-slate-200' }
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
        if (key === 'preset_ref') {
            if (!editingHasPresetReference) {
                return 'В шаблоне используется {{preset_ref}} — загрузите «Референсное фото пресета» выше или уберите плейсхолдер из шаблона.';
            }
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

async function clearPresetReferenceInForm() {
    if (!editingPresetId) {
        return;
    }
    if (!confirmAction('Удалить референсное фото с сервера?')) {
        return;
    }
    try {
        await api.clearGlobalStylePresetReference(editingPresetId);
        document.getElementById('current-reference-wrap').classList.add('hidden');
        document.getElementById('current-reference-img').src = '';
        document.getElementById('preset-reference').value = '';
        document.getElementById('clear-reference-btn').classList.add('hidden');
        editingHasPresetReference = false;
        showNotification('Референс удалён', 'success');
        await loadPresets();
    } catch (e) {
        console.error(e);
        showNotification(e.message || 'Не удалось удалить референс', 'error');
    }
}
