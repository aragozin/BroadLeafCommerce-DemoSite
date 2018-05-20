package info.ragozin.loadscript;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.gargoylesoftware.htmlunit.util.WebConnectionWrapper;

public class TracingWebConnection extends WebConnectionWrapper {

	public TracingWebConnection(WebConnection webConnection) throws IllegalArgumentException {
		super(webConnection);
	}

	@Override
	public WebResponse getResponse(WebRequest request) throws IOException {
		dumpRequest(request);
		
		String method = request.getHttpMethod().toString();
		String url = request.getUrl().toString();
		long time = System.nanoTime();
		String code = "fail";
		try {
			WebResponse response = super.getResponse(request);
			code = String.valueOf(response.getStatusCode());
			return response;
		}
		finally {
			long dur = System.nanoTime() - time;
			System.out.println(String.format("[%s] at %3.1fms - (%s) %s", method, 1d * dur / TimeUnit.MILLISECONDS.toNanos(1), code, url));
		}
	}

	private void dumpRequest(WebRequest request) {
		System.out.println("REQUEST");
		System.out.println("  Method: " + request.getHttpMethod());
		System.out.println("  URL: " + request.getUrl());
		System.out.println("  ENCODING: " + request.getEncodingType());
		for(String key: request.getAdditionalHeaders().keySet()) {
			System.out.println("  -" + key + ": " + request.getAdditionalHeaders().get(key));
		}
		for(NameValuePair nvr: request.getRequestParameters()) {
			System.out.println("  " + nvr.getName() + ": " + nvr.getValue());			
		}
		
		if (request.getHttpMethod() == HttpMethod.POST) {
			System.out.println();
			System.out.println(request.getRequestBody());
		}
	}
}
