package com.pergamon.llm.conversation;

public sealed interface MessageBlock
    permits TextBlock, UnknownBlock {
    
}
