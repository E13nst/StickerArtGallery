import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  Alert,
  CircularProgress
} from '@mui/material';
import TelegramIcon from '@mui/icons-material/Telegram';
import { configService } from '@/api/config';

interface TelegramAuthModalProps {
  open: boolean;
  onClose: () => void;
  onAuthSuccess: (initData: string) => void;
  onAuthError: (error: string) => void;
  onSkipAuth: () => void;
}

export const TelegramAuthModal: React.FC<TelegramAuthModalProps> = ({
  open,
  onClose,
  onAuthSuccess,
  onAuthError,
  onSkipAuth
}) => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [step, setStep] = useState<'init' | 'waiting' | 'success' | 'error'>('init');

  useEffect(() => {
    if (open) {
      setStep('init');
      setError(null);
    }
  }, [open]);

  const handleTelegramAuth = async () => {
    setIsLoading(true);
    setError(null);
    setStep('waiting');

    try {
      // Получаем конфигурацию с именем бота
      const config = await configService.getConfig();
      const botUsername = config.botName;
      
      // Создаем URL для авторизации через Telegram
      const redirectUrl = encodeURIComponent(window.location.origin + window.location.pathname);
      const telegramAuthUrl = `https://t.me/${botUsername}?startapp=${btoa(redirectUrl)}`;

      console.log('🔗 Telegram Auth URL:', telegramAuthUrl);

      // Открываем Telegram для авторизации
      window.open(telegramAuthUrl, '_blank', 'width=400,height=600');

      // Слушаем сообщения от Telegram Web App
      const handleMessage = (event: MessageEvent) => {
        if (event.origin !== 'https://web.telegram.org') {
          return;
        }

        if (event.data.type === 'telegram-auth') {
          const { initData } = event.data;
          if (initData) {
            console.log('✅ Получен initData от Telegram:', initData);
            setStep('success');
            onAuthSuccess(initData);
            window.removeEventListener('message', handleMessage);
            setTimeout(() => {
              onClose();
            }, 1000);
          } else {
            console.error('❌ initData не получен от Telegram');
            setStep('error');
            setError('Не удалось получить данные авторизации от Telegram');
            onAuthError('Не удалось получить данные авторизации от Telegram');
            window.removeEventListener('message', handleMessage);
          }
          setIsLoading(false);
        }
      };

      window.addEventListener('message', handleMessage);

      // Таймаут для авторизации
      setTimeout(() => {
        window.removeEventListener('message', handleMessage);
        setIsLoading(false);
        if (step === 'waiting') {
          setStep('error');
          setError('Время ожидания авторизации истекло');
          onAuthError('Время ожидания авторизации истекло');
        }
      }, 60000); // 60 секунд
    } catch (error) {
      console.error('❌ Ошибка при получении конфигурации:', error);
      setStep('error');
      setError('Ошибка при получении конфигурации бота');
      setIsLoading(false);
    }
  };

  const renderContent = () => {
    switch (step) {
      case 'init':
        return (
          <Box sx={{ textAlign: 'center', p: 2 }}>
            <TelegramIcon sx={{ fontSize: 48, color: '#0088cc', mb: 2 }} />
            <Typography variant="h6" gutterBottom>
              Авторизация через Telegram
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Для доступа к полному функционалу приложения необходимо авторизоваться через Telegram
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, alignItems: 'center' }}>
              <Button
                variant="contained"
                size="large"
                startIcon={<TelegramIcon />}
                onClick={handleTelegramAuth}
                sx={{
                  backgroundColor: '#0088cc',
                  '&:hover': {
                    backgroundColor: '#006699',
                  },
                }}
              >
                Войти через Telegram
              </Button>
              
              <Button
                variant="outlined"
                size="medium"
                onClick={onSkipAuth}
                sx={{ minWidth: 200 }}
              >
                Продолжить без авторизации
              </Button>
            </Box>
          </Box>
        );

      case 'waiting':
        return (
          <Box sx={{ textAlign: 'center', p: 2 }}>
            <CircularProgress sx={{ mb: 2 }} />
            <Typography variant="h6" gutterBottom>
              Ожидание авторизации
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Откройте Telegram и нажмите кнопку "Start" в боте
            </Typography>
          </Box>
        );

      case 'success':
        return (
          <Box sx={{ textAlign: 'center', p: 2 }}>
            <TelegramIcon sx={{ fontSize: 48, color: '#4caf50', mb: 2 }} />
            <Typography variant="h6" gutterBottom>
              Авторизация успешна!
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Вы успешно авторизованы через Telegram
            </Typography>
          </Box>
        );

      case 'error':
        return (
          <Box sx={{ textAlign: 'center', p: 2 }}>
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
            <Button
              variant="outlined"
              onClick={() => {
                setStep('init');
                setError(null);
              }}
            >
              Попробовать снова
            </Button>
          </Box>
        );

      default:
        return null;
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Авторизация</DialogTitle>
      <DialogContent>
        {renderContent()}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={isLoading}>
          {step === 'success' ? 'Закрыть' : 'Отмена'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};
