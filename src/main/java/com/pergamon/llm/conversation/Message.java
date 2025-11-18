package com.pergamon.llm.conversation;

import java.util.List;

public record Message(MessageRole role, List<MessageBlock> blocks) {}