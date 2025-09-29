import React, { useEffect, useState } from 'react';
import { 
  Container, 
  Box,
} from '@mui/material';
import { useTelegram } from '@/hooks/useTelegram';
import { useStickerStore } from '@/store/useStickerStore';
import { apiClient } from '@/api/client';
import { StickerSetResponse } from '@/types/sticker';

// Компоненты
import { Header } from '@/components/Header';
import { UserInfo } from '@/components/UserInfo';
import { AuthStatus } from '@/components/AuthStatus';
import { DebugPanel } from '@/components/DebugPanel';
import { SearchBar } from '@/components/SearchBar';
import { StickerSetList } from '@/components/StickerSetList';
import { StickerSetDetail } from '@/components/StickerSetDetail';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorDisplay } from '@/components/ErrorDisplay';
import { EmptyState } from '@/components/EmptyState';
import { BottomNav } from '@/components/BottomNav';
import { TelegramAuthModal } from '@/components/TelegramAuthModal';

export const GalleryPage: React.FC = () => {
  const { tg, user, initData, isReady, isInTelegramApp, checkInitDataExpiry } = useTelegram();
  const {
    isLoading,
    isAuthLoading,
    stickerSets,
    authStatus,
    error,
    authError,
    setLoading,
    setAuthLoading,
    setStickerSets,
    setAuthStatus,
    setError,
    setAuthError,
    removeStickerSet,
  } = useStickerStore();

  // Локальное состояние
  const [searchTerm, setSearchTerm] = useState('');
  const [viewMode, setViewMode] = useState<'list' | 'detail'>('list');
  const [selectedStickerSet, setSelectedStickerSet] = useState<StickerSetResponse | null>(null);
  const [showAuthModal, setShowAuthModal] = useState(false);
  const [manualInitData, setManualInitData] = useState<string>('');
  const [activeBottomTab, setActiveBottomTab] = useState(0);

  // Проверка авторизации
  const checkAuth = async () => {
    console.log('🔍 checkAuth вызван:');
    console.log('  isInTelegramApp:', isInTelegramApp);
    console.log('  initData:', initData ? `${initData.length} chars` : 'empty');
    console.log('  manualInitData:', manualInitData ? `${manualInitData.length} chars` : 'empty');
    console.log('  user:', user);

    // Используем manualInitData если есть, иначе initData от Telegram
    const currentInitData = manualInitData || initData;

    if (!isInTelegramApp && !manualInitData) {
      // В обычном браузере без авторизации - показываем модальное окно
      console.log('🌐 Браузерный режим - требуется авторизация');
      setShowAuthModal(true);
      setAuthStatus({
        authenticated: false,
        role: 'anonymous'
      });
      return false;
    }

    setAuthLoading(true);
    setAuthError(null);

    try {
      // Проверяем срок действия initData
      const initDataCheck = checkInitDataExpiry(currentInitData);
      console.log('🔍 Проверка initData:', initDataCheck);
      if (!initDataCheck.valid) {
        throw new Error(initDataCheck.reason);
      }

      // Устанавливаем заголовки аутентификации
      apiClient.setAuthHeaders(currentInitData);

      // Проверяем статус авторизации
      console.log('🔍 Отправка запроса на проверку авторизации...');
      const authResponse = await apiClient.checkAuthStatus();
      console.log('🔍 Ответ авторизации:', authResponse);
      setAuthStatus(authResponse);

      if (!authResponse.authenticated) {
        throw new Error(authResponse.message || 'Ошибка авторизации');
      }

      return true;
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Неизвестная ошибка';
      setAuthError(errorMessage);
      console.error('❌ Ошибка авторизации:', error);
      return false;
    } finally {
      setAuthLoading(false);
    }
  };

  // Обработка успешной авторизации
  const handleAuthSuccess = (newInitData: string) => {
    console.log('✅ Авторизация успешна, сохраняем initData');
    setManualInitData(newInitData);
    setShowAuthModal(false);
    // Перезапускаем проверку авторизации
    checkAuth();
  };

  // Обработка ошибки авторизации
  const handleAuthError = (error: string) => {
    console.error('❌ Ошибка авторизации:', error);
    setAuthError(error);
  };

  // Обработка пропуска авторизации
  const handleSkipAuth = () => {
    console.log('⏭️ Пользователь пропустил авторизацию');
    setShowAuthModal(false);
    setAuthStatus({
      authenticated: true,
      role: 'public'
    });
  };

  // Загрузка стикерсетов
  const fetchStickerSets = async (page: number = 0) => {
    setLoading(true);
    setError(null);

    try {
      // Проверяем авторизацию
      const isAuthenticated = await checkAuth();
      if (!isAuthenticated && isInTelegramApp) {
        throw new Error('Пользователь не авторизован');
      }

      // Загружаем стикерсеты
      const response = await apiClient.getStickerSets(page);
      setStickerSets(response.content || []);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Ошибка загрузки стикеров';
      setError(errorMessage);
      console.error('❌ Ошибка загрузки стикеров:', error);
    } finally {
      setLoading(false);
    }
  };

  // Поиск стикерсетов
  const searchStickerSets = async (query: string) => {
    if (!query.trim()) {
      fetchStickerSets();
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await apiClient.searchStickerSets(query);
      setStickerSets(response.content || []);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Ошибка поиска стикеров';
      setError(errorMessage);
      console.error('❌ Ошибка поиска стикеров:', error);
    } finally {
      setLoading(false);
    }
  };

  // Обработчики действий
  const handleViewStickerSet = (id: number, _name: string) => {
    const stickerSet = stickerSets.find(s => s.id === id);
    if (stickerSet) {
      setSelectedStickerSet(stickerSet);
      setViewMode('detail');
    }
  };

  const handleShareStickerSet = (name: string, _title: string) => {
    if (tg) {
      tg.openTelegramLink(`https://t.me/addstickers/${name}`);
    } else {
      // Fallback для браузера
      window.open(`https://t.me/addstickers/${name}`, '_blank');
    }
  };

  const handleDeleteStickerSet = async (id: number, title: string) => {
    if (!confirm(`Вы уверены, что хотите удалить набор стикеров "${title}"?`)) {
      return;
    }

    try {
      await apiClient.deleteStickerSet(id);
      removeStickerSet(id);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Ошибка удаления стикера';
      alert(`Ошибка удаления стикера: ${errorMessage}`);
    }
  };

  const handleBackToList = () => {
    setViewMode('list');
    setSelectedStickerSet(null);
  };

  const handleCreateSticker = () => {
    if (tg) {
      tg.openTelegramLink('https://t.me/StickerGalleryBot');
    } else {
      window.open('https://t.me/StickerGalleryBot', '_blank');
    }
  };

  const handleMenuClick = () => {
    console.log('🔍 Меню нажато');
    // TODO: Реализовать боковое меню
  };

  const handleOptionsClick = () => {
    console.log('🔍 Опции нажаты');
    // TODO: Реализовать меню опций
  };

  // Обработка поиска
  const handleSearchChange = (newSearchTerm: string) => {
    setSearchTerm(newSearchTerm);
    // Дебаунс поиска
    const delayedSearch = setTimeout(() => {
      searchStickerSets(newSearchTerm);
    }, 500);

    return () => clearTimeout(delayedSearch);
  };

  // Фильтрация стикерсетов (локальная фильтрация + серверный поиск)
  const filteredStickerSets = stickerSets.filter(stickerSet =>
    stickerSet.title.toLowerCase().includes(searchTerm.toLowerCase())
  );

  console.log('🔍 GalleryPage состояние:', {
    stickerSets: stickerSets.length,
    filteredStickerSets: filteredStickerSets.length,
    searchTerm,
    viewMode,
    isInTelegramApp,
    isLoading,
    isAuthLoading
  });

  // Инициализация при загрузке
  useEffect(() => {
    if (isReady) {
      fetchStickerSets();
    }
  }, [isReady]);

  // Обработка кнопки "Назад" в Telegram
  useEffect(() => {
    if (tg?.BackButton) {
      tg.BackButton.onClick(() => {
        if (viewMode === 'detail') {
          handleBackToList();
        } else {
          tg.close();
        }
      });

      if (viewMode === 'detail') {
        tg.BackButton.show();
      } else {
        tg.BackButton.hide();
      }
    }
  }, [tg, viewMode]);

  if (!isReady) {
    return <LoadingSpinner message="Инициализация..." />;
  }

  return (
    <Box sx={{ 
      minHeight: '100vh', 
      backgroundColor: 'background.default',
      paddingBottom: isInTelegramApp ? 0 : 8 // Отступ для BottomNav в браузере
    }}>
      {/* Заголовок */}
      <Header 
        title="🎨 Галерея стикеров"
        onMenuClick={handleMenuClick}
        onOptionsClick={handleOptionsClick}
      />

      <Container maxWidth={isInTelegramApp ? "sm" : "lg"} sx={{ py: 2 }}>
        {viewMode === 'list' ? (
          <>
            {/* Информация о пользователе */}
            <UserInfo user={user} isLoading={isAuthLoading} />

            {/* Статус авторизации */}
            <AuthStatus 
              authStatus={authStatus} 
              isLoading={isAuthLoading} 
              error={authError} 
            />

            {/* Отладочная панель */}
            <DebugPanel
              user={user}
              initData={initData}
              platform={tg?.platform}
              version={tg?.version}
              initDataValid={checkInitDataExpiry(initData).valid}
              initDataError={checkInitDataExpiry(initData).reason}
            />

            {/* Поиск */}
            <SearchBar
              value={searchTerm}
              onChange={handleSearchChange}
              disabled={isLoading}
            />

            {/* Контент */}
            {isLoading ? (
              <LoadingSpinner message="Загрузка стикеров..." />
            ) : error ? (
              <ErrorDisplay error={error} onRetry={() => fetchStickerSets()} />
            ) : filteredStickerSets.length === 0 ? (
              <EmptyState
                title="🎨 Стикеры не найдены"
                message={searchTerm ? 'По вашему запросу ничего не найдено' : 'У вас пока нет созданных наборов стикеров'}
                actionLabel="Создать стикер"
                onAction={handleCreateSticker}
              />
            ) : (
              <StickerSetList
                stickerSets={filteredStickerSets}
                onView={handleViewStickerSet}
                onShare={handleShareStickerSet}
                onDelete={handleDeleteStickerSet}
                isInTelegramApp={isInTelegramApp}
              />
            )}
          </>
        ) : (
          // Детальный просмотр стикерсета
          selectedStickerSet && (
            <StickerSetDetail
              stickerSet={selectedStickerSet}
              onBack={handleBackToList}
              onShare={handleShareStickerSet}
              onDelete={handleDeleteStickerSet}
              isInTelegramApp={isInTelegramApp}
            />
          )
        )}
      </Container>

      {/* Нижняя навигация */}
      <BottomNav
        activeTab={activeBottomTab}
        onChange={setActiveBottomTab}
        isInTelegramApp={isInTelegramApp}
      />

      {/* Модальное окно авторизации */}
      <TelegramAuthModal
        open={showAuthModal}
        onClose={() => setShowAuthModal(false)}
        onAuthSuccess={handleAuthSuccess}
        onAuthError={handleAuthError}
        onSkipAuth={handleSkipAuth}
      />
    </Box>
  );
};
