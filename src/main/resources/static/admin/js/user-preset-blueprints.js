checkAuth();

let rows = [];

/** Снимок presetDefaults при открытии модалки — сохраняем неуправляемые ключи. */
let editingPresetDefaultsSnapshot = {};

/** uiHints без трёх стандартных ключей — сохраняем при сохранении. */
let editingUiHintsExtras = {};

const DEFAULT_PROMPT_INPUT = {
    enabled: true,
    required: true,
    placeholder: 'Опишите, что должно получиться…',
    maxLength: 800
};

const DEFAULT_FIELDS = [
    {
        key: 'user_face',
        label: 'Фото только для этой генерации',
        type: 'reference',
        required: false,
        minImages: 0,
        maxImages: 1,
        promptTemplate: 'Image {index}'
    }
];

const DEFAULT_DEFAULTS_TMPL = {
    promptSuffix:
        'Идея стикера: {{prompt}}. Опорное изображение стиля пресета: {{preset_ref}}. Свежий эталон снимка пользователя под эту генерацию: {{user_face}}.',
    uiMode: 'STRUCTURED_FIELDS',
    promptInput: {
        enabled: true,
        required: true,
        placeholder: 'Опишите, что должно получиться…',
        maxLength: 800
    },
    fields: DEFAULT_FIELDS,
    removeBackgroundMode: 'PRESET_DEFAULT'
};

const UI_HINT_KEYS = ['presetReferenceHelp', 'userPhotoSlotHelp', 'publicationHint'];

const DEFAULT_HINTS_TMPL = {
    presetReferenceHelp:
        'Это фото сохраняется на сервере и используется как опора стиля для всех следующих генераций по этому пресету.',
    userPhotoSlotHelp: 'Можно менять под каждую генерацию; не фиксируется в пресете.',
    publicationHint:
        'После генерации можно опубликовать пресет в каталог. Превью в каталоге обычно берётся из последнего результата. Списание ART — см. поле estimatedPublicationCostArt в ответе API.'
};

/** Ключи presetDefaults, которые заполняет форма (остальное копируем из снимка). */
const MANAGED_DEFAULT_KEYS = ['promptSuffix', 'uiMode', 'promptInput', 'fields', 'removeBackground', 'removeBackgroundMode'];

const MAX_PRESET_REFERENCE_IMAGES = 14;

function $(id) {
    return document.getElementById(id);
}

function showErr(msg) {
    const b = $('banner-err');
    if (!msg) {
        b.classList.add('hidden');
        return;
    }
    b.textContent = msg;
    b.classList.remove('hidden');
}

function deepClone(o) {
    return o == null ? o : JSON.parse(JSON.stringify(o));
}

function stripManagedDefaults(map) {
    const out = { ...(map || {}) };
    for (const k of MANAGED_DEFAULT_KEYS) {
        delete out[k];
    }
    return out;
}

function mergePresetDefaultsFromForm(builtManaged) {
    const base = deepClone(editingPresetDefaultsSnapshot) || {};
    for (const k of MANAGED_DEFAULT_KEYS) {
        delete base[k];
    }
    return { ...base, ...builtManaged };
}

