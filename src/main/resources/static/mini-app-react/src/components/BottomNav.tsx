import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { 
  BottomNavigation, 
  BottomNavigationAction, 
  Paper
} from '@mui/material';
import HomeIcon from '@mui/icons-material/Home';
import CollectionsIcon from '@mui/icons-material/Collections';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import { apiClient } from '@/api/client';
import { AuthModal } from './AuthModal';

interface BottomNavProps {
  activeTab: number;
  onChange: (newValue: number) => void;
  isInTelegramApp?: boolean;
}

export const BottomNav: React.FC<BottomNavProps> = ({
  activeTab,
  onChange
}) => {
  const navigate = useNavigate();
  const location = useLocation();
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const [showAuthModal, setShowAuthModal] = useState(false);

  useEffect(() => {
    // Сначала проверяем InitData из Telegram WebApp
    const initData = localStorage.getItem('telegram_init_data');
    if (initData) {
      try {
        const data = JSON.parse(initData);
        if (data.user && data.user.id) {
          console.log('✅ Найден ID из Telegram InitData:', data.user.id);
          setCurrentUserId(data.user.id);
          // Сохраняем актуальный ID в localStorage
          localStorage.setItem('authenticated_user_id', data.user.id.toString());
          return;
        }
      } catch (e) {
        console.warn('⚠️ Ошибка парсинга InitData:', e);
      }
    }

    // Если InitData недоступен, проверяем сохраненный ID из localStorage
    const savedUserId = localStorage.getItem('authenticated_user_id');
    if (savedUserId) {
      const id = parseInt(savedUserId, 10);
      if (!isNaN(id)) {
        console.log('✅ Найден сохраненный ID пользователя:', id);
        setCurrentUserId(id);
      }
    } else {
      console.log('ℹ️ Пользователь не авторизован. Для очистки: localStorage.removeItem("authenticated_user_id")');
    }
  }, []);

  // Показываем нижнюю навигацию везде для лучшего UX

  // Определяем активную вкладку по маршруту
  const getCurrentTab = () => {
    if (location.pathname === '/') return 0;
    if (location.pathname.startsWith('/profile/')) return 3;
    return activeTab;
  };

  const handleNavigation = (_event: any, newValue: number) => {
    onChange(newValue);

    switch (newValue) {
      case 0:
        navigate('/');
        break;
      case 1:
        // TODO: Навигация к странице стикеров
        console.log('Навигация к стикерам (не реализовано)');
        break;
      case 2:
        // TODO: Навигация к маркету
        console.log('Навигация к маркету (не реализовано)');
        break;
      case 3:
        // Проверяем аутентификацию перед переходом в профиль
        console.log('🔍 Клик на профиль. Текущий ID:', currentUserId);
        console.log('🔍 localStorage ID:', localStorage.getItem('authenticated_user_id'));
        
        if (currentUserId) {
          console.log('✅ Переход в профиль:', currentUserId);
          navigate(`/profile/${currentUserId}`);
        } else {
          console.log('⚠️ Пользователь не авторизован, показываем модальное окно');
          // Показываем модальное окно для ввода ID
          setShowAuthModal(true);
        }
        break;
    }
  };

  const handleAuthSuccess = (userId: number) => {
    setCurrentUserId(userId);
    navigate(`/profile/${userId}`);
  };

  return (
    <>
      <Paper 
        sx={{ 
          position: 'fixed', 
          bottom: 0, 
          left: 0, 
          right: 0,
          zIndex: 1000
        }}
        elevation={8}
      >
        <BottomNavigation
          value={getCurrentTab()}
          onChange={handleNavigation}
          sx={{
            height: 64,
            '& .MuiBottomNavigationAction-root': {
              color: 'text.secondary',
              '&.Mui-selected': {
                color: 'primary.main',
              },
            },
          }}
        >
          <BottomNavigationAction 
            icon={<HomeIcon />}
            sx={{
              '&.Mui-selected': {
                '& .MuiSvgIcon-root': {
                  color: 'primary.main',
                },
              },
            }}
          />
          <BottomNavigationAction 
            icon={<CollectionsIcon />}
            sx={{
              '&.Mui-selected': {
                '& .MuiSvgIcon-root': {
                  color: 'primary.main',
                },
              },
            }}
          />
          <BottomNavigationAction 
            icon={<ShoppingCartIcon />}
            sx={{
              '&.Mui-selected': {
                '& .MuiSvgIcon-root': {
                  color: 'primary.main',
                },
              },
            }}
          />
          <BottomNavigationAction 
            icon={<AccountCircleIcon />}
            sx={{
              '&.Mui-selected': {
                '& .MuiSvgIcon-root': {
                  color: 'primary.main',
                },
              },
            }}
          />
        </BottomNavigation>
      </Paper>

      {/* Модальное окно аутентификации */}
      <AuthModal 
        open={showAuthModal}
        onClose={() => setShowAuthModal(false)}
        onSuccess={handleAuthSuccess}
      />
    </>
  );
};
