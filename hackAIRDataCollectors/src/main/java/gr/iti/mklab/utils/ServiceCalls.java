package gr.iti.mklab.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ServiceCalls {

	public static final int timeout = 60000;

	public static String makePostRequest(String endpoint, String request, String requestEncoding) throws Exception {
		HttpURLConnection.setFollowRedirects(false);
		HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
		conn.setRequestMethod("POST"); // optional default is GET
		conn.setConnectTimeout(timeout); // set timeout to 5 seconds

		// conn.setRequestProperty("User-Agent", "Mozilla/5.0");
		// conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		conn.setRequestProperty("Content-Type", "application/json");

		// Send post request
		System.out.println("\nSending 'POST' request to URL : " + endpoint);
		System.out.println("Post parameters : " + request);
		conn.setDoOutput(true);

		if (requestEncoding != null) {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), requestEncoding));
			bw.write(request);
			bw.flush();
			bw.close();
		} else {
			DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
			wr.writeBytes(request);
			wr.flush();
			wr.close();
		}

		int responseCode = conn.getResponseCode();
		String responseMsg = conn.getResponseMessage();
		System.out.println("Response Code : " + responseCode);
		System.out.println("Response Message : " + responseMsg);
		// if (responseCode != 200) {
		// throw new Exception("POST request to " + endpoint + " returned response code: " + responseCode
		// + " - message: " + responseMsg);
		// }

		if (responseCode < HttpURLConnection.HTTP_BAD_REQUEST) {
			return readResponseFromInputStream(conn.getInputStream());
		} else {
			/* error from server */
			return readResponseFromInputStream(conn.getErrorStream());
		}

	}

	public static String makePostRequestSSL(String endpoint, String request, String requestEncoding,
			boolean hackToAvoidValidation) throws Exception {
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

		HttpsURLConnection.setFollowRedirects(false);
		HttpsURLConnection conn = (HttpsURLConnection) new URL(endpoint).openConnection();
		conn.setRequestMethod("POST"); // optional default is GET
		conn.setConnectTimeout(timeout); // set timeout to 5 seconds

		// add request header
		// conn.setRequestProperty("User-Agent", "Mozilla/5.0");
		// conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		conn.setRequestProperty("Content-Type", "application/json");

		// Send post request
		System.out.println("\nSending 'POST' request to URL : " + endpoint);
		System.out.println("Post parameters : " + request);
		conn.setDoOutput(true);

		if (requestEncoding != null) {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), requestEncoding));
			bw.write(request);
			bw.flush();
			bw.close();
		} else {
			DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
			wr.writeBytes(request);
			wr.flush();
			wr.close();
		}

		int responseCode = conn.getResponseCode();
		String responseMsg = conn.getResponseMessage();
		System.out.println("Response Code : " + responseCode);
		System.out.println("Response Message : " + responseMsg);
		// if (responseCode != 200) {
		// throw new Exception("POST request to " + endpoint + " returned response code: " + responseCode
		// + " - message: " + responseMsg);
		// }

		if (responseCode < HttpURLConnection.HTTP_BAD_REQUEST) {
			return readResponseFromInputStream(conn.getInputStream());
		} else {
			/* error from server */
			return readResponseFromInputStream(conn.getErrorStream());
		}

	}

	// HTTP GET request
	public static String makeGetRequest(String endpoint) throws Exception {
		boolean verbose = false;
		return makeGetRequest(endpoint, null, verbose);
	}

	// HTTP GET request
	public static String makeGetRequest(String endpoint, HashMap<String, String> headerElems, boolean verbose)
			throws Exception {

		HttpURLConnection.setFollowRedirects(false);
		HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
		conn.setRequestMethod("GET"); // optional default is GET
		conn.setConnectTimeout(timeout); // set timeout to 5 seconds

		if (headerElems != null) {
			// add request header properties
			for (Map.Entry<String, String> headerElem : headerElems.entrySet()) {
				conn.setRequestProperty(headerElem.getKey(), headerElem.getValue());
				// conn.setRequestProperty("Content-Type", "application/json");
				// conn.setRequestProperty("User-Agent", "Mozilla/5.0");
			}
		}

		if (verbose) {
			System.out.println("Sending 'GET' request to URL : " + endpoint);
		}
		int responseCode = conn.getResponseCode();
		String responseMsg = conn.getResponseMessage();
		if (verbose) {
			System.out.println("Response Code : " + responseCode);
			System.out.println("Response Message : " + responseMsg);
		}
		// if (responseCode != 200) {
		// throw new Exception("POST request to " + endpoint + " returned response code: " + responseCode
		// + " - message: " + responseMsg);
		// }

		if (responseCode < HttpURLConnection.HTTP_BAD_REQUEST) {
			return readResponseFromInputStream(conn.getInputStream());
		} else {
			/* error from server */
			return readResponseFromInputStream(conn.getErrorStream());
		}

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
