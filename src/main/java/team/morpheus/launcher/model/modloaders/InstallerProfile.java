package team.morpheus.launcher.model.modloaders;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Client installation instructions supplied by Forge and NeoForge installers. */
public class InstallerProfile {

    @SerializedName("minecraft")
    public String minecraft;
    @SerializedName("json")
    public String json = "/version.json";
    @SerializedName("path")
    public String path;
    @SerializedName("install")
    public LegacyInstall install;
    @SerializedName("versionInfo")
    public ModLoaderVersion versionInfo;
    @SerializedName("libraries")
    public List<ModLoaderVersion.Library> libraries = new ArrayList<>();
    @SerializedName("data")
    public Map<String, Data> data = new LinkedHashMap<>();
    // An empty list still describes a processor-based installer; null is the legacy format.
    @SerializedName("processors")
    public List<Processor> processors;

    public static class LegacyInstall {
        @SerializedName("minecraft")
        public String minecraft;
        @SerializedName("path")
        public String path;
        @SerializedName("filePath")
        public String filePath;
    }

    public static class Data {
        @SerializedName("client")
        public String client;
        @SerializedName("server")
        public String server;
    }

    public static class Processor {
        @SerializedName("jar")
        public String jar;
        @SerializedName("sides")
        public List<String> sides;
        @SerializedName("classpath")
        public List<String> classpath = new ArrayList<>();
        @SerializedName("args")
        public List<String> args = new ArrayList<>();
        @SerializedName("outputs")
        public Map<String, String> outputs = new LinkedHashMap<>();

        public boolean isClient() {
            return sides == null || sides.contains("client");
        }
    }
}
