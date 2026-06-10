package com.terralite.launcher.net;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public final class MessageCodec {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MessageCodec() {}

    public static String encode(NetMessage msg) throws IOException {
        return MAPPER.writeValueAsString(msg);
    }

    public static NetMessage decode(String json) throws IOException {
        return MAPPER.readValue(json, NetMessage.class);
    }
}
