package gr.iti.mklab;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;

import downloader.IdAndUrl;
import downloader.ImageDownloader;
import gr.mklab.SkyLocalizationAndRatios;
import gr.mklab.classes.RatioCalculationResults;
import pojo.IARequest;
import pojo.Image;
import pojo.ImageAnalysis;
import utils.ServiceCalls;

/**
 * 
 * @author Eleftherios Spyromitros-Xioufis
 *
 */
public class ImageAnalysisServiceThread implements Runnable {

	// === The following variables are populated by the settings object. ===
	/** Whether the new or the old version of the concept detection service will be used. */
	private String skyDetectionVersion;
	/** Only images where sky is detected with higher confidence will be considered for localization. */
	private double skyThreshold;
	/** Only images where usable sky is detected with higher confidence will be considered for localization. */
	private double usableSkyThreshold;
	/**
	 * Supposing that the PC where this service is run has direct access on the images, this is the path to the
	 * root of the images folder.
	 */
	private String imagesRootFolder;
	/**
	 * When URLs are supplied to the service instead of local paths (e.g. when images come from hackAIR mobile
	 * app), images are downloaded in this subfolder (of the root images folder).
	 */
	private String imageDownloadSubfolder;
	/** The endpoint of the (usable) sky detection service. */
	private String detectionEndpoint;
	/** The endpoint of the sky localization service. */
	private String localizationEndpoint;
	private boolean SSLValidationOff;
	/** Whether requests with URLs should be processed (typically true). */
	private boolean processUrls;
	/** Whether the computed sky masks should be saved as well (typically true). */
	private boolean outputMasks;

	// === The variables that we want to keep and have access to when the thread is alive. ===
	/** The data of the post request */
	private InputStream incomingData;
	/** A unique is to keep track of the request */
	private long requestId;
	/** The JSON object that holds the image analysis settings. */
	private JSONObject settingsObj;
	/** When the thread fails for any reason, the exception message is stored in this variable */
	private String exceptionMessage;

	public void setExceptionMessage(String exceptionMessage) {
		this.exceptionMessage = exceptionMessage;
	}

	private JSONObject responseJson;
	private long actualExecutionTime;

	public static MongoCollection<Document> collection;

	public ImageAnalysisServiceThread(InputStream incomingData, long requestId, JSONObject settingsObj)
			throws Exception {
		this.incomingData = incomingData;
		this.requestId = requestId;
		this.settingsObj = settingsObj;
		if (collection == null) {
			collection = new MongoConnection("../mongosettings.json").getCollection();
		}
	}

