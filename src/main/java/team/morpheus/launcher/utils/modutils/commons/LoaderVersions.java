package team.morpheus.launcher.utils.modutils.commons;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LoaderVersions {
    private LoaderVersions() {
    }

    public static Request parse(String requested, String loader) throws IOException {
        if (requested == null) throw new IOException("Missing modloader version");
        String name = loader.equals("forge") ? "(?:opti)?forge" : Pattern.quote(loader);
        Pattern pattern = Pattern.compile("^(?:([0-9]+(?:\\.[0-9]+){1,2})-)?" + name + "(?:-?([0-9]+(?:\\.[0-9]+)+(?:-[a-z0-9.]+)?))?$");
        Matcher matcher = pattern.matcher(requested.toLowerCase(Locale.ROOT));
        if (!matcher.matches()) throw new IOException("Invalid " + loader + " request: " + requested);
        Request request = new Request(matcher.group(1), matcher.group(2));
        if (request.minecraft == null && request.version == null)
            throw new IOException("Specify Minecraft or an exact " + loader + " version");
        return request;
    }

    public static String latest(List<String> candidates, String requested) throws IOException {
        if (candidates.isEmpty()) throw new IOException("No installer matches " + requested);
        return Collections.max(candidates, LoaderVersions::compare);
    }

    /**
     * Numeric components sort numerically; release builds sort after their qualifiers.
     */
    public static int compare(String left, String right) {
        String[] a = left.split("[.-]");
        String[] b = right.split("[.-]");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            if (i == a.length) return b[i].matches("\\d+") ? -1 : 1;
            if (i == b.length) return a[i].matches("\\d+") ? 1 : -1;
            int result = a[i].matches("\\d+") && b[i].matches("\\d+") ? new BigInteger(a[i]).compareTo(new BigInteger(b[i])) : a[i].compareTo(b[i]);
            if (result != 0) return result;
        }
        // Equivalent numeric spellings still have a stable order.
        return left.compareTo(right);
    }

    public static class Request {
        public final String minecraft;
        public final String version;

        private Request(String minecraft, String version) {
            this.minecraft = minecraft;
            this.version = version;
        }
    }
}
