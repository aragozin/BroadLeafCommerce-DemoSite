package info.ragozin.loadscript;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.gargoylesoftware.htmlunit.util.WebConnectionWrapper;

public class DumpingWebConnection extends WebConnectionWrapper {

	private final String prefix;
	private int counter = 0;
	
	public DumpingWebConnection(String prefix, WebConnection webConnection) throws IllegalArgumentException {
		super(webConnection);
		this.prefix = prefix;
	}

	@Override
	public WebResponse getResponse(WebRequest request) throws IOException {
		String method = request.getHttpMethod().toString();
		String url = request.getUrl().toString();
		long time = System.nanoTime();
		String code = "fail";
		WebResponse response = null;
		try {
			response = super.getResponse(request);
			code = String.valueOf(response.getStatusCode());
			return response;
		}
		finally {
			long dur = System.nanoTime() - time;
			System.out.println(String.format("[%s] at %3.1fms - (%s) %s", method, 1d * dur / TimeUnit.MILLISECONDS.toNanos(1), code, url));
			
			int n = counter++;
			String req = String.format("%s%05d.req", prefix, n);
			dumpRequest(request, openFile(req));
			if (response != null) {
				String rsp = String.format("%s%05d.rsp", prefix, n);
				dumpResponse(response, openFile(rsp));
			}
		}
	}
	
	private OutputStream openFile(String rsp) throws FileNotFoundException {
		File f = new File(rsp);
		f.getAbsoluteFile().getParentFile().mkdirs();
		return new FileOutputStream(f);
	}

	private void dumpRequest(WebRequest request, OutputStream out) throws IOException {
		Properties props = new Properties();		
		
		props.setProperty("method", request.getHttpMethod().toString());
		props.setProperty("url", request.getUrl().toString());
		props.setProperty("encodingType", request.getEncodingType().toString());
		
		for(String key: request.getAdditionalHeaders().keySet()) {
			props.setProperty("extra-" + key, request.getAdditionalHeaders().get(key));
		}
		
		for(NameValuePair nvp: request.getRequestParameters()) {
			props.setProperty("header-" + nvp.getName(), nvp.getValue());
		}
		
		if (request.getRequestBody() != null) {
			props.setProperty("body", request.getRequestBody());
		}
		
		props.store(out, "");
		out.close();
		
	}
	
	private void dumpResponse(WebResponse data, OutputStream os) throws IOException {
		Properties props = new Properties();
		
		props.setProperty("statusCode", String.valueOf(data.getStatusCode()));
		props.setProperty("statusMessage", data.getStatusMessage());
		
		for(NameValuePair nvp: data.getResponseHeaders()) {
			props.setProperty("header-" + nvp.getName(), nvp.getValue());
		}
		
		props.store(os, "");
		os.write('\n');
		os.write('\n');

		int len = (int) data.getContentLength();
		byte[] body = new byte[len];
		data.getContentAsStream().read(body);
		os.write(body);
		os.close();
	}
}
