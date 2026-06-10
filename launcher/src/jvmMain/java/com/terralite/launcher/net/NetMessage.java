package com.terralite.launcher.net;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "t")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ChunkDataMessage.class,      name = "chunk"),
    @JsonSubTypes.Type(value = BlockChangeMessage.class,    name = "block"),
    @JsonSubTypes.Type(value = SetBlockMessage.class,       name = "set"),
    @JsonSubTypes.Type(value = RemoveBlockMessage.class,    name = "remove"),
    @JsonSubTypes.Type(value = PlayerMoveMessage.class,     name = "move"),
    @JsonSubTypes.Type(value = PlayerJoinMessage.class,     name = "pjoin"),
    @JsonSubTypes.Type(value = PlayerLeaveMessage.class,    name = "pleave"),
    @JsonSubTypes.Type(value = PlayerPositionMessage.class, name = "ppos"),
})
public sealed interface NetMessage
        permits ChunkDataMessage, BlockChangeMessage, SetBlockMessage, RemoveBlockMessage,
                PlayerMoveMessage, PlayerJoinMessage, PlayerLeaveMessage, PlayerPositionMessage {}
