/**
 * Логика авторизации для админ-панели
 */

// Проверить авторизацию
function isAuthenticated() {
    const initData = localStorage.getItem('telegram_init_data');
    return initData && initData.length > 0;
}

// Выйти из системы
function logout() {
    localStorage.removeItem('telegram_init_data');
    window.location.href = '/admin/login.html';
}

// Получить initData из Telegram Mini App
function getInitDataFromTelegram() {
    if (window.Telegram && window.Telegram.WebApp) {
        return window.Telegram.WebApp.initData;
    }
    return null;
}

// Авторизоваться с initData
async function login(initData) {
    try {
        // Валидируем initData
        const response = await api.validateInitData(initData);
        
        if (response.valid) {
            // Сохраняем initData
            api.setInitData(initData);
            showNotification('Успешная авторизация!', 'success');
            
            // Перенаправляем на главную страницу
            setTimeout(() => {
                window.location.href = '/admin/index.html';
            }, 500);
            
            return true;
        } else {
            showNotification('Неверная initData. Проверьте данные и попробуйте снова.', 'error');
            return false;
        }
    } catch (error) {
        console.error('Login error:', error);
        showNotification('Ошибка авторизации: ' + error.message, 'error');
        return false;
    }
}

// Проверить авторизацию на защищенных страницах
function checkAuth() {
    if (!isAuthenticated()) {
        window.location.href = '/admin/login.html';
        return false;
    }
    return true;
}
