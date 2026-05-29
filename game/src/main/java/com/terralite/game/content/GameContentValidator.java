package com.terralite.game.content;

import com.terralite.content.validation.ContentValidationIssue;
import com.terralite.content.validation.ContentValidationResult;
import com.terralite.core.registry.FrozenRegistry;
import com.terralite.core.registry.GameData;
import com.terralite.core.registry.RegistryKey;
import com.terralite.core.registry.ResourceId;
import com.terralite.game.block.Block;
import com.terralite.game.category.CreativeCategory;
import com.terralite.game.item.Item;
import com.terralite.game.registry.TerraliteRegistries;
import com.terralite.game.tag.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GameContentValidator {
    public ContentValidationResult validate(GameData gameData) {
        Objects.requireNonNull(gameData, "gameData");

        List<ContentValidationIssue> issues = new ArrayList<>();
        FrozenRegistry<Block> blocks = registryOrNull(gameData, TerraliteRegistries.BLOCKS, issues);
        FrozenRegistry<Item> items = registryOrNull(gameData, TerraliteRegistries.ITEMS, issues);
        FrozenRegistry<Tag> tags = registryOrNull(gameData, TerraliteRegistries.TAGS, issues);
        FrozenRegistry<CreativeCategory> categories =
                registryOrNull(gameData, TerraliteRegistries.CREATIVE_CATEGORIES, issues);

        validateItemPlacesBlock(items, blocks, issues);
        validateTagMembers(tags, blocks, items, issues);

        if (categories != null) {
            validateBlockCategories(blocks, categories, issues);
            validateItemCategories(items, categories, issues);
            validateCategoryEntries(blocks, items, categories, issues);
        }

        return new ContentValidationResult(issues);
    }

    private static void validateTagMembers(
            FrozenRegistry<Tag> tags,
            FrozenRegistry<Block> blocks,
            FrozenRegistry<Item> items,
            List<ContentValidationIssue> issues
    ) {
        if (tags == null) {
            return;
        }

        for (ResourceId tagId : tags.ids()) {
            Tag tag = tags.require(tagId);
            for (ResourceId memberId : tag.members()) {
                if (!contains(blocks, memberId) && !contains(items, memberId)) {
                    issues.add(ContentValidationIssue.of(
                            "tag.member.missing",
                            "Tag " + tagId + " references missing block or item " + memberId
                    ));
                }
            }
        }
    }

    private static void validateItemPlacesBlock(
            FrozenRegistry<Item> items,
            FrozenRegistry<Block> blocks,
            List<ContentValidationIssue> issues
    ) {
        if (items == null) {
            return;
        }

        for (ResourceId itemId : items.ids()) {
            Item item = items.require(itemId);
            String placesBlock = item.properties().placesBlock();
            if (placesBlock != null && !placesBlock.isBlank()) {
                ResourceId blockId = ResourceId.id(placesBlock);
                if (!contains(blocks, blockId)) {
                    issues.add(ContentValidationIssue.of(
                            "item.places_block.missing",
                            "Item " + itemId + " references missing block " + blockId + " in placesBlock"
                    ));
                }
            }
        }
    }

    private static void validateBlockCategories(
            FrozenRegistry<Block> blocks,
            FrozenRegistry<CreativeCategory> categories,
            List<ContentValidationIssue> issues
    ) {
        if (blocks == null) {
            return;
        }

        for (ResourceId blockId : blocks.ids()) {
            Block block = blocks.require(blockId);
            for (ResourceId categoryId : block.properties().categories()) {
                if (!categories.contains(categoryId)) {
                    issues.add(ContentValidationIssue.of(
                            "block.category.missing",
                            "Block " + blockId + " references missing creative category " + categoryId
                    ));
                }
            }
        }
    }

    private static void validateItemCategories(
            FrozenRegistry<Item> items,
            FrozenRegistry<CreativeCategory> categories,
            List<ContentValidationIssue> issues
    ) {
        if (items == null) {
            return;
        }

        for (ResourceId itemId : items.ids()) {
            Item item = items.require(itemId);
            for (ResourceId categoryId : item.properties().categories()) {
                if (!categories.contains(categoryId)) {
                    issues.add(ContentValidationIssue.of(
                            "item.category.missing",
                            "Item " + itemId + " references missing creative category " + categoryId
                    ));
                }
            }
        }
    }

    private static void validateCategoryEntries(
            FrozenRegistry<Block> blocks,
            FrozenRegistry<Item> items,
            FrozenRegistry<CreativeCategory> categories,
            List<ContentValidationIssue> issues
    ) {
        for (ResourceId categoryId : categories.ids()) {
            CreativeCategory category = categories.require(categoryId);
            if (!contains(blocks, category.icon()) && !contains(items, category.icon())) {
                issues.add(ContentValidationIssue.of(
                        "category.icon.missing",
                        "Creative category " + categoryId + " references missing icon " + category.icon()
                ));
            }

            for (ResourceId entryId : category.entries()) {
                if (!contains(blocks, entryId) && !contains(items, entryId)) {
                    issues.add(ContentValidationIssue.of(
                            "category.entry.missing",
                            "Creative category " + categoryId + " references missing entry " + entryId
                    ));
                }
            }
        }
    }

    private static boolean contains(FrozenRegistry<?> registry, ResourceId id) {
        return registry != null && registry.contains(id);
    }

    private static <T> FrozenRegistry<T> registryOrNull(
            GameData gameData,
            RegistryKey<T> key,
            List<ContentValidationIssue> issues
    ) {
        try {
            return gameData.registry(key);
        } catch (IllegalArgumentException exception) {
            issues.add(ContentValidationIssue.of(
                    "registry.missing",
                    "Missing game registry: " + key.id()
            ));
            return null;
        }
    }
}
