package team.morpheus.launcher.utils.modutils.commons;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import team.morpheus.launcher.logging.MyLogger;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Executes the client installation profiles shared by Forge and NeoForge.
 */
public final class ModLoaderInstaller {
    private static final MyLogger log = new MyLogger(ModLoaderInstaller.class);
    private static final Pattern TOKEN = Pattern.compile("\\{([^{}]+)\\}");

    private ModLoaderInstaller() {
    }

    public static void install(File installer, File versionJson, File gameFolder) throws IOException, ParseException, InterruptedException {
        Path work = Files.createTempDirectory("morpheus-installer-");
        try (ZipFile zip = new ZipFile(installer)) {
            JSONObject profile = readJson(zip, "install_profile.json");
            JSONObject version;
            File libraries = new File(gameFolder, "libraries");
            Files.createDirectories(libraries.toPath());
            if (profile.containsKey("processors")) {
                version = readJson(zip, string(profile, "json", "/version.json"));
                runProcessors(zip, installer, profile, version, libraries, gameFolder, work);
            } else {
                version = (JSONObject) profile.get("versionInfo");
                if (version == null) version = readJson(zip, string(profile, "json", "/version.json"));
                JSONObject legacy = (JSONObject) profile.get("install");
                String coordinate = legacy == null ? string(profile, "path", null) : string(legacy, "path", null);
                if (coordinate == null) throw new IOException("Missing legacy Forge artifact path");
                String embedded = legacy == null ? "maven/" + coordinateToPath(coordinate) : string(legacy, "filePath", null);
                if (embedded == null) throw new IOException("Missing legacy Forge embedded artifact");
                extract(zip, embedded, safeFile(libraries, coordinateToPath(coordinate)).toPath(), null);
                if (!version.containsKey("inheritsFrom") && legacy != null && legacy.get("minecraft") != null) {
                    version.put("inheritsFrom", legacy.get("minecraft"));
                }
            }
            // The version file is the launcher's installation marker. Publish only after success.
            writeJson(versionJson, version);
        } finally {
            try (Stream<Path> files = Files.walk(work)) {
                for (Path path : (Iterable<Path>) files.sorted(Comparator.reverseOrder())::iterator) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void runProcessors(ZipFile zip, File installer, JSONObject profile, JSONObject version, File libraries, File root, Path work) throws IOException, ParseException, InterruptedException {
        checkJavaVersion(version);
        downloadLibraries(zip, (JSONArray) profile.get("libraries"), libraries);
        downloadLibraries(zip, (JSONArray) version.get("libraries"), libraries);
        String minecraft = string(profile, "minecraft", null);
        if (minecraft == null) throw new IOException("Missing minecraft version in installer profile");
        File vanilla = ensureVanilla(zip, minecraft, root);
        Map<String, String> data = new HashMap<>();
        JSONObject profileData = (JSONObject) profile.get("data");
        if (profileData != null) {
            for (Object key : profileData.keySet()) {
                JSONObject sides = (JSONObject) profileData.get(key);
                String value = string(sides, "client", null);
                if (value == null) continue;
                if (value.startsWith("[") && value.endsWith("]")) {
                    value = safeFile(libraries, coordinateToPath(value.substring(1, value.length() - 1))).getAbsolutePath();
                } else if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                } else {
                    File target = safeFile(work.toFile(), stripSlash(value));
                    extract(zip, value, target.toPath(), null);
                    value = target.getAbsolutePath();
                }
                data.put(key.toString(), value);
            }
        }
        data.put("SIDE", "client");
        data.put("MINECRAFT_VERSION", minecraft);
        data.put("MINECRAFT_JAR", vanilla.getAbsolutePath());
        data.put("ROOT", root.getAbsolutePath());
        data.put("INSTALLER", installer.getAbsolutePath());
        data.put("LIBRARY_DIR", libraries.getAbsolutePath());

        for (Object entry : (JSONArray) profile.get("processors")) {
            JSONObject processor = (JSONObject) entry;
            JSONArray sides = (JSONArray) processor.get("sides");
            if (sides != null && !sides.contains("client")) continue;
            Map<File, String> outputs = new LinkedHashMap<>();
            JSONObject declaredOutputs = (JSONObject) processor.get("outputs");
            if (declaredOutputs != null) {
                for (Object key : declaredOutputs.keySet()) {
                    outputs.put(new File(resolve(key.toString(), data, libraries)), resolve((String) declaredOutputs.get(key), data, libraries));
                }
            }
            boolean cached = !outputs.isEmpty();
            for (Map.Entry<File, String> output : outputs.entrySet()) {
                if (!valid(output.getKey(), output.getValue())) cached = false;
            }
            if (cached) continue;
            // Remove only invalid declared outputs so processors cannot reuse a corrupt result.
            for (Map.Entry<File, String> output : outputs.entrySet()) {
                if (output.getKey().exists() && !valid(output.getKey(), output.getValue())) {
                    Path path = output.getKey().getCanonicalFile().toPath();
                    if (!path.startsWith(root.getCanonicalFile().toPath())) {
                        throw new IOException("Processor output outside game directory: " + path);
                    }
                    Files.delete(path);
                }
                Files.createDirectories(output.getKey().getAbsoluteFile().getParentFile().toPath());
            }
            String coordinate = string(processor, "jar", null);
            File jar = safeFile(libraries, coordinateToPath(coordinate));
            requireFile(jar);
            String main;
            try (JarFile processorJar = new JarFile(jar)) {
                main = processorJar.getManifest() == null ? null : processorJar.getManifest().getMainAttributes().getValue("Main-Class");
            }
            if (main == null || main.trim().isEmpty()) throw new IOException("Missing Main-Class: " + coordinate);
            StringBuilder classpath = new StringBuilder(jar.getAbsolutePath());
            JSONArray dependencies = (JSONArray) processor.get("classpath");
            if (dependencies != null) {
                for (Object dependency : dependencies) {
                    File file = safeFile(libraries, coordinateToPath(dependency.toString()));
                    requireFile(file);
                    classpath.append(File.pathSeparator).append(file.getAbsolutePath());
                }
            }
            List<String> command = new ArrayList<>(Arrays.asList(javaExecutable(), "-cp", classpath.toString(), main));
            JSONArray args = (JSONArray) processor.get("args");
            if (args != null) for (Object arg : args) command.add(resolve(arg.toString(), data, libraries));
            log.info("[Mod installer] Running " + coordinate);
            Process process = new ProcessBuilder(command).directory(root).inheritIO().start();
            int result;
            try {
                result = process.waitFor();
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
                throw e;
            }
            if (result != 0) throw new IOException("Processor " + coordinate + " failed with exit code " + result);
            for (Map.Entry<File, String> output : outputs.entrySet()) {
                if (!valid(output.getKey(), output.getValue()))
                    throw new IOException("Invalid processor output: " + output.getKey());
            }
        }
    }

    private static void downloadLibraries(ZipFile zip, JSONArray entries, File libraries) throws IOException {
        if (entries == null) return;
        for (Object entry : entries) {
            JSONObject library = (JSONObject) entry;
            JSONObject downloads = (JSONObject) library.get("downloads");
            JSONObject artifact = downloads == null ? null : (JSONObject) downloads.get("artifact");
            if (artifact == null) continue; // Native classifiers are handled by the launcher.
            String path = string(artifact, "path", coordinateToPath((String) library.get("name")));
            File target = safeFile(libraries, path);
            String hash = string(artifact, "sha1", null);
            if (valid(target, hash)) continue;
            if (zip.getEntry("maven/" + path) != null) {
                extract(zip, "maven/" + path, target.toPath(), hash);
            } else {
                String url = string(artifact, "url", "");
                // Empty URLs without embedded content identify artifacts produced by processors.
                if (!url.isEmpty()) download(new URL(url), target, hash);
            }
        }
    }

    private static File ensureVanilla(ZipFile zip, String minecraft, File root) throws IOException, ParseException {
        File jar = safeFile(root, "versions/" + minecraft + "/" + minecraft + ".jar");
        File json = safeFile(root, "versions/" + minecraft + "/" + minecraft + ".json");
        JSONObject version = null;
        if (json.isFile()) {
            try (Reader reader = Files.newBufferedReader(json.toPath(), StandardCharsets.UTF_8)) {
                version = (JSONObject) new JSONParser().parse(reader);
            }
        }
        if (version == null) {
            JSONObject manifest = readJson(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json"));
            for (Object entry : (JSONArray) manifest.get("versions")) {
                JSONObject candidate = (JSONObject) entry;
                if (minecraft.equals(candidate.get("id"))) {
                    version = readJson(new URL((String) candidate.get("url")));
                    break;
                }
            }
            if (version == null) throw new IOException("Unknown Minecraft version: " + minecraft);
            writeJson(json, version);
        }
        JSONObject downloads = (JSONObject) version.get("downloads");
        JSONObject client = downloads == null ? null : (JSONObject) downloads.get("client");
        if (client == null) throw new IOException("Missing vanilla client download: " + minecraft);
        String sha1 = string(client, "sha1", null);
        if (!valid(jar, sha1)) {
            String embedded = "maven/minecraft/" + minecraft + "/client.jar";
            if (zip.getEntry(embedded) != null) extract(zip, embedded, jar.toPath(), sha1);
            else download(new URL((String) client.get("url")), jar, sha1);
        }
        return jar;
    }

    static String resolve(String value, Map<String, String> data, File libraries) throws IOException {
        if (value == null) throw new IOException("Missing processor value");
        Matcher matcher = TOKEN.matcher(value);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String replacement = data.get(matcher.group(1));
            if (replacement == null) throw new IOException("Unknown installer variable: " + matcher.group());
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        String resolved = result.toString();
        if (resolved.startsWith("[") && resolved.endsWith("]")) {
            return safeFile(libraries, coordinateToPath(resolved.substring(1, resolved.length() - 1))).getAbsolutePath();
        }
        return resolved;
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
        return parts[0].replace('.', '/') + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2] + (parts.length == 4 ? "-" + parts[3] : "") + "." + ext;
    }

    static File safeFile(File root, String path) throws IOException {
        File file = new File(root, path).getCanonicalFile();
        if (!file.toPath().startsWith(root.getCanonicalFile().toPath()) || file.equals(root.getCanonicalFile())) {
            throw new IOException("Invalid installer path: " + path);
        }
        return file;
    }

    static boolean valid(File file, String sha1) throws IOException {
        if (!file.isFile()) return false;
        if (sha1 == null || sha1.isEmpty()) return file.length() > 0;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream stream = Files.newInputStream(file.toPath())) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = stream.read(buffer)) != -1) digest.update(buffer, 0, length);
            }
            StringBuilder actual = new StringBuilder();
            for (byte b : digest.digest()) actual.append(String.format("%02x", b & 255));
            return actual.toString().equalsIgnoreCase(sha1);
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

    private static void extract(ZipFile zip, String name, Path target, String sha1) throws IOException {
        ZipEntry entry = zip.getEntry(stripSlash(name));
        if (entry == null) throw new IOException("Missing installer resource: " + name);
        try (InputStream stream = zip.getInputStream(entry)) {
            copy(stream, target, sha1);
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

    private static JSONObject readJson(ZipFile zip, String name) throws IOException, ParseException {
        ZipEntry entry = zip.getEntry(stripSlash(name));
        if (entry == null) throw new IOException("Missing installer JSON: " + name);
        try (Reader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
            return (JSONObject) new JSONParser().parse(reader);
        }
    }

    private static JSONObject readJson(URL url) throws IOException, ParseException {
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        try (Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
            return (JSONObject) new JSONParser().parse(reader);
        }
    }

    private static void writeJson(File target, JSONObject json) throws IOException {
        try (InputStream stream = new ByteArrayInputStream(json.toJSONString().getBytes(StandardCharsets.UTF_8))) {
            copy(stream, target.toPath(), null);
        }
    }

    private static String string(JSONObject object, String key, String fallback) {
        Object value = object.get(key);
        return value == null ? fallback : value.toString();
    }

    private static String stripSlash(String value) {
        return value.startsWith("/") ? value.substring(1) : value;
    }

    private static void requireFile(File file) throws IOException {
        if (!file.isFile()) throw new IOException("Missing processor dependency: " + file);
    }

    private static String javaExecutable() {
        return new File(new File(System.getProperty("java.home"), "bin"), System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java").getAbsolutePath();
    }

    private static void checkJavaVersion(JSONObject version) throws IOException {
        JSONObject required = (JSONObject) version.get("javaVersion");
        if (required == null || required.get("majorVersion") == null) return;
        String current = System.getProperty("java.specification.version");
        int major = Integer.parseInt(current.startsWith("1.") ? current.substring(2) : current);
        int minimum = ((Number) required.get("majorVersion")).intValue();
        if (major < minimum)
            throw new IOException("This mod loader requires Java " + minimum + "+; restart Morpheus with that Java runtime (current: " + major + ")");
    }
}
