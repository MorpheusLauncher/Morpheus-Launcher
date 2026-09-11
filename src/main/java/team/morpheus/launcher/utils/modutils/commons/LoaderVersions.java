package team.morpheus.launcher.utils.modutils.commons;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LoaderVersions {
    private LoaderVersions() {
    }

    public static String minecraftVersion(String requested) throws IOException {
        Matcher matcher = Pattern.compile("^([0-9]+(?:\\.[0-9]+){1,2})(?:-|$)").matcher(requested);
        if (!matcher.find()) throw new IOException("Expected a Minecraft version prefix: " + requested);
        return matcher.group(1);
    }

    public static String loaderVersion(String requested, String loader) throws IOException {
        String value = requested.toLowerCase(Locale.ROOT);
        int index = value.indexOf(loader);
        if (index < 0) throw new IOException("Missing loader name: " + requested);
        String suffix = value.substring(index + loader.length());
        if (suffix.startsWith("-")) suffix = suffix.substring(1);
        if (suffix.isEmpty()) return null;
        if (!suffix.matches("[0-9]+(?:\\.[0-9]+)+(?:-[a-z0-9.]+)?")) {
            throw new IOException("Invalid " + loader + " version: " + suffix);
        }
        return suffix;
    }

    public static String latest(List<String> candidates, String requested) throws IOException {
        if (candidates.isEmpty()) throw new IOException("No installer matches " + requested);
        candidates.sort(LoaderVersions::compare);
        return candidates.get(candidates.size() - 1);
    }

    private static int compare(String left, String right) {
        String[] a = left.split("[.-]");
        String[] b = right.split("[.-]");
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            int result = a[i].matches("\\d+") && b[i].matches("\\d+") ? new BigInteger(a[i]).compareTo(new BigInteger(b[i])) : a[i].compareTo(b[i]);
            if (result != 0) return result;
        }
        // For the same numeric version, a release sorts after its prerelease.
        return Integer.compare(b.length, a.length);
    }

    public static File downloadInstaller(String base, String artifact, String version) throws IOException {
        File cache = new File(System.getProperty("java.io.tmpdir"), "morpheus-installers/" + artifact);
        Files.createDirectories(cache.toPath());
        File installer = ModLoaderInstaller.safeFile(cache, artifact + "-" + version + "-installer.jar");
        URL url = new URL(base + version + "/" + installer.getName());
        // Always download atomically: a prior partial installer must not poison the cache.
        ModLoaderInstaller.download(url, installer, null);
        return installer;
    }
}
