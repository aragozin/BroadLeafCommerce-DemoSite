package info.ragozin.site;

import static info.ragozin.demostarter.DemoInitializer.file;
import static info.ragozin.demostarter.DemoInitializer.kill;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hsqldb.Server;
import org.hsqldb.persist.HsqlProperties;

import info.ragozin.demostarter.DemoInitializer;

public class SiteStarter {

    protected HsqlProperties props;
    protected Server server;
    protected Thread serverThread;

    public static boolean check() {
        return DemoInitializer.check("storefront");
    }

    public static void start() {
        try {
            kill("storefront");
            String java = System.getProperty("java.home");
            File javaBin = new File(new File(java, "bin"), "java");

            String cp = ManagementFactory.getRuntimeMXBean().getClassPath();

            List<String> cmd = new ArrayList<>();
            cmd.add(javaBin.getPath());
            initJvmArgs(cmd);
            cmd.add("-cp");
            cmd.add(cp);
            cmd.add("com.community.SiteApplication");

            file("var/storefront/logs").mkdirs();

            ProcessBuilder pb = new ProcessBuilder(cmd.toArray(new String[0]));
            pb.directory(file("var/storefront"));
            pb.redirectOutput(Redirect.to(file("var/storefront/logs/console.out")));
            pb.redirectError(Redirect.to(file("var/storefront/logs/console.err")));
            if (pb.start().waitFor(10, TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to start");
            }

            waitForPort(8080);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("resource")
    private static void waitForPort(int port) {
        while(true) {
            if (!check()) {
                System.err.println("Startup failed, see logs in var/storefront/logs");
                throw new RuntimeException();
            }
            try {
                Socket sock = new Socket();
                sock.setSoTimeout(10);
                sock.connect(new InetSocketAddress("127.0.0.1", port));
                if (sock.isConnected()) {
                    sock.close();
                    return;
                }
            }
            catch(IOException e) {
                // ignore;
            }
        }
    }

    public static void stop() {
        DemoInitializer.kill("storefront");
    }

    private static void initJvmArgs(List<String> cmd) {
        cmd.add("-Xmx512m");
        cmd.add("-Xloggc:logs/gc.log");
    }
}
