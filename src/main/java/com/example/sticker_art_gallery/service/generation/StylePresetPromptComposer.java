package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.StylePresetFieldDto;
import com.example.sticker_art_gallery.dto.generation.StylePresetPromptInputDto;
import com.example.sticker_art_gallery.dto.generation.StylePresetReferenceInputDto;
import com.example.sticker_art_gallery.dto.generation.StylePresetSystemFields;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.generation.StylePresetUiMode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * Валидация ввода и сборка "сырого" промпта по пресету (до enhancers и суффикса стиля).
 */
@Component
public class StylePresetPromptComposer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");
    private static final Pattern SOURCE_IMAGE_ID = Pattern.compile("^img_[A-Za-z0-9_-]+$");
    private final ObjectMapper objectMapper;

    public StylePresetPromptComposer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildRawPrompt(StylePresetEntity preset, String userPrompt, Map<String, Object> presetFields) {
        StylePresetUiMode mode = defaultMode(preset);
        Map<String, Object> fields = normalizeFields(presetFields);
        String prompt = userPrompt == null ? "" : userPrompt.trim();
        String template = preset.getPromptSuffix() != null ? preset.getPromptSuffix() : "";

        return switch (mode) {
            case CUSTOM_PROMPT -> buildCustom(preset, prompt, fields, template);
            case STYLE_WITH_PROMPT -> buildStyleWithPrompt(preset, prompt, fields, template);
            case LOCKED_TEMPLATE -> buildLocked(preset, prompt, fields, template);
            case STRUCTURED_FIELDS -> buildStructured(preset, prompt, fields, template);
        };
    }

    private String buildCustom(StylePresetEntity preset, String prompt, Map<String, Object> fields, String template) {
        validateNoExtraKeys(fields, Set.of());
        var input = parsePromptInput(preset);
        if (input.getEnabled() == null || !input.getEnabled()) {
            if (!prompt.isEmpty()) {
                throw new IllegalArgumentException("This preset does not accept a custom prompt");
            }
            if (!template.isEmpty()) {
                return template;
            }
            throw new IllegalArgumentException("Empty preset output");
        }
        if (Boolean.TRUE.equals(input.getRequired()) && prompt.isEmpty()) {
            throw new IllegalArgumentException("Prompt is required for this preset");
        }
        if (!prompt.isEmpty() && input.getMaxLength() != null && prompt.length() > input.getMaxLength()) {
            throw new IllegalArgumentException("Prompt is too long for this preset (max " + input.getMaxLength() + ")");
        }
        if (!template.isEmpty()) {
            if (containsPlaceholders(template)) {
                return applyTemplate(
                        template,
                        prepareTemplateFieldMap(
                                preset,
                                prompt,
                                applyPresetReferenceSyntheticId(preset, fields)));
            }
            if (!prompt.isEmpty()) {
                return prompt;
            }
            return template;
        }
        if (prompt.isEmpty()) {
            throw new IllegalArgumentException("Prompt is required for this preset");
        }
        return prompt;
    }

    private String buildStyleWithPrompt(StylePresetEntity preset, String prompt, Map<String, Object> fields, String template) {
        if (!fields.isEmpty()) {
            throw new IllegalArgumentException("This preset does not support preset fields");
        }
        var input = parsePromptInput(preset);
        if (input.getEnabled() == null || !input.getEnabled()) {
            if (!prompt.isEmpty()) {
                throw new IllegalArgumentException("This preset does not accept a custom prompt");
            }
            return "";
        }
        if (Boolean.TRUE.equals(input.getRequired()) && prompt.isEmpty()) {
            throw new IllegalArgumentException("Prompt is required for this preset");
        }
        if (input.getMaxLength() != null && !prompt.isEmpty() && prompt.length() > input.getMaxLength()) {
            throw new IllegalArgumentException("Prompt is too long for this preset (max " + input.getMaxLength() + ")");
        }
        return prompt;
    }

    private String buildLocked(StylePresetEntity preset, String prompt, Map<String, Object> fields, String template) {
        if (!prompt.isEmpty()) {
            throw new IllegalArgumentException("This preset uses a fixed template: free text is not allowed");
        }
        if (template.isEmpty()) {
            throw new IllegalArgumentException("Template (prompt suffix) is empty for LOCKED preset");
        }
        Set<String> ph = extractPlaceholders(template);
        if (ph.isEmpty()) {
            throw new IllegalArgumentException("LOCKED template must contain {{placeholders}} or use a different mode");
        }
        Map<String, Object> withPresetRef = applyPresetReferenceSyntheticId(preset, fields);
        validatePlaceholderInputs(preset, withPresetRef, ph, "");
        return applyTemplate(template, prepareTemplateFieldMap(preset, "", withPresetRef));
    }

    private String buildStructured(StylePresetEntity preset, String prompt, Map<String, Object> fields, String template) {
        boolean templateUsesPrompt = extractPlaceholders(template).contains("prompt");
        var input = parsePromptInput(preset);
        boolean promptAllowed = templateUsesPrompt && Boolean.TRUE.equals(input.getEnabled());
        if (!prompt.isEmpty() && !promptAllowed) {
            throw new IllegalArgumentException("This preset is structured: free text is not allowed");
        }
        if (Boolean.TRUE.equals(input.getRequired()) && promptAllowed && prompt.isEmpty()) {
            throw new IllegalArgumentException("Prompt is required for this preset");
        }
        if (input.getMaxLength() != null && !prompt.isEmpty() && prompt.length() > input.getMaxLength()) {
            throw new IllegalArgumentException("Prompt is too long for this preset (max " + input.getMaxLength() + ")");
        }
        Map<String, Object> effective = applyPresetReferenceSyntheticId(preset, fields);
        List<StylePresetFieldDto> defs = listStructuredFieldDefinitions(preset);
        if (defs.isEmpty()) {
            if (template.isEmpty()) {
                throw new IllegalArgumentException("No structured fields defined for preset");
            }
            Set<String> ph = extractPlaceholders(template);
            validateNoExtraKeys(effective, ph.stream().filter(k -> !"prompt".equals(k)).collect(Collectors.toSet()));
            return applyTemplate(template, templateValues(effective, prompt));
        }
        validateStructured(defs, effective, template);
        return composeStructuredString(preset, defs, effective, template, prompt);
    }

    private void validateStructured(
            List<StylePresetFieldDto> defs,
            Map<String, Object> fields,
            String template
    ) {
        for (var def : defs) {
            if (def.getKey() == null || def.getKey().isBlank()) {
                throw new IllegalArgumentException("Invalid structured field: missing key");
            }
        }
        var keys = defs.stream().map(StylePresetFieldDto::getKey).collect(Collectors.toSet());
        validateNoExtraKeys(fields, keys);
        for (var def : defs) {
            Object val = fields.get(def.getKey());
            if (isReferenceField(def)) {
                List<String> ids = parseReferenceIds(val);
                int minRef = effectiveMinImages(def);
                int maxRef = effectiveMaxImages(def);
                if (ids.size() < minRef) {
                    throw new IllegalArgumentException("Not enough reference images for field: " + def.getKey());
                }
                if (ids.size() > maxRef) {
                    throw new IllegalArgumentException("Too many reference images for field: " + def.getKey());
                }
                for (String id : ids) {
                    if (!SOURCE_IMAGE_ID.matcher(id).matches()) {
                        throw new IllegalArgumentException("Invalid image id for field: " + def.getKey());
                    }
                }
                continue;
            }
            if (val == null || (val instanceof String s && s.isBlank())) {
                if (Boolean.TRUE.equals(def.getRequired())) {
                    throw new IllegalArgumentException("Missing value for field: " + def.getKey());
                }
                continue;
            }
            if ("select".equalsIgnoreCase(def.getType()) && def.getOptions() != null && !def.getOptions().isEmpty()) {
                String s = valueToString(val);
                if (def.getOptions().stream().noneMatch(o -> o.equals(s))) {
                    throw new IllegalArgumentException("Invalid value for field: " + def.getKey());
                }
            }
            if (def.getMaxLength() != null) {
                String s = valueToString(val);
                if (s.length() > def.getMaxLength()) {
                    throw new IllegalArgumentException("Value too long for field: " + def.getKey());
                }
            }
        }
        if (!template.isEmpty() && containsPlaceholders(template)) {
            Set<String> ph = extractPlaceholders(template);
            for (String k : ph) {
                if ("prompt".equals(k)) {
                    continue;
                }
                if (!keys.contains(k)) {
                    throw new IllegalArgumentException("Unknown template placeholder: " + k);
                }
            }
        }
    }

    private String composeStructuredString(
            StylePresetEntity preset,
            List<StylePresetFieldDto> defs,
            Map<String, Object> fields,
            String template,
            String prompt
    ) {
        if (containsPlaceholders(template)) {
            return applyTemplate(template, prepareTemplateFieldMap(preset, prompt, fields));
        }
        StringBuilder sb = new StringBuilder();
        for (var def : defs) {
            if (isReferenceField(def)) {
                continue;
            }
            Object val = fields.get(def.getKey());
            if (val == null) {
                continue;
            }
            String s = valueToString(val);
            if (s.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            if (def.getLabel() != null && !def.getLabel().isBlank()) {
                sb.append(def.getLabel().trim()).append(": ");
            } else {
                sb.append(def.getKey()).append(": ");
            }
            sb.append(s);
        }
        if (template.isEmpty()) {
            if (sb.isEmpty()) {
                throw new IllegalArgumentException("No field values for structured preset");
            }
            return sb.toString();
        }
        if (!template.endsWith(" ") && !template.endsWith("\n") && !sb.isEmpty()) {
            return template.trim() + " " + sb;
        }
        return template + sb;
    }

    private static Map<String, Object> templateValues(Map<String, Object> fields, String prompt) {
        Map<String, Object> values = new HashMap<>(fields);
        values.put("prompt", prompt == null ? "" : prompt.trim());
        return values;
    }

    private static Map<String, Object> templateValues(
            Map<String, Object> fields,
            String prompt,
            List<StylePresetFieldDto> defs
    ) {
        Map<String, Object> values = templateValues(fields, prompt);
        for (StylePresetFieldDto def : defs) {
            if (def.getKey() != null && !def.getKey().isBlank()) {
                values.putIfAbsent(def.getKey(), "");
            }
        }
        return values;
    }

    private void validatePlaceholderInputs(
            StylePresetEntity preset,
            Map<String, Object> fields,
            Set<String> placeholders,
            String promptText
    ) {
        validateNoExtraKeys(fields, placeholders);
        List<StylePresetFieldDto> defs = listStructuredFieldDefinitions(preset);
        Map<String, StylePresetFieldDto> defByKey = defs.stream()
                .filter(d -> d.getKey() != null && !d.getKey().isBlank())
                .collect(Collectors.toMap(StylePresetFieldDto::getKey, d -> d, (a, b) -> a));
        for (String key : placeholders) {
            if ("prompt".equals(key)) {
                if (promptText == null || promptText.isBlank()) {
                    throw new IllegalArgumentException("Missing value for: prompt");
                }
                continue;
            }
            if (!fields.containsKey(key) || fields.get(key) == null) {
                throw new IllegalArgumentException("Missing value for: " + key);
            }
            StylePresetFieldDto def = defByKey.get(key);
            if (def != null && isReferenceField(def)) {
                validateReferenceSlotInput(def, fields.get(key));
            } else {
                if (valueToString(fields.get(key)).isEmpty()) {
                    throw new IllegalArgumentException("Empty value for: " + key);
                }
            }
        }
    }

    private void validateNoExtraKeys(Map<String, Object> fields, Set<String> allowed) {
        for (String k : fields.keySet()) {
            if (!allowed.contains(k)) {
                throw new IllegalArgumentException("Unknown preset field: " + k);
            }
        }
    }

    public Map<String, Object> prepareTemplateFieldMap(StylePresetEntity preset, String prompt, Map<String, Object> fields) {
        Map<String, Object> merged = applyPresetReferenceSyntheticId(preset, normalizeFields(fields));
        List<StylePresetFieldDto> defs = listStructuredFieldDefinitions(preset);
        Map<String, Object> base = templateValues(merged, prompt, defs);
        List<StylePresetFieldDto> refDefs = defs.stream().filter(this::isReferenceField).toList();
        if (refDefs.isEmpty()) {
            return base;
        }
        List<String> canonical = buildCanonicalUniqueOrder(refDefs, merged);
        if (canonical.size() > GenerationV2Constants.MAX_SOURCE_IMAGE_IDS) {
            throw new IllegalArgumentException("Too many unique reference images (max "
                    + GenerationV2Constants.MAX_SOURCE_IMAGE_IDS + ")");
        }
        Map<String, Object> out = new HashMap<>(base);
        for (StylePresetFieldDto def : refDefs) {
            String key = def.getKey().trim();
            List<String> slotIds = parseReferenceIds(merged.get(key));
            String sub = formatReferenceSubstitution(def, slotIds, canonical);
            out.put(key, sub);
        }
        return out;
    }

    /**
     * Канонический порядок уникальных source image id для v2: слоты по порядку в пресете, дубликаты id отбрасываются при сохранении порядка первого вхождения.
     */
    public List<String> resolveV2SourceImageIds(
            StylePresetEntity preset,
            Map<String, Object> presetFields,
            List<String> flatImageIds,
            String singleImageId
    ) {
        if (preset == null) {
            return validateAndCopyImageIds(dedupeIds(flattenLegacyIds(flatImageIds, singleImageId)));
        }
        List<StylePresetFieldDto> refDefs = listStructuredFieldDefinitions(preset).stream()
                .filter(this::isReferenceField)
                .toList();
        if (refDefs.isEmpty()) {
            return validateAndCopyImageIds(dedupeIds(flattenLegacyIds(flatImageIds, singleImageId)));
        }
        final Map<String, Object> fields = applyPresetReferenceSyntheticId(
                preset, normalizeFields(presetFields));
        boolean anySlot = refDefs.stream()
                .anyMatch(d -> !parseReferenceIds(fields.get(d.getKey())).isEmpty());
        List<String> canonical = anySlot
                ? buildCanonicalUniqueOrder(refDefs, fields)
                : dedupeIds(flattenLegacyIds(flatImageIds, singleImageId));
        return validateAndCopyImageIds(canonical);
    }

    /**
     * Подставляет {@link StylePresetSystemFields#PRESET_REFERENCE_KEY} = {@code img_sagref_*}, если у пресета задано референсное фото.
     */
    private Map<String, Object> applyPresetReferenceSyntheticId(StylePresetEntity preset, Map<String, Object> fields) {
        if (preset.getReferenceImage() == null) {
            return fields;
        }
        String synthetic = StylePresetReferenceImageId.fromCachedImageId(preset.getReferenceImage().getId());
        Map<String, Object> merged = new HashMap<>(fields);
        if (parseReferenceIds(merged.get(StylePresetSystemFields.PRESET_REFERENCE_KEY)).isEmpty()) {
            merged.put(StylePresetSystemFields.PRESET_REFERENCE_KEY, synthetic);
        }
        return merged;
    }

    private List<String> validateAndCopyImageIds(List<String> canonical) {
        if (canonical.size() > GenerationV2Constants.MAX_SOURCE_IMAGE_IDS) {
            throw new IllegalArgumentException("Too many reference images (max "
                    + GenerationV2Constants.MAX_SOURCE_IMAGE_IDS + ")");
        }
        for (String id : canonical) {
            if (!SOURCE_IMAGE_ID.matcher(id).matches()) {
                throw new IllegalArgumentException("Invalid image id: " + id);
            }
        }
        return new ArrayList<>(canonical);
    }

    private boolean isReferenceField(StylePresetFieldDto def) {
        return def.getType() != null && "reference".equalsIgnoreCase(def.getType().trim());
    }

    private void validateReferenceSlotInput(StylePresetFieldDto def, Object raw) {
        List<String> ids = parseReferenceIds(raw);
        int minRef = effectiveMinImages(def);
        int maxRef = effectiveMaxImages(def);
        if (ids.size() < minRef) {
            throw new IllegalArgumentException("Not enough reference images for field: " + def.getKey());
        }
        if (ids.size() > maxRef) {
            throw new IllegalArgumentException("Too many reference images for field: " + def.getKey());
        }
        for (String id : ids) {
            if (!SOURCE_IMAGE_ID.matcher(id).matches()) {
                throw new IllegalArgumentException("Invalid image id for field: " + def.getKey());
            }
        }
    }

    private static int effectiveMinImages(StylePresetFieldDto def) {
        int r = Boolean.TRUE.equals(def.getRequired()) ? 1 : 0;
        if (def.getMinImages() != null) {
            r = Math.max(r, def.getMinImages());
        }
        return r;
    }

    private static int effectiveMaxImages(StylePresetFieldDto def) {
        return def.getMaxImages() != null ? def.getMaxImages() : 1;
    }

    static List<String> parseReferenceIds(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof String s) {
            String t = s.trim();
            return t.isEmpty() ? List.of() : List.of(t);
        }
        if (raw instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o == null) {
                    continue;
                }
                String t = o.toString().trim();
                if (!t.isEmpty()) {
                    out.add(t);
                }
            }
            return out;
        }
        String t = raw.toString().trim();
        return t.isEmpty() ? List.of() : List.of(t);
    }

    private List<String> buildCanonicalUniqueOrder(List<StylePresetFieldDto> refDefs, Map<String, Object> fields) {
        List<String> order = new ArrayList<>();
        for (StylePresetFieldDto def : refDefs) {
            for (String id : parseReferenceIds(fields.get(def.getKey()))) {
                if (!order.contains(id)) {
                    order.add(id);
                }
            }
        }
        return order;
    }

    private String formatReferenceSubstitution(StylePresetFieldDto def, List<String> slotIds, List<String> canonical) {
        if (slotIds.isEmpty()) {
            return "";
        }
        String tpl = def.getPromptTemplate();
        if (tpl == null || tpl.isBlank()) {
            tpl = "Image {index}";
        }
        List<String> parts = new ArrayList<>();
        for (String id : slotIds) {
            int idx = canonical.indexOf(id);
            if (idx < 0) {
                throw new IllegalArgumentException("Reference image not in canonical order: " + id);
            }
            parts.add(tpl.replace("{index}", String.valueOf(idx + 1)));
        }
        return String.join(", ", parts);
    }

    private static List<String> flattenLegacyIds(List<String> flatImageIds, String singleImageId) {
        List<String> out = new ArrayList<>();
        if (flatImageIds != null) {
            for (String id : flatImageIds) {
                if (id != null && !id.isBlank()) {
                    out.add(id.trim());
                }
            }
        }
        if (singleImageId != null && !singleImageId.isBlank()) {
            String t = singleImageId.trim();
            if (out.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    private static List<String> dedupeIds(List<String> ids) {
        List<String> deduped = new ArrayList<>();
        for (String id : ids) {
            if (!deduped.contains(id)) {
                deduped.add(id);
            }
        }
        return deduped;
    }

    public StylePresetPromptInputDto parsePromptInput(StylePresetEntity preset) {
        if (preset.getPromptInputJson() == null) {
            var def = new StylePresetPromptInputDto();
            def.setEnabled(true);
            def.setRequired(true);
            def.setMaxLength(1000);
            StylePresetReferenceInputDto refs = new StylePresetReferenceInputDto();
            refs.setEnabled(true);
            refs.setRequired(false);
            refs.setMinCount(0);
            refs.setMaxCount(GenerationV2Constants.MAX_SOURCE_IMAGE_IDS);
            def.setReferenceImages(refs);
            return def;
        }
        StylePresetPromptInputDto parsed =
                objectMapper.convertValue(preset.getPromptInputJson(), StylePresetPromptInputDto.class);
        if (parsed.getReferenceImages() == null) {
            StylePresetReferenceInputDto refs = new StylePresetReferenceInputDto();
            refs.setEnabled(true);
            refs.setRequired(false);
            refs.setMinCount(0);
            refs.setMaxCount(GenerationV2Constants.MAX_SOURCE_IMAGE_IDS);
            parsed.setReferenceImages(refs);
        }
        return parsed;
    }

    public List<StylePresetFieldDto> parseStructuredFields(StylePresetEntity preset) {
        if (preset.getStructuredFieldsJson() == null || preset.getStructuredFieldsJson().isEmpty()) {
            return new ArrayList<>();
        }
        List<StylePresetFieldDto> out = new ArrayList<>();
        for (Map<String, Object> row : preset.getStructuredFieldsJson()) {
            out.add(objectMapper.convertValue(row, StylePresetFieldDto.class));
        }
        return out;
    }

    /**
     * JSON полей из БД + при наличии референсного фото — системное поле {@code preset_ref} первым (для порядка Image 1, …).
     */
    public List<StylePresetFieldDto> listStructuredFieldDefinitions(StylePresetEntity preset) {
        List<StylePresetFieldDto> fromJson = parseStructuredFields(preset);
        if (preset.getReferenceImage() == null) {
            return fromJson;
        }
        List<StylePresetFieldDto> out = new ArrayList<>();
        out.add(StylePresetSystemFields.presetReferenceFieldDefinition());
        for (StylePresetFieldDto f : fromJson) {
            if (f.getKey() != null && StylePresetSystemFields.isReservedFieldKey(f.getKey())) {
                continue;
            }
            out.add(f);
        }
        return out;
    }

    private static StylePresetUiMode defaultMode(StylePresetEntity preset) {
        if (preset.getUiMode() == null) {
            return StylePresetUiMode.STYLE_WITH_PROMPT;
        }
        return preset.getUiMode();
    }

    private static Map<String, Object> normalizeFields(Map<String, Object> presetFields) {
        if (presetFields == null || presetFields.isEmpty()) {
            return new HashMap<>();
        }
        return new HashMap<>(presetFields);
    }

    public static String applyTemplate(String template, Map<String, Object> values) {
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            if (!values.containsKey(key) || values.get(key) == null) {
                throw new IllegalArgumentException("Missing value for template key: " + key);
            }
            String rep = Matcher.quoteReplacement(valueToString(values.get(key)));
            m.appendReplacement(sb, rep);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static Set<String> extractPlaceholders(String template) {
        Matcher m = PLACEHOLDER.matcher(template);
        Set<String> set = new java.util.HashSet<>();
        while (m.find()) {
            set.add(m.group(1));
        }
        return set;
    }

    public static boolean containsPlaceholders(String template) {
        return template != null && PLACEHOLDER.matcher(template).find();
    }

    public static String valueToString(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof String s) {
            return s.trim();
        }
        if (v instanceof Number || v instanceof Boolean) {
            return String.valueOf(v);
        }
        return v.toString().trim();
    }
}
