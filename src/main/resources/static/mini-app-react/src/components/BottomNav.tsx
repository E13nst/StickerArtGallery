import React from 'react';
import { 
  BottomNavigation, 
  BottomNavigationAction, 
  Paper,
  Box
} from '@mui/material';
import HomeIcon from '@mui/icons-material/Home';
import CollectionsIcon from '@mui/icons-material/Collections';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';

interface BottomNavProps {
  activeTab: number;
  onChange: (newValue: number) => void;
  isInTelegramApp?: boolean;
}

export const BottomNav: React.FC<BottomNavProps> = ({
  activeTab,
  onChange,
  isInTelegramApp = false
}) => {
  // В Telegram не показываем нижнюю навигацию - используем встроенную
  if (isInTelegramApp) {
    return null;
  }

  return (
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
        value={activeTab}
        onChange={(event, newValue) => onChange(newValue)}
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
          label="Главная" 
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
          label="Стикеры" 
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
          label="Маркет" 
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
          label="Профиль" 
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
  );
};
