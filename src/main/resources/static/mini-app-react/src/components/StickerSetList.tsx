import React, { useMemo, useCallback } from 'react';
import { Box } from '@mui/material';
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
  const handleView = useCallback((id: number, name: string) => {
    onView(id, name);
  }, [onView]);

  const maxVisibleItems = 20;
  const visibleStickerSets = useMemo(() => {
    return stickerSets.slice(0, maxVisibleItems);
  }, [stickerSets]);

  return (
    <Box sx={{ pb: isInTelegramApp ? 2 : 10, px: 1, py: 2 }}>
      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 2 }}>
        {visibleStickerSets.map((stickerSet) => (
          <StickerCard
            key={stickerSet.id}
            stickerSet={stickerSet}
            onView={handleView}
            isInTelegramApp={isInTelegramApp}
          />
        ))}
      </Box>
      
      {stickerSets.length > maxVisibleItems && (
        <Box sx={{ textAlign: 'center', py: 2, color: 'text.secondary' }}>
          <Box component="span" sx={{ fontSize: '0.875rem' }}>
            Показано {maxVisibleItems} из {stickerSets.length} наборов
          </Box>
        </Box>
      )}
    </Box>
  );
};
