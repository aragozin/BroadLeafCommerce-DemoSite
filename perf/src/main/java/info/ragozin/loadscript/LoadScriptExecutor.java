package info.ragozin.loadscript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
	
	public void perform() throws SAXException {
		next = script.iterator();
		WebClient client = new WebClient();
		connection = new HttpWebConnection(client);
		connection = new LoggingWebConnection(connection);
		connection = new TrivialCachingWebConnection(connection);
		while(next.hasNext()) {
			next.next().perform(connection, variables);
		}
	}	
}
