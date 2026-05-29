package com.terralite.content.manifest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.terralite.core.registry.ResourceId;

record PackDependencyDefinition(
        String id,
        @JsonProperty(defaultValue = "false")
        boolean optional
) {
    PackDependency toDependency() {
        return new PackDependency(ResourceId.id(id), optional);
    }
}
