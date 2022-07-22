package net.sorenon.physicality.physics_lib;

import com.jme3.system.JmeSystem;
import com.jme3.system.NativeLibraryLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.sorenon.physicality.PhysicalityMod;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;

/**
 * Facilitates copying of natives outside the jar so that LibBulletJME can load them.
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

            NativeLibraryLoader.loadLibbulletjme(true, nativesFolder.toFile(), "Release", "Sp");
        } catch (IOException | NoSuchElementException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to load bullet natives.");
        }
    }

    static String getPlatformSpecificName() {
        final var platform = JmeSystem.getPlatform();

        final var name = switch (platform) {
            case Windows32, Windows64 -> "bulletjme.dll";
            case Android_ARM7, Android_ARM8, Linux_ARM32, Linux_ARM64, Linux32, Linux64 -> "libbulletjme.so";
            case MacOSX32, MacOSX64, MacOSX_ARM64 -> "libbulletjme.dylib";
            default -> throw new RuntimeException("Invalid platform " + platform);
        };

        return platform + "Release" + "Sp" + "_" + name;
    }
}