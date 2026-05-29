package com.terralite.game.tag;

import com.terralite.core.registry.ResourceId;

import java.util.List;
import java.util.Objects;

public record Tag(String description, List<ResourceId> members) {
    public Tag {
        Objects.requireNonNull(description, "description");
        members = List.copyOf(Objects.requireNonNull(members, "members"));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String description = "";
        private final java.util.ArrayList<ResourceId> members = new java.util.ArrayList<>();

        private Builder() {}

        public Builder description(String description) {
            this.description = Objects.requireNonNull(description, "description");
            return this;
        }

        public Builder member(ResourceId member) {
            members.add(Objects.requireNonNull(member, "member"));
            return this;
        }

        public Builder member(String member) {
            return member(ResourceId.id(member));
        }

        public Tag build() {
            return new Tag(description, members);
        }
    }
}
