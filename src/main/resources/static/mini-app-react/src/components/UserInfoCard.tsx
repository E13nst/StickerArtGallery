import React from 'react';
import { 
  Card, 
  CardContent, 
  Typography, 
  Box, 
  Avatar,
  Chip,
  useTheme,
  useMediaQuery
} from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import { UserInfo } from '@/store/useProfileStore';

interface UserInfoCardProps {
  userInfo: UserInfo;
  isLoading?: boolean;
}

export const UserInfoCard: React.FC<UserInfoCardProps> = ({
  userInfo,
  isLoading = false
}) => {
  const theme = useTheme();
  const isSmallScreen = useMediaQuery(theme.breakpoints.down('sm'));

  if (isLoading) {
    return (
      <Card sx={{ mb: 2, borderRadius: 3 }}>
        <CardContent sx={{ p: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Avatar sx={{ width: 64, height: 64, bgcolor: 'grey.300' }}>
              <PersonIcon />
            </Avatar>
            <Box sx={{ flexGrow: 1 }}>
              <Typography variant="body2" color="text.secondary">
                Загрузка информации о пользователе...
              </Typography>
            </Box>
          </Box>
        </CardContent>
      </Card>
    );
  }

  const displayName = `${userInfo.firstName}${userInfo.lastName ? ` ${userInfo.lastName}` : ''}`;
  const registrationDate = new Date(userInfo.createdAt).toLocaleDateString('ru-RU', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });

  return (
    <Card 
      sx={{ 
        mb: 2, 
        borderRadius: 3,
        boxShadow: '0 2px 12px rgba(0,0,0,0.1)',
        transition: 'box-shadow 0.2s ease',
        '&:hover': {
          boxShadow: '0 4px 20px rgba(0,0,0,0.15)',
        }
      }}
    >
      <CardContent sx={{ p: isSmallScreen ? 2 : 3 }}>
        {/* Основная информация */}
        <Box sx={{ 
          display: 'flex', 
          alignItems: isSmallScreen ? 'flex-start' : 'center',
          flexDirection: isSmallScreen ? 'column' : 'row',
          gap: 2,
          mb: 3
        }}>
          {/* Аватар */}
          <Avatar 
            src={userInfo.avatarUrl} 
            sx={{ 
              width: isSmallScreen ? 56 : 64, 
              height: isSmallScreen ? 56 : 64,
              bgcolor: 'primary.main',
              fontSize: isSmallScreen ? '1.5rem' : '1.75rem',
              fontWeight: 'bold',
              alignSelf: isSmallScreen ? 'center' : 'flex-start'
            }}
          >
            {userInfo.firstName.charAt(0)}{userInfo.lastName?.charAt(0) || ''}
          </Avatar>

          {/* Информация о пользователе */}
          <Box sx={{ 
            flexGrow: 1,
            textAlign: isSmallScreen ? 'center' : 'left'
          }}>
            {/* Имя и фамилия */}
            <Typography 
              variant={isSmallScreen ? 'h6' : 'h5'} 
              component="h2"
              sx={{ 
                fontWeight: 'bold',
                mb: 0.5,
                fontSize: isSmallScreen ? '1.1rem' : '1.5rem'
              }}
            >
              {displayName}
            </Typography>

            {/* Username */}
            {userInfo.username && (
              <Typography 
                variant="body2" 
                color="text.secondary"
                sx={{ 
                  mb: 1,
                  fontSize: isSmallScreen ? '0.8rem' : '0.9rem'
                }}
              >
                @{userInfo.username}
              </Typography>
            )}

            {/* Роль */}
            <Chip 
              label={userInfo.role}
              size="small"
              variant="outlined"
              color="primary"
              sx={{ 
                fontSize: isSmallScreen ? '0.7rem' : '0.75rem',
                height: isSmallScreen ? 20 : 24
              }}
            />
          </Box>
        </Box>

        {/* Дополнительная информация */}
        <Box sx={{ 
          display: 'grid',
          gridTemplateColumns: isSmallScreen ? '1fr' : 'repeat(2, 1fr)',
          gap: 2
        }}>
          {/* Баланс */}
          <Box sx={{ 
            display: 'flex', 
            alignItems: 'center', 
            gap: 1,
            p: isSmallScreen ? 1.5 : 2,
            backgroundColor: 'rgba(36, 129, 204, 0.1)',
            borderRadius: 2,
            border: '1px solid rgba(36, 129, 204, 0.2)'
          }}>
            <AccountBalanceWalletIcon 
              color="primary" 
              sx={{ fontSize: isSmallScreen ? 20 : 24 }}
            />
            <Box>
              <Typography 
                variant="caption" 
                color="text.secondary"
                sx={{ fontSize: isSmallScreen ? '0.7rem' : '0.75rem' }}
              >
                Баланс
              </Typography>
              <Typography 
                variant={isSmallScreen ? 'subtitle2' : 'subtitle1'} 
                sx={{ 
                  fontWeight: 'bold',
                  color: 'primary.main',
                  fontSize: isSmallScreen ? '0.9rem' : '1rem'
                }}
              >
                {userInfo.artBalance} ART
              </Typography>
            </Box>
          </Box>

          {/* Дата регистрации */}
          <Box sx={{ 
            display: 'flex', 
            alignItems: 'center', 
            gap: 1,
            p: isSmallScreen ? 1.5 : 2,
            backgroundColor: 'rgba(158, 158, 158, 0.1)',
            borderRadius: 2,
            border: '1px solid rgba(158, 158, 158, 0.2)'
          }}>
            <CalendarTodayIcon 
              color="action" 
              sx={{ fontSize: isSmallScreen ? 20 : 24 }}
            />
            <Box>
              <Typography 
                variant="caption" 
                color="text.secondary"
                sx={{ fontSize: isSmallScreen ? '0.7rem' : '0.75rem' }}
              >
                Регистрация
              </Typography>
              <Typography 
                variant={isSmallScreen ? 'subtitle2' : 'subtitle1'} 
                sx={{ 
                  fontWeight: 'bold',
                  fontSize: isSmallScreen ? '0.8rem' : '0.9rem'
                }}
              >
                {registrationDate}
              </Typography>
            </Box>
          </Box>
        </Box>

        {/* Telegram ID */}
        <Box sx={{ 
          mt: 2, 
          pt: 2, 
          borderTop: '1px solid',
          borderColor: 'divider'
        }}>
          <Typography 
            variant="caption" 
            color="text.secondary"
            sx={{ 
              fontSize: isSmallScreen ? '0.7rem' : '0.75rem',
              textAlign: 'center',
              display: 'block'
            }}
          >
            Telegram ID: {userInfo.telegramId}
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
};
