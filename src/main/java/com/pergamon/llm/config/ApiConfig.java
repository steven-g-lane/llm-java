package com.pergamon.llm.config;

import com.pergamon.llm.conversation.Vendor;

import java.util.Optional;

/**
 * Configuration interface for accessing LLM vendor API keys.
 *
 * Implementations can load API keys from various sources such as
 * environment variables, configuration files, system properties, etc.
 */
public interface ApiConfig {

    /**
     * Returns the API key for the specified vendor.
     *
     * @param vendor the vendor to get the API key for
     * @return the API key, or empty if not configured
     */
    Optional<String> getApiKey(Vendor vendor);

    /**
     * Returns the API key for the specified vendor, throwing an exception if not found.
     *
     * @param vendor the vendor to get the API key for
     * @return the API key
     * @throws IllegalStateException if the API key is not configured
     */
    default String getApiKeyOrThrow(Vendor vendor) {
        return getApiKey(vendor)
                .orElseThrow(() -> new IllegalStateException(
                        "API key for " + vendor.friendlyName() + " is not configured. " +
                        "Please configure it using the appropriate method for your ApiConfig implementation."
                ));
    }

    /**
     * Convenience method to get the Anthropic API key.
     *
     * @return the Anthropic API key, or empty if not configured
     */
    default Optional<String> getAnthropicApiKey() {
        return getApiKey(Vendor.ANTHROPIC);
    }

    /**
     * Convenience method to get the OpenAI API key.
     *
     * @return the OpenAI API key, or empty if not configured
     */
    default Optional<String> getOpenAIApiKey() {
        return getApiKey(Vendor.OPENAI);
    }

    /**
     * Convenience method to get the Google API key.
     *
     * @return the Google API key, or empty if not configured
     */
    default Optional<String> getGoogleApiKey() {
        return getApiKey(Vendor.GOOGLE);
    }
}
