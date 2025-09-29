import React from 'react';
import { 
  Card, 
  CardContent, 
  Typography, 
  Box, 
  Button, 
  Chip,
  useTheme,
  useMediaQuery
} from '@mui/material';
import { StickerSetResponse } from '@/types/sticker';
import { StickerPreview } from './StickerPreview';

interface StickerCardProps {
  stickerSet: StickerSetResponse;
  onView: (id: number, name: string) => void;
  onShare: (name: string, title: string) => void;
  onDelete: (id: number, title: string) => void;
  isInTelegramApp?: boolean;
}

export const StickerCard: React.FC<StickerCardProps> = ({
  stickerSet,
  onView,
  onShare,
  onDelete,
  isInTelegramApp = false
}) => {
  const theme = useTheme();
  const isSmallScreen = useMediaQuery(theme.breakpoints.down('sm'));
  
  const getStickerCount = () => {
    return stickerSet.telegramStickerSetInfo?.stickers?.length || 0;
  };

  const getPreviewStickers = () => {
    const stickers = stickerSet.telegramStickerSetInfo?.stickers || [];
    return stickers.slice(0, 4);
  };

  const handleView = () => {
    onView(stickerSet.id, stickerSet.name);
  };

  const handleShare = () => {
    onShare(stickerSet.name, stickerSet.title);
  };

  const handleDelete = () => {
    onDelete(stickerSet.id, stickerSet.title);
  };

  const previewStickers = getPreviewStickers();
  const stickerCount = getStickerCount();

  // Адаптивные настройки
  const cardPadding = isSmallScreen ? 1 : 1.5; // 8px на маленьких, 12px на больших
  const buttonHeight = isSmallScreen ? 32 : 36;
  const titleVariant = isSmallScreen ? 'subtitle1' : 'h6';

  return (
    <Card 
      sx={{ 
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        borderRadius: 3, // 12px скругление
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        transition: 'box-shadow 0.2s ease',
        '&:hover': {
          boxShadow: '0 4px 16px rgba(0,0,0,0.15)',
        }
      }}
    >
      <CardContent 
        sx={{ 
          p: cardPadding,
          '&:last-child': { pb: cardPadding },
          display: 'flex',
          flexDirection: 'column',
          flexGrow: 1
        }}
      >
        {/* Заголовок */}
        <Box 
          sx={{ 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'flex-start',
            mb: isSmallScreen ? 1 : 1.5,
            minHeight: isSmallScreen ? 40 : 48
          }}
        >
          <Typography 
            variant={titleVariant} 
            component="h3"
            sx={{ 
              fontSize: isSmallScreen ? '0.9rem' : '1.25rem',
              lineHeight: 1.2,
              flexGrow: 1,
              mr: 1
            }}
          >
            {stickerSet.title}
          </Typography>
          <Chip 
            label={`${stickerCount}`}
            size="small"
            variant="outlined"
            sx={{ 
              fontSize: isSmallScreen ? '0.7rem' : '0.75rem',
              height: isSmallScreen ? 20 : 24
            }}
          />
        </Box>

        {/* Превью стикеров - CSS Grid 2x2 */}
        <Box 
          sx={{
            display: 'grid',
            gridTemplateColumns: 'repeat(2, 1fr)',
            gap: 0.5, // 4px gap
            mb: isSmallScreen ? 1 : 1.5,
            aspectRatio: '1 / 1'
          }}
        >
          {previewStickers.map((sticker) => {
            return (
              <Box
                key={sticker.file_id}
                sx={{
                  aspectRatio: '1 / 1',
                  overflow: 'hidden',
                  borderRadius: 1
                }}
              >
                <StickerPreview 
                  sticker={sticker} 
                  size="responsive"
                  showBadge={false}
                  isInTelegramApp={isInTelegramApp}
                />
              </Box>
            );
          })}
          {/* Заполняем пустые ячейки если стикеров меньше 4 */}
          {Array.from({ length: Math.max(0, 4 - previewStickers.length) }).map((_, index) => (
            <Box
              key={`empty-${index}`}
              sx={{
                aspectRatio: '1 / 1',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                backgroundColor: 'background.paper',
                borderRadius: 1,
                border: '1px dashed',
                borderColor: 'divider'
              }}
            >
              <Typography 
                color="text.secondary"
                sx={{ fontSize: isSmallScreen ? '1rem' : '1.2rem' }}
              >
                ➕
              </Typography>
            </Box>
          ))}
        </Box>

        {/* Информация о дате создания */}
        <Typography 
          variant="caption" 
          color="text.secondary" 
          sx={{ 
            mb: isSmallScreen ? 1 : 1.5,
            fontSize: isSmallScreen ? '0.7rem' : '0.75rem'
          }}
        >
          {new Date(stickerSet.createdAt).toLocaleDateString()}
        </Typography>

        {/* Действия - растягиваем до конца карточки */}
        <Box sx={{ mt: 'auto' }}>
          <Box 
            sx={{ 
              display: 'flex', 
              gap: 0.5,
              justifyContent: 'center',
              width: '90%',
              mx: 'auto'
            }}
          >
            <Button
              variant="contained"
              size="small"
              onClick={handleView}
              sx={{ 
                flex: 1,
                height: buttonHeight,
                fontSize: isSmallScreen ? '0.7rem' : '0.75rem',
                minWidth: 0,
                px: 0.5
              }}
            >
              {isSmallScreen ? '👁️' : '📱 Просмотр'}
            </Button>
            <Button
              variant="outlined"
              size="small"
              onClick={handleShare}
              sx={{ 
                flex: 1,
                height: buttonHeight,
                fontSize: isSmallScreen ? '0.7rem' : '0.75rem',
                minWidth: 0,
                px: 0.5
              }}
            >
              {isSmallScreen ? '📤' : '📤 Поделиться'}
            </Button>
            <Button
              variant="outlined"
              color="error"
              size="small"
              onClick={handleDelete}
              sx={{ 
                flex: 1,
                height: buttonHeight,
                fontSize: isSmallScreen ? '0.7rem' : '0.75rem',
                minWidth: 0,
                px: 0.5
              }}
            >
              {isSmallScreen ? '🗑️' : '🗑️ Удалить'}
            </Button>
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
};