	public void run() {
		long startAll = System.currentTimeMillis();
		System.out.println("===> Executing IA request " + requestId);
		try {
			parseSetttingsObj(); // always parse settings in the beginning
			System.out.println("Version:" + skyDetectionVersion);

			StringBuilder sb = new StringBuilder();
			BufferedReader in = new BufferedReader(new InputStreamReader(incomingData));
			String line = null;
			while ((line = in.readLine()) != null) {
				sb.append(line);
			}
			// System.out.println("Data Received: " + sb.toString());
			// the same request string is forwarded to both the concept detection and the localization service
			String requestString = sb.toString();
			System.out.println("Request:\n" + requestString);

			// From JSON String to POJO
			ObjectMapper mapper = new ObjectMapper();
			IARequest requestPOJO = mapper.readValue(requestString, IARequest.class);

			boolean debug = requestPOJO.isDebug();

			long start = System.currentTimeMillis();
			if (processUrls) {
				transformToLocalPaths(requestPOJO);
			}
			long downloadTime = System.currentTimeMillis() - start;

			// Check if some of the paths in this request are already in mongo.
			// If YES, then immediately get the IA information for the images with these paths from mongo
			// Use POJO to parse the original request and to strip paths already processed
			// Then send the remaining paths for processing
			// At the end merge the POJOs

			ArrayList<Image> alreadyProcessed = new ArrayList<Image>();
			ArrayList<Image> toBeProcessed = new ArrayList<Image>();
			for (Image im : requestPOJO.getImages()) {
				String relativeImgPath = im.getPath();
				// System.out.println("Image path: " + relativeImgPath);
				BasicDBObject query = new BasicDBObject("source_info.path", relativeImgPath);
				Document doc = collection.find(query).first();
				// determine if doc exists in mongo and if processing is ok
				boolean isProcessed = false;
				if (doc != null) {
					// System.out.println("Image doc not null!");
					// System.out.println(doc.toJson());
					Document iaDoc = ((Document) ((Document) doc.get("source_info")).get("ia"));
					if (iaDoc != null) {
						// System.out.println("Image IA doc not null!");
						ImageAnalysis iaPOJO = mapper.readValue(iaDoc.toJson(), ImageAnalysis.class);
						im.setConcepts(iaPOJO.getConcepts());
						im.setContainsSky(iaPOJO.isContainsSky());
						if (im.isContainsSky()) {
							im.setUsableSky(iaPOJO.isUsableSky());
							if (im.isUsableSky()) {
								im.setRG(iaPOJO.getRG());
								im.setGB(iaPOJO.getGB());
								im.setSky_pixels(iaPOJO.getSky_pixels());
								im.setAll_pixels(iaPOJO.getAll_pixels());
							}
						}
						isProcessed = true;
					}
				}

				if (isProcessed) {
					alreadyProcessed.add(im);
				} else {
					toBeProcessed.add(im);
				}
			}

			System.out.println(
					alreadyProcessed.size() + " of " + requestPOJO.getImages().size() + " images already analyzed!");

			// replace the original set of images with those to be processed
			requestPOJO.setImages(toBeProcessed);

			// perform concept detection and update each entry of the request's JSON array accordingly
			System.out.println("Calling detection service for " + requestPOJO.getImages().size() + " images");
			// System.out.println("Detection endpoint: " + detectionEndpoint);
			start = System.currentTimeMillis();
			String detectionResponse = ServiceCalls.makePostRequest(detectionEndpoint,
					mapper.writeValueAsString(requestPOJO), SSLValidationOff);
			System.out.println("Detection response:\n" + detectionResponse);

			JSONObject detectionJO = new JSONObject(detectionResponse);
			double detection_detection_time, detection_total_time, detectionTime;
			if (skyDetectionVersion.equals("new")) {
				double detection_sky_detection_time = detectionJO.getDouble("sky_inference_time");
				double detection_usable_detection_time = detectionJO.getDouble("usable_inference_time");
				detection_detection_time = detection_sky_detection_time + detection_usable_detection_time;
				detection_total_time = detectionJO.getDouble("total_time");
				detectionTime = System.currentTimeMillis() - start;
				System.out.println("Detection service responded in " + detectionTime + " s");
			} else {
				detection_detection_time = detectionJO.getDouble("detection_time");
				detection_total_time = detectionJO.getDouble("total_time");
				detectionTime = System.currentTimeMillis() - start;
				System.out.println("Detection service responded in " + detectionTime + " ms");
			}

			// parse the detection response to check which images should be forwarded to the sky localization
			// service and to construct a new request (where concepts have been removed)
			JSONArray detectionJA = detectionJO.getJSONArray("images");
			JSONArray localizationRequestJA;
			if (skyDetectionVersion.equals("new")) {
				localizationRequestJA = setSkyLocalizationFlag(detectionJA);
			} else {
				localizationRequestJA = setSkyLocalizationFlagOld(detectionJA);
			}

			double localization_loading_time = 0, localization_localization_time = 0, localization_encoding_time = 0,
					localization_total_time = 0, localizationTime = 0, mask_decoding_time = 0, heuristicTIme = 0,
					kourtidis_concomp_time = 0, kourtidis_monotonicity_time = 0, kourtidis_total_time = 0;

			// when parsing the sky localization response, put the results in a hashmap indexed by the image
			// paths, so that they can be easily (fast) retrieved when constructing the response.
			HashMap<String, RatioCalculationResults> ratiosMap = new HashMap<String, RatioCalculationResults>();
			if (localizationRequestJA.length() > 0) {
				// call the localization service to get sky masks
				System.out.println("Calling localization service for " + localizationRequestJA.length() + " images");
				start = System.currentTimeMillis();
				JSONObject localizationRequestJO = new JSONObject();
				localizationRequestJO.put("images", localizationRequestJA);
				String localizationResponse = ServiceCalls.makePostRequest(localizationEndpoint,
						localizationRequestJO.toString(), SSLValidationOff);
				JSONObject localizationResponseJO = new JSONObject(localizationResponse);
				localization_loading_time = localizationResponseJO.getDouble("loading_time");
				localization_localization_time = localizationResponseJO.getDouble("localization_time");
				localization_encoding_time = localizationResponseJO.getDouble("encoding_time");
				localization_total_time = localizationResponseJO.getDouble("total_time");
				localizationTime = System.currentTimeMillis() - start;
				System.out.println("FCN localization service responded in " + localizationTime + " s");

				// parse the response to reconstruct the masks and apply the method of Kourtidis!
				// the only purpose of this code is to populate correctly the ratiosMap HashMap
				JSONArray localizationResponseJA = localizationResponseJO.getJSONArray("images");
				long heuristicStart = System.currentTimeMillis();
				int withUsableSkyCounter = 0;
				for (int i = 0; i < localizationResponseJA.length(); i++) {
					JSONObject imageO = localizationResponseJA.getJSONObject(i);
					String relativeImgPath = imageO.getString("path");
					RatioCalculationResults imR = null;
					String encodedMaskString = imageO.getString("mask");
					// if something went wrong, mask string will be empty and an error field will be present
					if (encodedMaskString.length() > 0) { // mask could be calculated
						System.out.println("Applying heuristic algorithm on image: " + relativeImgPath);
						start = System.currentTimeMillis();
						boolean[][] mask = decodeMask(encodedMaskString, null);
						mask_decoding_time += System.currentTimeMillis() - start;
						// decide and define a path for the image's mask in case sky is usable for the image!
						String imageMaskOutputPath = null;
						if (outputMasks) {
							int index = relativeImgPath.lastIndexOf(".");
							String maskName = relativeImgPath.substring(0, index) + "_mask.png";
							imageMaskOutputPath = imagesRootFolder + maskName;
						}
						// call the method of Kourtidis on the FCN-masked image
						try {
							imR = SkyLocalizationAndRatios.processImage(imagesRootFolder + relativeImgPath, mask,
									imageMaskOutputPath);
						} catch (Exception e) {
							imR = new RatioCalculationResults(e.getMessage());
						}
						if (imR.getMeanGB() > 0) {
							withUsableSkyCounter++;
						}
					} else { // mask could not be calculated, store the error message in ImageRatioData
						System.out.println("FCN mask could not be calculated on image: " + relativeImgPath);
						String errorMessage = imageO.getString("sl_error");
						// System.out.println("Error: " + errorMessage); // print the error
						imR = new RatioCalculationResults(errorMessage);
					}
					ratiosMap.put(relativeImgPath, imR);
				}
				System.out.println(
						withUsableSkyCounter + " of " + localizationResponseJA.length() + " photos are usable!");
				heuristicTIme = System.currentTimeMillis() - heuristicStart;
			}

			// System.out.println("# with fcn mask: " + ratiosMap.size());

			// create the response using the detectionJA and detectionJO objects
			// and calculate internal times of kourtidis algorithm
			JSONArray responseJA = new JSONArray();
			for (int i = 0; i < detectionJA.length(); i++) {
				JSONObject responseImageJO = new JSONObject();

				String path = detectionJA.getJSONObject(i).getString("path");
				responseImageJO.put("path", path);

				try { // check if there was a problem in concept detection
					if (skyDetectionVersion.equals("new")) {
						double sky_conf = detectionJA.getJSONObject(i).getDouble("sky_conf");
						responseImageJO.put("sky_conf", sky_conf);
						if (sky_conf > 0.5) {
							double usable_conf = detectionJA.getJSONObject(i).getDouble("usable_conf");
							responseImageJO.put("usable_conf", usable_conf);
						}
					} else {
						JSONObject concepts = detectionJA.getJSONObject(i).getJSONObject("concepts");
						responseImageJO.put("concepts", concepts);
					}

					boolean containsSky = detectionJA.getJSONObject(i).getBoolean("containsSky");
					responseImageJO.put("containsSky", containsSky);

				} catch (JSONException e) {
					String cd_error = detectionJA.getJSONObject(i).getString("cd_error");
					responseImageJO.put("cd_error", cd_error);
				}

				try { // in the case of URLs
					String id = ((JSONObject) detectionJA.get(i)).getString("id");
					responseImageJO.put("id", id);
				} catch (JSONException e) {
					// no problem
				}

				// check if ratio information is available
				if (ratiosMap.containsKey(path)) { // update
					// System.out.println("Found in R/G map: " + path);
					RatioCalculationResults imR = ratiosMap.get(path);
					if (imR.getExceptionMessage() != null) {
						responseImageJO.put("sl_error", imR.getExceptionMessage());
					} else {
						// System.out.println("# sky pixels" + skyPixels);
						if (imR.getMeanGB() > 0) { // usable sky detected!
							responseImageJO.put("usableSky", true);
							double rg = imR.getMeanRG();
							double gb = imR.getMeanGB();
							int allPixels = imR.getAllPixels();
							int skyPixels = imR.getNumSkyPixels();
							responseImageJO.put("R/G", rg);
							responseImageJO.put("G/B", gb);
							responseImageJO.put("sky_pixels", skyPixels);
							responseImageJO.put("all_pixels", allPixels);
						} else {
							responseImageJO.put("usableSky", false);
						}
						kourtidis_concomp_time += imR.getConCompTime();
						kourtidis_monotonicity_time += imR.getMonotonicityTime();
						kourtidis_total_time += imR.getTotalTime();
					}
				}
				// else {
				// System.out.println("R/G not found for :" + path);
				// }
				responseJA.put(responseImageJO);
			}

			// append already processed images to response
			for (Image im : alreadyProcessed) {
				String imString = mapper.writeValueAsString(im);
				JSONObject responseImageJO = new JSONObject(imString);
				responseJA.put(responseImageJO);
			}

			responseJson = new JSONObject();
			responseJson.put("images", responseJA);

			// creating a time JSON object to store all processing times
			// this is useful when testing but can be removed in production to reduced network overhead
			if (debug) { // append times to the response only when on debug mode
				JSONObject timeJO = new JSONObject();

				JSONObject detectionTimeJO = new JSONObject();
				detectionTimeJO.put("total", detectionTime);
				detectionTimeJO.put("detection", detection_detection_time);
				detectionTimeJO.put("total_int", detection_total_time);

				JSONObject localizationTimeJO = new JSONObject();
				localizationTimeJO.put("total", localizationTime);
				localizationTimeJO.put("reading", localization_loading_time);
				localizationTimeJO.put("localization", localization_localization_time);
				localizationTimeJO.put("encoding", localization_encoding_time);
				localizationTimeJO.put("total_int", localization_total_time);

				JSONObject kourtidisTimeJO = new JSONObject();
				kourtidisTimeJO.put("total", heuristicTIme);
				// kourtidisTimeJO.put("decoding", mask_decoding_time);
				kourtidisTimeJO.put("concomp", kourtidis_concomp_time);
				kourtidisTimeJO.put("monotonicity", kourtidis_monotonicity_time);
				kourtidisTimeJO.put("total_int", kourtidis_total_time);

				// timeJO.put("waiting", waitingTime);
				timeJO.put("download", downloadTime);
				timeJO.put("detection", detectionTimeJO);
				timeJO.put("localization", localizationTimeJO);
				timeJO.put("kourtidis", kourtidisTimeJO);

				responseJson.put("time", timeJO);
			}

		} catch (Exception e) {
			e.printStackTrace();
			exceptionMessage = e.getMessage();
		}
		actualExecutionTime = System.currentTimeMillis() - startAll;
	}

