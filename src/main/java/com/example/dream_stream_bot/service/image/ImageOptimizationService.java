package com.example.dream_stream_bot.service.image;

import com.pngencoder.PngEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Сервис для оптимизации изображений для стикеров Telegram
 */
@Service
public class ImageOptimizationService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageOptimizationService.class);
    
    private static final int TARGET_SIZE = 512;
    private static final int MAX_FILE_SIZE = 512 * 1024; // 512KB
    private static final String OUTPUT_FORMAT = "png"; // Стабильное PNG решение
    

    
    /**
     * Оптимизирует изображение для использования в качестве стикера
     * Масштабирует до 512 пикселей по большей стороне, сохраняя пропорции
     * @param imagePath Путь к исходному изображению
     * @return Путь к оптимизированному изображению
     */
    public Path optimizeImageForSticker(Path imagePath) throws IOException {
        LOGGER.info("🔧 Начинаем оптимизацию изображения: {}", imagePath.getFileName());
        
        // 1. Загружаем изображение
        BufferedImage originalImage = ImageIO.read(imagePath.toFile());
        if (originalImage == null) {
            throw new IOException("Не удалось загрузить изображение: " + imagePath);
        }

        // 2. Получаем размеры исходного изображения
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        LOGGER.info("📏 Исходные размеры: {}x{}", originalWidth, originalHeight);

        // 3. Масштабируем изображение пропорционально, чтобы наибольшая сторона была 512px
        double scaleFactor = Math.min((double) TARGET_SIZE / originalWidth, (double) TARGET_SIZE / originalHeight);
        int newWidth = (int) Math.round(originalWidth * scaleFactor);
        int newHeight = (int) Math.round(originalHeight * scaleFactor);
        
        LOGGER.info("🔄 Масштабируем до: {}x{} (scale factor: {:.3f})", newWidth, newHeight, scaleFactor);
        
        Image scaledImg = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gResized = resized.createGraphics();
        gResized.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        gResized.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        gResized.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gResized.drawImage(scaledImg, 0, 0, null);
        gResized.dispose();

        // 4. Сжимаем PNG с максимальной оптимизацией (используем resized вместо square)
        byte[] optimizedImageData = encodePNGWithMaxCompression(resized);
        
        LOGGER.info("✅ Изображение оптимизировано: {} bytes | формат: PNG", optimizedImageData.length);
        
        Path outputPath = Files.createTempFile("sticker_optimized_", ".png");
        Files.write(outputPath, optimizedImageData);

        return outputPath;
    }

    /**
     * Кодирует изображение как PNG с максимальным сжатием используя PngEncoder
     */
    private byte[] encodePNGWithMaxCompression(BufferedImage image) throws IOException {
        // Пробуем самое быстрое сжатие с максимальным качеством
        byte[] bestCompression = encodeWithPngEncoder(image, 9, true, true);
        if (bestCompression.length <= MAX_FILE_SIZE) {
            LOGGER.info("✅ PngEncoder максимальное сжатие: {} bytes", bestCompression.length);
            return bestCompression;
        }

        // Если не помещается, пробуем без predictor encoding
        LOGGER.warn("⚠️ Максимальное сжатие дало {} bytes, убираем predictor encoding", bestCompression.length);
        byte[] withoutPredictor = encodeWithPngEncoder(image, 9, false, true);
        if (withoutPredictor.length <= MAX_FILE_SIZE) {
            LOGGER.info("✅ PngEncoder без predictor: {} bytes", withoutPredictor.length);
            return withoutPredictor;
        }

        // Если всё ещё велико, пробуем снизить уровень сжатия
        LOGGER.warn("⚠️ Без predictor {} bytes, снижаем уровень сжатия", withoutPredictor.length);
        byte[] lowerCompression = encodeWithPngEncoder(image, 6, false, true);
        if (lowerCompression.length <= MAX_FILE_SIZE) {
            LOGGER.info("✅ PngEncoder уровень 6: {} bytes", lowerCompression.length);
            return lowerCompression;
        }

        // Последняя попытка - убираем индексацию и минимальное сжатие
        LOGGER.warn("⚠️ Уровень 6 дал {} bytes, минимальное сжатие", lowerCompression.length);
        byte[] minimal = encodeWithPngEncoder(image, 1, false, false);
        
        if (minimal.length > MAX_FILE_SIZE) {
            LOGGER.error("❌ Даже минимальное сжатие превышает лимит: {} bytes", minimal.length);
            throw new IOException("Не удалось сжать изображение до требуемого размера " + MAX_FILE_SIZE + " bytes");
        }

        LOGGER.info("✅ PngEncoder минимальное сжатие: {} bytes", minimal.length);
        return minimal;
    }

    /**
     * Кодирует изображение с помощью PngEncoder с заданными параметрами
     */
    private byte[] encodeWithPngEncoder(BufferedImage image, int compressionLevel, 
                                       boolean withPredictor, boolean tryIndexed) throws IOException {
        return new PngEncoder()
                .withBufferedImage(image)
                .withCompressionLevel(compressionLevel)
                .withPredictorEncoding(withPredictor)
                .withTryIndexedEncoding(tryIndexed)
                .toBytes();
    }
}