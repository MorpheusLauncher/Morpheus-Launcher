package team.morpheus.launcher.utils.modutils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import team.morpheus.launcher.Launcher;
import team.morpheus.launcher.Main;
import team.morpheus.launcher.utils.Utils;
import team.morpheus.launcher.utils.modutils.commons.LoaderVersions;
import team.morpheus.launcher.utils.modutils.commons.ModLoaderInstaller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Forge version discovery; execution is shared with NeoForge.
 */
public class ForgeUtils {

    public static void doForgeSetup(String requested, File jsonFile) throws IOException, ParseException, InterruptedException {
        String metadata = Utils.makeGetRequest(new URL(Main.getForgeVersionsURL()));
        String version = selectVersion(requested, metadata);
        File installer = LoaderVersions.downloadInstaller(Main.getForgeInstallerURL(), "forge", version);
        doForgeUnpack(installer, jsonFile, version, Launcher.env.getGameFolder());
    }

    static String selectVersion(String requested, String metadata) throws ParseException, IOException {
        JSONObject versions = (JSONObject) new JSONParser().parse(metadata);
        String minecraft = LoaderVersions.minecraftVersion(requested);
        JSONArray available = (JSONArray) versions.get(minecraft);
        if (available == null) throw new IOException("No Forge versions for Minecraft " + minecraft);
        String wanted = LoaderVersions.loaderVersion(requested, "forge");
        List<String> matches = new ArrayList<>();
        for (Object entry : available) {
            String full = entry.toString();
            String prefix = minecraft + "-";
            if (!full.startsWith(prefix)) continue;
            String forge = full.substring(prefix.length());
            // Some legacy artifacts repeat the Minecraft version after the Forge build,
            // e.g. 1.8.9-11.15.1.2318-1.8.9.
            if (wanted == null || forge.equals(wanted) || forge.equals(wanted + "-" + minecraft)) matches.add(full);
        }
        return LoaderVersions.latest(matches, requested);
    }

    // Kept for callers that already have a Forge installer, including OptiForge.
    public static void doForgeUnpack(File installer, File jsonFile, String forgeLibName, File gameFolder) throws IOException, ParseException, InterruptedException {
        ModLoaderInstaller.install(installer, jsonFile, gameFolder);
    }
}
