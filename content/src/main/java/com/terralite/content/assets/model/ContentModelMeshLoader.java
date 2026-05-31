package com.terralite.content.assets.model;

import com.terralite.content.assets.ContentModelAsset;
import com.terralite.content.assets.ContentModelFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

public final class ContentModelMeshLoader {
    private final ObjModelParser objModelParser;
    private final BlockbenchModelParser blockbenchModelParser;

    public ContentModelMeshLoader() {
        this(new ObjModelParser(), new BlockbenchModelParser());
    }

    public ContentModelMeshLoader(ObjModelParser objModelParser, BlockbenchModelParser blockbenchModelParser) {
        this.objModelParser = Objects.requireNonNull(objModelParser, "objModelParser");
        this.blockbenchModelParser = Objects.requireNonNull(blockbenchModelParser, "blockbenchModelParser");
    }

    public ContentModelMesh load(ContentModelAsset model) throws IOException {
        Objects.requireNonNull(model, "model");
        if (model.format() == ContentModelFormat.WAVEFRONT_OBJ) {
            try (var reader = Files.newBufferedReader(model.path())) {
                return objModelParser.parse(reader);
            }
        }
        if (model.format() == ContentModelFormat.BLOCKBENCH) {
            try (var reader = Files.newBufferedReader(model.path())) {
                return blockbenchModelParser.parse(reader);
            }
        }
        throw new UnsupportedOperationException("Model format is not mesh-loadable yet: " + model.format());
    }
}
