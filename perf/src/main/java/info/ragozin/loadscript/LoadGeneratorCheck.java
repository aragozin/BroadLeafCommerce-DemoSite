package info.ragozin.loadscript;

import java.io.IOException;
import java.util.List;

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
	
}
