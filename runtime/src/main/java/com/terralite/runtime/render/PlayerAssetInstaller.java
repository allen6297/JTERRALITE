package com.terralite.runtime.render;

import org.slf4j.Logger;
import com.terralite.core.logging.Loggers;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Bootstraps the default player skin into the content pack directory.
 *
 * <p>On first launch (or if the file is missing), it writes the generated
 * skin from {@link PlayerSkinGenerator} to
 * {@code <terralite-pack>/assets/textures/entity/player.png}.
 *
 * <p>After the file exists on disk, users can replace it with any 32×32 PNG and the
 * game will pick it up automatically via the normal {@code ContentAssetScanner} pipeline.
 */
public final class PlayerAssetInstaller {
    private static final Logger log = Loggers.get(PlayerAssetInstaller.class);

    /** Relative path inside any pack root that holds the player skin. */
    private static final String SKIN_RELATIVE =
            "assets" + File.separator + "textures" + File.separator + "entity" + File.separator + "player.png";

    private PlayerAssetInstaller() {}

    /**
     * Scans {@code packsRoot} for a pack named {@code terralite} and writes the default
     * skin PNG there if it does not already exist.
     *
     * @param packsRoot the root directory that contains all pack subdirectories
     */
    public static void bootstrap(Path packsRoot) {
        bootstrap(packsRoot == null ? null : packsRoot.toFile());
    }

    /**
     * Scans {@code packsRoot} for a pack named {@code terralite} and writes the default
     * skin PNG there if it does not already exist.
     *
     * @param packsRoot the root directory that contains all pack subdirectories
     */
    public static void bootstrap(File packsRoot) {
        if (packsRoot == null || !packsRoot.isDirectory()) {
            log.warn("PlayerAssetInstaller: packsRoot not found, skipping skin bootstrap");
            return;
        }

        File packDir = new File(packsRoot, "terralite");
        if (!packDir.isDirectory()) {
            log.warn("PlayerAssetInstaller: terralite pack not found at {}", packDir);
            return;
        }

        File skinFile = new File(packDir, SKIN_RELATIVE);
        if (skinFile.exists()) {
            log.debug("PlayerAssetInstaller: skin already present at {}", skinFile);
            return;
        }

        File parent = skinFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            log.warn("PlayerAssetInstaller: could not create directory {}", parent);
            return;
        }

        try {
            ImageIO.write(PlayerSkinGenerator.generate(), "PNG", skinFile);
            log.info("PlayerAssetInstaller: wrote default skin to {}", skinFile);
        } catch (IOException e) {
            log.warn("PlayerAssetInstaller: could not write skin PNG — {}", e.getMessage());
        }
    }
}
