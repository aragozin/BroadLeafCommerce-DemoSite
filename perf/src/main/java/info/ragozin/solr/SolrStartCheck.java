package info.ragozin.solr;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.SimpleCloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViNode;
import org.junit.Test;

import info.ragozin.demostarter.DemoInitializer;

public class SolrStartCheck {

	@Test
	public void launchSolr() throws MalformedURLException {
		
		File solrHome = getSolrHome();
		
		Cloud cloud = SimpleCloudFactory.createCloud();
		cloud.node("**").x(VX.TYPE).setLocal();

		ViNode solr = cloud.node("solr");
		
		solr.x(VX.PROCESS).setWorkDir(new File(solrHome, "server").getPath());
		solr.x(VX.CLASSPATH).add(jarPath(solrHome, "server/start.jar"));
		solr.x(VX.JVM).addJvmArgs(
				"-server", 
				"-Xss256k", "-Xms512m", "-Xmx512m",
				"-Duser.timezone=UTC",
				"-XX:NewRatio=3", "-XX:SurvivorRatio=4", "-XX:TargetSurvivorRatio=90",
				"-XX:MaxTenuringThreshold=8", "-XX:+UseConcMarkSweepGC", "-XX:+UseParNewGC",
				"-XX:ConcGCThreads=4", "-XX:ParallelGCThreads=4", "-XX:+CMSScavengeBeforeRemark",
				"-XX:PretenureSizeThreshold=64m", "-XX:+UseCMSInitiatingOccupancyOnly", 
				"-XX:CMSInitiatingOccupancyFraction=50", "-XX:CMSMaxAbortablePrecleanTime=6000",
				"-XX:+CMSParallelRemarkEnabled", "-XX:+ParallelRefProcEnabled",
				"-verbose:gc", "-XX:+PrintHeapAtGC", "-XX:+PrintGCDetails", "-XX:+PrintGCDateStamps",
				"-XX:+PrintGCTimeStamps", "-XX:+PrintTenuringDistribution", "-XX:+PrintGCApplicationStoppedTime");
		solr.x(VX.JVM).addJvmArg("-Xloggc:logs/solr_gc.log");
		
		solr.exec(new Runnable() {
			@Override
			public void run() {
				SolrStarter.main();				
			}
		});
		
	}

	private File getSolrHome() {
		File solrHome = new File(DemoInitializer.getDemoHome() + "/var/solr");
		for(File sub: solrHome.listFiles()) {
			if (sub.isDirectory() && !sub.getName().startsWith(".")) {
				solrHome = sub;
				break;
			}
		}
		return solrHome;
	}
	
	@Test
	public void startSolrCmd() throws InterruptedException, IOException {		
		SolrStarter.startSolr();	
	}
	
	private URL jarPath(File base, String path) throws MalformedURLException {
		File jar = new File(base, path);
		if (!jar.isFile()) {
			throw new IllegalArgumentException("Jar not found: " + jar.getPath());
		}
		return jar.toURI().toURL();
	}
}
