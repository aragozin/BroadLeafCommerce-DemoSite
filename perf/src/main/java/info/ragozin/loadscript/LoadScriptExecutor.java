package info.ragozin.loadscript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.HttpWebConnection;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;

public class LoadScriptExecutor {

	private final List<LoadScriptStep> script = new ArrayList<>();
	private final Map<String, String> variables = new HashMap<>();

	private WebConnection connection;
	private Iterator<LoadScriptStep> next;

	public LoadScriptExecutor(List<LoadScriptStep> script) {
		this.script.addAll(script);
	}

	public void perform() {
		perform(cmd -> cmd.run(), () -> {});
	}

	public void perform(Executor exec, Runnable completeTask) {
		next = script.iterator();
		WebClient client = new WebClient();
		connection = new HttpWebConnection(client);
		connection = new LoggingWebConnection(connection);
		connection = new TrivialCachingWebConnection(connection);
		Runnable step = new Runnable() {

			@Override
			public void run() {
				try {
					next.next().perform(connection, variables);
				} catch (SAXException e) {
					e.printStackTrace();
				}
				if (variables.containsKey("RESTART")) {
				    variables.remove("RESTART");
				    next = script.iterator();
				}
				if (next.hasNext()) {
					exec.execute(this);
				}
				else {
					completeTask.run();
				}
			}
		};
		exec.execute(step);
	}
}
