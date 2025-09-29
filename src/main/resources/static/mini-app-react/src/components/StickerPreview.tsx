import React, { useEffect, useRef, useState } from 'react';
import { Box, Typography } from '@mui/material';
import Lottie from 'lottie-react';
import { Sticker } from '@/types/sticker';

interface StickerPreviewProps {
  sticker: Sticker;
  size?: 'small' | 'medium' | 'large' | 'auto' | 'responsive';
  showBadge?: boolean;
  isInTelegramApp?: boolean;
}

export const StickerPreview: React.FC<StickerPreviewProps> = ({ 
  sticker, 
  size = 'medium',
  showBadge = true,
  isInTelegramApp = false
}) => {
  const [isLoaded, setIsLoaded] = useState(false);
  const [error, setError] = useState(false);
  const [animationData, setAnimationData] = useState<any>(null);
  const lottieRef = useRef<any>(null);

  const sizeMap = {
    small: { width: 60, height: 60, fontSize: 16 },
    medium: { width: 120, height: 120, fontSize: 24 },
    large: { width: 200, height: 200, fontSize: 32 }
  };

  // Адаптивные размеры в зависимости от платформы
  const getAdaptiveSize = () => {
    if (size === 'responsive') {
      // Responsive - заполняет весь контейнер
      return { width: '100%', height: '100%', fontSize: 16 };
    }
    
    if (size === 'auto') {
      // В Telegram - компактнее, в браузере - крупнее
      if (isInTelegramApp) {
        console.log('🔍 StickerPreview: Telegram режим, размер medium (120x120)');
        return sizeMap.medium; // 120x120 в Telegram
      } else {
        console.log('🔍 StickerPreview: Браузер режим, размер large (200x200)');
        return sizeMap.large; // 200x200 в браузере
      }
    }
    return sizeMap[size] || sizeMap.medium;
  };

  const currentSize = getAdaptiveSize();
  
  console.log('🔍 StickerPreview рендер:', {
    size,
    isInTelegramApp,
    currentSize,
    stickerId: sticker.file_id
  });

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
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      
      const data = await response.json();
      setAnimationData(data);
      setIsLoaded(true);
    } catch (error) {
      console.error('❌ Ошибка загрузки Lottie анимации:', error);
      setError(true);
      setIsLoaded(true);
    }
  };

  const handleImageError = () => {
    console.error('❌ Ошибка загрузки изображения:', `/api/stickers/${sticker.file_id}`);
    setError(true);
  };

  const handleImageLoad = () => {
    setIsLoaded(true);
  };

  if (error) {
    return (
      <Box
        sx={{
          width: currentSize.width,
          height: currentSize.height,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: 'background.paper',
          borderRadius: 2,
          border: '1px solid',
          borderColor: 'divider'
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
        backgroundColor: 'background.paper',
        borderRadius: 2,
        border: '1px solid',
        borderColor: 'divider',
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

      {/* Обычный стикер */}
      {!sticker.is_animated && isLoaded && (
        <img
          src={`/api/stickers/${sticker.file_id}`}
          alt={sticker.emoji || 'sticker'}
          loading="lazy"
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'contain',
            display: isLoaded ? 'block' : 'none'
          }}
          onError={handleImageError}
          onLoad={handleImageLoad}
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
            height: '100%'
          }}
          lottieRef={lottieRef}
        />
      )}

      {/* Бейдж для анимированных стикеров */}
      {sticker.is_animated && showBadge && (
        <Box
          sx={{
            position: 'absolute',
            top: 4,
            right: 4,
            backgroundColor: 'rgba(255, 165, 0, 0.9)',
            color: 'white',
            fontSize: 10,
            fontWeight: 'bold',
            padding: '2px 4px',
            borderRadius: 1,
            pointerEvents: 'none'
          }}
        >
          LOTTIE
        </Box>
      )}
    </Box>
  );
};