	public String getExceptionMessage() {
		return exceptionMessage;
	}

	public JSONObject getResponseJson() {
		return responseJson;
	}

	public long getActualExecutionTime() {
		return actualExecutionTime;
	}

	/**
	 * Process the request's json to transform all paths to local paths. I.e. for each path that is a URL,
	 * download the image from that URL and replace the path with a local path.
	 * 
	 * @param requestJO
	 */
	public void transformToLocalPaths(JSONObject requestJO) {

		ArrayList<IdAndUrl> urls = new ArrayList<IdAndUrl>(); // where all URLs will be kept
		JSONArray ja = requestJO.getJSONArray("images");
		for (int i = 0; i < ja.length(); i++) {
			JSONObject entry = ja.getJSONObject(i);
			// extract path
			String path = entry.getString("path");
			// check if path is a URL
			boolean isURL = false;
			try {
				new URL(path); // just to check if valid url
				String id = entry.getString("id");
				urls.add(new IdAndUrl(id, path));
				isURL = true;
			} catch (MalformedURLException e) { // not a URL
				// e.printStackTrace();
			}
			entry.put("isURL", isURL); // temporarily set this field

			// if (!isURL) {
			// // check if path exists on server
			// boolean found = false;
			// if (new File(imagesRoot + path).exists()) {
			// found = true;
			// }
			// entry.put("found", found);
			// }
		}

		// multi-threaded download of all URLs!
		if (urls.size() > 0) {
			HashSet<String> failedDownloadIds = ImageDownloader
					.downloadImages(imagesRootFolder + imageDownloadSubfolder, urls);

			// iterate once again through the list and set the local paths of URLs!
			for (int i = 0; i < ja.length(); i++) {
				JSONObject entry = ja.getJSONObject(i);
				boolean isURL = entry.getBoolean("isURL");
				if (isURL) {
					// check if was downloaded successfully
					String id = entry.getString("id");
					if (!failedDownloadIds.contains(id)) {
						// update the path
						String path = entry.getString("path");
						// System.out.println(path);
						int lastIndexOfDot = path.lastIndexOf(".");
						String extension = path.substring(lastIndexOfDot, path.length());
						// System.out.println(extension);
						String localPath = imageDownloadSubfolder + id + extension;
						entry.put("path", localPath);
						// entry.put("found", true);
					} else {
						// entry.put("found", false);
					}
				}
				// remove the isURL field
				entry.remove("isURL");
			}
		}

	}

