package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.StylePresetFieldDto;
import com.example.sticker_art_gallery.dto.generation.StylePresetPromptInputDto;
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
                return applyTemplate(template, fields);
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
        validateAllPlaceholdersPresent(fields, ph);
        return applyTemplate(template, fields);
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
        List<StylePresetFieldDto> defs = parseStructuredFields(preset);
        if (defs.isEmpty()) {
            if (template.isEmpty()) {
                throw new IllegalArgumentException("No structured fields defined for preset");
            }
            Set<String> ph = extractPlaceholders(template);
            validateNoExtraKeys(fields, ph.stream().filter(k -> !"prompt".equals(k)).collect(Collectors.toSet()));
            return applyTemplate(template, templateValues(fields, prompt));
        }
        validateStructured(defs, fields, template);
        return composeStructuredString(defs, fields, template, prompt);
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
            List<StylePresetFieldDto> defs,
            Map<String, Object> fields,
            String template,
            String prompt
    ) {
        if (containsPlaceholders(template)) {
            return applyTemplate(template, templateValues(fields, prompt, defs));
        }
        StringBuilder sb = new StringBuilder();
        for (var def : defs) {
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

    private void validateAllPlaceholdersPresent(Map<String, Object> fields, Set<String> placeholders) {
        validateNoExtraKeys(fields, placeholders);
        for (String key : placeholders) {
            if (!fields.containsKey(key) || fields.get(key) == null) {
                throw new IllegalArgumentException("Missing value for: " + key);
            }
            if (valueToString(fields.get(key)).isEmpty()) {
                throw new IllegalArgumentException("Empty value for: " + key);
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

    public StylePresetPromptInputDto parsePromptInput(StylePresetEntity preset) {
        if (preset.getPromptInputJson() == null) {
            var def = new StylePresetPromptInputDto();
            def.setEnabled(true);
            def.setRequired(true);
            def.setMaxLength(1000);
            return def;
        }
        return objectMapper.convertValue(preset.getPromptInputJson(), StylePresetPromptInputDto.class);
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
