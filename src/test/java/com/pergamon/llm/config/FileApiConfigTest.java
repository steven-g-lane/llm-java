package com.pergamon.llm.config;

import com.pergamon.llm.conversation.Vendor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class FileApiConfigTest {

    @Test
    void testLoadFromPropertiesFile(@TempDir Path tempDir) throws IOException {
        // Create a temporary properties file
        Path configFile = tempDir.resolve("test-config.properties");
        String content = """
                anthropic.api.key=test-anthropic-key
                openai.api.key=test-openai-key
                google.api.key=test-google-key
                """;
        Files.writeString(configFile, content);

        // Load config from file
        FileApiConfig config = new FileApiConfig(configFile);

        // Verify keys are loaded
        assertEquals(Optional.of("test-anthropic-key"), config.getApiKey(Vendor.ANTHROPIC));
        assertEquals(Optional.of("test-openai-key"), config.getApiKey(Vendor.OPENAI));
        assertEquals(Optional.of("test-google-key"), config.getApiKey(Vendor.GOOGLE));
    }

    @Test
    void testLoadFromPropertiesObject() {
        Properties props = new Properties();
        props.setProperty("anthropic.api.key", "anthropic-123");
        props.setProperty("openai.api.key", "openai-456");
        props.setProperty("google.api.key", "google-789");

        FileApiConfig config = new FileApiConfig(props);

        assertEquals(Optional.of("anthropic-123"), config.getApiKey(Vendor.ANTHROPIC));
        assertEquals(Optional.of("openai-456"), config.getApiKey(Vendor.OPENAI));
        assertEquals(Optional.of("google-789"), config.getApiKey(Vendor.GOOGLE));
    }

    @Test
    void testMissingKeyReturnsEmpty() {
        Properties props = new Properties();
        props.setProperty("anthropic.api.key", "anthropic-key");
        // OpenAI and Google keys not set

        FileApiConfig config = new FileApiConfig(props);

        assertTrue(config.getApiKey(Vendor.ANTHROPIC).isPresent());
        assertFalse(config.getApiKey(Vendor.OPENAI).isPresent());
        assertFalse(config.getApiKey(Vendor.GOOGLE).isPresent());
    }

    @Test
    void testBlankKeyReturnsEmpty() {
        Properties props = new Properties();
        props.setProperty("anthropic.api.key", "   ");
        props.setProperty("openai.api.key", "");

        FileApiConfig config = new FileApiConfig(props);

        assertFalse(config.getApiKey(Vendor.ANTHROPIC).isPresent());
        assertFalse(config.getApiKey(Vendor.OPENAI).isPresent());
    }

    @Test
    void testConvenienceMethodsMatchVendorMethods() {
        Properties props = new Properties();
        props.setProperty("anthropic.api.key", "key1");
        props.setProperty("openai.api.key", "key2");
        props.setProperty("google.api.key", "key3");

        FileApiConfig config = new FileApiConfig(props);

        assertEquals(config.getApiKey(Vendor.ANTHROPIC), config.getAnthropicApiKey());
        assertEquals(config.getApiKey(Vendor.OPENAI), config.getOpenAIApiKey());
        assertEquals(config.getApiKey(Vendor.GOOGLE), config.getGoogleApiKey());
    }

    @Test
    void testGetApiKeyOrThrowWithPresentKey() {
        Properties props = new Properties();
        props.setProperty("anthropic.api.key", "valid-key");

        FileApiConfig config = new FileApiConfig(props);

        String key = config.getApiKeyOrThrow(Vendor.ANTHROPIC);
        assertEquals("valid-key", key);
    }

    @Test
    void testGetApiKeyOrThrowWithMissingKey() {
        Properties props = new Properties();
        // No keys set

        FileApiConfig config = new FileApiConfig(props);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            config.getApiKeyOrThrow(Vendor.ANTHROPIC);
        });

        assertTrue(exception.getMessage().contains("Anthropic"));
        assertTrue(exception.getMessage().contains("not configured"));
    }

    @Test
    void testLoadNonexistentFileThrows(@TempDir Path tempDir) {
        Path nonexistentFile = tempDir.resolve("does-not-exist.properties");

        assertThrows(IOException.class, () -> {
            new FileApiConfig(nonexistentFile);
        });
    }
}
