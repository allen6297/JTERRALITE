package com.terralite.content.scripting;

import com.terralite.content.pack.ContentPack;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class StartupScriptRunner {
    private final ScriptContentScanner scanner;

    public StartupScriptRunner() {
        this(new ScriptContentScanner());
    }

    public StartupScriptRunner(ScriptContentScanner scanner) {
        this.scanner = Objects.requireNonNull(scanner, "scanner");
    }

    public ScriptExecutionReport run(List<ContentPack> packs) throws IOException {
        Objects.requireNonNull(packs, "packs");

        int executedScripts = 0;
        List<ScriptExecutionMessage> messages = new ArrayList<>();
        for (ContentPack pack : packs) {
            for (ScriptContentFile script : scanner.scan(pack)) {
                if (script.scope() == ScriptScope.STARTUP) {
                    execute(script, messages);
                    executedScripts++;
                }
            }
        }

        return new ScriptExecutionReport(executedScripts, messages);
    }

    private static void execute(ScriptContentFile script, List<ScriptExecutionMessage> messages) throws IOException {
        String source = Files.readString(script.path());
        Context context = Context.enter();
        try {
            context.setOptimizationLevel(-1);
            Scriptable scope = context.initStandardObjects();
            ScriptRuntimeApi api = new ScriptRuntimeApi(script.scope(), script.path(), messages);
            ScriptableObject.putProperty(scope, "api", Context.javaToJS(api, scope));
            context.evaluateString(scope, source, script.path().toString(), 1, null);
        } catch (RhinoException exception) {
            throw new ScriptExecutionException("Failed to execute startup script: " + script.path(), exception);
        } finally {
            Context.exit();
        }
    }
}
