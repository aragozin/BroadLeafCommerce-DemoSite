package info.ragozin.loadscript;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebResponse;

public class RegExBodyExtractor implements InteractionProcessor {

	private String name;
	private Pattern regEx;
	
	public RegExBodyExtractor() {
	}

	public void name(String name) {
		this.name = name;
	}
	
	public void pattern(String pattern) {
		this.regEx = Pattern.compile(pattern);
	}

	@Override
	public void preprocess(Map<String, String> variables) {
		// do nothing		
	}

	@Override
	public void processResponse(WebConnection connection, WebResponse response, Map<String, String> variables) {
		Matcher mc = regEx.matcher(response.getContentAsString());
		if (mc.find()) {
			String value = mc.group(1);
			System.out.println("DEF " + name + " <- " + value);
			variables.put(name, value);
		}
	}
}
