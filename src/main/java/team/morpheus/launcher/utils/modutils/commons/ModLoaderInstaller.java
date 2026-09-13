package team.morpheus.launcher.utils.modutils.commons;

import team.morpheus.launcher.logging.MyLogger;
import team.morpheus.launcher.model.modloaders.InstallerProfile;
import team.morpheus.launcher.model.modloaders.ModLoaderVersion;
import team.morpheus.launcher.model.products.MojangProduct;
import team.morpheus.launcher.utils.VersionUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import static team.morpheus.launcher.utils.modutils.commons.InstallerFiles.*;

/**
 * Executes the client profile supplied by a Forge or NeoForge installer.
 */
public class ModLoaderInstaller {

    private static final MyLogger log = new MyLogger(ModLoaderInstaller.class);
    private static final Pattern TOKEN = Pattern.compile("\\{([^{}]+)\\}");

    private final File installer;
    private final File root;
    private final File libraries;
    private final Map<String, String> variables = new HashMap<>();

    public ModLoaderInstaller(File installer, File gameFolder) {
        this.installer = installer.getAbsoluteFile();
        this.root = gameFolder.getAbsoluteFile();
        this.libraries = new File(root, "libraries");
    }

    public void install(File versionJson) throws IOException, InterruptedException {
        Path work = Files.createTempDirectory("morpheus-installer-");
        try (ZipFile zip = new ZipFile(installer)) {
            InstallerProfile profile = readJson(zip, "install_profile.json", InstallerProfile.class);
            ModLoaderVersion version = profile.versionInfo;
            if (version == null) version = readJson(zip, profile.json, ModLoaderVersion.class);
            Files.createDirectories(libraries.toPath());

            if (profile.processors == null) {
                extractLegacy(zip, profile, version);
            } else {
                checkJavaVersion(version);
                prepareLibraries(zip, profile.libraries);
                prepareLibraries(zip, version.libraries);
                File minecraft = prepareMinecraft(zip, profile.minecraft);
                resolveData(zip, profile, minecraft, work.toFile());
                for (InstallerProfile.Processor processor : profile.processors) {
                    if (processor.isClient()) runProcessor(processor);
                }
            }
            // The launcher uses this file as its installation marker.
            writeJson(versionJson, version.getJson());
        } finally {
            try {
                deleteDirectory(work);
            } catch (IOException e) {
                log.warn("Cannot remove installer temporary files: " + e.getMessage());
            }
        }
    }

    public static InstallerProfile readProfile(File installer) throws IOException {
        try (ZipFile zip = new ZipFile(installer)) {
            return readJson(zip, "install_profile.json", InstallerProfile.class);
        }
    }

    private void extractLegacy(ZipFile zip, InstallerProfile profile, ModLoaderVersion version) throws IOException {
        InstallerProfile.LegacyInstall legacy = profile.install;
        String coordinate = legacy == null ? profile.path : legacy.path;
        String embedded = legacy == null ? "maven/" + coordinateToPath(coordinate) : legacy.filePath;
        extract(zip, embedded, libraryFile(coordinate), null);
        if (version.inheritsFrom == null && legacy != null && legacy.minecraft != null)
            version.setInheritsFrom(legacy.minecraft);
    }

    private void prepareLibraries(ZipFile zip, List<ModLoaderVersion.Library> entries) throws IOException {
        if (entries == null) return;
        for (ModLoaderVersion.Library library : entries) {
            ModLoaderVersion.Artifact artifact = library.downloads == null ? null : library.downloads.artifact;
            if (artifact == null) continue; // Native classifiers are handled at launch.
            String path = artifact.path == null ? coordinateToPath(library.name) : artifact.path;
            File target = safeFile(libraries, path);
            if (valid(target, artifact.sha1)) continue;
            if (zip.getEntry("maven/" + path) != null) {
                extract(zip, "maven/" + path, target, artifact.sha1);
            } else if (artifact.url != null && !artifact.url.isEmpty()) {
                download(new URL(artifact.url), target, artifact.sha1);
            }
            // An empty URL can describe an artifact that a processor will generate.
        }
    }

    private File prepareMinecraft(ZipFile zip, String minecraft) throws IOException {
        if (minecraft == null || minecraft.isEmpty())
            throw new IOException("Missing Minecraft version in installer profile");
        File json = safeFile(root, "versions/" + minecraft + "/" + minecraft + ".json");
        File jar = safeFile(root, "versions/" + minecraft + "/" + minecraft + ".jar");
        if (!json.isFile()) {
            MojangProduct.Version target = VersionUtils.findVersion(VersionUtils.retrieveVersions(), minecraft);
            if (target == null) throw new IOException("Unknown Minecraft version: " + minecraft);
            download(new URL(target.url), json, null);
        }
        ModLoaderVersion version = readJson(json, ModLoaderVersion.class);
        ModLoaderVersion.Artifact client = version.downloads == null ? null : version.downloads.client;
        if (client == null) throw new IOException("Missing Minecraft client download: " + minecraft);
        if (!valid(jar, client.sha1)) {
            String embedded = "maven/minecraft/" + minecraft + "/client.jar";
            if (zip.getEntry(embedded) != null) extract(zip, embedded, jar, client.sha1);
            else download(new URL(client.url), jar, client.sha1);
        }
        return jar;
    }

