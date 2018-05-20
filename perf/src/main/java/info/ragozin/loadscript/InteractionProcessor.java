package info.ragozin.loadscript;

import java.io.IOException;
import java.util.Map;

import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebResponse;

public interface InteractionProcessor {

	public void preprocess(Map<String, String> variables);
	
	public void processResponse(WebConnection webConnection, WebResponse response, Map<String, String> variables) throws SAXException, IOException;
	
}
