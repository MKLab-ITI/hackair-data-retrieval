package utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ServiceCalls {

	public static final int connectionTimeout = 10000; // 10 sec
	public static final int readTimeout = 600000; // 10 min

	public static String makePostRequest(String endpoint, String request, boolean hackToAvoidValidation)
			throws Exception {

		URL url = new URL(endpoint);
		HttpURLConnection conn = null;

		if (url.getProtocol().equals("https")) { // SSL
			if (hackToAvoidValidation) {
				// Create a trust manager that does not validate certificate chains
				TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}

					public void checkClientTrusted(X509Certificate[] certs, String authType) {
					}

					public void checkServerTrusted(X509Certificate[] certs, String authType) {
					}
				} };

				// Install the all-trusting trust manager
				SSLContext sc = SSLContext.getInstance("SSL");
				sc.init(null, trustAllCerts, new java.security.SecureRandom());
				HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

				// Create all-trusting host name verifier
				HostnameVerifier allHostsValid = new HostnameVerifier() {
					public boolean verify(String hostname, SSLSession session) {
						return true;
					}
				};

				// Install the all-trusting host verifier
				HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
			}
			conn = (HttpsURLConnection) url.openConnection();
		} else if (url.getProtocol().equals("http")) { // no SSL
			conn = (HttpURLConnection) url.openConnection();
		} else {
			throw new Exception("Unknown protocol in: " + endpoint);
		}

		// set the connection and the read timeout (IMPORTANT TO AVOID WAITING FOREVER!)
		conn.setConnectTimeout(connectionTimeout);
		conn.setReadTimeout(readTimeout);

		// add request header
		conn.setRequestMethod("POST");
		// conn.setRequestProperty("User-Agent", "Mozilla/5.0");
		// conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		conn.setRequestProperty("Content-Type", "application/json");

		// Send post request
		// System.out.println("\nSending 'POST' request to URL : " + endpoint);
		// System.out.println("Post parameters : " + request);
		conn.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
		wr.writeBytes(request);
		wr.flush();
		wr.close();

		int responseCode = conn.getResponseCode();
		String responseMsg = conn.getResponseMessage();
		// System.out.println("Response Code : " + responseCode);
		// System.out.println("Response Message : " + responseMsg);
		// if (responseCode != 200) {
		// throw new Exception("POST request to " + endpoint + " returned response code: " + responseCode
		// + " - message: " + responseMsg);
		// }

		return readResponseFromInputStream(conn.getInputStream());
	}

	// HTTP GET request
	public static String makeGetRequest(String endpoint) throws Exception {
		URL url = new URL(endpoint);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setRequestMethod("GET"); // optional default is GET

		// add request header
		// conn.setRequestProperty("Content-Type", "application/json");
		// conn.setRequestProperty("User-Agent", "Mozilla/5.0");

		System.out.println("Sending 'GET' request to URL : " + endpoint);
		int responseCode = conn.getResponseCode();
		String responseMsg = conn.getResponseMessage();
		System.out.println("Response Code : " + responseCode);
		System.out.println("Response Message : " + responseMsg);
		// if (responseCode != 200) {
		// throw new Exception("POST request to " + endpoint + " returned response code: " + responseCode
		// + " - message: " + responseMsg);
		// }

		return readResponseFromInputStream(conn.getInputStream());

	}

	private static String readResponseFromInputStream(InputStream is) throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine + "\n");
		}
		in.close();
		// print response
		// System.out.println(response.toString());
		return response.toString();
	}

}
