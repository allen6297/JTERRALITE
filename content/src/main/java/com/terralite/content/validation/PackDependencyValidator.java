package com.terralite.content.validation;

import com.terralite.content.pack.ContentPack;
import com.terralite.core.registry.ResourceId;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class PackDependencyValidator {
    public static final int SUPPORTED_FORMAT_VERSION = 1;

    public ContentValidationResult validate(List<ContentPack> packs) {
        Objects.requireNonNull(packs, "packs");

        Set<ResourceId> loadedPackIds = new LinkedHashSet<>();
        List<ContentValidationIssue> issues = new ArrayList<>();

        for (ContentPack pack : packs) {
            ResourceId packId = pack.manifest().id();
            if (pack.manifest().formatVersion() != SUPPORTED_FORMAT_VERSION) {
                issues.add(ContentValidationIssue.of(
                        "pack.format.unsupported",
                        "Pack " + packId + " uses unsupported format version "
                                + pack.manifest().formatVersion()
                                + "; supported version is " + SUPPORTED_FORMAT_VERSION
                ));
            }
            if (!loadedPackIds.add(packId)) {
                issues.add(ContentValidationIssue.of(
                        "pack.duplicate",
                        "Duplicate content pack id: " + packId
                ));
            }
        }

        for (ContentPack pack : packs) {
            ResourceId packId = pack.manifest().id();
            pack.manifest().dependencies().stream()
                    .filter(dependency -> !dependency.optional())
                    .filter(dependency -> !loadedPackIds.contains(dependency.id()))
                    .map(dependency -> ContentValidationIssue.of(
                            "pack.dependency.missing",
                            "Pack " + packId + " requires missing pack " + dependency.id()
                    ))
                    .forEach(issues::add);
        }

        return new ContentValidationResult(issues);
    }
}
