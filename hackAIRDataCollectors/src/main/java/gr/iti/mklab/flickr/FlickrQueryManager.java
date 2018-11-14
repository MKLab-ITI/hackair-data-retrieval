package gr.iti.mklab.flickr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.json.JSONObject;

/**
 * See https://www.flickr.com/services/api/flickr.photos.search.html
 * 
 * @author Eleftherios Spyromitros-Xioufis
 *
 */
public class FlickrQueryManager {

	public static int resultsSoFar = 0;

	/**
	 * A fixed size fifo queue that holds the timestamps of the last M calls to the Flickr API. Used for call
	 * throttling.
	 */
	public Queue<Long> callQueue;

	public boolean debug = true;

	private String flickrApiKey;

	public static final String endPoint = "https://api.flickr.com/services/rest/?method=flickr.photos.search";

	/** Flickr API query extras */
	public static final String[] fQExtras = { "geo", "date_taken", "date_upload", "media", "description",
			"views", "owner_name", "license", "tags" };

	public static final int fQMaxPerPage = 250; // use less than 250 to achieve stabilization

	public static final int flickrMaxCallsPer = 3600;

	public static final int flickrPerInMillis = 3600000;

	/**
	 * How many times to retry a call to Flickr in case of failure.
	 */
	public static final int maxCallRetries = 5;
	/**
	 * Waiting time between call retries.
	 */
	public static final int retryIntervalMillis = 5000;

	public int maxCallsPer; // 3600

	public long perInMillis; // 3600000

	/**
	 * Recorded accuracy level of the location information. Current range is 1-16 :
	 * <ul>
	 * World level is 1</li>
	 * <li>Country is ~3</li>
	 * <li>Region is ~6</li>
	 * <li>City is ~11</li>
	 * <li>Street is ~16</li>
	 * 
	 * Defaults to maximum value if not specified.
	 */
	public static final int fQAccuracy = 3;
	/**
	 * Content Type setting:
	 * <ul>
	 * <li>1 for photos only.</li>
	 * <li>2 for screenshots only.</li>
	 * <li>3 for 'other' only.</li>
	 * <li>4 for photos and screenshots.</li>
	 * <li>5 for screenshots and 'other'.</li>
	 * <li>6 for photos and 'other'.</li>
	 * <li>7 for photos, screenshots, and 'other' (all).</li>
	 * </ul>
	 */
	public static final int fQContentType = 1;

	public static final String fQFormat = "json";

	public static final String fQSort = "date-posted-desc"; // date-taken-desc/asc

	public static final int radius = 32; // km (max = 32)

	public boolean extras = false;

	public static void main(String args[]) throws Exception {
		// throttling test!
		int numCallsPer = 100;
		long perInMillis = 1000;
		double rate = perInMillis / numCallsPer;
		long callIntervalMillis = (long) (rate);
		String apiKey = "adf14621cb57ec92f0161703c87ea38c";

		FlickrQueryManager fqm = new FlickrQueryManager(apiKey, numCallsPer - 50, perInMillis);

		while (true) { // keep sending calls with the above set rate
			fqm.throttledGetApiResponse(null, maxCallRetries); // fake call
			Thread.sleep(callIntervalMillis);
		}

	}

	public FlickrQueryManager(String flickrApiKey) {
		this.flickrApiKey = flickrApiKey;
		this.maxCallsPer = flickrMaxCallsPer;
		this.perInMillis = flickrPerInMillis;
		callQueue = new CircularFifoQueue<Long>(maxCallsPer);
	}

	public FlickrQueryManager(String flickrApiKey, int maxCallsPer, long perInMillis) {
		this.flickrApiKey = flickrApiKey;
		this.maxCallsPer = maxCallsPer;
		this.perInMillis = perInMillis;
		callQueue = new CircularFifoQueue<Long>(maxCallsPer);
	}

	/**
	 * Common parameters for all queries to this endpoint.
	 * 
	 * @param min_taken_date
	 * @param max_taken_date
	 * @param page
	 * @return
	 * @throws Exception
	 */
	public String buildQuery(long min_taken_date, long max_taken_date, int page, FlickrQuery fq)
			throws Exception {
		String query = endPoint + "&api_key=" + flickrApiKey + "&format=" + fQFormat + "&nojsoncallback=1"
				+ "&per_page=" + fQMaxPerPage + "&sort=" + fQSort + "&min_taken_date=" + min_taken_date
				+ "&max_taken_date=" + max_taken_date + "&page=" + page;
		query += fq.toQueryString();
		return query;
	}

	public static String addExtraParameters(String query) {
		String fQExtrasString = "&extras=";
		for (String fQExtra : fQExtras) {
			fQExtrasString += fQExtra + ",";
		}
		fQExtrasString = fQExtrasString.substring(0, fQExtrasString.length() - 1); // eat last ,
		query += fQExtrasString;
		// query += "&accuracy=" + fQAccuracy;
		// query += "&content_type=" + fQContentType;
		return query;
	}

	public static String addExtraParameter(String query, String fQExtra) {
		String fQExtrasString = "&extras=" + fQExtra;
		query += fQExtrasString;
		// query += "&accuracy=" + fQAccuracy;
		// query += "&content_type=" + fQContentType;
		return query;

	}

