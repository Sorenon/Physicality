package net.sorenon.physicality.physics_lib.jni;

import net.fabricmc.loader.api.FabricLoader;
import net.sorenon.physicality.PhysicalityMod;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.util.NoSuchElementException;

/**
 * Facilitates copying of natives outside the jar so that we can load them.
 *
 * Code blatantly stolen from https://github.com/LazuriteMC/Rayon
 */
public class NativeLoader {
    public static void load() {
        final var fileName = getPlatformSpecificName();
        final var nativesFolder = FabricLoader.getInstance().getGameDir().resolve("natives/");
        final var url = NativeLoader.class.getResource("/assets/physicality/natives/" + fileName);

        try {
            if (!Files.exists(nativesFolder)) {
                Files.createDirectory(nativesFolder);
            }

            final var destination = nativesFolder.resolve(fileName);
            final var destinationFile = destination.toFile();

            if (Files.exists(destination)) {
                if (!destinationFile.delete()) {
                    PhysicalityMod.LOGGER.warn("Failed to remove old bullet natives.");
                }
            }

            try {
                FileUtils.copyURLToFile(url, destinationFile);
            } catch (IOException e) {
                PhysicalityMod.LOGGER.warn("Unable to copy natives.");
            }

            System.load(destinationFile.getAbsolutePath());
        } catch (IOException | NoSuchElementException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to load bullet natives.");
        }
    }

    static String getPlatformSpecificName() {
        return "mc_phys_jni.dll";
    }
}