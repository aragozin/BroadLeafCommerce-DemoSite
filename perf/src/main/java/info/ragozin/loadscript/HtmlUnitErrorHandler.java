package info.ragozin.loadscript;

import java.net.MalformedURLException;
import java.net.URL;

import com.gargoylesoftware.css.parser.CSSErrorHandler;
import com.gargoylesoftware.css.parser.CSSException;
import com.gargoylesoftware.css.parser.CSSParseException;
import com.gargoylesoftware.htmlunit.DefaultCssErrorHandler;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.html.HTMLParserListener;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.DefaultJavaScriptErrorListener;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;

public class HtmlUnitErrorHandler implements IncorrectnessListener, CSSErrorHandler, JavaScriptErrorListener, HTMLParserListener{

	@SuppressWarnings("unused")
	private final DefaultCssErrorHandler defaultCssErrorHandler = new DefaultCssErrorHandler();
	@SuppressWarnings("unused")
	private final DefaultJavaScriptErrorListener defaultJsErrorHandler = new DefaultJavaScriptErrorListener();
	
	@Override
	public void error(String message, URL url, String html, int line, int column, String key) {
		// DO NOTHING		
	}

	@Override
	public void warning(String message, URL url, String html, int line, int column, String key) {
		// DO NOTHING		
	}

	@Override
	public void scriptException(HtmlPage page, ScriptException scriptException) {
		// DO NOTHING		
	}

	@Override
	public void timeoutError(HtmlPage page, long allowedTime, long executionTime) {
		// DO NOTHING		
	}

	@Override
	public void malformedScriptURL(HtmlPage page, String url, MalformedURLException malformedURLException) {
		// DO NOTHING		
	}

	@Override
	public void loadScriptError(HtmlPage page, URL scriptUrl, Exception exception) {
		// DO NOTHING		
	}

	@Override
	public void warning(CSSParseException exception) throws CSSException {
		// DO NOTHING		
	}

	@Override
	public void error(CSSParseException exception) throws CSSException {
		// DO NOTHING		
	}

	@Override
	public void fatalError(CSSParseException exception) throws CSSException {
		// DO NOTHING		
	}

	@Override
	public void notify(String message, Object origin) {
		// DO NOTHING		
	}
}
