import React from 'react';
import { Box, Grid, Typography } from '@mui/material';
import { Sticker } from '@/types/sticker';
import { StickerPreview } from './StickerPreview';

interface StickerGridProps {
  stickers: Sticker[];
  onStickerClick?: (sticker: Sticker) => void;
  isInTelegramApp?: boolean;
}

export const StickerGrid: React.FC<StickerGridProps> = ({ 
  stickers, 
  onStickerClick,
  isInTelegramApp = false
}) => {
  if (!stickers || stickers.length === 0) {
    return (
      <Box 
        sx={{ 
          textAlign: 'center', 
          py: 4,
          color: 'text.secondary' 
        }}
      >
        <Typography variant="h6">Стикеры не найдены</Typography>
      </Box>
    );
  }

  // Адаптивная сетка в зависимости от платформы
  const getGridColumns = () => {
    if (isInTelegramApp) {
      return { xs: 6, sm: 4, md: 3 }; // Компактнее в Telegram
    } else {
      return { xs: 4, sm: 3, md: 2 }; // Крупнее в браузере
    }
  };

  return (
    <Grid container spacing={2}>
      {stickers.map((sticker) => (
        <Grid item {...getGridColumns()} key={sticker.file_id}>
          <Box
            onClick={() => onStickerClick?.(sticker)}
            sx={{
              cursor: onStickerClick ? 'pointer' : 'default',
              transition: 'transform 0.2s ease',
              '&:hover': onStickerClick ? {
                transform: 'scale(1.05)'
              } : {}
            }}
          >
            <StickerPreview 
              sticker={sticker} 
              size="auto"
              showBadge={true}
              isInTelegramApp={isInTelegramApp}
            />
          </Box>
        </Grid>
      ))}
    </Grid>
  );
};
