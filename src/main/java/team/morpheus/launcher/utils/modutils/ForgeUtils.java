package team.morpheus.launcher.utils.modutils;

import team.morpheus.launcher.Launcher;
import team.morpheus.launcher.Main;
import team.morpheus.launcher.model.modloaders.ForgeProduct;
import team.morpheus.launcher.utils.modutils.commons.InstallerFiles;
import team.morpheus.launcher.utils.modutils.commons.LoaderVersions;
import team.morpheus.launcher.utils.modutils.commons.ModLoaderInstaller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches Forge's catalog and delegates installation to the shared profile executor.
 */
public class ForgeUtils {

    public static void doForgeSetup(String requested, File jsonFile) throws IOException, InterruptedException {
        ForgeProduct catalog = InstallerFiles.readJson(new URL(Main.getForgeVersionsURL()), ForgeProduct.class);
        String version = selectVersion(requested, catalog);
        URL url = new URL(String.format("%s%s/forge-%s-installer.jar", Main.getForgeInstallerURL(), version, version));
        File installer = InstallerFiles.downloadInstaller(url, "net.minecraftforge:forge:" + version + ":installer", null);
        new ModLoaderInstaller(installer, Launcher.env.getGameFolder()).install(jsonFile);
    }

    static String selectVersion(String requested, ForgeProduct catalog) throws IOException {
        LoaderVersions.Request request = LoaderVersions.parse(requested, "forge");
        if (request.minecraft == null) throw new IOException("Specify a Minecraft version for Forge");
        List<String> available = catalog.get(request.minecraft);
        if (available == null) throw new IOException("No Forge versions for Minecraft " + request.minecraft);
        String prefix = request.minecraft + "-";
        String suffix = "-" + request.minecraft;
        String wanted = request.version;
        if (wanted != null && wanted.endsWith(suffix)) wanted = wanted.substring(0, wanted.length() - suffix.length());
        List<String> matches = new ArrayList<>();
        for (String full : available) {
            if (!full.startsWith(prefix)) continue;
            String version = full.substring(prefix.length());
            // Maven can repeat the Minecraft version after the loader build.
            if (version.endsWith(suffix)) version = version.substring(0, version.length() - suffix.length());
            if (wanted == null || version.equals(wanted) || version.startsWith(wanted + "."))
                matches.add(full);
        }
        return LoaderVersions.latest(matches, requested);
    }
}
