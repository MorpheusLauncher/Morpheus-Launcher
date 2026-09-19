package team.morpheus.launcher.model.modloaders;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/** Only installation fields are modeled; the complete launch JSON is preserved. */
public class ModLoaderVersion {

    @SerializedName("id")
    public String id;
    @SerializedName("inheritsFrom")
    public String inheritsFrom;
    @SerializedName("javaVersion")
    public JavaVersion javaVersion;
    @SerializedName("libraries")
    public List<Library> libraries = new ArrayList<>();
    @SerializedName("downloads")
    public Downloads downloads;

    private transient JsonObject json;

    public JsonObject getJson() {
        return json;
    }

    public void setInheritsFrom(String minecraft) {
        inheritsFrom = minecraft;
        json.addProperty("inheritsFrom", minecraft);
    }

    public static class JavaVersion {
        @SerializedName("majorVersion")
        public Integer majorVersion;
    }

    public static class Library {
        @SerializedName("name")
        public String name;
        @SerializedName("downloads")
        public Downloads downloads;
    }

    public static class Downloads {
        @SerializedName("artifact")
        public Artifact artifact;
        @SerializedName("client")
        public Artifact client;
    }

    public static class Artifact {
        @SerializedName("path")
        public String path;
        @SerializedName("url")
        public String url;
        @SerializedName("sha1")
        public String sha1;
    }

    public static class Deserializer implements JsonDeserializer<ModLoaderVersion> {
        // A plain Gson reads the fields without invoking this adapter recursively.
        private final Gson gson = new Gson();

        @Override
        public ModLoaderVersion deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
            ModLoaderVersion version = gson.fromJson(json, ModLoaderVersion.class);
            version.json = json.getAsJsonObject();
            return version;
        }
    }
}
