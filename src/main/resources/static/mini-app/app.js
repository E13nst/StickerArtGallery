// Инициализация Telegram Web App
const tg = window.Telegram.WebApp;
tg.expand();
tg.ready();

// Настройка темы
document.documentElement.style.setProperty('--tg-theme-bg-color', tg.themeParams.bg_color || '#ffffff');
document.documentElement.style.setProperty('--tg-theme-text-color', tg.themeParams.text_color || '#000000');
document.documentElement.style.setProperty('--tg-theme-hint-color', tg.themeParams.hint_color || '#999999');
document.documentElement.style.setProperty('--tg-theme-button-color', tg.themeParams.button_color || '#2481cc');
document.documentElement.style.setProperty('--tg-theme-button-text-color', tg.themeParams.button_text_color || '#ffffff');
document.documentElement.style.setProperty('--tg-theme-secondary-bg-color', tg.themeParams.secondary_bg_color || '#f8f9fa');

// Получение информации о пользователе
const user = tg.initDataUnsafe?.user;
const userId = user?.id;

// Отображение информации о пользователе
if (user) {
    document.getElementById('userInfo').innerHTML = `
        <p>Привет, <strong>${user.first_name}${user.last_name ? ' ' + user.last_name : ''}</strong>!</p>
        <p>ID: <strong>${user.id}</strong></p>
    `;
} else {
    document.getElementById('userInfo').innerHTML = `
        <p>Пользователь не определен</p>
    `;
}

// API базовый URL
const API_BASE = '/api/stickersets';

// Загрузка стикеров
async function loadStickers() {
    try {
        const loading = document.getElementById('loading');
        loading.innerHTML = '<p>Загрузка стикеров...</p>';

        let url = API_BASE;
        if (userId) {
            url = `${API_BASE}/user/${userId}`;
        }

        const response = await fetch(url);
        if (response.ok) {
            const stickers = await response.json();
            displayStickers(stickers);
        } else {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
    } catch (error) {
        console.error('Ошибка загрузки стикеров:', error);
        document.getElementById('content').innerHTML = `
            <div class="error">
                <p>Ошибка загрузки стикеров: ${error.message}</p>
                <button class="btn btn-primary" onclick="loadStickers()">Повторить</button>
            </div>
        `;
    }
}

// Отображение стикеров
function displayStickers(stickers) {
    const content = document.getElementById('content');

    if (!stickers || stickers.length === 0) {
        content.innerHTML = `
            <div class="empty-state">
                <h3>�� Стикеры не найдены</h3>
                <p>У вас пока нет созданных наборов стикеров</p>
                <button class="btn btn-primary" onclick="tg.openTelegramLink('https://t.me/your_bot_username')">
                    Создать стикер
                </button>
            </div>
        `;
        return;
    }

    const stickersHtml = stickers.map(sticker => `
        <div class="sticker-card" data-title="${sticker.title.toLowerCase()}">
            <h3>${sticker.title}</h3>
            <p>ID: ${sticker.id}</p>
            <p>Создан: ${new Date(sticker.createdAt).toLocaleDateString()}</p>
            <div class="sticker-actions">
                <button class="btn btn-primary" onclick="openStickerSet('${sticker.name}')">
                    Открыть
                </button>
                <button class="btn btn-secondary" onclick="shareStickerSet('${sticker.name}', '${sticker.title}')">
                    Поделиться
                </button>
                <button class="btn btn-danger" onclick="deleteStickerSet(${sticker.id}, '${sticker.title}')">
                    Удалить
                </button>
            </div>
        </div>
    `).join('');

    content.innerHTML = `
        <div class="sticker-grid" id="stickerGrid">
            ${stickersHtml}
        </div>
    `;
}

// Фильтрация стикеров
function filterStickers() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    const stickerCards = document.querySelectorAll('.sticker-card');

    stickerCards.forEach(card => {
        const title = card.getAttribute('data-title');
        if (title.includes(searchTerm)) {
            card.style.display = 'block';
        } else {
            card.style.display = 'none';
        }
    });
}

// Открытие набора стикеров в Telegram
function openStickerSet(stickerSetName) {
    const url = `https://t.me/addstickers/${stickerSetName}`;
    tg.openTelegramLink(url);
}

// Поделиться набором стикеров
function shareStickerSet(stickerSetName, title) {
    const shareText = `🎨 Посмотрите мой набор стикеров "${title}": https://t.me/addstickers/${stickerSetName}`;
    tg.shareUrl(shareText, `https://t.me/addstickers/${stickerSetName}`);
}

// Удаление набора стикеров
async function deleteStickerSet(id, title) {
    if (!confirm(`Вы уверены, что хотите удалить набор стикеров "${title}"?`)) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/${id}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            tg.showAlert(`Набор стикеров "${title}" успешно удален`);
            loadStickers(); // Перезагружаем список
        } else {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
    } catch (error) {
        console.error('Ошибка удаления:', error);
        tg.showAlert(`Ошибка удаления: ${error.message}`);
    }
}

// Обработка кнопки "Назад"
tg.BackButton.onClick(() => {
    tg.close();
});

// Показываем кнопку "Назад"
tg.BackButton.show();

// Загружаем стикеры при загрузке страницы
document.addEventListener('DOMContentLoaded', loadStickers);

// Обработка ошибок
window.addEventListener('error', (event) => {
    console.error('Ошибка:', event.error);
    tg.showAlert('Произошла ошибка в приложении');
});