function parseRemoveBackgroundValue(value) {
    if (value === 'true') {
        return true;
    }
    if (value === 'false') {
        return false;
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

function deriveUiMode(template, promptEnabled, fields) {
    const placeholders = extractTemplatePlaceholders(template);
    const hasFields = fields.length > 0;
    const templateHasNonPromptPlaceholders = [...placeholders].some((k) => k !== 'prompt');

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
    const template = $('bpf-style-prompt').value;
    const promptEnabled = $('bpf-prompt-enabled').checked;
    const fields = readFieldsFromForm();
    const mode = deriveUiMode(template, promptEnabled, fields);

    $('bpf-ui-mode').value = mode;

    const badge = $('bpf-mode-badge');
    const text = $('bpf-mode-badge-text');
    badge.classList.remove('hidden');

    const labels = {
        STYLE_WITH_PROMPT: {
            label: 'Свободный текст + суффикс стиля',
            cls: 'bg-blue-100 text-blue-700 dark:bg-blue-950/50 dark:text-blue-200'
        },
        STRUCTURED_FIELDS: {
            label: 'Шаблон с полями и текстом',
            cls: 'bg-green-100 text-green-700 dark:bg-green-950/50 dark:text-green-200'
        },
        LOCKED_TEMPLATE: {
            label: 'Фиксированный шаблон без ввода',
            cls: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-950/50 dark:text-yellow-200'
        },
        CUSTOM_PROMPT: {
            label: 'Кастомный prompt',
            cls: 'bg-gray-100 text-gray-700 dark:bg-slate-700 dark:text-slate-200'
        }
    };
    const l = labels[mode] || labels.STYLE_WITH_PROMPT;
    text.textContent = 'Режим: ' + l.label;
    text.className = 'text-xs px-2 py-1 rounded-full font-medium ' + l.cls;
}

function applyPromptInputToForm(promptInput) {
    const input = promptInput || DEFAULT_PROMPT_INPUT;
    const enabled = input.enabled !== false;
    $('bpf-prompt-enabled').checked = enabled;
    $('bpf-prompt-required').checked = !!input.required;
    $('bpf-prompt-placeholder').value = input.placeholder || '';
    $('bpf-prompt-max-length').value = input.maxLength || '';
    const refs = input.referenceImages || {};
    $('bpf-ref-min-count').value = refs.minCount != null ? refs.minCount : 0;
    $('bpf-ref-max-count').value = refs.maxCount != null ? refs.maxCount : MAX_PRESET_REFERENCE_IMAGES;
    const opts = $('bpf-prompt-options');
    if (enabled) {
        opts.classList.remove('hidden');
    } else {
        opts.classList.add('hidden');
    }
    updateModeBadge();
}

function readPromptInputFromForm() {
    const maxLengthRaw = $('bpf-prompt-max-length').value;
    const enabled = $('bpf-prompt-enabled').checked;
    const refMinRaw = $('bpf-ref-min-count').value;
    const refMaxRaw = $('bpf-ref-max-count').value;
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
        required: enabled && $('bpf-prompt-required').checked,
        placeholder: $('bpf-prompt-placeholder').value.trim() || null,
        maxLength: maxLengthRaw ? parseInt(maxLengthRaw, 10) : null,
        referenceImages
    };
}

/** Убираем зарезервированные ключи — в админке их не задают вручную. Черновики без key оставляем (кнопка «+ Поле»). */
function filterUserFields(fields) {
    if (!Array.isArray(fields)) {
        return [];
    }
    return fields.filter((f) => {
        if (!f) {
            return false;
        }
        const k = f.key != null ? String(f.key).trim().toLowerCase() : '';
        return k !== 'preset_ref';
    });
}

function renderFieldEditor(fields) {
    const list = $('bpf-fields-list');
    const cleaned = filterUserFields(fields);
    if (!cleaned.length) {
        list.innerHTML =
            '<div class="text-xs text-gray-400 dark:text-slate-500 py-2">Поля не заданы — добавьте, если в шаблоне есть плейсхолдеры кроме <code>{{prompt}}</code> и <code>{{preset_ref}}</code>.</div>';
        updateModeBadge();
        return;
    }

    list.innerHTML = cleaned.map((field, index) => renderFieldRow(field, index)).join('');
    list.querySelectorAll('[data-bpf-field-type]').forEach((sel) => {
        sel.addEventListener('change', () => bpfSyncRefOpts(sel.closest('[data-bpf-field-row]')));
    });
    list.querySelectorAll('[data-bpf-field-row]').forEach((row) => bpfSyncRefOpts(row));
    updateModeBadge();
}

function bpfSyncRefOpts(row) {
    if (!row) {
        return;
    }
    const typeEl = row.querySelector('[data-bpf-field-type]');
    const opts = row.querySelector('[data-bpf-field-ref-opts]');
    if (!typeEl || !opts) {
        return;
    }
    const isRef = typeEl.value === 'reference';
    opts.classList.toggle('hidden', !isRef);
}

function renderFieldRow(field, index) {
    const type = field.type || 'text';
    const minImg = field.minImages != null ? field.minImages : 0;
    const maxImg = field.maxImages != null ? field.maxImages : 1;
    return `
        <div class="field-row border border-gray-200 dark:border-slate-600 rounded-lg p-3 bg-white dark:bg-slate-900 space-y-3" data-bpf-field-row>
            <div class="flex items-center justify-between gap-3">
                <div class="text-xs font-semibold text-gray-500 dark:text-slate-400">Поле #${index + 1}</div>
                <button type="button" data-bpf-remove-field="${index}" class="text-xs text-red-600 dark:text-red-400 hover:underline">Удалить</button>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
                <input data-bpf-field-key value="${escapeHtml(field.key || '')}" class="px-3 py-2 border border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="key">
                <input data-bpf-field-label value="${escapeHtml(field.label || '')}" class="px-3 py-2 border border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="Подпись">
                <select data-bpf-field-type class="px-3 py-2 border border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                    <option value="text" ${type === 'text' ? 'selected' : ''}>text</option>
                    <option value="emoji" ${type === 'emoji' ? 'selected' : ''}>emoji</option>
                    <option value="select" ${type === 'select' ? 'selected' : ''}>select</option>
                    <option value="reference" ${type === 'reference' ? 'selected' : ''}>reference</option>
                </select>
            </div>
            <div data-bpf-field-ref-opts class="grid grid-cols-1 md:grid-cols-2 gap-3 ${type === 'reference' ? '' : 'hidden'}">
                <div>
                    <label class="block text-xs text-gray-500 dark:text-slate-400 mb-0.5">minImages</label>
                    <input data-bpf-field-min-images type="number" min="0" max="14" value="${minImg}" class="w-full px-3 py-2 border border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 rounded-lg text-sm">
                </div>
                <div>
                    <label class="block text-xs text-gray-500 dark:text-slate-400 mb-0.5">maxImages</label>
                    <input data-bpf-field-max-images type="number" min="1" max="14" value="${maxImg}" class="w-full px-3 py-2 border border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 rounded-lg text-sm">
                </div>
            </div>
            <label class="flex items-center gap-2 text-sm text-gray-700 dark:text-slate-300">
                <input data-bpf-field-required type="checkbox" ${field.required ? 'checked' : ''} class="h-4 w-4 text-blue-600 border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-800 rounded">
                Обязательное
            </label>
        </div>
    `;
}

/** Сохраняем свойства полей, которых нет в форме (promptTemplate у reference, options у select). */
function enrichFieldsFromSnapshot(formFields, snapshot) {
    const olds = snapshot && Array.isArray(snapshot.fields) ? snapshot.fields : [];
    const byKey = {};
    olds.forEach((f) => {
        if (f && f.key) {
            byKey[String(f.key).trim()] = f;
        }
    });
    return formFields.map((f) => {
        const o = byKey[f.key.trim()];
        if (!o) {
            return { ...f };
        }
        return { ...o, ...f };
    });
}

function readFieldsFromForm() {
    return Array.from(document.querySelectorAll('[data-bpf-field-row]'))
        .map((row) => {
            const key = row.querySelector('[data-bpf-field-key]').value.trim();
            const type = row.querySelector('[data-bpf-field-type]').value;
            const base = {
                key,
                label: row.querySelector('[data-bpf-field-label]').value.trim() || null,
                type,
                required: row.querySelector('[data-bpf-field-required]').checked
            };
            if (type === 'reference') {
                const minEl = row.querySelector('[data-bpf-field-min-images]');
                const maxEl = row.querySelector('[data-bpf-field-max-images]');
                const minV = minEl ? parseInt(minEl.value, 10) : 0;
                const maxV = maxEl ? parseInt(maxEl.value, 10) : 1;
                base.minImages = Number.isFinite(minV) ? minV : 0;
                base.maxImages = Number.isFinite(maxV) ? maxV : 1;
            }
            return base;
        })
        .filter((field) => field.key);
}

function bpfWireFieldListEvents() {
    const list = $('bpf-fields-list');
    list.onclick = (e) => {
        const btn = e.target.closest('[data-bpf-remove-field]');
        if (!btn) {
            return;
        }
        const idx = parseInt(btn.getAttribute('data-bpf-remove-field'), 10);
        const fields = readFieldsFromForm();
        fields.splice(idx, 1);
        renderFieldEditor(fields);
    };
}

function validateBlueprintPresetUiContract(promptSuffix, promptInput, fields) {
    const uiMode = $('bpf-ui-mode').value;
    if (uiMode !== 'STRUCTURED_FIELDS' && uiMode !== 'LOCKED_TEMPLATE' && uiMode !== 'CUSTOM_PROMPT') {
        return null;
    }

    const placeholders = extractTemplatePlaceholders(promptSuffix);
    const fieldKeys = new Set(fields.map((field) => field.key));
    for (const key of placeholders) {
        if (key === 'prompt') {
            continue;
        }
        if (key === 'preset_ref') {
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

    const presetRefMax =
        promptInput.referenceImages && promptInput.referenceImages.maxCount != null
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

function applyDefaultsToForm(d) {
    const defs = d || {};
    $('bpf-style-prompt').value = defs.promptSuffix || '';
    const sel = $('bpf-remove-background');
    if (defs.removeBackground === true) {
        sel.value = 'true';
    } else if (defs.removeBackground === false) {
        sel.value = 'false';
    } else {
        const m = defs.removeBackgroundMode != null ? String(defs.removeBackgroundMode).toUpperCase() : '';
        if (m === 'FORCE_ON') {
            sel.value = 'true';
        } else if (m === 'FORCE_OFF') {
            sel.value = 'false';
        } else {
            sel.value = '';
        }
    }
    applyPromptInputToForm(defs.promptInput || DEFAULT_PROMPT_INPUT);
    renderFieldEditor(filterUserFields(defs.fields));
}

function fillUiHintsForm(hints) {
    const h = hints || {};
    editingUiHintsExtras = { ...h };
    for (const k of UI_HINT_KEYS) {
        delete editingUiHintsExtras[k];
    }
    $('f-hint-preset-ref').value = h.presetReferenceHelp || '';
    $('f-hint-user-photo').value = h.userPhotoSlotHelp || '';
    $('f-hint-publication').value = h.publicationHint || '';
}

function buildUiHintsFromForm() {
    const out = { ...editingUiHintsExtras };
    const a = $('f-hint-preset-ref').value.trim();
    const b = $('f-hint-user-photo').value.trim();
    const c = $('f-hint-publication').value.trim();
    if (a) {
        out.presetReferenceHelp = a;
    } else {
        delete out.presetReferenceHelp;
    }
    if (b) {
        out.userPhotoSlotHelp = b;
    } else {
        delete out.userPhotoSlotHelp;
    }
    if (c) {
        out.publicationHint = c;
    } else {
        delete out.publicationHint;
    }
    return Object.keys(out).length ? out : null;
}

function buildManagedPresetDefaults() {
    const promptSuffix = $('bpf-style-prompt').value.trim();
    const promptInput = readPromptInputFromForm();
    const fields = enrichFieldsFromSnapshot(readFieldsFromForm(), editingPresetDefaultsSnapshot);
    const uiMode = $('bpf-ui-mode').value;
    const rbRaw = $('bpf-remove-background').value;
    const removeBackground = parseRemoveBackgroundValue(rbRaw);

    const managed = {
        promptSuffix,
        uiMode,
        promptInput,
        fields
    };
    if (removeBackground === null) {
        managed.removeBackgroundMode = 'PRESET_DEFAULT';
        managed.removeBackground = null;
    } else {
        managed.removeBackground = removeBackground;
    }
    return { managed, promptInput, fields, promptSuffix };
}

function renderTable() {
    const tb = $('tbody');
    if (!rows.length) {
        tb.innerHTML =
            '<tr><td colspan="6" class="px-3 py-6 text-center text-slate-400">Шаблоны не загружены</td></tr>';
        return;
    }
    tb.innerHTML = rows
        .map(
            (r) => `
        <tr class="hover:bg-slate-50 dark:hover:bg-slate-800/50">
            <td class="px-3 py-2 font-mono text-xs">${r.id}</td>
            <td class="px-3 py-2 font-mono text-xs">${escapeHtml(r.code)}</td>
            <td class="px-3 py-2">${escapeHtml(r.adminTitle)}</td>
            <td class="px-3 py-2">${r.enabled !== false ? 'да' : 'нет'}</td>
            <td class="px-3 py-2">${r.sortOrder ?? 0}</td>
            <td class="px-3 py-2 whitespace-nowrap text-right space-x-2">
                <button type="button" data-edit="${r.id}" class="text-blue-600 dark:text-blue-400 text-xs hover:underline">Изменить</button>
                <button type="button" data-del="${r.id}" class="text-red-600 dark:text-red-400 text-xs hover:underline">Удалить</button>
            </td>
        </tr>
    `
        )
        .join('');

    tb.querySelectorAll('[data-edit]').forEach((btn) => {
        btn.addEventListener('click', () =>
            openModal(rows.find((x) => x.id === parseInt(btn.getAttribute('data-edit'), 10)))
        );
    });
    tb.querySelectorAll('[data-del]').forEach((btn) => {
        btn.addEventListener('click', () => removeRow(parseInt(btn.getAttribute('data-del'), 10)));
    });
}

async function load() {
    showErr('');
    try {
        rows = await api.getUserPresetCreationBlueprintsAdmin();
        if (!Array.isArray(rows)) {
            rows = [];
        }
        renderTable();
    } catch (e) {
        showErr(e.message || 'Ошибка загрузки');
    }
}

function openModal(row) {
    $('modal').classList.remove('hidden');
    bpfWireFieldListEvents();

    if (row) {
        $('modal-title').textContent = 'Редактировать шаблон';
        $('f-id').value = row.id;
        $('f-code').value = row.code || '';
        $('f-title').value = row.adminTitle || '';
        $('f-order').value = row.sortOrder ?? 0;
        $('f-enabled').checked = row.enabled !== false;
        editingPresetDefaultsSnapshot = deepClone(row.presetDefaults) || {};
        applyDefaultsToForm(editingPresetDefaultsSnapshot);
        fillUiHintsForm(row.uiHints || null);
        updateModeBadge();
    } else {
        $('modal-title').textContent = 'Новый шаблон';
        $('f-id').value = '';
        $('f-code').value = '';
        $('f-title').value = '';
        $('f-order').value = '0';
        $('f-enabled').checked = true;
        editingPresetDefaultsSnapshot = stripManagedDefaults(deepClone(DEFAULT_DEFAULTS_TMPL));
        applyDefaultsToForm(DEFAULT_DEFAULTS_TMPL);
        fillUiHintsForm(DEFAULT_HINTS_TMPL);
        updateModeBadge();
    }
}

function closeModal() {
    $('modal').classList.add('hidden');
}

async function removeRow(id) {
    if (!confirm('Удалить шаблон #' + id + '?')) {
        return;
    }
    try {
        await api.deleteUserPresetCreationBlueprint(id);
        await load();
    } catch (e) {
        showErr(e.message || 'Ошибка удаления');
    }
}

$('btn-reload').addEventListener('click', load);
$('btn-add').addEventListener('click', () => openModal(null));
$('modal-close').addEventListener('click', closeModal);
$('btn-cancel').addEventListener('click', closeModal);

$('bpf-prompt-enabled').addEventListener('change', function () {
    const opts = $('bpf-prompt-options');
    if (this.checked) {
        opts.classList.remove('hidden');
    } else {
        opts.classList.add('hidden');
        $('bpf-prompt-required').checked = false;
    }
    updateModeBadge();
});

$('bpf-style-prompt').addEventListener('input', updateModeBadge);

$('bpf-add-field-btn').addEventListener('click', () => {
    const fields = readFieldsFromForm();
    fields.push({
        key: '',
        label: '',
        type: 'text',
        required: false
    });
    renderFieldEditor(fields);
});

$('form').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    showErr('');
    let promptInput;
    try {
        promptInput = readPromptInputFromForm();
    } catch (e) {
        showErr(e.message || 'Некорректные настройки референсов');
        return;
    }
    const fields = readFieldsFromForm();
    const promptSuffix = $('bpf-style-prompt').value.trim();
    updateModeBadge();
    const err = validateBlueprintPresetUiContract(promptSuffix, promptInput, fields);
    if (err) {
        showErr(err);
        return;
    }

    const { managed } = buildManagedPresetDefaults();
    const presetDefaults = mergePresetDefaultsFromForm(managed);
    const uiHints = buildUiHintsFromForm();

    const body = {
        code: $('f-code').value.trim(),
        adminTitle: $('f-title').value.trim(),
        enabled: $('f-enabled').checked,
        sortOrder: parseInt($('f-order').value || '0', 10),
        presetDefaults,
        uiHints
    };
    const idRaw = $('f-id').value;
    try {
        if (idRaw) {
            await api.updateUserPresetCreationBlueprint(parseInt(idRaw, 10), body);
        } else {
            await api.createUserPresetCreationBlueprint(body);
        }
        closeModal();
        await load();
    } catch (e) {
        showErr(e.message || 'Ошибка сохранения (проверьте согласованность шаблона и полей)');
    }
});

document.addEventListener('DOMContentLoaded', () => {
    bpfWireFieldListEvents();
    load();
});
