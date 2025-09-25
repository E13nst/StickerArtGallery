package com.example.sticker_art_gallery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "dream.states")
public class DreamStateConfig {

    private Map<String, String> descriptions = new HashMap<>();

    public String getDescription(String stateName) {
        return descriptions.getOrDefault(stateName, "No description available");
    }

    public Map<String, String> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(Map<String, String> descriptions) {
        this.descriptions = descriptions;
    }
}
