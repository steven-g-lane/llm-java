package com.pergamon.llm.conversation;

import java.util.Map;
import java.util.Optional;

public final class ModelRegistry {

    private final Map<ModelId, ModelDescriptor> byId;

    public ModelRegistry(Map<ModelId, ModelDescriptor> byId) {
        this.byId = Map.copyOf(byId);
    }

    public Optional<ModelDescriptor> find(ModelId id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<ModelDescriptor> find(Vendor vendor, String slug) {
        return find(new ModelId(vendor, slug));
    }
}
