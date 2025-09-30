import React, { useMemo, useCallback } from 'react';
import { Grid, Box, useTheme, useMediaQuery } from '@mui/material';
import { StickerSetResponse } from '@/types/sticker';
import { StickerCard } from './StickerCard';

interface StickerSetListProps {
  stickerSets: StickerSetResponse[];
  onView: (id: number, name: string) => void;
  isInTelegramApp?: boolean;
}

export const StickerSetList: React.FC<StickerSetListProps> = ({
  stickerSets,
  onView,
  isInTelegramApp = false
}) => {
  const theme = useTheme();
  const isSmallScreen = useMediaQuery(theme.breakpoints.down('sm'));

  // Мемоизируем обработчики для предотвращения лишних ре-рендеров
  const handleView = useCallback((id: number, name: string) => {
    onView(id, name);
  }, [onView]);

  // Адаптивные настройки спейсинга - мемоизируем
  const spacing = useMemo(() => {
    return isSmallScreen ? 2 : 3; // 16px на мобилках, 24px на десктопе
  }, [isSmallScreen]);

  // Ограничиваем количество отображаемых элементов для производительности
  const maxVisibleItems = 20; // Показываем максимум 20 элементов за раз
  const visibleStickerSets = useMemo(() => {
    return stickerSets.slice(0, maxVisibleItems);
  }, [stickerSets]);

  console.log('🔍 StickerSetList рендер:', {
    stickerSetsCount: stickerSets.length,
    visibleCount: visibleStickerSets.length,
    isInTelegramApp,
    isSmallScreen,
    spacing
  });

  return (
    <Box sx={{ 
      pb: isInTelegramApp ? 2 : 10, // Добавляем отступ для Bottom Navigation
      px: { xs: 0, md: 2 }, // Убираем горизонтальные отступы на мобильных, добавляем на desktop
      py: { xs: 2, md: 3 }, // Адаптивные вертикальные отступы
      // Desktop стили для полноценного web-интерфейса
      ...(isSmallScreen ? {} : {
        backgroundColor: 'background.default',
        borderRadius: 2,
        boxShadow: '0 2px 8px rgba(0,0,0,0.05)',
        border: '1px solid',
        borderColor: 'divider'
      })
    }}>
      <Grid container spacing={{ xs: 2, md: 3 }}> {/* Адаптивные отступы между карточками */}
        {visibleStickerSets.map((stickerSet) => {
          console.log('🔍 StickerSetList рендер карточки:', {
            stickerSetId: stickerSet.id,
            stickerSetTitle: stickerSet.title,
            isInTelegramApp
          });
          
          return (
            <Grid 
              item 
              xs={6}     // xs: 2 карточки (50% каждая) - минимум 2 в ряд
              sm={4}     // sm: 3 карточки (33% каждая)
              md={3}     // md: 4 карточки (25% каждая)
              lg={2.4}   // lg: 5 карточек (20% каждая)
              xl={2.4}   // xl: 5 карточек (20% каждая)
              key={stickerSet.id}
              sx={{
                display: 'flex',
                justifyContent: 'center',
                minHeight: 280 // Минимальная высота для консистентности
              }}
            >
              <StickerCard
                stickerSet={stickerSet}
                onView={handleView}
                isInTelegramApp={isInTelegramApp}
              />
            </Grid>
          );
        })}
      </Grid>
      
      {/* Показываем индикатор, если есть скрытые элементы */}
      {stickerSets.length > maxVisibleItems && (
        <Box sx={{ 
          textAlign: 'center', 
          py: 2,
          color: 'text.secondary'
        }}>
          <Box component="span" sx={{ fontSize: '0.875rem' }}>
            Показано {maxVisibleItems} из {stickerSets.length} наборов
          </Box>
        </Box>
      )}
    </Box>
  );
};
