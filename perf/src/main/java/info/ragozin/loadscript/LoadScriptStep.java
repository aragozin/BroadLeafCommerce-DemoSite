package info.ragozin.loadscript;

import java.util.Map;

import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.WebConnection;

public interface LoadScriptStep {

	public void perform(WebConnection connection, Map<String, String> variables) throws SAXException;
	
}
