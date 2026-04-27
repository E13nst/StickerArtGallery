package com.example.sticker_art_gallery.service.generation;

import java.util.Optional;
import java.util.UUID;

/**
 * Стабильный synthetic image id для референса пресета, хранящегося в {@code cached_images}.
 * Формат совместим с валидацией {@code img_...} в generation v2.
 */
public final class StylePresetReferenceImageId {

    private static final String PREFIX = "img_sagref_";

    private StylePresetReferenceImageId() {
    }

    public static String fromCachedImageId(UUID cachedImageId) {
        return PREFIX + cachedImageId.toString().replace("-", "");
    }

    /**
     * @return UUID кэшированного изображения, если строка — наш synthetic id
     */
    public static Optional<UUID> parseCachedImageId(String imageId) {
        if (imageId == null || !imageId.startsWith(PREFIX)) {
            return Optional.empty();
        }
        String hex = imageId.substring(PREFIX.length());
        if (hex.length() != 32) {
            return Optional.empty();
        }
        try {
            String uuidStr = hex.substring(0, 8)
                    + "-" + hex.substring(8, 12)
                    + "-" + hex.substring(12, 16)
                    + "-" + hex.substring(16, 20)
                    + "-" + hex.substring(20, 32);
            return Optional.of(UUID.fromString(uuidStr));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
