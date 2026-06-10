package com.terralite.content.assets.model;

import java.util.List;
import java.util.Objects;

/**
 * A named, pivot-anchored group of vertices within a {@link ContentModelMesh}.
 *
 * <p>Corresponds to one {@code element} in a Blockbench {@code .bbmodel} file.
 * The pivot point (in block units, Y=0 at model feet) is the rotation origin used
 * when animating the bone.
 */
public record ContentModelBone(
        String name,
        float pivotX,
        float pivotY,
        float pivotZ,
        List<ContentModelVertex> vertices
) {
    public ContentModelBone {
        Objects.requireNonNull(name, "name");
        vertices = List.copyOf(Objects.requireNonNull(vertices, "vertices"));
    }
}
