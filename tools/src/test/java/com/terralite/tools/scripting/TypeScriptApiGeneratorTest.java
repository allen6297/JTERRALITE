package com.terralite.tools.scripting;

import com.terralite.content.scripting.ScriptRuntimeApi;
import com.terralite.content.scripting.ServerWorldScriptApi;
import com.terralite.game.scripting.BiomeScriptBuilder;
import com.terralite.game.scripting.BlockScriptBuilder;
import com.terralite.game.scripting.CreativeCategoryScriptBuilder;
import com.terralite.game.scripting.ItemScriptBuilder;
import com.terralite.game.scripting.RegistryScriptApi;
import com.terralite.game.scripting.StartupEventsScriptApi;
import com.terralite.game.scripting.TagScriptBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TypeScriptApiGeneratorTest {
    @TempDir
    Path tempDir;

    @Test
    void writesStartupScriptDeclarations() throws Exception {
        Path output = tempDir.resolve("terralite-scripting.d.ts");

        TypeScriptApiGenerator.generate(output);

        String declarations = Files.readString(output);
        assertTrue(declarations.contains("registry(type: 'creative_category'"));
        assertTrue(declarations.contains("tag(id: ResourceId): this;"));
        assertTrue(declarations.contains("interface ServerWorldApi"));
        assertTrue(declarations.contains("/** Set the block this item places. */"));
        assertTrue(declarations.contains("model(path: string): this;"));
        assertTrue(declarations.contains("loadChunk(x: number, y: number, z: number): boolean;"));
    }

    @Test
    void checkPassesWhenDeclarationsMatch() throws Exception {
        Path output = tempDir.resolve("terralite-scripting.d.ts");
        TypeScriptApiGenerator.generate(output);

        TypeScriptApiGenerator.check(output);
    }

    @Test
    void rejectsOutputOutsideWorkingDirectory() {
        assertThrows(IllegalArgumentException.class,
                () -> TypeScriptApiGenerator.resolveInsideWorkingDirectory("../outside.d.ts", tempDir));
    }

    @Test
    void staleCheckMessageMentionsUnixAndWindowsCommands() throws Exception {
        Path output = tempDir.resolve("terralite-scripting.d.ts");
        Files.writeString(output, "stale");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> TypeScriptApiGenerator.check(output));

        assertTrue(exception.getMessage().contains("./gradlew :tools:generateTypeScriptApi"));
        assertTrue(exception.getMessage().contains(".\\gradlew.bat :tools:generateTypeScriptApi"));
    }

    @Test
    void declarationsCoverPublicScriptApiMethods() {
        Map<String, List<DeclaredMethod>> declarations = parseDeclaredMethods(TypeScriptApiGenerator.declarations());

        List<ParityTarget> targets = List.of(
                new ParityTarget("StartupEventsApi", StartupEventsScriptApi.class, Set.of()),
                new ParityTarget("BlockRegistryEvent", StartupEventsScriptApi.BlockRegistryEvent.class, Set.of()),
                new ParityTarget("ItemRegistryEvent", StartupEventsScriptApi.ItemRegistryEvent.class, Set.of()),
                new ParityTarget("BiomeRegistryEvent", StartupEventsScriptApi.BiomeRegistryEvent.class, Set.of()),
                new ParityTarget("TagRegistryEvent", StartupEventsScriptApi.TagRegistryEvent.class, Set.of()),
                new ParityTarget("CreativeCategoryRegistryEvent", StartupEventsScriptApi.CreativeCategoryRegistryEvent.class, Set.of()),
                new ParityTarget("BlockBuilder", BlockScriptBuilder.class, Set.of("id")),
                new ParityTarget("ItemBuilder", ItemScriptBuilder.class, Set.of("id")),
                new ParityTarget("BiomeBuilder", BiomeScriptBuilder.class, Set.of("id")),
                new ParityTarget("TagBuilder", TagScriptBuilder.class, Set.of("id")),
                new ParityTarget("CreativeCategoryBuilder", CreativeCategoryScriptBuilder.class, Set.of("id")),
                new ParityTarget("RegistryApi", RegistryScriptApi.class, Set.of()),
                new ParityTarget("ScriptRuntimeApi", ScriptRuntimeApi.class, Set.of()),
                new ParityTarget("ServerWorldApi", ServerWorldScriptApi.class, Set.of())
        );

        List<String> missing = new ArrayList<>();
        for (ParityTarget target : targets) {
            for (Method method : publicDeclaredMethods(target.javaType()).toList()) {
                if (target.ignoredMethods().contains(method.getName())) {
                    continue;
                }
                DeclaredMethod expected = new DeclaredMethod(
                        method.getName(),
                        method.getParameterCount(),
                        expectedTypeName(target.interfaceName(), method.getReturnType())
                );
                if (!declarations.getOrDefault(target.interfaceName(), List.of()).contains(expected)) {
                    missing.add(target.interfaceName() + "." + expected);
                }
            }
        }

        assertTrue(missing.isEmpty(), () -> "Missing or stale TypeScript declarations: " + missing);
    }

    private static Stream<Method> publicDeclaredMethods(Class<?> type) {
        return Stream.of(type.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> !method.isSynthetic());
    }

    private static String expectedTypeName(String interfaceName, Class<?> type) {
        if (type == Void.TYPE) {
            return "void";
        }
        if (type == Boolean.TYPE || type == Boolean.class) {
            return "boolean";
        }
        if (type == Integer.TYPE || type == Long.TYPE || type == Float.TYPE || type == Double.TYPE
                || Number.class.isAssignableFrom(type)) {
            return "number";
        }
        if (type == String.class) {
            return "string";
        }
        if (type == ScriptRuntimeApi.class) {
            return "ScriptRuntimeApi";
        }
        if (type == ServerWorldScriptApi.class) {
            return "ServerWorldApi";
        }
        if (type == BlockScriptBuilder.class) {
            return interfaceName.equals("BlockBuilder") ? "this" : "BlockBuilder";
        }
        if (type == ItemScriptBuilder.class) {
            return interfaceName.equals("ItemBuilder") ? "this" : "ItemBuilder";
        }
        if (type == BiomeScriptBuilder.class) {
            return interfaceName.equals("BiomeBuilder") ? "this" : "BiomeBuilder";
        }
        if (type == TagScriptBuilder.class) {
            return interfaceName.equals("TagBuilder") ? "this" : "TagBuilder";
        }
        if (type == CreativeCategoryScriptBuilder.class) {
            return interfaceName.equals("CreativeCategoryBuilder") ? "this" : "CreativeCategoryBuilder";
        }
        return "unknown";
    }

    private static Map<String, List<DeclaredMethod>> parseDeclaredMethods(String declarations) {
        Pattern interfacePattern = Pattern.compile("interface\\s+(\\w+)\\s*\\{([\\s\\S]*?)\\n\\}");
        Pattern methodPattern = Pattern.compile("^\\s*(\\w+)\\((.*)\\):\\s*([^;]+);", Pattern.MULTILINE);
        return interfacePattern.matcher(declarations).results()
                .collect(java.util.stream.Collectors.toMap(
                        match -> match.group(1),
                        match -> methodPattern.matcher(match.group(2)).results()
                                .map(method -> new DeclaredMethod(
                                        method.group(1),
                                        topLevelParameterCount(method.group(2)),
                                        method.group(3).trim()
                                ))
                                .toList()
                ));
    }

    private static int topLevelParameterCount(String parameters) {
        String trimmed = parameters.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        int count = 1;
        int parenDepth = 0;
        for (int index = 0; index < trimmed.length(); index++) {
            char current = trimmed.charAt(index);
            if (current == '(') {
                parenDepth++;
            } else if (current == ')') {
                parenDepth--;
            } else if (current == ',' && parenDepth == 0) {
                count++;
            }
        }
        return count;
    }

    private record ParityTarget(String interfaceName, Class<?> javaType, Set<String> ignoredMethods) {
    }

    private record DeclaredMethod(String name, int parameterCount, String returnType) {
        @Override
        public String toString() {
            return name + "/" + parameterCount + " -> " + returnType;
        }
    }
}
