package info.ragozin.loadscript;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

		Executor service = createRandomDelayExecutor(15);
		int sessions = 40;

		Random rnd = new Random(1);
		for(int i = 0; i != sessions; ++i) {
			int delay = rnd.nextInt(5);
			Thread.sleep(delay + 1000);
			startSession(service, steps);
		}

		while(true) {
			Thread.sleep(1000);
		}
	}

	private Executor createRandomDelayExecutor(int threads) {
		final Random rnd = new Random();
		final ScheduledExecutorService schedule = Executors.newScheduledThreadPool(threads);
		Executor exec = new Executor() {

			@Override
			public void execute(Runnable command) {
				schedule.schedule(command, rnd.nextInt(1000), TimeUnit.MILLISECONDS);

			}
		};
		return exec;
	}

	private void startSession(Executor service, List<LoadScriptStep> steps) {

		LoadScriptExecutor executor = new LoadScriptExecutor(steps);

		executor.perform(service, new Runnable() {

			@Override
			public void run() {
				startSession(service, steps);
			}
		});
	}
}
