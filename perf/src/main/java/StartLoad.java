import java.io.IOException;

import org.junit.Test;

import info.ragozin.loadscript.LoadGeneratorCheck;

public class StartLoad {

	@Test
	public void startLoad() throws IOException, InterruptedException {
		new LoadGeneratorCheck().go_mt();
	}
	
}
