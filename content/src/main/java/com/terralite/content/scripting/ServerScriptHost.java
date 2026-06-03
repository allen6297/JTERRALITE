package com.terralite.content.scripting;

import com.terralite.content.pack.ContentPack;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ServerScriptHost {
    private final ScriptContentScanner scanner;
    private final ServerWorldScriptApi worldApi;
    private final List<ScriptExecutionMessage> messages = new ArrayList<>();
    private final List<TickHandler> tickHandlers = new ArrayList<>();
    private int executedScripts;

    public ServerScriptHost() {
        this(new ScriptContentScanner(), null);
    }

    public ServerScriptHost(ScriptContentScanner scanner) {
        this(scanner, null);
    }

    public ServerScriptHost(ScriptContentScanner scanner, ServerWorldScriptApi worldApi) {
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        this.worldApi = worldApi;
    }

    public ScriptExecutionReport load(List<ContentPack> packs) throws IOException {
        Objects.requireNonNull(packs, "packs");

        messages.clear();
        tickHandlers.clear();
        executedScripts = 0;

        for (ContentPack pack : packs) {
            for (ScriptContentFile script : scanner.scan(pack)) {
                if (script.scope() == ScriptScope.SERVER) {
                    load(script);
                    executedScripts++;
                }
            }
        }

        return report();
    }

    public void tick(long index, Duration delta, Duration totalTime) {
        Objects.requireNonNull(delta, "delta");
        Objects.requireNonNull(totalTime, "totalTime");

        for (TickHandler tickHandler : tickHandlers) {
            Context context = Context.enter();
            try {
                context.setOptimizationLevel(-1);
                context.setClassShutter(className -> className.startsWith("com.terralite."));
                NativeObject tick = new NativeObject();
                ScriptableObject.putProperty(tick, "index", index);
                ScriptableObject.putProperty(tick, "deltaMillis", delta.toMillis());
                ScriptableObject.putProperty(tick, "totalMillis", totalTime.toMillis());
                tickHandler.function().call(context, tickHandler.scope(), tickHandler.scope(), new Object[]{tick});
            } catch (RhinoException exception) {
                throw new ScriptExecutionException("Failed to execute server tick handler: "
                        + tickHandler.script().path(), exception);
            } finally {
                Context.exit();
            }
        }
    }

    public ScriptExecutionReport report() {
        return new ScriptExecutionReport(executedScripts, messages);
    }

    private void load(ScriptContentFile script) throws IOException {
        String source = Files.readString(script.path());
        Context context = Context.enter();
        try {
            context.setOptimizationLevel(-1);
            context.setClassShutter(className -> className.startsWith("com.terralite."));
            Scriptable scope = context.initStandardObjects();
            ScriptRuntimeApi api = new ScriptRuntimeApi(
                    script.scope(),
                    script.path(),
                    messages,
                    function -> tickHandlers.add(new TickHandler(script, scope, function)),
                    worldApi
            );
            ScriptableObject.putProperty(scope, "api", Context.javaToJS(api, scope));
            context.evaluateString(scope, source, script.path().toString(), 1, null);
        } catch (RhinoException exception) {
            throw new ScriptExecutionException("Failed to execute server script: " + script.path(), exception);
        } finally {
            Context.exit();
        }
    }

    private record TickHandler(ScriptContentFile script, Scriptable scope, Function function) {
        private TickHandler {
            Objects.requireNonNull(script, "script");
            Objects.requireNonNull(scope, "scope");
            Objects.requireNonNull(function, "function");
        }
    }
}
