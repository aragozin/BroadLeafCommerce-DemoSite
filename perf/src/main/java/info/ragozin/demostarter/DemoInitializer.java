package info.ragozin.demostarter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class DemoInitializer {

    static final Properties PROPS;

    static {
        try {
            PROPS = new Properties();
            PROPS.load(new FileInputStream(file("demo.properties")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void initConfiguration() {
        set("demo.database.workingDirectory", getDemoHome() + "/var/hsqldb/");
        set("solr.server.workingDirectory", getDemoHome() + "/var/solr/");
        for(Object prop: PROPS.keySet()) {
            set((String)prop, (String)PROPS.get(prop));
        }
    }

    public static void initLifeGrant(String name) {
        File file = new File(getDemoHome() + "/pids/" + name + ".lg");
        new ProcessWatchDog(file);
    }

    public static String prop(String name) {
        return prop(name, null);
    }

    public static String prop(String name, String defaultValue) {
        return PROPS.getProperty(name, defaultValue);
    }

    public static int propAsInt(String name, int defaultValue) {
        return Integer.parseInt(PROPS.getProperty(name, String.valueOf(defaultValue)));
    }

    public static boolean check(String name) {
        File file = new File(getDemoHome() + "/pids/" + name + ".lg");
        return ProcessWatchDog.check(file);
    }

    public static void kill(String name) {
        File file = new File(getDemoHome() + "/pids/" + name + ".lg");
        ProcessWatchDog.kill(file);
    }

    private static void set(String prop, String value) {
        System.out.println("CONF " + prop + ": " + value);
        System.setProperty(prop, value);
    }

    public static String getDemoHome() {
        File dir = new File(".").getAbsoluteFile();
        while (true) {
            if (new File(dir, "demo.properties").exists()) {
                return dir.getPath().replace("\\", "/");
            } else if (dir.getParentFile() == null) {
                throw new RuntimeException("Demo home is not found");
            } else {
                dir = dir.getParentFile();
            }
        }
    }

    public static String path(String path) {
        File base = new File(getDemoHome());
        File fpath = new File(base, path);
        return fpath.getPath();
    }

    public static File file(String path) {
        File file = new File(path(path));
        return file;
    }
}
