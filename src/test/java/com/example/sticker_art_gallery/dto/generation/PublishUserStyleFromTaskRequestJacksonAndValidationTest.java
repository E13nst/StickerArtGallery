package com.example.sticker_art_gallery.dto.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublishUserStyleFromTaskRequestJacksonAndValidationTest {

    private static ValidatorFactory vf;
    private static Validator validator;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void setupValidator() {
        vf = Validation.buildDefaultValidatorFactory();
        validator = vf.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        if (vf != null) {
            vf.close();
        }
    }

    @Test
    void jackson_canonicalCamelCase_deserializes() throws Exception {
        String json = """
                {"code":"c1","displayName":"Egg","idempotencyKey":"%s",
                "consentResultPublicShow":true,"userStyleBlueprintCode":"style_anchor_standard"}
                """.formatted(UUID.randomUUID());

        PublishUserStyleFromTaskRequest r =
                mapper.readValue(json, PublishUserStyleFromTaskRequest.class);
        assertEquals("c1", r.getCode());
        assertEquals("Egg", r.getDisplayName());
        assertEquals("style_anchor_standard", r.getUserStyleBlueprintCode());
        assertTrue(r.getConsentResultPublicShow());

        Set<ConstraintViolation<PublishUserStyleFromTaskRequest>> v = validator.validate(r);
        assertTrue(v.isEmpty(), v::toString);
    }

    @Test
    void jackson_snakeCase_deserializes() throws Exception {
        String json = """
                {"code":"c1","display_name":"Яйцо","idempotency_key":"ik-1",
                "consent_result_public_show":true,"user_style_blueprint_code":"bp_x"}
                """;

        PublishUserStyleFromTaskRequest r =
                mapper.readValue(json, PublishUserStyleFromTaskRequest.class);
        assertEquals("Яйцо", r.getDisplayName());
        assertEquals("ik-1", r.getIdempotencyKey());
        assertEquals("bp_x", r.getUserStyleBlueprintCode());
        Set<ConstraintViolation<PublishUserStyleFromTaskRequest>> v = validator.validate(r);
        assertTrue(v.isEmpty(), v::toString);
    }

    @Test
    void jackson_mixedAliases_deserializes() throws Exception {
        String json = """
                {"code":"x","display_name":"N","idempotencyKey":"idem",
                "consent_result_public_show":true,"category_id":5}
                """;

        PublishUserStyleFromTaskRequest r =
                mapper.readValue(json, PublishUserStyleFromTaskRequest.class);
        assertEquals("idem", r.getIdempotencyKey());
        assertEquals(5L, r.getCategoryId());

        Set<ConstraintViolation<PublishUserStyleFromTaskRequest>> violations = validator.validate(r);
        assertTrue(violations.isEmpty(), violations::toString);
    }

    @Test
    void validation_missingRequiredFields() {
        PublishUserStyleFromTaskRequest r = new PublishUserStyleFromTaskRequest();
        r.setCode("c");
        Set<ConstraintViolation<PublishUserStyleFromTaskRequest>> violations = validator.validate(r);
        assertFalse(violations.isEmpty());
        long fields = violations.stream().map(v -> v.getPropertyPath().toString()).distinct().count();
        assertTrue(fields >= 3, violations.toString());
    }
}
