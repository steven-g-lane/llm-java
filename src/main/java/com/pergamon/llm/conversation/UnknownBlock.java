package com.pergamon.llm.conversation;

/**
 * A fallback MessageBlock used when the library encounters vendor-specific
 * content that it does not yet understand or does not map to a known Block type.
 *
 * The vendorData field contains the raw vendor-specific JSON representation.
 */
public record UnknownBlock(String vendorData) implements MessageBlock {

    public UnknownBlock {
        if (vendorData == null || vendorData.isBlank()) {
            throw new IllegalArgumentException("vendorData cannot be null or empty");
        }
    }

    @Override
    public String toString() {
        return "UnknownBlock(vendorData=" + vendorData + ")";
    }
}
