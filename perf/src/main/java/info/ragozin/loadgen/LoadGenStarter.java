package info.ragozin.loadgen;

import static info.ragozin.demostarter.DemoInitializer.file;
import static info.ragozin.demostarter.DemoInitializer.initLifeGrant;
import static info.ragozin.demostarter.DemoInitializer.kill;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import info.ragozin.demostarter.DemoInitializer;
import info.ragozin.loadscript.LoadGeneratorCheck;

public class LoadGenStarter {

    public static boolean check() {
        return DemoInitializer.check("loadgen");
    }

    public static void start() {
        try {
            kill("loadgen");
            String java = System.getProperty("java.home");
            File javaBin = new File(new File(java, "bin"), "java");

            String cp = ManagementFactory.getRuntimeMXBean().getClassPath();

            List<String> cmd = new ArrayList<>();
            cmd.add(javaBin.getPath());
            initJvmArgs(cmd);
            cmd.add("-cp");
            cmd.add(cp);
            boolean jettyStarter = false;
            if (jettyStarter) {
                cmd.add("org.eclipse.jetty.start.Main");
                cmd.add("--help");
            } else {
                cmd.add(LoadGenStarter.class.getName());
            }

            file("var/loadgen/logs").mkdirs();

            ProcessBuilder pb = new ProcessBuilder(cmd.toArray(new String[0]));
            pb.directory(file("perf"));
            pb.redirectOutput(Redirect.to(file("var/loadgen/logs/console.out")));
            pb.redirectError(Redirect.to(file("var/loadgen/logs/console.err")));
            if (pb.start().waitFor(10, TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to start");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void initJvmArgs(List<String> cmd) {
        cmd.add("-Xmx256m");
        cmd.add("-Xloggc:../var/loadgen/logs/gc.log");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        initLifeGrant("loadgen");
        new LoadGeneratorCheck().go_mt();
    }
}
