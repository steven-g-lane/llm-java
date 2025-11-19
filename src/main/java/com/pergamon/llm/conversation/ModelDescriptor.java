package com.pergamon.llm.conversation;

public record ModelDescriptor(
        ModelId id,
        String friendlyName,
        String apiModelName,
        ModelCapabilities capabilitie
){}