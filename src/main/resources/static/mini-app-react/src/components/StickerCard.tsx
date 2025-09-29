import React from 'react';
import { 
  Card, 
  CardContent, 
  Typography, 
  Box, 
  Button, 
  Chip,
  Grid 
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

  return (
    <Card sx={{ mb: 2 }}>
      <CardContent>
        {/* Заголовок */}
        <Box 
          sx={{ 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'center',
            mb: 2 
          }}
        >
          <Typography variant="h6" component="h3">
            {stickerSet.title}
          </Typography>
          <Chip 
            label={`${stickerCount} стикеров`}
            size="small"
            variant="outlined"
          />
        </Box>

        {/* Превью стикеров */}
        <Grid container spacing={1} sx={{ mb: 2 }}>
          {previewStickers.map((sticker) => {
            console.log('🔍 StickerCard рендер превью:', {
              stickerId: sticker.file_id,
              isInTelegramApp,
              size: 'auto'
            });
            return (
              <Grid item xs={6} key={sticker.file_id}>
                <StickerPreview 
                  sticker={sticker} 
                  size="auto"
                  showBadge={false}
                  isInTelegramApp={isInTelegramApp}
                />
              </Grid>
            );
          })}
          {/* Заполняем пустые ячейки если стикеров меньше 4 */}
          {Array.from({ length: Math.max(0, 4 - previewStickers.length) }).map((_, index) => (
            <Grid item xs={6} key={`empty-${index}`}>
              <Box
                sx={{
                  width: 60,
                  height: 60,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  backgroundColor: 'background.paper',
                  borderRadius: 2,
                  border: '1px dashed',
                  borderColor: 'divider'
                }}
              >
                <Typography color="text.secondary">➕</Typography>
              </Box>
            </Grid>
          ))}
        </Grid>

        {/* Информация о дате создания */}
        <Typography 
          variant="body2" 
          color="text.secondary" 
          sx={{ mb: 2 }}
        >
          Создан: {new Date(stickerSet.createdAt).toLocaleDateString()}
        </Typography>

        {/* Действия */}
        <Box 
          sx={{ 
            display: 'flex', 
            gap: 1, 
            flexWrap: 'wrap' 
          }}
        >
          <Button
            variant="contained"
            size="small"
            onClick={handleView}
            sx={{ flex: 1, minWidth: 80 }}
          >
            📱 Просмотр
          </Button>
          <Button
            variant="outlined"
            size="small"
            onClick={handleShare}
            sx={{ flex: 1, minWidth: 80 }}
          >
            📤 Поделиться
          </Button>
          <Button
            variant="outlined"
            color="error"
            size="small"
            onClick={handleDelete}
            sx={{ flex: 1, minWidth: 80 }}
          >
            🗑️ Удалить
          </Button>
        </Box>
      </CardContent>
    </Card>
  );
};
