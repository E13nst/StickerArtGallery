// Prompt Enhancers Management
// ========================================

checkAuth();

let enhancers = [];
let editingEnhancerId = null;

// Загрузка энхансеров
async function loadEnhancers() {
    try {
        document.getElementById('loading').classList.remove('hidden');
        document.getElementById('no-data').classList.add('hidden');
        
        enhancers = await api.getPromptEnhancers();
        
        // Валидация данных
        if (!Array.isArray(enhancers)) {
            console.warn('Expected array, got:', enhancers);
            enhancers = [];
        }
        
        renderEnhancers();
        
        document.getElementById('loading').classList.add('hidden');
        if (enhancers.length === 0) {
            document.getElementById('no-data').classList.remove('hidden');
        }
    } catch (error) {
        console.error('Failed to load enhancers:', error);
        showNotification('Ошибка загрузки энхансеров: ' + (error.message || 'Неизвестная ошибка'), 'error');
        document.getElementById('loading').classList.add('hidden');
    }
}

// Отрисовка таблицы
function renderEnhancers() {
    const tbody = document.getElementById('enhancers-table-body');
    
    if (enhancers.length === 0) {
        tbody.innerHTML = '';
        return;
    }
    
    tbody.innerHTML = enhancers
        .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
        .map(enhancer => `
            <tr class="hover:bg-gray-50">
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${enhancer.id}</td>
                <td class="px-6 py-4 text-sm">
                    <div class="font-medium text-gray-900">${escapeHtml(enhancer.name)}</div>
                    ${enhancer.description ? `<div class="text-xs text-gray-500 mt-1">${escapeHtml(enhancer.description)}</div>` : ''}
                </td>
                <td class="px-6 py-4 text-sm">
                    <div class="font-mono text-xs bg-gray-50 p-2 rounded max-w-md overflow-x-auto">
                        ${enhancer.systemPrompt ? escapeHtml(enhancer.systemPrompt.substring(0, 120)) + (enhancer.systemPrompt.length > 120 ? '...' : '') : '<span class="text-gray-400">—</span>'}
                    </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${enhancer.sortOrder ?? 0}</td>
                <td class="px-6 py-4 whitespace-nowrap">
                    ${enhancer.isEnabled 
                        ? '<span class="px-2 py-1 text-xs font-semibold text-green-800 bg-green-100 rounded-full">Активен</span>'
                        : '<span class="px-2 py-1 text-xs font-semibold text-gray-800 bg-gray-100 rounded-full">Неактивен</span>'
                    }
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm">
                    ${renderActionDropdown([
                        { label: 'Изменить', onclick: `editEnhancer(${enhancer.id})`, className: 'text-blue-600' },
                        { label: enhancer.isEnabled ? 'Выкл' : 'Вкл', onclick: `toggleEnhancer(${enhancer.id}, ${!enhancer.isEnabled})`, className: enhancer.isEnabled ? 'text-yellow-600' : 'text-green-600' },
                        { label: 'Удалить', onclick: `deleteEnhancer(${enhancer.id})`, className: 'text-red-600' }
                    ])}
                </td>
            </tr>
        `).join('');
}

// Открыть форму добавления
function openAddModal() {
    editingEnhancerId = null;
    document.getElementById('modal-title').textContent = 'Добавить энхансер';
    document.getElementById('enhancer-form').reset();
    document.getElementById('enhancer-id').value = '';
    document.getElementById('enhancer-enabled').checked = true;
    document.getElementById('edit-modal').classList.remove('hidden');
}

// Открыть форму редактирования
function editEnhancer(id) {
    const enhancer = enhancers.find(e => e.id === id);
    if (!enhancer) return;
    
    editingEnhancerId = id;
    document.getElementById('modal-title').textContent = 'Редактировать энхансер';
    document.getElementById('enhancer-id').value = enhancer.id;
    document.getElementById('enhancer-code').value = enhancer.code || '';
    document.getElementById('enhancer-name').value = enhancer.name;
    document.getElementById('enhancer-description').value = enhancer.description || '';
    document.getElementById('enhancer-system-prompt').value = enhancer.systemPrompt || '';
    document.getElementById('enhancer-sort-order').value = enhancer.sortOrder ?? 0;
    document.getElementById('enhancer-enabled').checked = !!enhancer.isEnabled;
    document.getElementById('edit-modal').classList.remove('hidden');
}

// Закрыть модальное окно
function closeModal() {
    document.getElementById('edit-modal').classList.add('hidden');
    editingEnhancerId = null;
}

// Сохранить энхансер
async function saveEnhancer(event) {
    event.preventDefault();
    
    const data = {
        code: document.getElementById('enhancer-code').value.trim(),
        name: document.getElementById('enhancer-name').value.trim(),
        description: document.getElementById('enhancer-description').value.trim() || null,
        systemPrompt: document.getElementById('enhancer-system-prompt').value.trim(),
        sortOrder: parseInt(document.getElementById('enhancer-sort-order').value) || 0
    };
    const enabled = document.getElementById('enhancer-enabled').checked;
    
    try {
        if (editingEnhancerId) {
            await api.updatePromptEnhancer(editingEnhancerId, data);
            showNotification('Энхансер успешно обновлен', 'success');
            await api.togglePromptEnhancerEnabled(editingEnhancerId, enabled);
        } else {
            await api.createPromptEnhancer(data);
            showNotification('Энхансер успешно создан', 'success');
            // created enhancer default enabled is handled in backend; if нужно выключить — пользователь сделает toggle
        }
        
        closeModal();
        await loadEnhancers();
    } catch (error) {
        console.error('Failed to save enhancer:', error);
        showNotification(error.message || 'Ошибка сохранения энхансера', 'error');
    }
}

// Переключить статус
async function toggleEnhancer(id, enabled) {
    try {
        await api.togglePromptEnhancerEnabled(id, enabled);
        showNotification(`Энхансер ${enabled ? 'активирован' : 'деактивирован'}`, 'success');
        await loadEnhancers();
    } catch (error) {
        console.error('Failed to toggle enhancer:', error);
        showNotification('Ошибка изменения статуса', 'error');
    }
}

// Удалить энхансер
async function deleteEnhancer(id) {
    if (!confirmAction('Удалить этот энхансер? Это действие нельзя отменить.')) {
        return;
    }
    
    try {
        await api.deletePromptEnhancer(id);
        showNotification('Энхансер удален', 'success');
        await loadEnhancers();
    } catch (error) {
        console.error('Failed to delete enhancer:', error);
        showNotification(error.message || 'Ошибка удаления энхансера', 'error');
    }
}

// Event listeners
document.getElementById('add-enhancer-btn').addEventListener('click', openAddModal);
document.getElementById('enhancer-form').addEventListener('submit', saveEnhancer);

// Загрузка при старте
loadEnhancers();
