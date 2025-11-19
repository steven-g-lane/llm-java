package com.pergamon.llm.conversation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ModelLoader {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Loads a ModelRegistry from a classpath resource containing JSON model definitions.
     *
     * @param resourcePath the path to the resource file (e.g., "/models.json")
     * @return a ModelRegistry populated with the models from the resource
     * @throws IOException if the resource cannot be read or parsed
     * @throws IllegalArgumentException if the resource is not found
     */
    public static ModelRegistry loadFromResource(String resourcePath) throws IOException {
        try (InputStream inputStream = ModelLoader.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }

            JsonNode rootNode = objectMapper.readTree(inputStream);
            if (!rootNode.isArray()) {
                throw new IllegalArgumentException("Expected JSON array of models");
            }

            Map<ModelId, ModelDescriptor> models = new HashMap<>();

            for (JsonNode modelNode : rootNode) {
                ModelDescriptor descriptor = parseModelDescriptor(modelNode);
                models.put(descriptor.id(), descriptor);
            }

            return new ModelRegistry(models);
        }
    }

    private static ModelDescriptor parseModelDescriptor(JsonNode node) {
        // Parse vendor
        String vendorSlug = node.get("vendorSlug").asText();
        Vendor vendor = parseVendor(vendorSlug);

        // Parse basic fields
        String apiModelName = node.get("apiModelName").asText();
        String friendlyName = node.get("friendlyName").asText();

        // Parse capabilities
        ModelCapabilities capabilities = new ModelCapabilities(
                true, // supportsChat - always true for now
                node.get("supportsTools").asBoolean(),
                node.get("supportsImages").asBoolean(),
                node.get("supportsFiles").asBoolean(),
                node.get("supportsStreaming").asBoolean(),
                node.get("contextWindowTokens").asInt(),
                node.get("maxOutputTokens").asInt()
        );

        // Create ModelId and ModelDescriptor
        ModelId modelId = new ModelId(vendor, apiModelName);
        return new ModelDescriptor(modelId, friendlyName, apiModelName, capabilities);
    }

    private static Vendor parseVendor(String slug) {
        return switch (slug.toLowerCase()) {
            case "openai" -> Vendor.OPENAI;
            case "anthropic" -> Vendor.ANTHROPIC;
            case "google" -> Vendor.GOOGLE;
            default -> throw new IllegalArgumentException("Unknown vendor: " + slug);
        };
    }
}
