package com.terralite.content.scripting;

import com.terralite.content.pack.ContentPack;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public final class ServerScriptRunner {
    private final ServerScriptHost host;

    public ServerScriptRunner() {
        this(new ServerScriptHost());
    }

    public ServerScriptRunner(ScriptContentScanner scanner) {
        this(new ServerScriptHost(Objects.requireNonNull(scanner, "scanner")));
    }

    ServerScriptRunner(ServerScriptHost host) {
        this.host = Objects.requireNonNull(host, "host");
    }

    public ScriptExecutionReport run(List<ContentPack> packs) throws IOException {
        return host.load(packs);
    }
}
