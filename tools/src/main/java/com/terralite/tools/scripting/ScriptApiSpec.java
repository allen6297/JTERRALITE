package com.terralite.tools.scripting;

import java.util.List;
import java.util.Objects;

public record ScriptApiSpec(
        List<TypeAlias> aliases,
        List<Global> globals,
        List<Interface> interfaces
) {
    public ScriptApiSpec {
        aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
        globals = List.copyOf(Objects.requireNonNull(globals, "globals"));
        interfaces = List.copyOf(Objects.requireNonNull(interfaces, "interfaces"));
    }

    public static ScriptApiSpec create() {
        return new ScriptApiSpec(
                List.of(new TypeAlias(
                        "ResourceId",
                        "`${string}:${string}`",
                        "Namespaced Terralite resource id, such as `terralite:block/stone`."
                )),
                List.of(
                        new Global("api", "ScriptRuntimeApi", "Runtime API available to server-side scripts."),
                        new Global(
                                "StartupEvents",
                                "StartupEventsApi",
                                "Startup-only event API used to register content before game data freezes."
                        ),
                        new Global(
                                "Registry",
                                "RegistryApi",
                                "Startup-only registry API used to inspect or mutate content before freeze."
                        )
                ),
                List.of(
                        iface("ScriptRuntimeApi",
                                method("info", "void", "Record an informational script diagnostic.", param("message", "string")),
                                method("onTick", "void", "Run a handler once per authoritative server tick.",
                                        param("handler", "(tick: ServerTick) => void")),
                                method("world", "ServerWorldApi", "Access world operations exposed to server scripts.")
                        ),
                        iface("ServerTick",
                                property("index", "number", "Monotonic tick index.", true),
                                property("deltaMillis", "number", "Milliseconds elapsed during this tick.", true),
                                property("totalMillis", "number", "Milliseconds elapsed since the server script host started.", true)
                        ),
                        iface("ServerWorldApi",
                                method("entityCount", "number", "Number of entities currently known to the script world view."),
                                method("chunkCount", "number", "Number of loaded chunks currently known to the script world view."),
                                method("hasChunk", "boolean", "Returns true when the chunk at the given chunk coordinates is loaded.",
                                        param("x", "number"), param("y", "number"), param("z", "number")),
                                method("loadChunk", "boolean", "Load or create the chunk at the given chunk coordinates.",
                                        param("x", "number"), param("y", "number"), param("z", "number")),
                                method("unloadChunk", "boolean", "Unload the chunk at the given chunk coordinates.",
                                        param("x", "number"), param("y", "number"), param("z", "number"))
                        ),
                        iface("StartupEventsApi",
                                registryMethod("block", "BlockRegistryEvent", "Register or modify block content during startup."),
                                registryMethod("item", "ItemRegistryEvent", "Register or modify item content during startup."),
                                registryMethod("biome", "BiomeRegistryEvent", "Register or modify biome content during startup."),
                                registryMethod("tag", "TagRegistryEvent", "Register or modify tag content during startup."),
                                registryMethod(
                                        "creative_category",
                                        "CreativeCategoryRegistryEvent",
                                        "Register or modify creative category content during startup."
                                )
                        ),
                        registryEvent("BlockRegistryEvent", "BlockBuilder", "Create a block with the given resource id."),
                        registryEvent("ItemRegistryEvent", "ItemBuilder", "Create an item with the given resource id."),
                        registryEvent("BiomeRegistryEvent", "BiomeBuilder", "Create a biome with the given resource id."),
                        registryEvent("TagRegistryEvent", "TagBuilder", "Create a tag with the given resource id."),
                        registryEvent(
                                "CreativeCategoryRegistryEvent",
                                "CreativeCategoryBuilder",
                                "Create a creative category with the given resource id."
                        ),
                        iface("BlockBuilder",
                                method("displayName", "this", "Set the player-facing block name.", param("value", "string")),
                                method("solid", "this", "Set whether this block has full solid collision.", param("value", "boolean")),
                                method("translucent", "this", "Set whether this block should be treated as transparent.", param("value", "boolean")),
                                method("material", "this", "Set the block material key, such as `stone`, `dirt`, or `plant`.", param("value", "string")),
                                method("hardness", "this", "Set break hardness; must be non-negative when content is built.", param("value", "number")),
                                method("resistance", "this", "Set explosion resistance; must be non-negative when content is built.", param("value", "number")),
                                method("requiresTool", "this", "Set whether the block requires an appropriate tool.", param("value", "boolean")),
                                method("soundType", "this", "Set the sound type key.", param("value", "string")),
                                method("category", "this", "Add this block to a creative category.", param("id", "ResourceId")),
                                method("tag", "this", "Add this block as a member of a tag.", param("id", "ResourceId")),
                                method("model", "this", "Placeholder for future block model path support. Currently chainable only.", param("path", "string")),
                                method("texture", "this", "Placeholder for future block texture path support. Currently chainable only.", param("path", "string")),
                                method("renderType", "this", "Placeholder for future block render type support. Currently chainable only.", param("type", "string")),
                                method("color", "this", "Placeholder for future block color support. Currently chainable only.",
                                        param("r", "number"), param("g", "number"), param("b", "number")),
                                method("opacity", "this", "Placeholder for future block opacity support. Currently chainable only.", param("value", "number")),
                                method("tintKey", "this", "Placeholder for future tint support. Currently chainable only.", param("value", "boolean")),
                                method("drops", "this", "Placeholder for future drop table support. Currently chainable only.", param("value", "unknown")),
                                method("property", "this", "Placeholder for future custom block properties. Currently chainable only.",
                                        param("key", "string"), param("value", "unknown")),
                                method("states", "this", "Placeholder for future block state definitions. Currently chainable only.", param("value", "unknown")),
                                method("variants", "this", "Placeholder for future block variant definitions. Currently chainable only.", param("value", "unknown"))
                        ),
                        iface("ItemBuilder",
                                method("displayName", "this", "Set the player-facing item name.", param("value", "string")),
                                method("stackSize", "this", "Set maximum stack size.", param("value", "number")),
                                method("weight", "this", "Set item weight for future inventory/balance systems.", param("value", "number")),
                                method("placesBlock", "this", "Set the block this item places.", param("id", "ResourceId")),
                                method("category", "this", "Add this item to a creative category.", param("id", "ResourceId")),
                                method("tag", "this", "Add this item as a member of a tag.", param("id", "ResourceId")),
                                method("icon", "this", "Placeholder for future item icon path support. Currently chainable only.", param("path", "string"))
                        ),
                        iface("BiomeBuilder",
                                method("name", "this", "Set the player-facing biome name.", param("value", "string")),
                                method("priority", "this", "Set biome selection priority.", param("value", "number")),
                                method("rarity", "this", "Set biome rarity weighting.", param("value", "number")),
                                method("temperature", "this", "Set inclusive temperature range for biome selection.",
                                        param("min", "number"), param("max", "number")),
                                method("humidity", "this", "Set inclusive humidity range for biome selection.",
                                        param("min", "number"), param("max", "number")),
                                method("terrain", "this", "Set base terrain height and variation.",
                                        param("baseHeight", "number"), param("heightVariation", "number")),
                                method("surfaceTop", "this", "Set the top surface block id.", param("id", "ResourceId")),
                                method("surfaceMiddle", "this", "Set the middle surface block id.", param("id", "ResourceId")),
                                method("surfaceMiddleDepth", "this", "Set the depth of the middle surface layer.", param("depth", "number")),
                                method("surfaceBase", "this", "Set the base surface block id.", param("id", "ResourceId"))
                        ),
                        iface("TagBuilder",
                                method("description", "this", "Set the tag description.", param("value", "string")),
                                method("member", "this", "Add a block or item member id.", param("id", "ResourceId"))
                        ),
                        iface("CreativeCategoryBuilder",
                                method("title", "this", "Set the player-facing category title.", param("value", "string")),
                                method("icon", "this", "Set the block or item id used as the category icon.", param("id", "ResourceId")),
                                method("entry", "this", "Add a block or item id to the category ordering.", param("id", "ResourceId"))
                        ),
                        iface("RegistryApi",
                                method("getBlock", "unknown", "Get a startup-registered block by id.", param("id", "ResourceId")),
                                method("getItem", "unknown", "Get a startup-registered item by id.", param("id", "ResourceId")),
                                method("getBiome", "unknown", "Get a startup-registered biome by id.", param("id", "ResourceId")),
                                method("modifyBlock", "void", "Modify a registered block before registries freeze.",
                                        param("id", "ResourceId"), param("handler", "(block: BlockModifier) => void"))
                        ),
                        iface("BlockModifier",
                                property("displayName", "string", "Player-facing block name.", false),
                                property("hardness", "number", "Break hardness.", false),
                                property("resistance", "number", "Explosion resistance.", false),
                                property("solid", "boolean", "Whether this block has full solid collision.", false),
                                property("transparent", "boolean", "Whether this block should be treated as transparent.", false),
                                property("requiresTool", "boolean", "Whether this block requires an appropriate tool.", false),
                                property("material", "string", "Block material key.", false),
                                property("soundType", "string", "Block sound type key.", false)
                        )
                )
        );
    }

    private static Interface iface(String name, Member... members) {
        return new Interface(name, List.of(members));
    }

    private static Interface registryEvent(String name, String builderType, String doc) {
        return iface(name, method("create", builderType, doc, param("id", "ResourceId")));
    }

    private static Method registryMethod(String type, String eventType, String doc) {
        return method(
                "registry",
                "void",
                doc,
                param("type", "'" + type + "'"),
                param("handler", "(event: " + eventType + ") => void")
        );
    }

    private static Method method(String name, String returnType, String doc, Param... params) {
        return new Method(name, List.of(params), returnType, doc);
    }

    private static Param param(String name, String type) {
        return new Param(name, type);
    }

    private static Property property(String name, String type, String doc, boolean readonly) {
        return new Property(name, type, doc, readonly);
    }

    public sealed interface Member permits Method, Property {
        String doc();
    }

    public record TypeAlias(String name, String value, String doc) {
    }

    public record Global(String name, String type, String doc) {
    }

    public record Interface(String name, List<Member> members) {
        public Interface {
            members = List.copyOf(Objects.requireNonNull(members, "members"));
        }
    }

    public record Method(String name, List<Param> params, String returnType, String doc) implements Member {
        public Method {
            params = List.copyOf(Objects.requireNonNull(params, "params"));
        }
    }

    public record Property(String name, String type, String doc, boolean readonly) implements Member {
    }

    public record Param(String name, String type) {
    }
}
