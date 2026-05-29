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

final class ScriptScopeRunner {
    private final ScriptContentScanner scanner;

    ScriptScopeRunner(ScriptContentScanner scanner) {
        this.scanner = Objects.requireNonNull(scanner, "scanner");
    }

    ScriptExecutionReport run(List<ContentPack> packs, ScriptScope scope) throws IOException {
        return run(packs, scope, List.of());
    }

    ScriptExecutionReport run(List<ContentPack> packs, ScriptScope scope, List<StartupScriptGlobal> extraGlobals) throws IOException {
        Objects.requireNonNull(packs, "packs");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(extraGlobals, "extraGlobals");

        int executedScripts = 0;
        List<ScriptExecutionMessage> messages = new ArrayList<>();
        for (ContentPack pack : packs) {
            for (ScriptContentFile script : scanner.scan(pack)) {
                if (script.scope() == scope) {
                    execute(script, messages, extraGlobals);
                    executedScripts++;
                }
            }
        }

        return new ScriptExecutionReport(executedScripts, messages);
    }

    private static void execute(ScriptContentFile script, List<ScriptExecutionMessage> messages, List<StartupScriptGlobal> extraGlobals) throws IOException {
        String source = Files.readString(script.path());
        Context context = Context.enter();
        try {
            context.setOptimizationLevel(-1);
            Scriptable scope = context.initStandardObjects();
            ScriptRuntimeApi api = new ScriptRuntimeApi(script.scope(), script.path(), messages);
            ScriptableObject.putProperty(scope, "api", Context.javaToJS(api, scope));
            for (StartupScriptGlobal global : extraGlobals) {
                ScriptableObject.putProperty(scope, global.name(), Context.javaToJS(global.value(), scope));
            }
            context.evaluateString(scope, source, script.path().toString(), 1, null);
        } catch (RhinoException exception) {
            throw new ScriptExecutionException("Failed to execute " + script.scope().directoryName()
                    + " script: " + script.path(), exception);
        } finally {
            Context.exit();
        }
    }
}
