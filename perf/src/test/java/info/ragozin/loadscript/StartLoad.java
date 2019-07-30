package info.ragozin.loadscript;
import java.io.IOException;

import org.junit.Test;

public class StartLoad {

	@Test
	public void startLoad() throws IOException, InterruptedException {
		new LoadGeneratorCheck().go_mt();
	}

}
