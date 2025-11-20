package com.pergamon.llm.config;

import com.pergamon.llm.conversation.Vendor;

import java.util.Optional;

/**
 * API configuration that loads keys from environment variables.
 *
 * Expected environment variables:
 * - ANTHROPIC_API_KEY for Anthropic/Claude
 * - OPENAI_API_KEY for OpenAI/GPT
 * - GOOGLE_API_KEY for Google/Gemini
 */
public class EnvironmentApiConfig implements ApiConfig {

    private static final String ANTHROPIC_ENV_VAR = "ANTHROPIC_API_KEY";
    private static final String OPENAI_ENV_VAR = "OPENAI_API_KEY";
    private static final String GOOGLE_ENV_VAR = "GOOGLE_API_KEY";

    @Override
    public Optional<String> getApiKey(Vendor vendor) {
        String envVar = switch (vendor) {
            case ANTHROPIC -> ANTHROPIC_ENV_VAR;
            case OPENAI -> OPENAI_ENV_VAR;
            case GOOGLE -> GOOGLE_ENV_VAR;
        };

        String value = System.getenv(envVar);
        return (value != null && !value.isBlank())
                ? Optional.of(value)
                : Optional.empty();
    }
}
