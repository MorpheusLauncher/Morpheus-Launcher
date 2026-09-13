package team.morpheus.launcher.utils.modutils.commons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import team.morpheus.launcher.model.modloaders.ModLoaderVersion;
import team.morpheus.launcher.utils.CryptoEngine;
import team.morpheus.launcher.utils.Utils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * JSON and artifact I/O shared by modloader discovery and installation.
 */
public final class InstallerFiles {

    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(ModLoaderVersion.class, new ModLoaderVersion.Deserializer()).create();

    private InstallerFiles() {
    }

    public static <T> T readJson(String json, Class<T> type) throws IOException {
        return readJson(new StringReader(json), type);
    }

    public static <T> T readJson(URL url, Class<T> type) throws IOException {
        return readJson(Utils.makeGetRequest(url), type);
    }

    static <T> T readJson(File file, Class<T> type) throws IOException {
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return readJson(reader, type);
        }
    }

    static <T> T readJson(ZipFile zip, String name, Class<T> type) throws IOException {
        try (Reader reader = new InputStreamReader(zip.getInputStream(entry(zip, name)), StandardCharsets.UTF_8)) {
            return readJson(reader, type);
        }
    }

    private static <T> T readJson(Reader reader, Class<T> type) throws IOException {
        try {
            T value = GSON.fromJson(reader, type);
            if (value == null) throw new IOException("Missing " + type.getSimpleName() + " JSON");
            return value;
        } catch (JsonParseException | IllegalStateException e) {
            throw new IOException("Invalid " + type.getSimpleName() + " JSON", e);
        }
    }

    public static File downloadInstaller(URL url, String coordinate, String sha1) throws IOException {
        File cache = new File(System.getProperty("java.io.tmpdir"), "morpheus-installers");
        File installer = safeFile(cache, coordinateToPath(coordinate));
        // Reuse only an installer whose expected checksum is known and verified.
        if (sha1 == null || sha1.isEmpty() || !valid(installer, sha1)) download(url, installer, sha1);
        return installer;
    }

    static String coordinateToPath(String coordinate) throws IOException {
        if (coordinate == null) throw new IOException("Missing Maven coordinate");
        String[] extension = coordinate.split("@", -1);
        String[] parts = extension[0].split(":", -1);
        if (extension.length > 2 || parts.length < 3 || parts.length > 4)
            throw new IOException("Invalid Maven coordinate: " + coordinate);
        for (String part : parts) {
            if (!part.matches("[A-Za-z0-9_.+\\-]+") || part.equals(".."))
                throw new IOException("Invalid Maven coordinate: " + coordinate);
        }
        String ext = extension.length == 2 ? extension[1] : "jar";
        if (!ext.matches("[A-Za-z0-9]+")) throw new IOException("Invalid artifact extension: " + coordinate);
        String classifier = parts.length == 4 ? "-" + parts[3] : "";
        return String.format("%s/%s/%s/%s-%s%s.%s", parts[0].replace('.', '/'), parts[1], parts[2], parts[1], parts[2], classifier, ext);
    }

    static File safeFile(File root, String path) throws IOException {
        if (path == null) throw new IOException("Missing installer path");
        return requireInside(root, new File(root, path));
    }

    static File requireInside(File root, File file) throws IOException {
        Path directory = root.getCanonicalFile().toPath();
        File target = file.getCanonicalFile();
        if (!target.toPath().startsWith(directory) || target.toPath().equals(directory))
            throw new IOException("Installer path outside directory: " + file);
        return target;
    }

    static boolean valid(File file, String sha1) throws IOException {
        if (!file.isFile()) return false;
        if (sha1 == null || sha1.isEmpty()) return file.length() > 0;
        try {
            return sha1.equalsIgnoreCase(CryptoEngine.fileHash(file, "SHA-1"));
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    static void download(URL url, File target, String sha1) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        try (InputStream stream = connection.getInputStream()) {
            copy(stream, target.toPath(), sha1);
        }
    }

    static void extract(ZipFile zip, String name, File target, String sha1) throws IOException {
        try (InputStream stream = zip.getInputStream(entry(zip, name))) {
            copy(stream, target.toPath(), sha1);
        }
    }

    private static ZipEntry entry(ZipFile zip, String name) throws IOException {
        if (name == null) throw new IOException("Missing installer resource path");
        ZipEntry entry = zip.getEntry(stripSlash(name));
        if (entry == null) throw new IOException("Missing installer resource: " + name);
        return entry;
    }

    static String stripSlash(String name) {
        return name.startsWith("/") ? name.substring(1) : name;
    }

    static void writeJson(File target, JsonObject json) throws IOException {
        try (InputStream stream = new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8))) {
            copy(stream, target.toPath(), null);
        }
    }

    private static void copy(InputStream stream, Path target, String sha1) throws IOException {
        target = target.toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".install-", ".tmp");
        try {
            Files.copy(stream, temporary, StandardCopyOption.REPLACE_EXISTING);
            if (sha1 != null && !sha1.isEmpty() && !valid(temporary.toFile(), sha1))
                throw new IOException("Checksum mismatch: " + target);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static void deleteDirectory(Path directory) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            for (Path path : (Iterable<Path>) files.sorted(Comparator.reverseOrder())::iterator) {
                Files.deleteIfExists(path);
            }
        }
    }
}
