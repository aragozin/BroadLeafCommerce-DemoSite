package info.ragozin.solr;

import static info.ragozin.demostarter.DemoInitializer.getDemoHome;
import static info.ragozin.demostarter.DemoInitializer.kill;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import info.ragozin.demostarter.DemoInitializer;


public class SolrStarter {

	public static void startSolr() {
		try {
			kill("solr");
			File solrHome = getSolrHome();
			String java = System.getProperty("java.home");
			File javaBin = new File(new File(java, "bin"), "java");
			
			List<String> cmd = new ArrayList<>();
			cmd.add(javaBin.getPath());
			initJvmArgs(cmd);
			cmd.add("-cp");
			cmd.add(new File(getDemoHome() + "/perf/target/classes").getPath() 
					+ System.getProperty("path.separator") + "start.jar"
					+ System.getProperty("path.separator") + "resources/"
					);
			boolean jettyStarter = false;
			if (jettyStarter) {
				cmd.add("org.eclipse.jetty.start.Main");
				cmd.add("--help");
			}			
			else { 
				cmd.add(SolrStarter.class.getName());
			}
			
			ProcessBuilder pb = new ProcessBuilder(cmd.toArray(new String[0]));
			pb.directory(new File(solrHome, "server"));
			pb.redirectOutput(Redirect.to(new File(solrHome, "server/logs/console.out")));
			pb.redirectError(Redirect.to(new File(solrHome, "server/logs/console.err")));
			if (pb.start().waitFor(10, TimeUnit.SECONDS)) {
				throw new RuntimeException("Failed to start");
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void stopSolr() {
		DemoInitializer.kill("solr");
	}
	
	private static void initJvmArgs(List<String> cmd) {
		cmd.addAll(Arrays.asList("-server", 
		"-Xss256k", "-Xms512m", "-Xmx512m",
		"-Duser.timezone=UTC",
		"-XX:NewRatio=3", "-XX:SurvivorRatio=4", "-XX:TargetSurvivorRatio=90",
		"-XX:MaxTenuringThreshold=8", "-XX:+UseConcMarkSweepGC", "-XX:+UseParNewGC",
		"-XX:ConcGCThreads=4", "-XX:ParallelGCThreads=4", "-XX:+CMSScavengeBeforeRemark",
		"-XX:PretenureSizeThreshold=64m", "-XX:+UseCMSInitiatingOccupancyOnly", 
		"-XX:CMSInitiatingOccupancyFraction=50", "-XX:CMSMaxAbortablePrecleanTime=6000",
		"-XX:+CMSParallelRemarkEnabled", "-XX:+ParallelRefProcEnabled",
		"-verbose:gc", "-XX:+PrintHeapAtGC", "-XX:+PrintGCDetails", "-XX:+PrintGCDateStamps",
		"-XX:+PrintGCTimeStamps", "-XX:+PrintTenuringDistribution", "-XX:+PrintGCApplicationStoppedTime"));
		
		cmd.add("-Xloggc:logs/solr_gc.log");			
	}

	private static File getSolrHome() {
		File solrHome = new File(DemoInitializer.getDemoHome() + "/var/solr");
		for(File sub: solrHome.listFiles()) {
			if (sub.isDirectory() && !sub.getName().startsWith(".")) {
				solrHome = sub;
				break;
			}
		}
		return solrHome;
	}
	
	public static void main(String... args) {
		DemoInitializer.initLifeGrant("solr");		
		conf("STOP.PORT", "7983");
		conf("STOP.KEY", "solrrocks");
		conf("jetty.port", "8983");
		conf("solr.solr.home", "solr");
		conf("solr.install.dir", new File("..").getAbsolutePath());
		conf("jetty.home", ".");
		start("org.eclipse.jetty.start.Main", "--module=http");
	}
	
	private static void conf(String key, String value) {
		System.setProperty(key, value);		
	}

	private static void start(String main, String... args) {
		try {
			Class<?> cls = Class.forName(main);
			Method m = cls.getMethod("main", String[].class);
			m.invoke(null, (Object)args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}	
}
