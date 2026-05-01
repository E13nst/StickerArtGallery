package com.example.sticker_art_gallery.dto.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublishUserStylePayloadKeyFlavorDetectorTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void classify_camelOnly() {
        ObjectNode n = mapper.createObjectNode();
        n.put("displayName", "x");
        n.put("idempotencyKey", "k");
        assertEquals(PublishUserStylePayloadKeyFlavor.CAMEL_CASE, PublishUserStylePayloadKeyFlavorDetector.detect(n));
    }

    @Test
    void classify_snakeOnly() {
        ObjectNode n = mapper.createObjectNode();
        n.put("display_name", "x");
        n.put("user_style_blueprint_code", "bp");
        assertEquals(PublishUserStylePayloadKeyFlavor.SNAKE_CASE, PublishUserStylePayloadKeyFlavorDetector.detect(n));
    }

    @Test
    void classify_mixed() {
        ObjectNode n = mapper.createObjectNode();
        n.put("display_name", "x");
        n.put("userStyleBlueprintCode", "bp");
        assertEquals(PublishUserStylePayloadKeyFlavor.MIXED, PublishUserStylePayloadKeyFlavorDetector.detect(n));
    }

    @Test
    void classify_unknownWhenNoKnownKeys() {
        ObjectNode n = mapper.createObjectNode();
        n.put("code", "c");
        assertEquals(PublishUserStylePayloadKeyFlavor.UNKNOWN, PublishUserStylePayloadKeyFlavorDetector.detect(n));
    }
}
