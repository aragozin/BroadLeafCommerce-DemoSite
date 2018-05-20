package info.ragozin.loadscript;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebResponseData;
import com.gargoylesoftware.htmlunit.util.WebConnectionWrapper;

public class TrivialCachingWebConnection extends WebConnectionWrapper {

	private static final Field F_responseData;
	static {
		try {
			F_responseData = WebResponse.class.getDeclaredField("responseData_");
			F_responseData.setAccessible(true);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Map<String, WebResponseData> cache = new HashMap<>();

	public TrivialCachingWebConnection(WebConnection webConnection) throws IllegalArgumentException {
		super(webConnection);
	}

	@Override
	public WebResponse getResponse(WebRequest request) throws IOException {
		String url = request.getUrl().toString();
		if (HttpMethod.GET.equals(request.getHttpMethod())) {
			if (cache.containsKey(url)) {
				return new WebResponse(cache.get(url), request, 0);
			}
		}
		WebResponse resp = super.getResponse(request);
		WebResponseData data = getResponseData(resp);
		if (resp.getStatusCode() == 200 && HttpMethod.GET.equals(request.getHttpMethod())) {
			String cc = resp.getResponseHeaderValue("Cache-Control");
			if (cc == null || !cc.contains("no-cache")) {
				cache.put(url, data);
			}
		}
		return resp;
	}

	private WebResponseData getResponseData(WebResponse resp) {
		try {
			return (WebResponseData) F_responseData.get(resp);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
