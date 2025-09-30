import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Container, 
  Card, 
  CardContent, 
  Typography, 
  Box, 
  Avatar, 
  Grid, 
  Alert,
  Skeleton
} from '@mui/material';
import { Header } from '@/components/Header';
import { BottomNav } from '@/components/BottomNav';
import { apiClient } from '@/api/client';
import { UserInfo } from '@/store/useProfileStore';

export const UserProfilePage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null);

  useEffect(() => {
    if (!id) {
      setError('ID пользователя не указан');
      setLoading(false);
      return;
    }

    const userId = parseInt(id, 10);
    if (isNaN(userId)) {
      setError('Некорректный ID пользователя');
      setLoading(false);
      return;
    }

    loadUserProfile(userId);
  }, [id]);

  const loadUserProfile = async (userId: number) => {
    try {
      setLoading(true);
      setError(null);
      
      console.log('🔍 Загрузка профиля пользователя:', userId);
      const data = await apiClient.getUserInfo(userId);
      console.log('✅ Профиль загружен:', data);
      
      setUserInfo(data);
    } catch (err) {
      console.error('❌ Ошибка загрузки профиля:', err);
      setError(err instanceof Error ? err.message : 'Ошибка загрузки профиля');
    } finally {
      setLoading(false);
    }
  };

  const handleBack = () => {
    navigate('/');
  };

  const user = userInfo;

  return (
    <Box sx={{ 
      minHeight: '100vh', 
      backgroundColor: 'background.default',
      paddingBottom: 8 // Отступ для BottomNav
    }}>
      {/* Заголовок */}
      <Header 
        title="Профиль пользователя"
        onMenuClick={handleBack}
        showOptions={false}
      />

      <Container maxWidth="sm" sx={{ py: 2 }}>
        {loading ? (
          // Скелетон загрузки
          <Card sx={{ p: 3 }}>
            <CardContent>
              <Grid container spacing={3} alignItems="center">
                <Grid item>
                  <Skeleton variant="circular" width={80} height={80} />
                </Grid>
                <Grid item xs>
                  <Skeleton variant="text" width="60%" height={32} />
                  <Skeleton variant="text" width="40%" height={24} />
                  <Skeleton variant="text" width="30%" height={20} />
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        ) : error ? (
          // Ошибка
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        ) : user ? (
          // Карточка профиля
          <Card sx={{ p: 3 }}>
            <CardContent>
              <Grid container spacing={3} alignItems="center">
                {/* Аватар */}
                <Grid item>
                  <Avatar
                    src={user.avatarUrl}
                    sx={{ 
                      width: 80, 
                      height: 80,
                      fontSize: '2rem',
                      bgcolor: 'primary.main'
                    }}
                  >
                    {user.firstName.charAt(0)}{user.lastName?.charAt(0) || ''}
                  </Avatar>
                </Grid>

                {/* Данные пользователя */}
                <Grid item xs>
                  <Box>
                    {/* Полное имя */}
                    <Typography 
                      variant="h5" 
                      component="h1"
                      sx={{ 
                        fontWeight: 700,
                        mb: 1,
                        color: 'text.primary'
                      }}
                    >
                      {user.firstName} {user.lastName || ''}
                    </Typography>

                    {/* Username */}
                    {user.username && (
                      <Typography 
                        variant="body1" 
                        color="text.secondary"
                        sx={{ mb: 1 }}
                      >
                        @{user.username}
                      </Typography>
                    )}

                    {/* Роль */}
                    <Typography 
                      variant="body2" 
                      color="text.secondary"
                      sx={{ mb: 1 }}
                    >
                      Роль: {user.role}
                    </Typography>

                    {/* Баланс */}
                    <Typography 
                      variant="body2" 
                      color="text.secondary"
                      sx={{ mb: 2 }}
                    >
                      Баланс: {user.artBalance} ART
                    </Typography>

                    {/* Дата регистрации */}
                    <Typography 
                      variant="body2" 
                      color="text.secondary"
                    >
                      Регистрация: {new Date(user.createdAt).toLocaleDateString()}
                    </Typography>
                  </Box>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        ) : (
          // Нет данных
          <Alert severity="info">
            Данные пользователя не найдены
          </Alert>
        )}
      </Container>

      {/* Нижняя навигация */}
      <BottomNav 
        activeTab={3} 
        onChange={(tab) => {
          if (tab === 0) navigate('/');
          if (tab === 3) navigate('/profile');
        }}
      />
    </Box>
  );
};
