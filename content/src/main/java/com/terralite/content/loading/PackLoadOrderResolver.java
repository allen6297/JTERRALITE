package com.terralite.content.loading;

import com.terralite.content.manifest.PackDependency;
import com.terralite.content.pack.ContentPack;
import com.terralite.content.validation.ContentValidationException;
import com.terralite.content.validation.ContentValidationIssue;
import com.terralite.content.validation.ContentValidationResult;
import com.terralite.content.validation.PackDependencyValidator;
import com.terralite.core.registry.ResourceId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PackLoadOrderResolver {
    private final PackDependencyValidator dependencyValidator;

    public PackLoadOrderResolver() {
        this(new PackDependencyValidator());
    }

    public PackLoadOrderResolver(PackDependencyValidator dependencyValidator) {
        this.dependencyValidator = Objects.requireNonNull(dependencyValidator, "dependencyValidator");
    }

    public List<ContentPack> resolve(List<ContentPack> packs) {
        Objects.requireNonNull(packs, "packs");
        dependencyValidator.validate(packs).requireValid();

        Map<ResourceId, ContentPack> packsById = new LinkedHashMap<>();
        for (ContentPack pack : packs) {
            packsById.put(pack.manifest().id(), pack);
        }

        Map<ResourceId, VisitState> states = new LinkedHashMap<>();
        List<ContentPack> ordered = new ArrayList<>();
        List<ContentValidationIssue> issues = new ArrayList<>();

        for (ContentPack pack : packs) {
            visit(pack, packsById, states, ordered, issues);
        }

        if (!issues.isEmpty()) {
            throw new ContentValidationException(new ContentValidationResult(issues));
        }

        return List.copyOf(ordered);
    }

    private static void visit(
            ContentPack pack,
            Map<ResourceId, ContentPack> packsById,
            Map<ResourceId, VisitState> states,
            List<ContentPack> ordered,
            List<ContentValidationIssue> issues
    ) {
        ResourceId packId = pack.manifest().id();
        VisitState state = states.get(packId);
        if (state == VisitState.VISITED) {
            return;
        }
        if (state == VisitState.VISITING) {
            issues.add(ContentValidationIssue.of(
                    "pack.dependency.cycle",
                    "Content pack dependency cycle includes " + packId
            ));
            return;
        }

        states.put(packId, VisitState.VISITING);
        for (PackDependency dependency : pack.manifest().dependencies()) {
            ContentPack dependencyPack = packsById.get(dependency.id());
            if (dependencyPack != null) {
                visit(dependencyPack, packsById, states, ordered, issues);
            }
        }
        states.put(packId, VisitState.VISITED);
        ordered.add(pack);
    }

    private enum VisitState {
        VISITING,
        VISITED
    }
}