	/**
	 * Some calls could be avoided but a first attempt of implementing it was not successful partly due to
	 * inconsistencies of Flickr's API.
	 * 
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public ArrayList<String> generateRequiredApiCalls(String query) throws Exception {
		ArrayList<String> calls = new ArrayList<String>();
		generateRequiredApiCalls(query, calls, 0, 0);
		// System.out.println("Num API calls: " + callCounter);
		return calls;
	}

	public void generateRequiredApiCalls(String query, ArrayList<String> calls, int prevNumResults,
			int failedSplitAttempts) throws Exception {

		// System.out.println("failed split attempts ==>" + failedSplitAttempts);
		// try to also configure min-max upload dates here based on min-max taken dates?

		long min_taken_date = Long.parseLong(query.split("&min_taken_date=")[1].split("&")[0]);
		long max_taken_date = Long.parseLong(query.split("&max_taken_date=")[1].split("&")[0]);

		String response = throttledGetApiResponse(query, maxCallRetries);
		JSONObject json = new JSONObject(response);
		JSONObject photosObject = json.getJSONObject("photos");
		int totalResults = Integer.parseInt(photosObject.getString("total"));

		if (totalResults == prevNumResults) { // # results was not reduced
			failedSplitAttempts++;
			if (failedSplitAttempts >= 2) {// stop splitting further
				System.out.println(new Date() + " - can't prevent missing some results!");
			}
		}

		if (debug) {
			System.out.println(new Date() + " - Total images for query: " + totalResults);
		}

		if (totalResults <= 4000 || failedSplitAttempts >= 2) {
			// if (totalResults <= 4000) {
			FlickrQueryManager.resultsSoFar += totalResults;
			int numPages = photosObject.getInt("pages");
			for (int pageIndex = 0; pageIndex < numPages; pageIndex++) { // one call for every result page!
				calls.add(query.split("&page=")[0] + "&page=" + (pageIndex + 1) + query.split("&page=1")[1]);
			}
			System.out.println(new Date() + " - Results so far: " + resultsSoFar);
		} else {
			if (debug) {
				System.out.println(
						"Total results are > 4000, query should be split or some results will be missed!");
			}
			long duration = max_taken_date - min_taken_date;
			long halfDuration = duration / 2;
			String q1 = query.replace(String.valueOf(max_taken_date),
					String.valueOf(min_taken_date + halfDuration));
			String q2 = query.replace(String.valueOf(min_taken_date),
					String.valueOf(min_taken_date + halfDuration));

			generateRequiredApiCalls(q1, calls, totalResults, failedSplitAttempts);
			generateRequiredApiCalls(q2, calls, totalResults, failedSplitAttempts);
		}

	}

	public String throttledGetApiResponse(String queryUrl, int maxNumRetries) throws Exception {
		int retryCount = 0;
		String response = null;
		while (true) {
			try {
				response = throttledGetApiResponseInternal(queryUrl);
				break; // call succeeded, thus break and return result
			} catch (Exception e) { // if an exception was thrown during the call, retry
				// unless no more retries are left, in which case we should throw the Exception!
				if (retryCount >= maxNumRetries) {
					throw e;
				} else {
					// log exception, sleep and increase retry counter
					System.err.println(e.getMessage());
					TimeUnit.MILLISECONDS.sleep(retryIntervalMillis);
					retryCount++;
				}
			}
		}
		// now we have a valid response
		return response;
	}

	private String throttledGetApiResponseInternal(String queryUrl) throws Exception {
		if (debug) {
			// System.out.println("Attempting call, current size: " + callQueue.size());
		}
		if (callQueue.size() == maxCallsPer) { // queue is full
			// use throttling
			Long now = Instant.now().toEpochMilli();
			Long lastCallTimeStamp = callQueue.peek();
			long difference = now - lastCallTimeStamp;
			if (difference < perInMillis) { // not enough time has passed!
				if (debug) {
					System.out.println("Sleep is needed, sleeping for " + difference + " ms!");
				}
				Thread.sleep(difference); // sleep for the time difference
			}
			// else {
			// System.out.println("No sleep needed!");
			// }
		}

		// if the queue is not full execute immediately
		// System.out.println("Executing call!");
		String response = null;
		try {
			response = makeCall(queryUrl);
		} catch (Exception e) {
			// System.err.println("Call failed: " + queryUrl);
			throw e;
		}
		callQueue.add(Instant.now().toEpochMilli());
		return response;

	}

	/**
	 * Used just to test that throttling works as expected..
	 * 
	 * @param queryUrl
	 * @return
	 * @throws IOException
	 */
	private String makeCall(String queryUrl) throws Exception {
		if (queryUrl == null) {
			// a fake call
			return null;
		} else {
			return getApiResponse(queryUrl);
		}
	}

	/**
	 * Should never by called directly if we want throttling
	 * 
	 * @param queryUrl
	 * @return
	 * @throws IOException
	 */
	private String getApiResponse(String queryUrl) throws Exception {
		if (debug) {
			System.out.println(new Date() + " - Performing API call: " + queryUrl);
		}
		URL url = new URL(queryUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/json");
		// callCounter++;
		if (conn.getResponseCode() != 200) {
			throw new Exception("Failed : HTTP error code : " + conn.getResponseCode());
		}
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
		StringBuilder sb = new StringBuilder();
		String oline = "";
		while ((oline = br.readLine()) != null) {
			sb.append(oline + "\n");
		}
		conn.disconnect();
		return sb.toString();
	}
}
