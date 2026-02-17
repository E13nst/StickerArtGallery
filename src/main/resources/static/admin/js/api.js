/**
 * API Client для админ-панели
 */

class AdminApiClient {
    constructor() {
        this.baseUrl = '/api';
        this.initData = localStorage.getItem('telegram_init_data');
    }
    
    // Обновить initData
    setInitData(initData) {
        this.initData = initData;
        localStorage.setItem('telegram_init_data', initData);
    }
    
    // Базовый метод для запросов
    async request(endpoint, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            'X-Telegram-Init-Data': this.initData,
            ...options.headers
        };
        
        try {
            const response = await fetch(`${this.baseUrl}${endpoint}`, {
                ...options,
                headers
            });
            
            // Если 401 - очищаем данные и перенаправляем на страницу авторизации
            if (response.status === 401) {
                localStorage.removeItem('telegram_init_data');
                showNotification('Сессия истекла. Пожалуйста, авторизуйтесь снова.', 'error');
                setTimeout(() => {
                    window.location.href = '/admin/login.html';
                }, 1000);
                throw new Error('Unauthorized');
            }
            
            // Если 403 - очищаем данные и показываем ошибку доступа
            if (response.status === 403) {
                localStorage.removeItem('telegram_init_data');
                showNotification('Доступ запрещен. Требуются права администратора.', 'error');
                setTimeout(() => {
                    window.location.href = '/admin/login.html';
                }, 2000);
                throw new Error('Forbidden');
            }
            
            // Если 204 No Content - возвращаем null
            if (response.status === 204) {
                return null;
            }
            
            const data = await response.json();
            
            if (!response.ok) {
                throw new Error(data.message || `HTTP error! status: ${response.status}`);
            }
            
            return data;
        } catch (error) {
            console.error('API request failed:', error);
            throw error;
        }
    }
    
    // ============ Auth API ============
    
    async validateInitData(initData) {
        return this.request('/auth/validate', {
            method: 'POST',
            body: JSON.stringify({ initData })
        });
    }
    
    // ============ Users API ============
    
    async getUsers(filters = {}, page = 0, size = 20, sort = 'createdAt', direction = 'DESC') {
        const params = {
            page,
            size,
            sort,
            direction,
            ...filters
        };
        
        const queryString = buildQueryString(params);
        return this.request(`/profiles${queryString}`);
    }
    
    async getUserById(userId) {
        return this.request(`/users/${userId}`);
    }

    async getUserProfileByUserId(userId) {
        return this.request(`/users/${userId}/profile`);
    }
    
    async updateUserProfile(userId, data) {
        return this.request(`/users/${userId}/profile`, {
            method: 'PATCH',
            body: JSON.stringify(data)
        });
    }
    
    async bulkBlockUsers(userIds) {
        const promises = userIds.map(userId => 
            this.updateUserProfile(userId, { isBlocked: true })
        );
        return Promise.all(promises);
    }
    
    async bulkUnblockUsers(userIds) {
        const promises = userIds.map(userId => 
            this.updateUserProfile(userId, { isBlocked: false })
        );
        return Promise.all(promises);
    }
    
    // ============ Stickers API ============
    
    async getStickersets(filters = {}, page = 0, size = 20, sort = 'createdAt', direction = 'DESC') {
        const params = {
            page,
            size,
            sort,
            direction,
            ...filters
        };
        
        const queryString = buildQueryString(params);
        return this.request(`/stickersets${queryString}`);
    }
    
    async getStickersetById(id) {
        return this.request(`/stickersets/${id}`);
    }
    
    async blockStickerset(id, reason) {
        return this.request(`/stickersets/${id}/block`, {
            method: 'PUT',
            body: JSON.stringify({ reason })
        });
    }
    
    async unblockStickerset(id) {
        return this.request(`/stickersets/${id}/unblock`, {
            method: 'PUT'
        });
    }
    
    async deleteStickerset(id) {
        return this.request(`/stickersets/${id}`, {
            method: 'DELETE'
        });
    }
    
    async setOfficial(id) {
        return this.request(`/stickersets/${id}/official`, {
            method: 'PUT'
        });
    }
    
    async unsetOfficial(id) {
        return this.request(`/stickersets/${id}/official`, {
            method: 'DELETE'
        });
    }
    
    async bulkBlockStickersets(ids, reason) {
        const promises = ids.map(id => this.blockStickerset(id, reason));
        return Promise.all(promises);
    }
    
    async bulkUnblockStickersets(ids) {
        const promises = ids.map(id => this.unblockStickerset(id));
        return Promise.all(promises);
    }
    
    async bulkDeleteStickersets(ids) {
        const promises = ids.map(id => this.deleteStickerset(id));
        return Promise.all(promises);
    }
    
    async bulkSetOfficial(ids) {
        const promises = ids.map(id => this.setOfficial(id));
        return Promise.all(promises);
    }
    
    async bulkUnsetOfficial(ids) {
        const promises = ids.map(id => this.unsetOfficial(id));
        return Promise.all(promises);
    }

    // ============ Generation logs (Admin audit) ============

    async getGenerationLogs(filters = {}, page = 0, size = 20) {
        const params = { page, size, ...filters };
        const queryString = buildQueryString(params);
        return this.request(`/admin/generation-logs${queryString}`);
    }

    async getGenerationLogDetail(taskId) {
        return this.request(`/admin/generation-logs/${encodeURIComponent(taskId)}`);
    }

    async getGenerationLogEvents(taskId) {
        return this.request(`/admin/generation-logs/${encodeURIComponent(taskId)}/events`);
    }
}

// Создаем глобальный экземпляр API client
const api = new AdminApiClient();
