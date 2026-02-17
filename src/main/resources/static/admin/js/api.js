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
                const errorMessage = data.message || data.error || `HTTP error! status: ${response.status}`;
                const error = new Error(errorMessage);
                error.status = response.status;
                error.data = data;
                throw error;
            }
            
            return data;
        } catch (error) {
            console.error('API request failed:', error);
            throw error;
        }
    }
    
    // Обработка массовых операций с partial success
    async bulkOperation(items, operation, operationName = 'операция') {
        const results = await Promise.allSettled(
            items.map(item => operation(item))
        );
        
        const successful = results.filter(r => r.status === 'fulfilled').length;
        const failed = results.filter(r => r.status === 'rejected');
        
        const report = {
            total: items.length,
            successful,
            failed: failed.length,
            errors: failed.map((r, idx) => ({
                item: items[results.indexOf(r)],
                error: r.reason?.message || 'Неизвестная ошибка'
            }))
        };
        
        if (report.failed === 0) {
            showNotification(`${operationName}: успешно выполнено для ${successful} элементов`, 'success');
        } else if (report.successful === 0) {
            showNotification(`${operationName}: все операции завершились ошибкой`, 'error');
        } else {
            showNotification(`${operationName}: ${successful} успешно, ${report.failed} ошибок`, 'warning');
        }
        
        return report;
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
    
    async publishStickerset(id) {
        return this.request(`/stickersets/${id}/publish`, {
            method: 'POST'
        });
    }
    
    async unpublishStickerset(id) {
        return this.request(`/stickersets/${id}/unpublish`, {
            method: 'POST'
        });
    }
    
    async bulkUnsetOfficial(ids) {
        const promises = ids.map(id => this.unsetOfficial(id));
        return Promise.all(promises);
    }

    // ============ Stars Packages API ============
    
    async getStarsPackages() {
        return this.request('/admin/stars/packages');
    }
    
    async getStarsPackage(id) {
        return this.request(`/admin/stars/packages/${id}`);
    }
    
    async createStarsPackage(data) {
        return this.request('/admin/stars/packages', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }
    
    async updateStarsPackage(id, data) {
        return this.request(`/admin/stars/packages/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }
    
    async toggleStarsPackage(id) {
        return this.request(`/admin/stars/packages/${id}/toggle`, {
            method: 'PATCH'
        });
    }
    
    async deleteStarsPackage(id) {
        return this.request(`/admin/stars/packages/${id}`, {
            method: 'DELETE'
        });
    }
    
    async getStarsPackagePurchases(packageId, page = 0, size = 20) {
        const params = { page, size };
        const queryString = buildQueryString(params);
        return this.request(`/admin/stars/packages/${packageId}/purchases${queryString}`);
    }

    // ============ ART Rules API ============
    
    async getArtRules() {
        return this.request('/admin/art-rules');
    }
    
    async createArtRule(data) {
        return this.request('/admin/art-rules', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }
    
    async updateArtRule(code, data) {
        return this.request(`/admin/art-rules/${encodeURIComponent(code)}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }

    // ============ Prompt Enhancers API ============
    
    async getPromptEnhancers() {
        return this.request('/admin/prompt-enhancers');
    }
    
    async getPromptEnhancer(id) {
        return this.request(`/admin/prompt-enhancers/${id}`);
    }
    
    async createPromptEnhancer(data) {
        return this.request('/admin/prompt-enhancers', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }
    
    async updatePromptEnhancer(id, data) {
        return this.request(`/admin/prompt-enhancers/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }
    
    async togglePromptEnhancer(id) {
        throw new Error('togglePromptEnhancer now requires enabled param');
    }

    async togglePromptEnhancerEnabled(id, enabled) {
        return this.request(`/admin/prompt-enhancers/${id}/toggle?enabled=${enabled}`, {
            method: 'PUT'
        });
    }
    
    async deletePromptEnhancer(id) {
        return this.request(`/admin/prompt-enhancers/${id}`, {
            method: 'DELETE'
        });
    }

    // ============ Global Style Presets API ============
    
    async getGlobalStylePresets() {
        return this.request('/generation/style-presets/global');
    }
    
    async getGlobalStylePreset(id) {
        return this.request(`/generation/style-presets/global/${id}`);
    }
    
    async createGlobalStylePreset(data) {
        return this.request('/generation/style-presets/global', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }
    
    async updateGlobalStylePreset(id, data) {
        return this.request(`/generation/style-presets/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }
    
    async toggleGlobalStylePreset(id, enabled) {
        return this.request(`/generation/style-presets/${id}/toggle?enabled=${enabled}`, {
            method: 'PUT'
        });
    }
    
    async deleteGlobalStylePreset(id) {
        return this.request(`/generation/style-presets/${id}`, {
            method: 'DELETE'
        });
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
