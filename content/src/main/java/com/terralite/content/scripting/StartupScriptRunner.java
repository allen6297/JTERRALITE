package com.terralite.content.scripting;

import com.terralite.content.pack.ContentPack;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public final class StartupScriptRunner {
    private final ScriptScopeRunner scopeRunner;

    public StartupScriptRunner() {
        this(new ScriptContentScanner());
    }

    public StartupScriptRunner(ScriptContentScanner scanner) {
        this.scopeRunner = new ScriptScopeRunner(Objects.requireNonNull(scanner, "scanner"));
    }

    public ScriptExecutionReport run(List<ContentPack> packs) throws IOException {
        return run(packs, List.of());
    }

    public ScriptExecutionReport run(List<ContentPack> packs, List<StartupScriptGlobal> extraGlobals) throws IOException {
        return scopeRunner.run(packs, ScriptScope.STARTUP, extraGlobals);
    }
}
