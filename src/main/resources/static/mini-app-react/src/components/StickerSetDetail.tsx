import React from 'react';
import { 
  Box, 
  Typography, 
  Button, 
  IconButton,
  Card,
  CardContent
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import ShareIcon from '@mui/icons-material/Share';
import DeleteIcon from '@mui/icons-material/Delete';
import { StickerSetResponse } from '@/types/sticker';
import { StickerGrid } from './StickerGrid';

interface StickerSetDetailProps {
  stickerSet: StickerSetResponse;
  onBack: () => void;
  onShare: (name: string, title: string) => void;
  onDelete: (id: number, title: string) => void;
  isInTelegramApp?: boolean;
}

export const StickerSetDetail: React.FC<StickerSetDetailProps> = ({
  stickerSet,
  onBack,
  onShare,
  onDelete,
  isInTelegramApp = false
}) => {
  const stickerCount = stickerSet.telegramStickerSetInfo?.stickers?.length || 0;
  const createdDate = new Date(stickerSet.createdAt).toLocaleDateString();

  const handleShare = () => {
    onShare(stickerSet.name, stickerSet.title);
  };

  const handleDelete = () => {
    onDelete(stickerSet.id, stickerSet.title);
  };

  return (
    <Box sx={{ pb: 2 }}>
      {/* Заголовок с кнопкой назад */}
      <Box sx={{ 
        display: 'flex', 
        alignItems: 'center', 
        mb: 3,
        gap: 2
      }}>
        <IconButton 
          onClick={onBack}
          sx={{ 
            backgroundColor: 'primary.main',
            color: 'white',
            '&:hover': {
              backgroundColor: 'primary.dark',
            }
          }}
        >
          <ArrowBackIcon />
        </IconButton>
        
        <Box sx={{ flexGrow: 1 }}>
          <Typography variant="h4" component="h1" gutterBottom>
            {stickerSet.title}
          </Typography>
          <Typography variant="body1" color="text.secondary">
            {stickerCount} стикер{stickerCount === 1 ? '' : stickerCount < 5 ? 'а' : 'ов'}
          </Typography>
        </Box>
      </Box>

      {/* Информационная карточка */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ 
            display: 'flex', 
            justifyContent: 'space-between',
            alignItems: 'center',
            mb: 2
          }}>
            <Typography variant="h6" component="h2">
              О наборе стикеров
            </Typography>
          </Box>
          
          <Box sx={{ mb: 2 }}>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              <strong>Название:</strong> {stickerSet.name}
            </Typography>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              <strong>Создан:</strong> {createdDate}
            </Typography>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              <strong>Количество стикеров:</strong> {stickerCount}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              <strong>Тип:</strong> {
                stickerSet.telegramStickerSetInfo?.is_animated ? 'Анимированные' : 
                stickerSet.telegramStickerSetInfo?.is_video ? 'Видео' :
                stickerSet.telegramStickerSetInfo?.contains_masks ? 'Маски' : 'Обычные'
              }
            </Typography>
          </Box>

          {/* Кнопки действий */}
          <Box sx={{ 
            display: 'flex', 
            gap: 2, 
            flexWrap: 'wrap',
            justifyContent: 'center'
          }}>
            <Button
              variant="contained"
              startIcon={<ShareIcon />}
              onClick={handleShare}
              sx={{ 
                flex: 1, 
                minWidth: 140,
                backgroundColor: 'primary.main',
                '&:hover': {
                  backgroundColor: 'primary.dark',
                }
              }}
            >
              Поделиться
            </Button>
            <Button
              variant="outlined"
              color="error"
              startIcon={<DeleteIcon />}
              onClick={handleDelete}
              sx={{ 
                flex: 1, 
                minWidth: 140,
                borderColor: 'error.main',
                color: 'error.main',
                '&:hover': {
                  backgroundColor: 'error.light',
                  borderColor: 'error.dark',
                  color: 'error.dark',
                }
              }}
            >
              Удалить
            </Button>
          </Box>
        </CardContent>
      </Card>

      {/* Сетка стикеров */}
      <Box>
        <Typography variant="h6" component="h2" gutterBottom sx={{ mb: 2 }}>
          Стикеры в наборе
        </Typography>
        
        {stickerCount > 0 ? (
          <StickerGrid 
            stickers={stickerSet.telegramStickerSetInfo?.stickers || []}
            isInTelegramApp={isInTelegramApp}
          />
        ) : (
          <Card>
            <CardContent>
              <Typography 
                variant="body1" 
                color="text.secondary" 
                textAlign="center"
                sx={{ py: 4 }}
              >
                🎨 В этом наборе пока нет стикеров
              </Typography>
            </CardContent>
          </Card>
        )}
      </Box>
    </Box>
  );
};
