package com.terralite.game.category;

import com.terralite.core.registry.ResourceId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record CreativeCategory(String title, ResourceId icon, List<ResourceId> entries) {
    public CreativeCategory {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Category title cannot be blank");
        }
        Objects.requireNonNull(icon, "icon");
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title = "Category";
        private ResourceId icon = ResourceId.id("terralite:air");
        private final List<ResourceId> entries = new ArrayList<>();

        private Builder() {
        }

        public Builder title(String title) {
            this.title = Objects.requireNonNull(title, "title");
            return this;
        }

        public Builder icon(ResourceId icon) {
            this.icon = Objects.requireNonNull(icon, "icon");
            return this;
        }

        public Builder icon(String icon) {
            return icon(ResourceId.id(icon));
        }

        public Builder entry(ResourceId entry) {
            entries.add(Objects.requireNonNull(entry, "entry"));
            return this;
        }

        public Builder entry(String entry) {
            return entry(ResourceId.id(entry));
        }

        public Builder entries(Collection<ResourceId> entries) {
            this.entries.clear();
            this.entries.addAll(Objects.requireNonNull(entries, "entries"));
            return this;
        }

        public CreativeCategory build() {
            return new CreativeCategory(title, icon, List.copyOf(entries));
        }
    }
}
