package gr.iti.mklab;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;

@Path("/post")
public class ImageAnalysisService {

	/** The location of the settings file, relative to the classes folder! */
	public static final String iaSettingsFile = "../ia_settings.json"; // this means inside WEB-INF

	/**
	 * The max number of unserved IA requests to wait in the queue before the service starts rejecting requests.
	 * This does not imply any parallelism. Only 1 request will be served each time.
	 **/
	public static final int maxQueueSize = 100;
	public static final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(maxQueueSize);
	public static ExecutorService es;

	/** The total number of requests that have been handled by the service. */
	public static long requestCounter = 0;

	// @Path("/subpath")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response imageAnalysis(@Context HttpServletRequest request, InputStream incomingData) throws Exception {
		long start = System.currentTimeMillis();
		requestCounter++;
		long requestId = requestCounter; // each IA request received a unique id using the requestCounter
		String requestIP = request.getRemoteAddr(); // the IP can be used to reject requests from unknown IPs!
		System.out.println("===> IA request #" + requestId + ", IP: " + requestIP);

		if (es == null) {
			es = new ThreadPoolExecutor(1, 1, Long.MAX_VALUE, TimeUnit.NANOSECONDS, queue); // only 1 thread
		}

		// read IA settings from a file to know the details of image analysis (e.g. the version to use!
		// settings are reloaded each time the service is called to allow online settings updates
		JSONObject settingsObj = loadSettings(iaSettingsFile);
		ImageAnalysisServiceThread ias = new ImageAnalysisServiceThread(incomingData, requestId, settingsObj);
		Future<?> future = null;
		try {
			System.out.println("IA queue size: " + queue.size());
			future = es.submit(ias);
		} catch (RejectedExecutionException e) {
			System.err.println("IA task rejected because max queue size (" + maxQueueSize + ") reached!");
			ias.setExceptionMessage(e.getMessage());
		}
		future.get();

		JSONObject responseJson = ias.getResponseJson();

		String responseString = "";
		if (responseJson != null) { // IA thread completed successfully
			try { // compute and append waiting_time and total_time if the time field is present (only in debug mode)
				responseJson.get("time");
				long totalTime = System.currentTimeMillis() - start;
				long actualExecutionTime = ias.getActualExecutionTime();
				long waitingTime = totalTime - actualExecutionTime;
				((JSONObject) responseJson.get("time")).put("waiting_time", waitingTime);
				((JSONObject) responseJson.get("time")).put("total_time", totalTime);
			} catch (JSONException e) {
				// do not append something when the time object is not present (not in debug mode)
			}
			responseString = responseJson.toString();
		} else { // IA thread did NOT complete successfully
			responseString = "Exception: " + ias.getExceptionMessage();
		}
		System.out.println("===> IA request #" + requestId + " completed!");
		return Response.status(201).entity(responseString).build();

	}

	public JSONObject loadSettings(String iaSettingsFile) throws Exception {
		StringBuffer sb = new StringBuffer();
		String line;
		// class.getClassLoader().getResourceAsStream("") looks at the classes folder
		// ia_settings resides in the folder outside classes, i.e. WEB-INF
		InputStream inputStream = ImageAnalysisServiceThread.class.getClassLoader().getResourceAsStream(iaSettingsFile);
		BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
		while ((line = in.readLine()) != null) {
			sb.append(line);
		}
		in.close();
		JSONObject json = new JSONObject(sb.toString());
		// parse only the whole settings object
		JSONObject settingsObj = (JSONObject) json.getJSONArray("ia_settings").get(0);
		return settingsObj;
	}

}