	/**
	 * Process the request's json to transform all paths to local paths. I.e. for each path that is a URL,
	 * download the image from that URL and replace the path with a local path.
	 * 
	 * @param requestJO
	 */
	public void transformToLocalPaths(IARequest requestJO) {

		ArrayList<IdAndUrl> urls = new ArrayList<IdAndUrl>(); // where all URLs will be kept
		ArrayList<Image> images = requestJO.getImages();
		for (Image im : images) {
			if (im.pathIsURL()) {
				urls.add(new IdAndUrl(im.getId(), im.getPath()));
			}
		}

		// multi-threaded download of all URLs
		if (urls.size() > 0) {
			HashSet<String> failedDownloadIds = ImageDownloader
					.downloadImages(imagesRootFolder + imageDownloadSubfolder, urls);

			// iterate once again through the list and set the local paths of URLs!
			for (Image im : images) {
				if (im.pathIsURL()) {
					// check if was downloaded successfully
					String id = im.getId();
					if (!failedDownloadIds.contains(id)) {
						// update the original path (URL)
						String path = im.getPath();
						// System.out.println(path);
						int lastIndexOfDot = path.lastIndexOf(".");
						String extension = path.substring(lastIndexOfDot, path.length());
						// System.out.println(extension);
						String localPath = imageDownloadSubfolder + id + extension;
						im.setPath(localPath);
					}
				}
			}
		}

	}

