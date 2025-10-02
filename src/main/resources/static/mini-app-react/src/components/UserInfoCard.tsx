import React from 'react';
import { 
  Card, 
  CardContent, 
  Typography, 
  Box, 
  Avatar,
  Chip,
  Button
} from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';
import ShareIcon from '@mui/icons-material/Share';
import MessageIcon from '@mui/icons-material/Message';
import { UserInfo } from '@/store/useProfileStore';

interface UserInfoCardProps {
  userInfo: UserInfo;
  isLoading?: boolean;
  onShareProfile?: () => void;
  onMessageUser?: () => void;
}

export const UserInfoCard: React.FC<UserInfoCardProps> = ({
  userInfo,
  isLoading = false,
  onShareProfile,
  onMessageUser
}) => {
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

  return (
    <Card sx={{ mb: 2, borderRadius: 3, boxShadow: '0 2px 12px rgba(0,0,0,0.1)', transition: 'box-shadow 0.2s ease', '&:hover': { boxShadow: '0 4px 20px rgba(0,0,0,0.15)' } }}>
      <CardContent sx={{ p: 2 }}>
        {/* Основная информация */}
        <Box sx={{ display: 'flex', alignItems: 'flex-start', flexDirection: 'column', gap: 2, mb: 3 }}>
          <Avatar 
            src={userInfo.avatarUrl} 
            sx={{ width: 56, height: 56, bgcolor: 'primary.main', fontSize: '1.5rem', fontWeight: 'bold', alignSelf: 'center' }}
          >
            {userInfo.firstName.charAt(0)}{userInfo.lastName?.charAt(0) || ''}
          </Avatar>

          <Box sx={{ flexGrow: 1, textAlign: 'center' }}>
            <Typography variant="h6" component="h2" sx={{ fontWeight: 'bold', mb: 0.5, fontSize: '1.1rem' }}>
              {displayName}
            </Typography>

            {userInfo.username && (
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1, fontSize: '0.8rem' }}>
                @{userInfo.username}
              </Typography>
            )}

            <Chip 
              label={userInfo.role}
              size="small"
              variant="outlined"
              color="primary"
              sx={{ fontSize: '0.7rem', height: 20 }}
            />
          </Box>
        </Box>

        {/* Действия */}
        <Box sx={{ mt: 2, pt: 2, borderTop: '1px solid', borderColor: 'divider' }}>
          <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
            {onShareProfile && (
              <Button
                variant="outlined"
                size="small"
                startIcon={<ShareIcon />}
                onClick={onShareProfile}
                sx={{ flex: 1, fontSize: '0.7rem' }}
              >
                Поделиться
              </Button>
            )}
            
            {onMessageUser && (
              <Button
                variant="contained"
                size="small"
                startIcon={<MessageIcon />}
                onClick={onMessageUser}
                sx={{ flex: 1, fontSize: '0.7rem' }}
              >
                Написать
              </Button>
            )}
          </Box>
        </Box>

        {/* Telegram ID */}
        <Box sx={{ mt: 2, pt: 2, borderTop: '1px solid', borderColor: 'divider' }}>
          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '0.7rem', textAlign: 'center', display: 'block' }}>
            Telegram ID: {userInfo.telegramId}
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
};
