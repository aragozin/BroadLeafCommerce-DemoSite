package info.ragozin.loadscript;

import java.util.Map;

import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebResponse;

public class RedirectHandler implements InteractionProcessor {

	@Override
	public void preprocess(Map<String, String> variables) {
	}

	@Override
	public void processResponse(WebConnection connection, WebResponse response, Map<String, String> variables) {
		String redirect = response.getResponseHeaderValue("Location");
		if (redirect == null) {
			throw new RuntimeException("No redirect location");
		}
		variables.put("REDIRECT", redirect);		
	}
}
