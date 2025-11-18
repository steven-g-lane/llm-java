package com.pergamon.llm.conversation;

public record ModelCapabilities(
        boolean supportsChat,
        boolean supportsTools,
        boolean supportsImages,
        boolean supportsFiles,
        boolean supportsStreaming,
        int contextWindowTokens,
        int maxOutputTokens
) {}


