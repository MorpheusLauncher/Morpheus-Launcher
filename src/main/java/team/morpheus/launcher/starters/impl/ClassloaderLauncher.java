package team.morpheus.launcher.starters.impl;

import team.morpheus.launcher.logging.MyLogger;
import team.morpheus.launcher.model.products.MojangProduct;
import team.morpheus.launcher.starters.ILibraryManager;
import team.morpheus.launcher.utils.Utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassloaderLauncher implements ILibraryManager {

    private final MyLogger log = new MyLogger(ClassloaderLauncher.class);
    private final MojangProduct.Game game;
    private final List<URL> paths;

    public ClassloaderLauncher(MojangProduct.Game game, List<URL> paths) {
        this.game = game;
        this.paths = paths;
    }

    @Override
    public MojangProduct.Game getGame() {
        return game;
    }

    @Override
    public List<URL> getPaths() {
        return paths;
    }

    @Override
    public void launch(List<String> gameargs) throws Exception {
        log.info("Launching game with classloader");

        /* Add all url paths to class loader */
        IsolatedClassloader ucl = new IsolatedClassloader(getPaths().toArray(new URL[getPaths().size()]), ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(ucl);
        Class<?> c = ucl.loadClass(getGame().mainClass);

        /* Mangle game arguments */
        String[] args = new String[]{};
        String[] concat = Utils.concat(gameargs.toArray(new String[gameargs.size()]), args);
        String[] startArgs = Arrays.copyOfRange(concat, 0, concat.length);

        /* Method Handle instead of reflection, to make compatible with jre higher than 8 */
        MethodHandle mainMethodHandle = MethodHandles.lookup().findStatic(c, "main", MethodType.methodType(void.class, String[].class));

        /* Invoke the main with the given arguments */
        try {
            log.debug(String.format("Invoking: %s", c.getName()));
            mainMethodHandle.invokeExact(startArgs);
        } catch (Throwable e) {
            log.error("Game entrypoint invocation failed", e);
        }
    }

    /**
     * Loads game dependencies before delegating to the launcher classloader.
     */
    private static final class IsolatedClassloader extends URLClassLoader {

        private static final Set<String> PARENT_FIRST = new HashSet<>(Arrays.asList("java.", "javax.", "sun.", "com.sun.", "jdk.", "team.morpheus."));

        private IsolatedClassloader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (isParentFirst(name)) return super.loadClass(name, resolve);

            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    try {
                        loaded = findClass(name);
                    } catch (ClassNotFoundException notInGame) {
                        loaded = super.loadClass(name, false);
                    }
                }
                if (resolve) resolveClass(loaded);
                return loaded;
            }
        }

        private static boolean isParentFirst(String name) {
            for (String prefix : PARENT_FIRST) {
                if (name.startsWith(prefix)) return true;
            }
            return false;
        }
    }
}
