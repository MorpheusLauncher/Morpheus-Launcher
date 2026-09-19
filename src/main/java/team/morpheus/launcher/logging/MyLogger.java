package team.morpheus.launcher.logging;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MyLogger {

    private final Class<?> clazz;

    public MyLogger(Class clazz) {
        this.clazz = clazz;
    }

    public static void installGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> new MyLogger(MyLogger.class).error("Uncaught exception in thread " + thread.getName(), error));
    }

    public void info(String message) {
        printLog(LogLevel.INFO, message);
    }

    public void warn(String message) {
        printLog(LogLevel.WARN, message);
    }

    public void warn(String message, Throwable error) {
        printLog(LogLevel.WARN, message, error);
    }

    public void error(String message) {
        printLog(LogLevel.ERROR, message);
    }

    public void error(String message, Throwable error) {
        printLog(LogLevel.ERROR, message, error);
    }

    public void debug(String message) {
        printLog(LogLevel.DEBUG, message);
    }

    public synchronized void printLog(LogLevel type, String message) {
        printLog(type, message, null);
    }

    public synchronized void printLog(LogLevel type, String message, Throwable error) {
        String timestamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        String line = String.format("%s [%s] [%s] %s", timestamp, type.name(), clazz.getSimpleName(), message);
        if (type == LogLevel.ERROR || type == LogLevel.WARN) System.err.println(line);
        else System.out.println(line);
        if (error != null)
            error.printStackTrace(type == LogLevel.ERROR || type == LogLevel.WARN ? System.err : System.out);
    }
}