	/**
	 * Sets the containsSky and usableSky flags to the given JSON array and returns a new JSON array that contains
	 * only the images (paths) that should undergo localization.
	 * 
	 * @param inputJA
	 * @return
	 */
	public JSONArray setSkyLocalizationFlag(JSONArray inputJA) {
		JSONArray outputJA = new JSONArray();
		for (int i = 0; i < inputJA.length(); i++) {
			JSONObject entry = inputJA.getJSONObject(i);

			double skyConf = entry.getDouble("sky_conf");
			boolean containsSky = false;
			boolean usableSky = false;
			if (skyConf >= skyThreshold) {
				containsSky = true;
				if (entry.has("usable_conf")) {
					double usableConf = entry.getDouble("usable_conf");
					if (usableConf >= usableSkyThreshold) {
						usableSky = true;
						// if both values are above threshold, image should be sent for localization
						JSONObject joOut = new JSONObject();
						joOut.put("path", entry.getString("path"));
						outputJA.put(joOut);
					}
				}
			}
			entry.put("containsSky", containsSky); // set the sky flag appropriately
			entry.put("usableSky", usableSky); // set the usable sky flag appropriately

		}
		return outputJA;
	}

	/**
	 * Sets the containsSky flag to the given JSON array and returns a new JSON array that contains only the
	 * images (paths) that should undergo localization. If for some reason the containsSky flag cannot be set, a
	 * "sky detection failed" message is used
	 * 
	 * @param inputJA
	 * @return
	 */
	public JSONArray setSkyLocalizationFlagOld(JSONArray inputJA) {
		JSONArray outputJA = new JSONArray();
		for (int i = 0; i < inputJA.length(); i++) {
			JSONObject entry = inputJA.getJSONObject(i);

			try {
				double skyConf = entry.getJSONObject("concepts").getDouble("sky");
				// double cloudsConf = ((JSONObject) entry.get("concepts")).getDouble("clouds");
				// double sunConf = ((JSONObject) entry.get("concepts")).getDouble("sun");
				boolean containsSky = false;
				if (skyConf >= skyThreshold) { // set sky flag = true;
					containsSky = true;
					JSONObject joOut = new JSONObject();
					joOut.put("path", entry.getString("path"));
					outputJA.put(joOut);
				}
				entry.put("containsSky", containsSky);
			} catch (JSONException e) {
				// concept detection has probably failed, get the exception message
				// entry.put("containsSky", "sky detection failed");
			}
		}
		return outputJA;
	}

