package info.ragozin.demostarter;

import java.io.File;

public class DemoInitializer {

	public static void initConfiguration() {
		set("demo.database.workingDirectory", getDemoHome() + "/var/hsqldb/");
		set("solr.server.workingDirectory", getDemoHome() + "/var/solr/");
	}
	
	public static void initLifeGrant(String name) {
		File file = new File(getDemoHome() + "/var/life-grants/" + name + ".lg");
		new ProcessWatchDog(file);
	}
	
	public static void kill(String name) {
		File file = new File(getDemoHome() + "/var/life-grants/" + name + ".lg");
		if (file.exists()) {
			file.delete();
			try {
				// wait for process watch preriod
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}
	
	private static void set(String prop, String value) {
		System.out.println("CONF " + prop + ": " + value);
		System.setProperty(prop, value);
	}

	public static String getDemoHome() {
		File dir = new File(".").getAbsoluteFile();
		while(true) {
			if (new File(dir, "DEMO.md").exists()) {
				return dir.getPath().replace("\\", "/");
			}
			else if (dir.getParentFile() == null) {
				throw new RuntimeException("Demo home is not found");
			}
			else {
				dir = dir.getParentFile();
			}
		}
	}
	
	public static String path(String path) {
		File base  = new File(getDemoHome());
		File fpath = new File(base, path);
		return fpath.getPath();
	}	
	
	public static File file(String path) {
		File file = new File(path(path));
		return file;
	}
}
