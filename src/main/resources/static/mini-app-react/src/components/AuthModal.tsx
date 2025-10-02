import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Typography,
  Box,
  CircularProgress,
  Alert
} from '@mui/material';
import { apiClient } from '@/api/client';

interface AuthModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: (userId: number) => void;
}

export const AuthModal: React.FC<AuthModalProps> = ({ open, onClose, onSuccess }) => {
  const [userId, setUserId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const id = parseInt(userId, 10);
    if (isNaN(id) || id <= 0) {
      setError('Введите корректный ID пользователя');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      // Проверяем существование пользователя в БД
      const userInfo = await apiClient.getUserInfo(id);
      
      if (!userInfo || !userInfo.id) {
        throw new Error('Пользователь не найден в базе данных');
      }

      // Сохраняем ID в localStorage для последующих сессий
      localStorage.setItem('authenticated_user_id', id.toString());
      
      onSuccess(id);
      handleClose();
    } catch (err) {
      console.error('Ошибка аутентификации:', err);
      setError('Пользователь с таким ID не найден в базе данных');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setUserId('');
    setError(null);
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        <Typography variant="h6" component="div">
          Вход в профиль
        </Typography>
      </DialogTitle>

      <form onSubmit={handleSubmit}>
        <DialogContent>
          <Box sx={{ mb: 2 }}>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Введите ваш Telegram ID для доступа к профилю
            </Typography>

            <TextField
              autoFocus
              fullWidth
              label="Telegram ID"
              type="number"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              disabled={loading}
              error={!!error}
              helperText={error || 'Например: 123456789'}
              variant="outlined"
            />
          </Box>

          {error && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {error}
            </Alert>
          )}

          <Box sx={{ mt: 2 }}>
            <Typography variant="caption" color="text.secondary">
              💡 Подсказка: Вы можете узнать свой Telegram ID у бота @userinfobot
            </Typography>
          </Box>
        </DialogContent>

        <DialogActions>
          <Button onClick={handleClose} disabled={loading}>
            Отмена
          </Button>
          <Button 
            type="submit" 
            variant="contained" 
            disabled={loading || !userId}
            startIcon={loading ? <CircularProgress size={20} /> : null}
          >
            {loading ? 'Проверка...' : 'Войти'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};



