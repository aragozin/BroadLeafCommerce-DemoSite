package info.ragozin.loadscript;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;

import net.sourceforge.htmlunit.cyberneko.parsers.DOMParser;

public class PageAssetFetchProcessor implements InteractionProcessor {

	@Override
	public void preprocess(Map<String, String> variables) {
		// do nothing		
	}

	@Override
	public void processResponse(WebConnection webConnection, WebResponse response, Map<String, String> variables) throws SAXException, IOException {
		if (response.getContentType().contains("text/html")) {
			DOMParser parser = new DOMParser();
			parser.parse(new InputSource(response.getContentAsStream()));
			Document doc = parser.getDocument();
			try {
				URI url  = response.getWebRequest().getUrl().toURI();
				loadRefs(webConnection, url.toURL(), doc.getDocumentElement());
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}		
	}

	private void loadRefs(WebConnection webConnection, URL base, Element element) {
		String uri = element.getAttribute("src");
		if (uri != null && uri.length() > 0) {
			request(webConnection, base, uri);
		}
		NodeList nl = element.getChildNodes();
		for(int i = 0; i != nl.getLength(); ++i) {
			if (nl.item(i) instanceof Element) {
				loadRefs(webConnection, base, (Element)nl.item(i));
			}
		}
	}

	private void request(WebConnection webConnection, URL base, String uri) {
		try {
			URL url = new URL(base, uri);
			if (url.toString().startsWith("http://localhost")) {
				WebRequest req = new WebRequest(url, HttpMethod.GET);
				webConnection.getResponse(req);
			}
		} catch (MalformedURLException e) {
			// ignore
		} catch (IOException e) {
			// ignore
		}		
	}
}
