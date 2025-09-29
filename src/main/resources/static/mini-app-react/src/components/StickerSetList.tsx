import React from 'react';
import { Grid, Box, useTheme, useMediaQuery } from '@mui/material';
import { StickerSetResponse } from '@/types/sticker';
import { StickerCard } from './StickerCard';

interface StickerSetListProps {
  stickerSets: StickerSetResponse[];
  onView: (id: number, name: string) => void;
  onShare: (name: string, title: string) => void;
  onDelete: (id: number, title: string) => void;
  isInTelegramApp?: boolean;
}

export const StickerSetList: React.FC<StickerSetListProps> = ({
  stickerSets,
  onView,
  onShare,
  onDelete,
  isInTelegramApp = false
}) => {
  const theme = useTheme();
  const isSmallScreen = useMediaQuery(theme.breakpoints.down('sm'));

  // Адаптивные настройки спейсинга
  const getSpacing = () => {
    return isSmallScreen ? 1 : 2; // 8px на маленьких экранах, 16px на больших
  };

  console.log('🔍 StickerSetList рендер:', {
    stickerSetsCount: stickerSets.length,
    isInTelegramApp,
    isSmallScreen,
    spacing: getSpacing()
  });

  return (
    <Box sx={{ 
      pb: isInTelegramApp ? 2 : 10 // Добавляем отступ для Bottom Navigation
    }}>
      <Grid container spacing={getSpacing()}>
        {stickerSets.map((stickerSet) => {
          console.log('🔍 StickerSetList рендер карточки:', {
            stickerSetId: stickerSet.id,
            stickerSetTitle: stickerSet.title,
            isInTelegramApp
          });
          
          return (
            <Grid 
              item 
              xs={6}    // всегда минимум 2 карточки (50% каждая)
              sm={6}    // до 900px - 2 карточки 
              md={4}    // 900px+ - 3 карточки
              lg={3}    // 1200px+ - 4 карточки
              key={stickerSet.id}
            >
              <StickerCard
                stickerSet={stickerSet}
                onView={onView}
                onShare={onShare}
                onDelete={onDelete}
                isInTelegramApp={isInTelegramApp}
              />
            </Grid>
          );
        })}
      </Grid>
    </Box>
  );
};
