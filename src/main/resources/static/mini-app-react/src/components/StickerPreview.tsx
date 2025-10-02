import React, { useEffect, useRef, useState, memo, useCallback } from 'react';
import { Box, Typography } from '@mui/material';
import Lottie from 'lottie-react';
import { Sticker } from '@/types/sticker';
import { LazyImage } from './LazyImage';

interface StickerPreviewProps {
  sticker: Sticker;
  size?: 'small' | 'medium' | 'large' | 'auto' | 'responsive';
  showBadge?: boolean;
  isInTelegramApp?: boolean;
}

const StickerPreviewComponent: React.FC<StickerPreviewProps> = ({ 
  sticker, 
  size = 'medium',
  isInTelegramApp = false
}) => {
  const [isLoaded, setIsLoaded] = useState(false);
  const [error, setError] = useState(false);
  const [animationData, setAnimationData] = useState<any>(null);
  const lottieRef = useRef<any>(null);

  const sizeMap = {
    small: { width: 90, height: 90, fontSize: 20 },     // Галерея карточек: 90x90px (увеличено)
    medium: { width: 120, height: 120, fontSize: 24 },   // Планшеты: 120x120px
    large: { width: 160, height: 160, fontSize: 28 }      // Desktop: 160x160px
  };

  const getAdaptiveSize = () => {
    if (size === 'responsive') {
      return { width: '100%', height: '100%', fontSize: 16 };
    }
    if (size === 'auto') {
      return isInTelegramApp ? sizeMap.medium : sizeMap.large;
    }
    return sizeMap[size] || sizeMap.medium;
  };

  const currentSize = getAdaptiveSize();

  useEffect(() => {
    if (sticker.is_animated) {
      loadLottieAnimation();
    } else {
      setIsLoaded(true);
    }
  }, [sticker]);

  const loadLottieAnimation = async () => {
    try {
      const response = await fetch(`/api/stickers/${sticker.file_id}`);
      if (!response.ok) throw new Error();
      
      const data = await response.json();
      setAnimationData(data);
      setIsLoaded(true);
    } catch {
      setError(true);
      setIsLoaded(true);
    }
  };

  const handleImageError = useCallback(() => {
    setError(true);
  }, []);

  const handleImageLoad = useCallback(() => {
    setIsLoaded(true);
  }, []);

  if (error) {
    return (
      <Box
        sx={{
          width: currentSize.width,
          height: currentSize.height,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: 'transparent'
        }}
      >
        <Typography
          sx={{
            fontSize: currentSize.fontSize,
            color: 'text.secondary'
          }}
        >
          {sticker.emoji || '🎨'}
        </Typography>
      </Box>
    );
  }

  return (
    <Box
      sx={{
        position: 'relative',
        width: currentSize.width,
        height: currentSize.height,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'transparent',
        overflow: 'hidden'
      }}
    >
      {/* Placeholder */}
      {!isLoaded && (
        <Typography
          sx={{
            fontSize: currentSize.fontSize,
            color: 'text.secondary'
          }}
        >
          {sticker.emoji || '🎨'}
        </Typography>
      )}

      {/* Обычный стикер с ленивой загрузкой */}
      {!sticker.is_animated && (
        <LazyImage
          src={`/api/stickers/${sticker.file_id}`}
          alt={sticker.emoji || 'sticker'}
          onLoad={handleImageLoad}
          onError={handleImageError}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'contain',
            maxWidth: '100%',
            maxHeight: '100%'
          }}
          placeholder={
            <Typography
              sx={{
                fontSize: currentSize.fontSize,
                color: 'text.secondary'
              }}
            >
              {sticker.emoji || '🎨'}
            </Typography>
          }
          fallback={
            <Typography
              sx={{
                fontSize: currentSize.fontSize,
                color: 'text.secondary'
              }}
            >
              {sticker.emoji || '🎨'}
            </Typography>
          }
        />
      )}

      {/* Анимированный стикер */}
      {sticker.is_animated && animationData && (
        <Lottie
          animationData={animationData}
          loop={true}
          autoplay={true}
          style={{
            width: '100%',
            height: '100%',
            maxWidth: '100%',
            maxHeight: '100%',
            objectFit: 'contain'
          }}
          lottieRef={lottieRef}
        />
      )}

    </Box>
  );
};

// Мемоизируем компонент для предотвращения лишних ре-рендеров
export const StickerPreview = memo(StickerPreviewComponent);
