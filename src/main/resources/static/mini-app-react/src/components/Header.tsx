import React from 'react';
import { 
  AppBar, 
  Toolbar, 
  Typography, 
  IconButton, 
  Box 
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import MoreVertIcon from '@mui/icons-material/MoreVert';

interface HeaderProps {
  title?: string;
  onMenuClick?: () => void;
  onOptionsClick?: () => void;
  showMenu?: boolean;
  showOptions?: boolean;
}

export const Header: React.FC<HeaderProps> = ({
  title = "🎨 Галерея стикеров",
  onMenuClick,
  onOptionsClick,
  showMenu = true,
  showOptions = true
}) => {
  return (
    <AppBar 
      position="static" 
      color="primary"
      sx={{ 
        height: 56,
        minHeight: 56
      }}
    >
      <Toolbar 
        sx={{ 
          minHeight: '56px !important',
          paddingX: 2
        }}
      >
        {/* Кнопка меню слева */}
        {showMenu && (
          <IconButton
            edge="start"
            color="inherit"
            aria-label="menu"
            onClick={onMenuClick}
            sx={{ mr: 2 }}
          >
            <MenuIcon />
          </IconButton>
        )}

        {/* Название по центру */}
        <Box sx={{ flexGrow: 1, textAlign: 'center' }}>
          <Typography 
            variant="h6" 
            component="h1"
            sx={{ 
              fontSize: '20px',
              fontWeight: 'bold',
              color: 'white'
            }}
          >
            {title}
          </Typography>
        </Box>

        {/* Кнопка опций справа */}
        {showOptions && (
          <IconButton
            edge="end"
            color="inherit"
            aria-label="options"
            onClick={onOptionsClick}
          >
            <MoreVertIcon />
          </IconButton>
        )}
      </Toolbar>
    </AppBar>
  );
};