    private void resolveData(ZipFile zip, InstallerProfile profile, File minecraft, File work) throws IOException {
        variables.clear();
        if (profile.data != null) {
            for (Map.Entry<String, InstallerProfile.Data> entry : new TreeMap<>(profile.data).entrySet()) {
                String value = entry.getValue().client;
                if (value == null) continue;
                if (wrapped(value, '[', ']')) {
                    value = libraryFile(unwrap(value)).getAbsolutePath();
                } else if (wrapped(value, '\'', '\'')) {
                    value = unwrap(value);
                } else {
                    File target = safeFile(work, stripSlash(value));
                    extract(zip, value, target, null);
                    value = target.getAbsolutePath();
                }
                variables.put(entry.getKey(), value);
            }
        }
        variables.put("SIDE", "client");
        variables.put("MINECRAFT_VERSION", profile.minecraft);
        variables.put("MINECRAFT_JAR", minecraft.getAbsolutePath());
        variables.put("ROOT", root.getAbsolutePath());
        variables.put("INSTALLER", installer.getAbsolutePath());
        variables.put("LIBRARY_DIR", libraries.getAbsolutePath());
    }

    private void runProcessor(InstallerProfile.Processor processor) throws IOException, InterruptedException {
        Map<File, String> outputs = resolveOutputs(processor);
        List<File> invalid = new ArrayList<>();
        for (Map.Entry<File, String> output : outputs.entrySet()) {
            if (!valid(output.getKey(), output.getValue())) invalid.add(output.getKey());
        }
        if (!outputs.isEmpty() && invalid.isEmpty()) return;
        for (File file : invalid) {
            Files.deleteIfExists(file.toPath());
            Files.createDirectories(file.getParentFile().toPath());
        }

        log.info("[Mod installer] Running " + processor.jar);
        execute(buildCommand(processor));
        for (Map.Entry<File, String> output : outputs.entrySet()) {
            if (!valid(output.getKey(), output.getValue()))
                throw new IOException("Invalid processor output: " + output.getKey());
        }
    }

    private Map<File, String> resolveOutputs(InstallerProfile.Processor processor) throws IOException {
        Map<File, String> outputs = new LinkedHashMap<>();
        if (processor.outputs == null) return outputs;
        for (Map.Entry<String, String> entry : new TreeMap<>(processor.outputs).entrySet()) {
            File file = new File(resolve(entry.getKey()));
            if (!file.isAbsolute()) file = new File(root, file.getPath());
            outputs.put(requireInside(root, file), resolve(entry.getValue()));
        }
        return outputs;
    }

    private List<String> buildCommand(InstallerProfile.Processor processor) throws IOException {
        File jar = requireLibrary(processor.jar);
        String main;
        try (JarFile processorJar = new JarFile(jar)) {
            main = processorJar.getManifest() == null ? null : processorJar.getManifest().getMainAttributes().getValue("Main-Class");
        }
        if (main == null || main.trim().isEmpty()) throw new IOException("Missing Main-Class: " + processor.jar);
        StringJoiner classpath = new StringJoiner(File.pathSeparator);
        classpath.add(jar.getAbsolutePath());
        if (processor.classpath != null) {
            for (String dependency : processor.classpath) classpath.add(requireLibrary(dependency).getAbsolutePath());
        }
        List<String> command = new ArrayList<>(Arrays.asList(javaExecutable(), "-cp", classpath.toString(), main));
        if (processor.args != null) {
            for (String arg : processor.args) command.add(resolve(arg));
        }
        return command;
    }

    private void execute(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).directory(root).inheritIO().start();
        int result;
        try {
            result = process.waitFor();
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw e;
        }
        if (result != 0) throw new IOException("Installer processor failed with exit code " + result);
    }

    String resolve(String value) throws IOException {
        if (value == null) throw new IOException("Missing processor value");
        Matcher matcher = TOKEN.matcher(value);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String replacement = variables.get(matcher.group(1));
            if (replacement == null) throw new IOException("Unknown installer variable: " + matcher.group());
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        String resolved = result.toString();
        if (wrapped(resolved, '[', ']')) return libraryFile(unwrap(resolved)).getAbsolutePath();
        return wrapped(resolved, '\'', '\'') ? unwrap(resolved) : resolved;
    }

    private File libraryFile(String coordinate) throws IOException {
        return safeFile(libraries, coordinateToPath(coordinate));
    }

    private File requireLibrary(String coordinate) throws IOException {
        File file = libraryFile(coordinate);
        if (!file.isFile()) throw new IOException("Missing processor dependency: " + coordinate);
        return file;
    }

    private static boolean wrapped(String value, char start, char end) {
        return value.length() >= 2 && value.charAt(0) == start && value.charAt(value.length() - 1) == end;
    }

    private static String unwrap(String value) {
        return value.substring(1, value.length() - 1);
    }

    private static String javaExecutable() {
        String executable = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows") ? "java.exe" : "java";
        return new File(new File(System.getProperty("java.home"), "bin"), executable).getAbsolutePath();
    }

    private static void checkJavaVersion(ModLoaderVersion version) throws IOException {
        if (version.javaVersion == null || version.javaVersion.majorVersion == null) return;
        String current = System.getProperty("java.specification.version");
        int major = Integer.parseInt(current.startsWith("1.") ? current.substring(2) : current);
        int minimum = version.javaVersion.majorVersion;
        if (major < minimum)
            throw new IOException("This mod loader requires Java " + minimum + "+; restart Morpheus with that Java runtime (current: " + major + ")");
    }
}
