package info.ragozin.loadscript;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.xml.sax.SAXException;

public class LoadGeneratorCheck {

	@Test
	public void go() throws IOException, InterruptedException, SAXException {
		
		List<LoadScriptStep> steps = ScriptLoader.loadScript("load-scripts/check-script");
		
		LoadScriptExecutor executor = new LoadScriptExecutor(steps);
		
		while(true) {
			executor.perform();
//			break;
		}		
	}

	@Test
	public void go_mt() throws IOException, InterruptedException {
		
		List<LoadScriptStep> steps = ScriptLoader.loadScript("load-scripts/check-script");

		ExecutorService service = Executors.newFixedThreadPool(4);
		int sessions = 50;
		
		for(int i = 0; i != sessions; ++i) {
			startSession(service, steps);
		}		
		
		while(true) {
			Thread.sleep(1000);
		}
	}

	private void startSession(ExecutorService service, List<LoadScriptStep> steps) {
		
		LoadScriptExecutor executor = new LoadScriptExecutor(steps);
		
		executor.perform(service, new Runnable() {
			
			@Override
			public void run() {
				startSession(service, steps);				
			}
		});		
	}	
}
