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
 * NeoForge discovery, including the original Minecraft 1.20.1 artifact.
 */
public final class NeoForgeUtils {

    private NeoForgeUtils() {
    }

    public static void doNeoForgeSetup(String requested, File jsonFile) throws IOException, ParseException, InterruptedException {
        String minecraft = Character.isDigit(requested.charAt(0)) ? LoaderVersions.minecraftVersion(requested) : null;
        String artifact = "1.20.1".equals(minecraft) ? "forge" : "neoforge";
        String base = Main.getNeoForgeInstallURL() + artifact + "/";
        String metadata = Utils.makeGetRequest(new URL(Main.getNeoForgeVersionsURL() + artifact));
        String version = selectVersion(requested, metadata);
        File installer = LoaderVersions.downloadInstaller(base, artifact, version);
        ModLoaderInstaller.install(installer, jsonFile, Launcher.env.getGameFolder());
    }

    static String selectVersion(String requested, String metadata) throws IOException {
        String minecraft = Character.isDigit(requested.charAt(0)) ? LoaderVersions.minecraftVersion(requested) : null;
        String wanted = LoaderVersions.loaderVersion(requested, "neoforge");
        if (minecraft == null && wanted == null)
            throw new IOException("Specify Minecraft or an exact NeoForge version");
        String prefix = null;
        if (minecraft != null) {
            if (minecraft.equals("1.20.1")) prefix = "1.20.1-";
            else if (minecraft.startsWith("1.")) {
                String[] parts = minecraft.split("\\.");
                prefix = parts[1] + "." + (parts.length > 2 ? parts[2] : "0") + ".";
            } else {
                prefix = minecraft + ".";
            }
        }
        try {
            JSONObject response = (JSONObject) new JSONParser().parse(metadata);
            JSONArray entries = (JSONArray) response.get("versions");
            if (entries == null) throw new IOException("NeoForge version response has no versions array");
            List<String> candidates = new ArrayList<>();
            for (Object entry : entries) {
                String version = entry.toString().trim();
                if (!version.matches("[0-9]+(?:\\.[0-9]+)+(?:-[a-zA-Z0-9.-]+)?")) continue;
                if (prefix != null && !version.startsWith(prefix)) continue;
                String comparable = version.startsWith("1.20.1-") ? version.substring("1.20.1-".length()) : version;
                if (wanted == null || wanted.equals(comparable)) candidates.add(version);
            }
            return LoaderVersions.latest(candidates, requested);
        } catch (IOException e) {
            throw e;
        } catch (ParseException | ClassCastException e) {
            throw new IOException("Cannot parse NeoForge version metadata", e);
        }
    }
}
