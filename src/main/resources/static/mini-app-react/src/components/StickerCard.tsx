import React, { memo, useCallback } from 'react';
import { 
  Card, 
  CardContent, 
  Typography, 
  Box, 
  Chip
} from '@mui/material';
import { StickerSetResponse } from '@/types/sticker';
import { StickerPreview } from './StickerPreview';

interface StickerCardProps {
  stickerSet: StickerSetResponse;
  onView: (id: number, name: string) => void;
  isInTelegramApp?: boolean;
}

const StickerCardComponent: React.FC<StickerCardProps> = ({
  stickerSet,
  onView,
  isInTelegramApp = false
}) => {
  const handleCardClick = useCallback(() => {
    onView(stickerSet.id, stickerSet.name);
  }, [onView, stickerSet.id, stickerSet.name]);

  const stickers = stickerSet.telegramStickerSetInfo?.stickers || [];
  const previewStickers = stickers.slice(0, 4);
  const stickerCount = stickers.length;

  return (
    <Card 
      onClick={handleCardClick}
      sx={{ 
        height: '100%',
        minHeight: 280,
        width: '100%',
        display: 'flex',
        flexDirection: 'column',
        borderRadius: 3,
        boxShadow: 1,
        transition: 'all 0.3s ease',
        cursor: 'pointer',
        '&:hover': {
          boxShadow: 2,
          transform: 'translateY(-1px)'
        }
      }}
    >
      <CardContent 
        sx={{ 
          p: 1.5,
          '&:last-child': { pb: 1.5 },
          display: 'flex',
          flexDirection: 'column',
          flexGrow: 1,
          height: '100%'
        }}
      >
        {/* Заголовок */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1.5, minHeight: 40 }}>
          <Typography variant="h6" component="h3" sx={{ fontSize: '1.1rem', lineHeight: 1.2, flexGrow: 1, mr: 1, fontWeight: 700 }}>
            {stickerSet.title}
          </Typography>
          <Chip 
            label={`${stickerCount}`}
            size="small"
            variant="outlined"
            sx={{ fontSize: '0.8rem', height: 24, fontWeight: 600, color: 'primary.main', borderColor: 'primary.main' }}
          />
        </Box>

        {/* Превью стикеров 2x2 */}
        <Box sx={{
          display: 'grid',
          gridTemplateColumns: 'repeat(2, 1fr)',
          gap: 1,
          aspectRatio: '1 / 1',
          minHeight: 180,
          flexGrow: 1,
          alignSelf: 'center',
          p: 0.5
        }}>
          {previewStickers.map((sticker, index) => (
            <Box
              key={sticker.file_id}
              sx={{
                aspectRatio: '1 / 1',
                overflow: 'hidden',
                borderRadius: 1.5,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                backgroundColor: 'background.paper',
                border: '1px solid',
                borderColor: 'divider',
                minHeight: 80,
                minWidth: 80,
                position: 'relative'
              }}
            >
              <StickerPreview 
                sticker={sticker} 
                size="medium"
                showBadge={index === 0}
                isInTelegramApp={isInTelegramApp}
              />
            </Box>
          ))}
          {/* Пустые ячейки */}
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
              <Typography color="text.secondary" sx={{ fontSize: '1.2rem' }}>➕</Typography>
            </Box>
          ))}
        </Box>

        {/* Дата создания */}
        <Box sx={{ mt: 'auto', pt: 1, alignSelf: 'flex-end', width: '100%' }}>
          <Typography 
            variant="caption" 
            color="text.secondary" 
            sx={{ fontSize: '0.7rem', fontWeight: 400, display: 'block', textAlign: 'center', opacity: 0.8 }}
          >
            {new Date(stickerSet.createdAt).toLocaleDateString()}
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
};

// Мемоизируем компонент для предотвращения лишних ре-рендеров
export const StickerCard = memo(StickerCardComponent);
