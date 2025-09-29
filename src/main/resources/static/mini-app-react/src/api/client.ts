import axios, { AxiosInstance } from 'axios';
import { StickerSetListResponse, StickerSetResponse, AuthResponse } from '@/types/sticker';

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: '/api',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      }
    });

    // Добавляем interceptor для логирования
    this.client.interceptors.request.use(
      (config) => {
        console.log('🌐 API запрос:', config.method?.toUpperCase(), config.url);
        return config;
      },
      (error) => {
        console.error('❌ Ошибка запроса:', error);
        return Promise.reject(error);
      }
    );

    this.client.interceptors.response.use(
      (response) => {
        console.log('✅ API ответ:', response.status, response.config.url);
        return response;
      },
      (error) => {
        console.error('❌ Ошибка ответа:', error.response?.status, error.response?.data);
        return Promise.reject(error);
      }
    );
  }

  // Добавляем заголовки аутентификации
  setAuthHeaders(initData: string, botName: string = 'StickerGallery') {
    this.client.defaults.headers.common['X-Telegram-Init-Data'] = initData;
    this.client.defaults.headers.common['X-Telegram-Bot-Name'] = botName;
    console.log('✅ Заголовки аутентификации установлены');
  }

  // Удаляем заголовки аутентификации
  clearAuthHeaders() {
    delete this.client.defaults.headers.common['X-Telegram-Init-Data'];
    delete this.client.defaults.headers.common['X-Telegram-Bot-Name'];
    console.log('🧹 Заголовки аутентификации удалены');
  }

  // Получение списка стикерсетов
  async getStickerSets(): Promise<StickerSetListResponse> {
    const response = await this.client.get<StickerSetListResponse>('/stickersets');
    return response.data;
  }

  // Получение стикерсета по ID
  async getStickerSet(id: number): Promise<StickerSetResponse> {
    const response = await this.client.get<StickerSetResponse>(`/stickersets/${id}`);
    return response.data;
  }

  // Удаление стикерсета
  async deleteStickerSet(id: number): Promise<void> {
    await this.client.delete(`/stickersets/${id}`);
  }

  // Проверка статуса аутентификации
  async checkAuthStatus(): Promise<AuthResponse> {
    const response = await this.client.get<AuthResponse>('/auth/status');
    return response.data;
  }

  // Получение стикера по file_id
  async getSticker(fileId: string): Promise<Blob> {
    const response = await this.client.get(`/stickers/${fileId}`, {
      responseType: 'blob'
    });
    return response.data;
  }

  // Создание URL для стикера
  getStickerUrl(fileId: string): string {
    return `/api/stickers/${fileId}`;
  }
}

export const apiClient = new ApiClient();
