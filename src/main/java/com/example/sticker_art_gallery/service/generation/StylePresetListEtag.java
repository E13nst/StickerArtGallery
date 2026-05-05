package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.StylePresetDto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/**
 * Слабый ETag для кэширования лёгких списков пресетов (browse / только метаданные).
 */
public final class StylePresetListEtag {

    private StylePresetListEtag() {
    }

    /**
     * Значение для {@link org.springframework.http.ResponseEntity#eTag(String)} —
     * только hex без префикса {@code W/}; Spring экранирует кавычки сам.
     */
    public static String weakHexDigest(List<StylePresetDto> presets) {
        String payload = presets.stream()
                .sorted(Comparator.comparing(StylePresetDto::getId, Comparator.nullsLast(Long::compareTo)))
                .map(p -> p.getId() + ":" + (p.getUpdatedAt() == null ? "-" : p.getUpdatedAt().toInstant().toEpochMilli()))
                .reduce((a, b) -> a + "|" + b)
                .orElse("");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
