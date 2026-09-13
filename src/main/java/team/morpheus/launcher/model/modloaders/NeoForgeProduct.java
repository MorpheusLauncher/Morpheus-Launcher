package team.morpheus.launcher.model.modloaders;

import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The public NeoForge Maven catalog supplies artifact versions. */
public class NeoForgeProduct {
    @SerializedName("versions")
    public List<String> versions;

    public static class Release {
        private static final Pattern PUBLISHED = Pattern.compile("(?:(\\d+(?:\\.\\d+){1,2})-)?([0-9][a-zA-Z0-9.+-]*)");

        public final String artifact;
        public final String version;
        public final String loaderVersion;
        private final String minecraft;

        public Release(String artifact, String version) throws IOException {
            Matcher matcher = PUBLISHED.matcher(version);
            if (!matcher.matches()) throw new IOException("Invalid NeoForge version in catalog: " + version);
            this.artifact = artifact;
            this.version = version;
            this.minecraft = matcher.group(1);
            this.loaderVersion = matcher.group(2);
        }

        public String coordinate() {
            return "net.neoforged:" + artifact + ":" + version + ":installer";
        }

        /**
         * Published builds use either minor.patch.build or major.minor.patch.build.
         * A missing Minecraft patch is zero. These prefixes only narrow the candidates;
         * the install profile supplies the Minecraft version used for the final check.
         */
        public boolean mayTarget(String requested) {
            if (minecraft != null) return minecraft.equals(requested);
            String[] parts = requested.split("\\.");
            String patch = parts.length > 2 ? parts[2] : "0";
            String full = parts[0] + "." + parts[1] + "." + patch + ".";
            String shortened = parts[1] + "." + patch + ".";
            return version.startsWith(full) || version.startsWith(shortened);
        }
    }
}
