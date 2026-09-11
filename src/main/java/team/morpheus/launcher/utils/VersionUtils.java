package team.morpheus.launcher.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import team.morpheus.launcher.Main;
import team.morpheus.launcher.model.products.MojangProduct;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Date;

public class VersionUtils {

    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Date.class, new DateDeserializer()).create();

    /* Retrieve versions list from mojang server */
    public static MojangProduct retrieveVersions() throws IOException {
        return GSON.fromJson(Utils.makeGetRequest(new URL(Main.getVersionsURL())), MojangProduct.class);
    }

    /* Retrieve the version json */
    public static MojangProduct.Game retrieveGame(File file) throws IOException {
        return GSON.fromJson(new String(Files.readAllBytes(file.toPath())), MojangProduct.Game.class);
    }

    /* Search a version by "name" from the version list */
    public static MojangProduct.Version findVersion(MojangProduct data, String name) {
        for (Object version : data.versions.stream().filter(f -> f.id.equalsIgnoreCase(name)).toArray()) {
            return (MojangProduct.Version) version;
        }
        return null;
    }
}
