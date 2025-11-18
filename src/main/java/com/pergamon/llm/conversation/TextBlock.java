package com.pergamon.llm.conversation;

public record TextBlock(TextBlockFormat format, String text) implements MessageBlock {}
