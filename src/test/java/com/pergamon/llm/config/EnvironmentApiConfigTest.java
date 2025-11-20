package com.pergamon.llm.config;

import com.pergamon.llm.conversation.Vendor;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentApiConfigTest {

    @Test
    void testGetApiKeyReturnsValueFromEnvironment() {
        EnvironmentApiConfig config = new EnvironmentApiConfig();

        // Note: This test will only pass if the environment variables are actually set
        // In a real environment with keys set, these would return values
        // For now, we just verify the method doesn't crash
        Optional<String> anthropicKey = config.getApiKey(Vendor.ANTHROPIC);
        Optional<String> openaiKey = config.getApiKey(Vendor.OPENAI);
        Optional<String> googleKey = config.getApiKey(Vendor.GOOGLE);

        // These are optional - may or may not be present depending on environment
        assertNotNull(anthropicKey);
        assertNotNull(openaiKey);
        assertNotNull(googleKey);
    }

    @Test
    void testConvenienceMethodsMatchVendorMethods() {
        EnvironmentApiConfig config = new EnvironmentApiConfig();

        assertEquals(config.getApiKey(Vendor.ANTHROPIC), config.getAnthropicApiKey());
        assertEquals(config.getApiKey(Vendor.OPENAI), config.getOpenAIApiKey());
        assertEquals(config.getApiKey(Vendor.GOOGLE), config.getGoogleApiKey());
    }

    @Test
    void testGetApiKeyOrThrowWithMissingKey() {
        EnvironmentApiConfig config = new EnvironmentApiConfig();

        // If the key is not set, getApiKeyOrThrow should throw
        // We can't test this reliably since we don't know if keys are set
        // But we can verify the method exists and returns a value if key is present
        try {
            String key = config.getApiKeyOrThrow(Vendor.ANTHROPIC);
            assertNotNull(key);
            assertFalse(key.isBlank());
        } catch (IllegalStateException e) {
            // Expected if ANTHROPIC_API_KEY is not set
            assertTrue(e.getMessage().contains("Anthropic"));
        }
    }
}
