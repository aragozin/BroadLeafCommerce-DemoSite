package info.ragozin.demostarter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;

public class ProcessWatchDog extends Thread {

	private final File lifeGrant;
	private final String vmname;
	
	public ProcessWatchDog(File lifeGrant) {
		try {
			if (lifeGrant.getParentFile() != null) {
				lifeGrant.getParentFile().mkdirs();
			}
			setDaemon(true);
			setName("ProcessWatchDog");
			vmname = ManagementFactory.getRuntimeMXBean().getName();
			this.lifeGrant = lifeGrant;
			lifeGrant.delete();
			if (lifeGrant.exists()) {
				throw new RuntimeException("Cannot remove life grant file: " + lifeGrant.getPath());
			}
			FileOutputStream fos = new FileOutputStream(lifeGrant);
			fos.write(vmname.getBytes());
			fos.close();
			start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void run() {
		try {
			while(true) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(lifeGrant)));
					String id = br.readLine();
					br.close();
					if (!vmname.equals(id)) {
						break;
					}
				}
				catch(IOException e) {
					// ignore
					break;
				}
			}
		}
		finally {
			System.err.println("Life grant missing, terminating ...");
			System.err.flush();
			System.out.flush();
			Runtime.getRuntime().halt(0);
		}
	}	
}
