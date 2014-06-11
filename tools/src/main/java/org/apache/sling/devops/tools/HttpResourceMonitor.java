package org.apache.sling.devops.tools;
import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResourceMonitor {

	private static final Logger logger = LoggerFactory.getLogger(HttpResourceMonitor.class);

	public static void main(String[] args) throws IOException {
		String host = args.length < 1 ? "localhost" : args[0];
		String resource = args.length < 2 ? "/" : args[1];

		try (CloseableHttpClient httpClient = HttpClients.custom().setRetryHandler(new DefaultHttpRequestRetryHandler(0, false)).build()) {

			// Custom response handler
			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
				@Override
				public String handleResponse(final HttpResponse response) throws IOException {
					return String.format(
							"%s - %s",
							response.getStatusLine(),
							EntityUtils.toString(response.getEntity()).replaceAll("\n", "\\\\n")
							);
				}
			};

			// Send requests
			String prevResponse = null;
			while (true) {
				String curResponse;
				try {
					curResponse = httpClient.execute(
							new HttpHost(host),
							new HttpGet(resource),
							responseHandler
							);
				} catch (NoHttpResponseException e) {
					curResponse = "No response!";
				} catch (IOException e) {
					curResponse = "Connection aborted!";
				}
				if (prevResponse == null || !prevResponse.equals(curResponse)) {
					logger.info(curResponse);
				}
				prevResponse = curResponse;
			}
		}
	}
}
