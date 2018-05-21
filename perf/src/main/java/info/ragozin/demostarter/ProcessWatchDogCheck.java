package info.ragozin.demostarter;

import java.io.File;

import org.junit.Test;

public class ProcessWatchDogCheck {

	@SuppressWarnings("unused")
	@Test
	public void check() throws InterruptedException {
		ProcessWatchDog watchdog = new ProcessWatchDog(new File("target/check.lg"));
		while(true) {
			Thread.sleep(1000);
		}
	}
	
}
