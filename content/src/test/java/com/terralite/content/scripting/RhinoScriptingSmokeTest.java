package com.terralite.content.scripting;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RhinoScriptingSmokeTest {
    @Test
    void exposesAnnotatedJavaFunctionsToScripts() throws Exception {
        Context context = Context.enter();
        try {
            Scriptable scope = context.initStandardObjects();
            ScriptableObject.defineClass(scope, CounterApi.class);

            Object result = context.evaluateString(
                scope,
                """
                const counter = new CounterApi();
                counter.increment();
                counter.increment();
                counter.resetCount();
                counter.getCount();
                """,
                "rhino-smoke-test.js",
                1,
                null
            );

            assertEquals(0, ((Number) Context.jsToJava(result, Number.class)).intValue());
        } finally {
            Context.exit();
        }
    }

    public static final class CounterApi extends ScriptableObject {
        private int count;

        @Override
        public String getClassName() {
            return "CounterApi";
        }

        @JSFunction
        public void increment() {
            count++;
        }

        @JSFunction
        public void resetCount() {
            count = 0;
        }

        @JSFunction
        public int getCount() {
            return count;
        }
    }
}
