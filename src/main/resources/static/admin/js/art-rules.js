// ART Rules Management
// ========================================

checkAuth();

let rules = [];
let editingRuleCode = null;

// Загрузка правил
async function loadRules() {
    try {
        document.getElementById('loading').classList.remove('hidden');
        document.getElementById('no-data').classList.add('hidden');
        
        rules = await api.getArtRules();
        
        // Валидация данных
        if (!Array.isArray(rules)) {
            console.warn('Expected array, got:', rules);
            rules = [];
        }
        
        renderRules();
        
        document.getElementById('loading').classList.add('hidden');
        if (rules.length === 0) {
            document.getElementById('no-data').classList.remove('hidden');
        }
    } catch (error) {
        console.error('Failed to load rules:', error);
        showNotification('Ошибка загрузки правил: ' + (error.message || 'Неизвестная ошибка'), 'error');
        document.getElementById('loading').classList.add('hidden');
    }
}

function renderDirectionBadge(direction) {
    if (direction === 'CREDIT') {
        return '<span class="px-2 py-1 text-xs font-semibold text-green-800 bg-green-100 rounded-full">CREDIT</span>';
    }
    return '<span class="px-2 py-1 text-xs font-semibold text-red-800 bg-red-100 rounded-full">DEBIT</span>';
}

// Отрисовка таблицы
function renderRules() {
    const tbody = document.getElementById('rules-table-body');
    
    if (rules.length === 0) {
        tbody.innerHTML = '';
        return;
    }
    
    tbody.innerHTML = rules
        .sort((a, b) => (a.code || '').localeCompare(b.code || ''))
        .map(rule => `
            <tr class="hover:bg-gray-50">
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${rule.id}</td>
                <td class="px-6 py-4 text-sm font-mono text-gray-900">
                    ${escapeHtml(rule.code || '')}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm">
                    ${renderDirectionBadge(rule.direction)}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">${rule.amount ?? 0} ART</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    ${rule.description ? escapeHtml(rule.description) : '—'}
                </td>
                <td class="px-6 py-4 text-xs text-gray-700 max-w-xs">
                    ${rule.metadataSchema ? `<span class="font-mono">${escapeHtml(rule.metadataSchema.substring(0, 80))}${rule.metadataSchema.length > 80 ? '...' : ''}</span>` : '—'}
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                    ${rule.isEnabled
                        ? '<span class="px-2 py-1 text-xs font-semibold text-green-800 bg-green-100 rounded-full">Активно</span>'
                        : '<span class="px-2 py-1 text-xs font-semibold text-gray-800 bg-gray-100 rounded-full">Неактивно</span>'
                    }
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm">
                    ${renderActionDropdown([
                        { label: 'Изменить', onclick: `editRule('${String(rule.code || '').replace(/'/g, "\\'")}')`, className: 'text-blue-600' },
                        { label: rule.isEnabled ? 'Отключить' : 'Включить', onclick: `toggleRule('${String(rule.code || '').replace(/'/g, "\\'")}')`, className: rule.isEnabled ? 'text-yellow-600' : 'text-green-600' }
                    ])}
                </td>
            </tr>
        `).join('');
}

// Открыть форму добавления правила
function openAddModal() {
    editingRuleCode = null;
    document.getElementById('modal-title').textContent = 'Добавить правило';
    document.getElementById('rule-form').reset();
    document.getElementById('rule-id').value = '';
    document.getElementById('rule-code').disabled = false;
    document.getElementById('rule-direction').value = 'CREDIT';
    document.getElementById('rule-amount').value = '0';
    document.getElementById('rule-metadata-schema').value = '';
    document.getElementById('rule-enabled').checked = true;
    document.getElementById('edit-modal').classList.remove('hidden');
}

// Открыть форму редактирования
function editRule(code) {
    const rule = rules.find(r => r.code === code);
    if (!rule) return;
    
    editingRuleCode = code;
    document.getElementById('modal-title').textContent = 'Редактировать правило';
    document.getElementById('rule-id').value = rule.id;
    document.getElementById('rule-code').value = rule.code || '';
    document.getElementById('rule-code').disabled = true;
    document.getElementById('rule-direction').value = rule.direction || 'CREDIT';
    document.getElementById('rule-amount').value = rule.amount ?? 0;
    document.getElementById('rule-description').value = rule.description || '';
    document.getElementById('rule-metadata-schema').value = rule.metadataSchema || '';
    document.getElementById('rule-enabled').checked = !!rule.isEnabled;
    document.getElementById('edit-modal').classList.remove('hidden');
}

// Закрыть модальное окно
function closeModal() {
    document.getElementById('edit-modal').classList.add('hidden');
    editingRuleCode = null;
}

// Сохранить правило
async function saveRule(event) {
    event.preventDefault();
    
    const data = {
        code: document.getElementById('rule-code').value.trim(),
        direction: document.getElementById('rule-direction').value,
        amount: parseInt(document.getElementById('rule-amount').value, 10),
        isEnabled: document.getElementById('rule-enabled').checked,
        description: document.getElementById('rule-description').value.trim() || null,
        metadataSchema: document.getElementById('rule-metadata-schema').value.trim() || null
    };
    
    try {
        if (editingRuleCode) {
            await api.updateArtRule(editingRuleCode, data);
            showNotification('Правило успешно обновлено', 'success');
        } else {
            await api.createArtRule(data);
            showNotification('Правило успешно создано', 'success');
        }
        
        closeModal();
        await loadRules();
    } catch (error) {
        console.error('Failed to save rule:', error);
        showNotification(error.message || 'Ошибка сохранения правила', 'error');
    }
}

// Переключить статус правила
async function toggleRule(code) {
    const rule = rules.find(r => r.code === code);
    if (!rule) return;

    const data = {
        code: rule.code,
        direction: rule.direction,
        amount: rule.amount,
        isEnabled: !rule.isEnabled,
        description: rule.description || null,
        metadataSchema: rule.metadataSchema || null
    };

    try {
        await api.updateArtRule(code, data);
        showNotification(`Правило ${data.isEnabled ? 'включено' : 'отключено'}`, 'success');
        await loadRules();
    } catch (error) {
        console.error('Failed to toggle rule:', error);
        showNotification('Ошибка изменения статуса', 'error');
    }
}

// Event listeners
document.getElementById('add-rule-btn').addEventListener('click', openAddModal);
document.getElementById('rule-form').addEventListener('submit', saveRule);

// Загрузка при старте
loadRules();
