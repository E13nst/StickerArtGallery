package com.example.sticker_art_gallery.dto.generation;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.Set;

/**
 * Классифицирует набор ключей верхнего уровня JSON по отношению к camel/snake именам контрактных полей publish-user-style.
 */
public final class PublishUserStylePayloadKeyFlavorDetector {

    private static final Set<String> CAMEL_MARKERS = Set.of(
            "displayName",
            "categoryId",
            "sortOrder",
            "userStyleBlueprintCode",
            "idempotencyKey",
            "consentResultPublicShow");

    private static final Set<String> SNAKE_MARKERS = Set.of(
            "display_name",
            "category_id",
            "sort_order",
            "user_style_blueprint_code",
            "idempotency_key",
            "consent_result_public_show");

    private PublishUserStylePayloadKeyFlavorDetector() {
    }

    public static PublishUserStylePayloadKeyFlavor detect(JsonNode root) {
        if (root == null || !root.isObject()) {
            return PublishUserStylePayloadKeyFlavor.UNKNOWN;
        }
        boolean camel = false;
        boolean snake = false;
        Iterator<String> fields = root.fieldNames();
        while (fields.hasNext()) {
            String name = fields.next();
            if (CAMEL_MARKERS.contains(name)) {
                camel = true;
            }
            if (SNAKE_MARKERS.contains(name)) {
                snake = true;
            }
        }
        if (camel && snake) {
            return PublishUserStylePayloadKeyFlavor.MIXED;
        }
        if (snake) {
            return PublishUserStylePayloadKeyFlavor.SNAKE_CASE;
        }
        if (camel) {
            return PublishUserStylePayloadKeyFlavor.CAMEL_CASE;
        }
        return PublishUserStylePayloadKeyFlavor.UNKNOWN;
    }
}
