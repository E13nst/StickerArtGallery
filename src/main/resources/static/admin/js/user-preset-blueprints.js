checkAuth();

let rows = [];

const DEFAULT_DEFAULTS_TMPL = {
    promptSuffix: 'Идея стикера: {{prompt}}. Опора стилей: {{preset_ref}}. Снимок на эту генерацию: {{user_face}}.',
    uiMode: 'STRUCTURED_FIELDS',
    promptInput: {
        enabled: true,
        required: true,
        placeholder: 'Опишите, что должно получиться…',
        maxLength: 800
    },
    fields: [
        {
            key: 'user_face',
            label: 'Фото только для этой генерации',
            type: 'reference',
            required: false,
            minImages: 0,
            maxImages: 1,
            promptTemplate: 'Image {index}'
        }
    ],
    removeBackgroundMode: 'PRESET_DEFAULT'
};

const DEFAULT_HINTS_TMPL = {
    presetReferenceHelp: 'Это фото сохраняется на сервере и используется как опора стиля для всех следующих генераций по этому пресету.',
    userPhotoSlotHelp: 'Можно менять под каждую генерацию; не фиксируется в пресете.',
    publicationHint: 'После генерации можно опубликовать пресет в каталог. Превью в каталоге обычно берётся из последнего результата. Списание ART — см. поле estimatedPublicationCostArt в ответе API.'
};

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

function esc(s) {
    const d = document.createElement('div');
    d.textContent = s ?? '';
    return d.innerHTML;
}

function renderTable() {
    const tb = $('tbody');
    if (!rows.length) {
        tb.innerHTML = '<tr><td colspan="6" class="px-3 py-6 text-center text-slate-400">Шаблоны не загружены</td></tr>';
        return;
    }
    tb.innerHTML = rows.map((r) => `
        <tr class="hover:bg-slate-50 dark:hover:bg-slate-800/50">
            <td class="px-3 py-2 font-mono text-xs">${r.id}</td>
            <td class="px-3 py-2 font-mono text-xs">${esc(r.code)}</td>
            <td class="px-3 py-2">${esc(r.adminTitle)}</td>
            <td class="px-3 py-2">${r.enabled !== false ? 'да' : 'нет'}</td>
            <td class="px-3 py-2">${r.sortOrder ?? 0}</td>
            <td class="px-3 py-2 whitespace-nowrap text-right space-x-2">
                <button type="button" data-edit="${r.id}" class="text-blue-600 dark:text-blue-400 text-xs hover:underline">Изменить</button>
                <button type="button" data-del="${r.id}" class="text-red-600 dark:text-red-400 text-xs hover:underline">Удалить</button>
            </td>
        </tr>
    `).join('');

    tb.querySelectorAll('[data-edit]').forEach((btn) => {
        btn.addEventListener('click', () => openModal(rows.find((x) => x.id === parseInt(btn.getAttribute('data-edit'), 10))));
    });
    tb.querySelectorAll('[data-del]').forEach((btn) => {
        btn.addEventListener('click', () => removeRow(parseInt(btn.getAttribute('data-del'), 10)));
    });
}

async function load() {
    showErr('');
    try {
        rows = await api.getUserPresetCreationBlueprintsAdmin();
        if (!Array.isArray(rows)) rows = [];
        renderTable();
    } catch (e) {
        showErr(e.message || 'Ошибка загрузки');
    }
}

function openModal(row) {
    $('modal').classList.remove('hidden');
    if (row) {
        $('modal-title').textContent = 'Редактировать шаблон';
        $('f-id').value = row.id;
        $('f-code').value = row.code || '';
        $('f-title').value = row.adminTitle || '';
        $('f-order').value = row.sortOrder ?? 0;
        $('f-enabled').checked = row.enabled !== false;
        $('f-defaults-json').value = JSON.stringify(row.presetDefaults || {}, null, 2);
        $('f-hints-json').value = row.uiHints ? JSON.stringify(row.uiHints, null, 2) : '';
    } else {
        $('modal-title').textContent = 'Новый шаблон';
        $('f-id').value = '';
        $('f-code').value = '';
        $('f-title').value = '';
        $('f-order').value = '0';
        $('f-enabled').checked = true;
        $('f-defaults-json').value = JSON.stringify(DEFAULT_DEFAULTS_TMPL, null, 2);
        $('f-hints-json').value = JSON.stringify(DEFAULT_HINTS_TMPL, null, 2);
    }
}

function closeModal() {
    $('modal').classList.add('hidden');
}

async function removeRow(id) {
    if (!confirm('Удалить шаблон #' + id + '?')) return;
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

$('form').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    showErr('');
    let presetDefaults;
    let uiHints;
    try {
        presetDefaults = JSON.parse($('f-defaults-json').value.trim());
        const hintsRaw = $('f-hints-json').value.trim();
        uiHints = hintsRaw ? JSON.parse(hintsRaw) : null;
    } catch (_e) {
        showErr('Некорректный JSON');
        return;
    }
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
    load();
});
