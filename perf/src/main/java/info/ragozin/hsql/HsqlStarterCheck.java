package info.ragozin.hsql;

import org.junit.Test;

public class HsqlStarterCheck {

	@Test
	public void startHsql() throws InterruptedException {
		HsqlStarter.main(new String[0]);
		while(true) {
			Thread.sleep(1000);
		}
	}
	
	@Test
	public void testStart() {
		HsqlStarter.start();
	}
	
}
