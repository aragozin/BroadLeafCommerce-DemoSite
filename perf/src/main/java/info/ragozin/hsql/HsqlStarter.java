package info.ragozin.hsql;

import static info.ragozin.demostarter.DemoInitializer.file;
import static info.ragozin.demostarter.DemoInitializer.initLifeGrant;
import static info.ragozin.demostarter.DemoInitializer.kill;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hsqldb.Server;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.ServerAcl;

import info.ragozin.demostarter.DemoInitializer;

public class HsqlStarter {

    private static final Log LOG = LogFactory.getLog(HsqlStarter.class);
    protected HsqlProperties props;
    protected Server server;
    protected Thread serverThread;

    public static boolean check() {
        return DemoInitializer.check("hsqldb");
    }

    public static void start() {
        try {
            kill("hsqldb");
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
                cmd.add(HsqlStarter.class.getName());
            }

            file("var/hsqldb/logs").mkdirs();

            ProcessBuilder pb = new ProcessBuilder(cmd.toArray(new String[0]));
            pb.directory(file("var/hsqldb"));
            pb.redirectOutput(Redirect.to(file("var/hsqldb/logs/console.out")));
            pb.redirectError(Redirect.to(file("var/hsqldb/logs/console.err")));
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
        cmd.addAll(Arrays.asList("-server", "-Xss256k", "-Xms512m", "-Xmx512m", "-Duser.timezone=UTC", "-XX:NewRatio=3",
                "-XX:SurvivorRatio=4", "-XX:TargetSurvivorRatio=90", "-XX:MaxTenuringThreshold=8",
                "-XX:+UseConcMarkSweepGC", "-XX:+UseParNewGC", "-XX:ConcGCThreads=4", "-XX:ParallelGCThreads=4",
                "-XX:+CMSScavengeBeforeRemark", "-XX:PretenureSizeThreshold=64m", "-XX:+UseCMSInitiatingOccupancyOnly",
                "-XX:CMSInitiatingOccupancyFraction=50", "-XX:CMSMaxAbortablePrecleanTime=6000",
                "-XX:+CMSParallelRemarkEnabled", "-XX:+ParallelRefProcEnabled", "-verbose:gc", "-XX:+PrintHeapAtGC",
                "-XX:+PrintGCDetails", "-XX:+PrintGCDateStamps", "-XX:+PrintGCTimeStamps",
                "-XX:+PrintTenuringDistribution", "-XX:+PrintGCApplicationStoppedTime"));

        cmd.add("-Xloggc:logs/hsql_gc.log");
    }

    public static void main(String[] args) {
        initLifeGrant("hsqldb");
        new HsqlStarter().startServer();
    }

    public HsqlStarter() {
        Properties databaseConfig = new Properties();
        databaseConfig.setProperty("server.database.0", "file:" + DemoInitializer.path("var/hsqldb/broadleaf"));
        databaseConfig.setProperty("server.dbname.0", "broadleaf");
        databaseConfig.setProperty("server.remote_open", "true");
        databaseConfig.setProperty("hsqldb.reconfig_logging", "false");
        databaseConfig.setProperty("server.port", DemoInitializer.prop("demo.database.port", "9001"));

        this.props = new HsqlProperties(databaseConfig);
    }

    private void startServer() {
        LOG.warn(
                "HSQL embedded database server is for demonstration purposes only and is not intended for production usage.");
        server = new Server();

        try {
            server.setProperties(props);
            serverThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    server.start();
                }
            }, "HSQLDB Background Thread");
            serverThread.setDaemon(false);
            serverThread.start();
        } catch (ServerAcl.AclFormatException | IOException e) {
            LOG.error("Error starting HSQL server.", e);
        }
    }
}
