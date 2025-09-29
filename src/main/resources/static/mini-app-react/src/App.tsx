import React, { useEffect, useState } from 'react';
import { 
  Container, 
  Typography, 
  Box, 
  TextField, 
  InputAdornment,
  AppBar,
  Toolbar
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import { useTelegram } from '@/hooks/useTelegram';
import { useStickerStore } from '@/store/useStickerStore';
import { apiClient } from '@/api/client';
import { UserInfo } from '@/components/UserInfo';
import { AuthStatus } from '@/components/AuthStatus';
import { DebugPanel } from '@/components/DebugPanel';
import { StickerCard } from '@/components/StickerCard';
import { StickerGrid } from '@/components/StickerGrid';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorDisplay } from '@/components/ErrorDisplay';
import { EmptyState } from '@/components/EmptyState';
import { StickerSetResponse } from '@/types/sticker';

const App: React.FC = () => {
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
    // clearErrors
  } = useStickerStore();

  const [searchTerm, setSearchTerm] = useState('');
  const [viewMode, setViewMode] = useState<'list' | 'detail'>('list');
  const [selectedStickerSet, setSelectedStickerSet] = useState<StickerSetResponse | null>(null);
  const [manualInitData, setManualInitData] = useState<string>('');

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
          // Проверяем заголовки от Chrome расширений
          const hasExtensionHeaders = apiClient.checkExtensionHeaders();
          
          if (!hasExtensionHeaders) {
            // В обычном браузере без авторизации - работаем в публичном режиме
            console.log('🌐 Браузерный режим - публичный доступ');
            setAuthStatus({
              authenticated: true,
              role: 'public'
            });
            return true;
          }
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

  // Загрузка initData из localStorage при инициализации
  useEffect(() => {
    const savedInitData = localStorage.getItem('telegram_init_data');
    if (savedInitData) {
      setManualInitData(savedInitData);
    }
  }, []);

  // Загрузка стикерсетов
  const loadStickers = async () => {
    setLoading(true);
    setError(null);

    try {
      // Проверяем авторизацию
      const isAuthenticated = await checkAuth();
      if (!isAuthenticated && isInTelegramApp) {
        throw new Error('Пользователь не авторизован');
      }

      // Загружаем стикерсеты
      const response = await apiClient.getStickerSets();
      setStickerSets(response.content || []);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Ошибка загрузки стикеров';
      setError(errorMessage);
      console.error('❌ Ошибка загрузки стикеров:', error);
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

  // Фильтрация стикерсетов
  const filteredStickerSets = stickerSets.filter(stickerSet =>
    stickerSet.title.toLowerCase().includes(searchTerm.toLowerCase())
  );

  console.log('🔍 App состояние:', {
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
      loadStickers();
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
    <Box sx={{ minHeight: '100vh', backgroundColor: 'background.default' }}>
      {/* Заголовок */}
      <AppBar position="static" color="primary">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            🎨 Галерея стикеров
          </Typography>
        </Toolbar>
      </AppBar>

      <Container maxWidth="sm" sx={{ py: 2 }}>
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
        <TextField
          fullWidth
          placeholder="🔍 Поиск стикеров..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          }}
          sx={{ mb: 2 }}
        />

        {/* Контент */}
        {isLoading ? (
          <LoadingSpinner message="Загрузка стикеров..." />
        ) : error ? (
          <ErrorDisplay error={error} onRetry={loadStickers} />
        ) : viewMode === 'detail' && selectedStickerSet ? (
          // Детальный просмотр стикерсета
          <Box>
            <Box sx={{ mb: 2, textAlign: 'center' }}>
              <Typography variant="h4" gutterBottom>
                {selectedStickerSet.title}
              </Typography>
              <Typography variant="body1" color="text.secondary" gutterBottom>
                {selectedStickerSet.telegramStickerSetInfo?.stickers?.length || 0} стикеров
              </Typography>
            </Box>

            <StickerGrid 
              stickers={selectedStickerSet.telegramStickerSetInfo?.stickers || []}
              isInTelegramApp={isInTelegramApp}
            />

            <Box sx={{ mt: 3, display: 'flex', gap: 2, justifyContent: 'center' }}>
              <button
                onClick={() => handleShareStickerSet(selectedStickerSet.name, selectedStickerSet.title)}
                style={{
                  padding: '8px 16px',
                  backgroundColor: '#2481cc',
                  color: 'white',
                  border: 'none',
                  borderRadius: '6px',
                  cursor: 'pointer'
                }}
              >
                📤 Поделиться
              </button>
              <button
                onClick={() => handleDeleteStickerSet(selectedStickerSet.id, selectedStickerSet.title)}
                style={{
                  padding: '8px 16px',
                  backgroundColor: '#dc3545',
                  color: 'white',
                  border: 'none',
                  borderRadius: '6px',
                  cursor: 'pointer'
                }}
              >
                🗑️ Удалить
              </button>
            </Box>
          </Box>
        ) : filteredStickerSets.length === 0 ? (
          <EmptyState
            title="🎨 Стикеры не найдены"
            message={searchTerm ? 'По вашему запросу ничего не найдено' : 'У вас пока нет созданных наборов стикеров'}
            actionLabel="Создать стикер"
            onAction={handleCreateSticker}
          />
        ) : (
          // Список стикерсетов
          <Box>
            {filteredStickerSets.map((stickerSet) => {
              console.log('🔍 App рендер StickerCard:', {
                stickerSetId: stickerSet.id,
                isInTelegramApp,
                stickerSetTitle: stickerSet.title
              });
              return (
                <StickerCard
                  key={stickerSet.id}
                  stickerSet={stickerSet}
                  onView={handleViewStickerSet}
                  onShare={handleShareStickerSet}
                  onDelete={handleDeleteStickerSet}
                  isInTelegramApp={isInTelegramApp}
                />
              );
            })}
          </Box>
        )}
      </Container>

    </Box>
  );
};

export default App;
