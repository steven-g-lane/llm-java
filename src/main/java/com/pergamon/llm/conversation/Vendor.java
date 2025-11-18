package com.pergamon.llm.conversation;

public enum Vendor {
    OPENAI("OpenAI", "openai", "https://openai.com/"),
    ANTHROPIC("Anthropic", "anthropic", "https://www.anthropic.com/"),
    GOOGLE("Google", "google", "https://ai.google/");

    private final String friendlyName;
    private final String slug;
    private final String website;

    Vendor(String friendlyName, String slug, String website) {
        this.friendlyName = friendlyName;
        this.slug = slug;
        this.website = website;
    }

    public String friendlyName() { return friendlyName; }
    public String slug()         { return slug; }
    public String website()      { return website; }

    @Override
    public String toString() {
        return friendlyName;
    }
}