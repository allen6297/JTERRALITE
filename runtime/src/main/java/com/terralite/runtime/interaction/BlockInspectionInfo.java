package com.terralite.runtime.interaction;

import com.terralite.core.registry.GameData;
import com.terralite.core.registry.ResourceId;
import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockState;
import com.terralite.engine.world.World;
import com.terralite.game.block.Block;
import com.terralite.game.item.Item;
import com.terralite.game.registry.TerraliteRegistries;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class BlockInspectionInfo {
    private BlockInspectionInfo() {
    }

    public static String title(World world, GameData gameData, BlockPos pos) {
        return "TERRALITE - Inspect: " + String.join(" | ", lines(world, gameData, pos));
    }

    public static List<String> lines(World world, GameData gameData, BlockPos pos) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(gameData, "gameData");
        Objects.requireNonNull(pos, "pos");

        BlockState state = world.getBlock(pos);
        ResourceId blockId = state.id();
        Block block = gameData.registry(TerraliteRegistries.BLOCKS).get(blockId);

        String displayName = block == null || block.properties().displayName().isBlank()
                ? prettify(blockId.path())
                : block.properties().displayName();

        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        lines.add(displayName);
        lines.add(blockId.toString());

        if (block != null) {
            var properties = block.properties();
            lines.add(properties.material()
                    + "  H " + trim(properties.hardness())
                    + "  R " + trim(properties.resistance()));
            if (properties.requiresTool()) {
                lines.add("Requires tool");
            }
            if (!state.properties().isEmpty()) {
                lines.add("State " + state.properties());
            }
        }

        List<String> placingItems = gameData.registry(TerraliteRegistries.ITEMS).ids().stream()
                .filter(itemId -> placesBlock(gameData.registry(TerraliteRegistries.ITEMS).require(itemId), blockId))
                .map(ResourceId::toString)
                .toList();
        if (!placingItems.isEmpty()) {
            lines.add("Use: " + compactId(placingItems.get(0)));
        }

        return List.copyOf(lines);
    }

    private static boolean placesBlock(Item item, ResourceId blockId) {
        String placesBlock = item.properties().placesBlock();
        return placesBlock != null && placesBlock.equals(blockId.toString());
    }

    private static String compactId(String id) {
        int slash = id.lastIndexOf('/');
        return slash >= 0 ? id.substring(slash + 1) : id;
    }

    private static String trim(float value) {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String prettify(String path) {
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        String[] words = name.split("_+");
        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                words[i] = Character.toUpperCase(words[i].charAt(0)) + words[i].substring(1);
            }
        }
        return String.join(" ", words);
    }
}
