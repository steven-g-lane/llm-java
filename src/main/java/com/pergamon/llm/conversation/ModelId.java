package com.pergamon.llm.conversation;

public record ModelId(Vendor vendor, String apiModelName) {

    @Override
    public String toString() {
        return vendor.slug() + ":" + apiModelName;
    }
}
