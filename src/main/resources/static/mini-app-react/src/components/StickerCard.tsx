import React, { memo, useCallback } from 'react';
import { 
  Card, 
  CardContent, 
  Typography, 
  Box, 
  Chip,
  useTheme,
  useMediaQuery
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
  const theme = useTheme();
  const isSmallScreen = useMediaQuery(theme.breakpoints.down('sm'));

  // 🚀 20/80 ОПТИМИЗАЦИЯ: детекция медленного интернета
  const isSlowConnection = (navigator as any).connection?.effectiveType?.includes('2g') || false;
  
  const getStickerCount = useCallback(() => {
    return stickerSet.telegramStickerSetInfo?.stickers?.length || 0;
  }, [stickerSet.telegramStickerSetInfo?.stickers?.length]);

  const getPreviewStickers = useCallback(() => {
    const stickers = stickerSet.telegramStickerSetInfo?.stickers || [];
    return stickers.slice(0, isSlowConnection ? 2 : 4); // Меньше стикеров на медленном интернете
  }, [stickerSet.telegramStickerSetInfo?.stickers, isSlowConnection]);

  const handleCardClick = useCallback(() => {
    onView(stickerSet.id, stickerSet.name);
  }, [onView, stickerSet.id, stickerSet.name]);

  const previewStickers = getPreviewStickers();
  const stickerCount = getStickerCount();

  // Фиксированные настройки для одинакового отображения на всех экранах
  const titleVariant = 'h6'; // Фиксированный размер заголовка
  
  // Размеры стикеров для галереи карточек - адаптивные
  const previewSize = isSmallScreen ? 'small' : 'medium'; // Адаптивные размеры

  return (
    <Card 
      onClick={handleCardClick}
      sx={{ 
        height: '100%',
        minHeight: 280, // Увеличиваем минимальную высоту
        width: '100%',
        maxWidth: { xs: 280, md: 320 }, // Больше на desktop
        minWidth: 200, // Увеличиваем минимальную ширину
        display: 'flex',
        flexDirection: 'column',
        borderRadius: 3,
        boxShadow: { xs: 1, md: 2 }, // Адаптивные тени
        transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
        cursor: 'pointer',
        '&:hover': {
          boxShadow: { xs: 2, md: 4 },
          transform: { xs: 'translateY(-1px)', md: 'scale(1.02)' }, // Разные hover эффекты
          transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)'
        }
      }}
    >
      <CardContent 
        sx={{ 
          p: { xs: 1.5, md: 2 }, // Адаптивные отступы
          '&:last-child': { pb: { xs: 1.5, md: 2 } },
          display: 'flex',
          flexDirection: 'column',
          flexGrow: 1,
          height: '100%'
        }}
      >
        {/* Верхняя секция: Заголовок */}
        <Box>
          <Box 
            sx={{ 
              display: 'flex', 
              justifyContent: 'space-between', 
              alignItems: 'flex-start',
              mb: 1.5,
              minHeight: 40
            }}
          >
            <Typography 
              variant={titleVariant} 
              component="h3"
              sx={{ 
                fontSize: '1.1rem',
                lineHeight: 1.2,
                flexGrow: 1,
                mr: 1,
                fontWeight: 700 // Жирное название
              }}
            >
              {stickerSet.title}
            </Typography>
            <Chip 
              label={`${stickerCount}`}
              size="small"
              variant="outlined"
              sx={{ 
                fontSize: '0.8rem',
                height: 24,
                fontWeight: 600, // Четкий счетчик
                color: 'primary.main',
                borderColor: 'primary.main'
              }}
            />
          </Box>
        </Box>

        {/* Средняя секция: Превью стикеров 2x2 */}
        <Box 
          sx={{
            display: 'grid',
            gridTemplateColumns: 'repeat(2, 1fr)',
            gap: { xs: 1, md: 1.5 }, // Адаптивные отступы между стикерами
            aspectRatio: '1 / 1', // Квадратная сетка
            minHeight: { xs: 180, md: 200 }, // Больше на desktop
            flexGrow: 1,
            alignSelf: 'center',
            p: { xs: 0.5, md: 1 } // Внутренние отступы
          }}
        >
          {previewStickers.map((sticker, index) => {
            return (
              <Box
                key={sticker.file_id}
                sx={{
                  aspectRatio: '1 / 1', // Квадратные ячейки
                  overflow: 'hidden',
                  borderRadius: 1.5,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  backgroundColor: 'background.paper',
                  border: '1px solid',
                  borderColor: 'divider',
                  minHeight: { xs: 80, md: 100 }, // Адаптивные размеры ячеек
                  minWidth: { xs: 80, md: 100 }
                }}
              >
                <StickerPreview 
                  sticker={sticker} 
                  size={previewSize}
                  showBadge={index === 0} // Бейдж только на первом стикере
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

        {/* Нижняя секция: Дата создания (прижата к низу) */}
        <Box sx={{ 
          mt: 'auto', 
          pt: { xs: 1, md: 1.5 },
          alignSelf: 'flex-end',
          width: '100%'
        }}>
          <Typography 
            variant="caption" 
            color="text.secondary" 
            sx={{ 
              fontSize: { xs: '0.7rem', md: '0.75rem' },
              fontWeight: 400,
              display: 'block',
              textAlign: 'center',
              opacity: 0.8
            }}
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
