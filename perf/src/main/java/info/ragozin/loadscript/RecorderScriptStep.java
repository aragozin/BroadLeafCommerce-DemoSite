package info.ragozin.loadscript;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.FormEncodingType;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

public class RecorderScriptStep implements LoadScriptStep {

	private HttpMethod method;
	private String url;
	private FormEncodingType encodingType;
	private String body;
	private String expectedStatus = "200";
	
	private Map<String, String> extraHeader = new HashMap<>();
	private Map<String, String> requestParams = new HashMap<>();
	
	private List<InteractionProcessor> processors = new ArrayList<>();
	
	public RecorderScriptStep(InputStream stream) throws IOException {
		load(stream);
	}
	
	public void add(InteractionProcessor proc) {
		processors.add(proc);
	}

	private void load(InputStream stream) throws IOException {
		Properties prop = new Properties();
		prop.load(stream);
		stream.close();
		
		method = HttpMethod.valueOf(prop.getProperty("method"));
		url = prop.getProperty("url");
		encodingType = FormEncodingType.getInstance(prop.getProperty("encodingType"));
		if (prop.getProperty("responseStatus") != null) {
			expectedStatus = prop.getProperty("responseStatus");
		}
		
		for(Object k: prop.keySet()) {
			String key = (String) k;
			if (key.startsWith("extra-")) {
				extraHeader.put(key.substring("extra-".length()), prop.getProperty(key));
			}
			else if (key.startsWith("header-")) {
				requestParams.put(key.substring("header-".length()), prop.getProperty(key));
			}
		}		
		
		body = prop.getProperty("body");
		
		for(Object k: prop.keySet()) {
			String key = (String) k;
			if (key.startsWith("processor-") && key.substring("processor-".length()).indexOf('-') < 0) {
				String procName = key.substring("processor-".length());
				initProcessor(procName, prop);
			}
		}
	}

	private void initProcessor(String procName, Properties prop) {
		try {
			String className = prop.getProperty("processor-" + procName);
			if (className.indexOf('.') < 0) {
				className = this.getClass().getPackage().getName() + "." + className; 
			}
			Class<?> cls = Class.forName(className);
			Object obj = cls.newInstance();
			String pref = "processor-" + procName + "-";
			for(Object k: prop.keySet()) {
				String key = (String) k;
				if (key.startsWith(pref)) {
					String pn = key.substring(pref.length());
					Method m = cls.getMethod(pn, String.class);
					m.invoke(obj, prop.get(key));
				}
			}
			processors.add((InteractionProcessor) obj);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void perform(WebConnection connection, Map<String, String> variables) throws SAXException {
		try {
			for(InteractionProcessor vp: processors) {
				vp.preprocess(variables);
			}
			WebRequest req = composeRequest(variables);
			WebResponse resp = connection.getResponse(req);
			processResponse(connection, resp, variables);
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	private WebRequest composeRequest(Map<String, String> variables) throws MalformedURLException {
		
		String redircetUrl = variables.remove("REDIRECT");
		URL u = new URL(redircetUrl == null ? realise(url, variables) : redircetUrl);
		
		WebRequest req = new WebRequest(u, method);
		req.setEncodingType(encodingType);
		
		for(String key: extraHeader.keySet()) {
			req.setAdditionalHeader(key, realise(extraHeader.get(key), variables));
		}
		
		List<NameValuePair> params = new ArrayList<>();
		for(String key: requestParams.keySet()) {
			String val = realise(requestParams.get(key), variables);
			params.add(new NameValuePair(key, val));
		}
		req.setRequestParameters(params);
		
		if (body != null) {
			req.setRequestBody(realise(body, variables));
		}
		
		return req;
	}

	private void processResponse(WebConnection connection, WebResponse resp, Map<String, String> variables) throws SAXException, IOException {
		if (!expectedStatus.endsWith(String.valueOf(resp.getStatusCode()))) {
			System.out.println(String.format("[%s] (%d) - %s", method, resp.getStatusCode(), url));
			System.out.println(resp.getContentAsString());
			throw new RuntimeException("Response status (" + resp.getStatusCode() + ") but expected (" + expectedStatus + ")");
		}
		for(InteractionProcessor vp: processors) {
			vp.processResponse(connection, resp, variables);
		}
	}
	
	private String realise(String text, Map<String, String> variables) {
		for(String name: variables.keySet()) {
			String holder = "[!" + name + "!]";
			if (text.contains(holder)) {
				text = text.replace(holder, variables.get(name));
			}
		}
		return text;
	}
}