	private void parseSetttingsObj() throws Exception {
		skyDetectionVersion = settingsObj.getString("skyDetectionVersion");
		if (!skyDetectionVersion.equals("new") && !skyDetectionVersion.equals("old")) {
			throw new Exception("skyDetectionVersion in settings file should be either 'new' or 'old'");
		}
		skyThreshold = settingsObj.getDouble("skyThreshold");
		if (skyDetectionVersion.equals("new")) { // parse the usable sky threshold as well
			usableSkyThreshold = settingsObj.getDouble("usableSkyThreshold");
		}
		imagesRootFolder = settingsObj.getString("imagesRoot");
		imageDownloadSubfolder = settingsObj.getString("imagesDownload");

		detectionEndpoint = settingsObj.getString("detectionEndpoint");
		localizationEndpoint = settingsObj.getString("localizationEndpoint");

		processUrls = settingsObj.getBoolean("processUrls");
		outputMasks = settingsObj.getBoolean("outputMasks");
		SSLValidationOff = settingsObj.getBoolean("SSLValidationOff");
	}

	public static boolean[][] decodeMask(String encodedString, String decodedFile) throws Exception {
		boolean[][] decoded = null;
		int rowIndex = 0;
		try {
			String[] parts = encodedString.split("\\|", 2);
			String size = parts[0];
			String mask = parts[1];
			int height = Integer.parseInt(size.split("X")[0]);
			int width = Integer.parseInt(size.split("X")[1]);
			decoded = new boolean[height][];

			ArrayList<Character> rowCharsList = new ArrayList<Character>();
			for (int i = 0; i < mask.length(); i++) {
				char c = mask.charAt(i);
				if (c != 'R') { // append row
					rowCharsList.add(c);
				} else { // new row
					decoded[rowIndex] = new boolean[width];
					// decode previous row
					if (rowCharsList.size() > 0) { // previous row is not empty
						// transform arraylist of characters to String for easier manipulation
						char[] rowCharsArray = new char[rowCharsList.size()];
						for (int j = 0; j < rowCharsList.size(); j++) {
							rowCharsArray[j] = rowCharsList.get(j);
						}
						String row = String.valueOf(rowCharsArray);
						String[] segments = row.split(",");
						for (String segment : segments) {
							if (!segment.contains("-")) {
								int index = Integer.parseInt(segment);
								decoded[rowIndex][index] = true;
							} else {
								int startIndex = Integer.parseInt(segment.split("-", 2)[0]);
								int endIndex = Integer.parseInt(segment.split("-", 2)[1]);
								for (int j = startIndex; j <= endIndex; j++) {
									decoded[rowIndex][j] = true;
								}
							}
						}
						// empty previous row
						rowCharsList.clear();
					}
					rowIndex++;
				}
			}

			if (decodedFile != null) {
				BufferedWriter out = new BufferedWriter(new FileWriter(new File(decodedFile)));
				for (int i = 0; i < decoded.length; i++) {
					StringBuffer sb = new StringBuffer();
					for (int j = 0; j < decoded[i].length; j++) {
						if (decoded[i][j]) {
							sb.append("1 ");
						} else {
							sb.append("0 ");
						}
					}
					String row = sb.toString();
					out.write(row.substring(0, row.length() - 1) + "\n");
				}
				out.close();
			}
		} catch (Exception e) {
			System.out.println("Problem at row: " + (rowIndex + 1));
			throw e;
		}
		return decoded;
	}
}
