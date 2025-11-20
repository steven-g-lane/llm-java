package com.pergamon.llm.config;

import com.pergamon.llm.conversation.Vendor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * API configuration that loads keys from a properties file.
 *
 * Expected properties file format:
 * <pre>
 * anthropic.api.key=sk-ant-api03-...
 * openai.api.key=sk-...
 * google.api.key=...
 * </pre>
 *
 * The properties file should NOT be committed to version control.
 * Add it to .gitignore to keep API keys secure.
 */
public class FileApiConfig implements ApiConfig {

    private static final String ANTHROPIC_PROPERTY = "anthropic.api.key";
    private static final String OPENAI_PROPERTY = "openai.api.key";
    private static final String GOOGLE_PROPERTY = "google.api.key";

    private final Properties properties;

    /**
     * Creates a FileApiConfig by loading from the specified file path.
     *
     * @param configFilePath path to the properties file
     * @throws IOException if the file cannot be read
     */
    public FileApiConfig(Path configFilePath) throws IOException {
        this.properties = new Properties();
        try (InputStream input = Files.newInputStream(configFilePath)) {
            properties.load(input);
        }
    }

    /**
     * Creates a FileApiConfig by loading from a classpath resource.
     *
     * @param resourcePath classpath resource path (e.g., "/config.properties")
     * @throws IOException if the resource cannot be read
     */
    public static FileApiConfig fromResource(String resourcePath) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = FileApiConfig.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            properties.load(input);
        }
        return new FileApiConfig(properties);
    }

    /**
     * Creates a FileApiConfig from an already-loaded Properties object.
     *
     * @param properties the properties containing API keys
     */
    public FileApiConfig(Properties properties) {
        this.properties = new Properties();
        this.properties.putAll(properties);
    }

    @Override
    public Optional<String> getApiKey(Vendor vendor) {
        String propertyName = switch (vendor) {
            case ANTHROPIC -> ANTHROPIC_PROPERTY;
            case OPENAI -> OPENAI_PROPERTY;
            case GOOGLE -> GOOGLE_PROPERTY;
        };

        String value = properties.getProperty(propertyName);
        return (value != null && !value.isBlank())
                ? Optional.of(value)
                : Optional.empty();
    }
}
