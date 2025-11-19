package com.pergamon.llm.conversation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ModelRegistryTest {

    private ModelRegistry registry;
    private ModelDescriptor claudeSonnet45;
    private ModelDescriptor gpt4o;

    @BeforeEach
    void setUp() {
        // Create test model descriptors
        claudeSonnet45 = new ModelDescriptor(
                new ModelId(Vendor.ANTHROPIC, "claude-sonnet-4-5"),
                "Claude Sonnet 4.5",
                "claude-sonnet-4-5",
                new ModelCapabilities(
                        true,  // supportsChat
                        true,  // supportsTools
                        true,  // supportsImages
                        true,  // supportsFiles
                        true,  // supportsStreaming
                        64000, // contextWindowTokens
                        64000  // maxOutputTokens
                )
        );

        gpt4o = new ModelDescriptor(
                new ModelId(Vendor.OPENAI, "gpt-4o"),
                "GPT-4o",
                "gpt-4o",
                new ModelCapabilities(
                        true,   // supportsChat
                        true,   // supportsTools
                        true,   // supportsImages
                        false,  // supportsFiles
                        true,   // supportsStreaming
                        128000, // contextWindowTokens
                        128000  // maxOutputTokens
                )
        );

        // Build registry
        Map<ModelId, ModelDescriptor> models = new HashMap<>();
        models.put(claudeSonnet45.id(), claudeSonnet45);
        models.put(gpt4o.id(), gpt4o);
        registry = new ModelRegistry(models);
    }

    @Test
    void testFindByModelId() {
        ModelId searchId = new ModelId(Vendor.ANTHROPIC, "claude-sonnet-4-5");
        Optional<ModelDescriptor> result = registry.find(searchId);

        assertTrue(result.isPresent(), "Should find Claude Sonnet 4.5");
        assertEquals("Claude Sonnet 4.5", result.get().friendlyName());
        assertEquals(64000, result.get().capabilitie().contextWindowTokens());
        assertTrue(result.get().capabilitie().supportsFiles());
    }

    @Test
    void testFindByVendorAndName() {
        Optional<ModelDescriptor> result = registry.find(Vendor.OPENAI, "gpt-4o");

        assertTrue(result.isPresent(), "Should find GPT-4o");
        assertEquals("GPT-4o", result.get().friendlyName());
        assertEquals(128000, result.get().capabilitie().contextWindowTokens());
        assertFalse(result.get().capabilitie().supportsFiles());
    }

    @Test
    void testFindNonexistentModelReturnsEmpty() {
        ModelId nonexistentId = new ModelId(Vendor.GOOGLE, "gemini-ultra-99");
        Optional<ModelDescriptor> result = registry.find(nonexistentId);

        assertFalse(result.isPresent(), "Should return empty Optional for unknown model");
    }

    @Test
    void testFindByVendorAndNameNonexistentReturnsEmpty() {
        Optional<ModelDescriptor> result = registry.find(Vendor.ANTHROPIC, "claude-mega-100");

        assertFalse(result.isPresent(), "Should return empty Optional for unknown model");
    }

    @Test
    void testRegistryImmutability() {
        // Create a mutable source map
        Map<ModelId, ModelDescriptor> sourceMap = new HashMap<>();
        sourceMap.put(claudeSonnet45.id(), claudeSonnet45);
        ModelRegistry testRegistry = new ModelRegistry(sourceMap);

        // Verify model exists
        Optional<ModelDescriptor> before = testRegistry.find(Vendor.ANTHROPIC, "claude-sonnet-4-5");
        assertTrue(before.isPresent(), "Model should exist before modification");

        // Modify the source map
        sourceMap.clear();

        // Registry should still contain the model (defensive copy protects it)
        Optional<ModelDescriptor> after = testRegistry.find(Vendor.ANTHROPIC, "claude-sonnet-4-5");
        assertTrue(after.isPresent(), "Registry should be immutable - defensive copy should protect it");
    }

    @Test
    void testLoadFromRealModelsJson() throws IOException {
        ModelRegistry loadedRegistry = ModelLoader.loadFromResource("/models.json");

        // Test that at least one known model loads correctly
        Optional<ModelDescriptor> claudeSonnet = loadedRegistry.find(Vendor.ANTHROPIC, "claude-sonnet-4-5");
        assertTrue(claudeSonnet.isPresent(), "Should load Claude Sonnet 4.5 from models.json");

        ModelDescriptor model = claudeSonnet.get();
        assertEquals("Claude Sonnet 4.5", model.friendlyName());
        assertEquals("claude-sonnet-4-5", model.apiModelName());
        assertEquals(Vendor.ANTHROPIC, model.id().vendor());
        assertEquals(64000, model.capabilitie().contextWindowTokens());
        assertEquals(64000, model.capabilitie().maxOutputTokens());
        assertTrue(model.capabilitie().supportsImages());
        assertTrue(model.capabilitie().supportsFiles());
        assertTrue(model.capabilitie().supportsTools());
        assertTrue(model.capabilitie().supportsStreaming());
    }

    @Test
    void testLoadFromResourceUnknownModelReturnsEmpty() throws IOException {
        ModelRegistry loadedRegistry = ModelLoader.loadFromResource("/models.json");

        Optional<ModelDescriptor> unknownModel = loadedRegistry.find(Vendor.GOOGLE, "gemini-ultra-999");
        assertFalse(unknownModel.isPresent(), "Should return empty Optional for model not in models.json");
    }

    @Test
    void testModelIdStringRepresentation() {
        ModelId id = new ModelId(Vendor.ANTHROPIC, "claude-sonnet-4-5");
        assertEquals("anthropic:claude-sonnet-4-5", id.toString());
    }
}
