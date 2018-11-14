package gr.iti.mklab.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import gr.iti.mklab.utils.ServiceCalls;
import gr.mklab.SkyLocalizationAndRatiosOld;
import gr.mklab.classes.RatioCalculationResults;

public class IAServicesConsumer {

	public static final String localizationServiceEndpoint = "http://160.40.50.236:8080/SkyLocalizationFCN/post";
	public static String detectionServiceEndpoint;
	public static String analysisServiceEndpoint;

	public static void main(String args[]) throws Exception {

		// == declare service host
		String servicesHost = "http://173.212.212.242:8080"; // 173.212.212.242 OR 160.40.50.230
		// == declare whether to send URLs instead of paths (URLs work only for IA service)
		boolean urls = false;
		// == declare images folder or image URLs folder
		// String imagesFolderPathFromThisPC = args[0]; // used to test remotely from a jar
		// String imagesFolderPathFromThisPC = "//160.40.51.145/Data/Images/flickr/2017-02-24/";
		// String imagesFolderPathFromThisPC = "//160.40.51.145/Data/Images/masks/";
		String imagesFolderPathFromThisPC = "F:/test/";
		String imagesURLfolder = "http://173.212.212.242:8081/test/";
		// == declare relative path in service computer
		// String imagesRelativePathInServicePC = "flickr/2017-02-24/";
		String imagesRelativePathInServicePC = "test/";
		// == declare from-to range of images to send to service
		int fromIndex = 60;
		int toIndex = 70;
		// == declare number of times to call the service
		int numCalls = 1;
		// == declare whether wrong images should be added to test response
		boolean addWrong = false;
		// == declare whether debug should be requested in response
		boolean debug = true;

		detectionServiceEndpoint = servicesHost + "/ConceptDetectionService-0.0.1-SNAPSHOT/cd/v1/";
		analysisServiceEndpoint = servicesHost + "/ImageAnalysisService-v1/post/";

		// create a list with the name of the images that will be sent to the service
		File dir = new File(imagesFolderPathFromThisPC);
		String[] imageNames = dir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				// return true;
				return name.toLowerCase().endsWith(".jpg");
			}
		});

		String request;
		if (urls) {
			request = createRequestURLs(imageNames, imagesURLfolder, fromIndex, toIndex, addWrong, debug);
		} else {
			request = createRequestPaths(imageNames, imagesRelativePathInServicePC, fromIndex, toIndex, addWrong,
					debug);
		}

		// testSkyDetectionService(request, numCalls); // tested OK!!!
		testSkyLocalizationService(request, numCalls); // tested OK!!!
		// validateMasks(imageNames, request, testMasksFolder); // tested OK!!!
		// testKourtidisCode(request, imagesRootFolder); // tested OK!!!
		// String imageAnalysisResponse = testImageAnalysisService(request, numCalls);

	}

	private static void validateMasks(String[] imageNames, String request, String masksFolder) throws Exception {
		String localizationResponse = testSkyLocalizationService(request, 1);
		JSONArray ja = new JSONObject(localizationResponse).getJSONArray("images");
		long decodingTime = 0;
		for (int maskIndex = 0; maskIndex < ja.length(); maskIndex++) {
			System.out.print("Testing image: " + imageNames[maskIndex] + " ...");
			String mask = ja.getJSONObject(maskIndex).getString("mask");
			BufferedWriter out = new BufferedWriter(
					new FileWriter(new File(masksFolder + imageNames[maskIndex] + "-enc.txt")));
			out.write(mask);
			out.close();
			long start = System.currentTimeMillis();
			String decodedFile = null;
			boolean[][] decoded = decodeMask(mask, decodedFile);
			decodingTime += System.currentTimeMillis() - start;

			// check if the decoded mask matches the original
			BufferedReader in = new BufferedReader(
					new FileReader(new File(masksFolder + imageNames[maskIndex] + ".txt")));
			String line;
			int lineCounter = 0;
			while ((line = in.readLine()) != null) {
				String[] parts = line.split(" ");
				for (int i = 0; i < parts.length; i++) {
					boolean isSky = parts[i].equals("1");
					if (isSky != decoded[lineCounter][i]) {
						throw new Exception("Masks dont match!");
					}
				}
				lineCounter++;
			}
			in.close();

			System.out.println("success!");
		}
		System.out.print("Average decoding time: " + (double) decodingTime / imageNames.length);
	}

	public static void testKourtidisCode(String request, String imagesFolder) throws Exception {
		String response = ServiceCalls.makePostRequest(localizationServiceEndpoint, request, "UTF-8");
		System.out.println(response);
		JSONArray ja = new JSONObject(response).getJSONArray("images");

		for (int i = 0; i < ja.length(); i++) {
			JSONObject jo = (JSONObject) ja.get(i);
			String imagePath = jo.getString("path");
			System.out.println("Image: " + imagePath);
			String encodedMaskString = jo.getString("mask");
			boolean[][] mask = decodeMask(encodedMaskString, null);
			RatioCalculationResults imr = SkyLocalizationAndRatiosOld.processImage(imagesFolder + imagePath, mask, "");
			imr.printTimes();
		}

	}

	public static String createRequestPaths(String[] imageNames, String relativePath, int from, int to,
			boolean addWrong, boolean debug) {
		String debugString = "";
		if (debug) {
			debugString = "\"debug\":true,";
		}
		String request = "{" + debugString + "\"images\":[";
		for (int i = from; i < Math.min(to, imageNames.length); i++) {
			request += "{\"path\":\"" + relativePath + imageNames[i] + "\"},";
		}
		if (addWrong) {
			request += "{\"path\":\"whateverWrongPathForTestingPurposes.jpg\"},";
		}
		request = request.substring(0, request.length() - 1); // eat last ,
		request += "]}";
		return request;
	}

	public static String createRequestURLs(String[] imageNames, String urlsRoot, int from, int to, boolean addWrong,
			boolean debug) {
		String debugString = "";
		if (debug) {
			debugString = "\"debug\":true,";
		}
		String request = "{" + debugString + "\"images\":[";
		for (int i = from; i < Math.min(to, imageNames.length); i++) {
			request += "{\"path\":\"" + urlsRoot + imageNames[i] + "\",\"id\":\"" + imageNames[i].split("\\.")[0]
					+ "\"},";
		}
		if (addWrong) {
			request += "{\"path\":\"whateverWrongPathForTestingPurposes.jpg\"},";
		}
		request = request.substring(0, request.length() - 1); // eat last ,
		request += "]}";
		return request;
	}

	public static String testImageAnalysisService(String request, int numCalls) throws Exception {
		String prevResponse = "";
		for (int i = 0; i < numCalls; i++) {
			System.out.println("Request");
			long start = System.currentTimeMillis();
			String response = ServiceCalls.makePostRequest(analysisServiceEndpoint, request, "UTF-8");
			long analysisTime = System.currentTimeMillis() - start;
			System.out.println("Response (" + analysisTime + " ms): " + response);

			if (i > 0) {
				if (!prevResponse.equals(response)) {
					throw new Exception("Responses should be identical!");
				}
			}
			prevResponse = response;
		}
		return prevResponse;
	}

	public static String testSkyLocalizationService(String request, int numCalls) throws Exception {
		double loading_time = 0, localization_time = 0, encoding_time = 0, pngmask_time = 0, total_time_int = 0,
				total_time_ext = 0;
		String response = "";
		for (int i = 0; i < numCalls; i++) { // test 10 times and record results
			long start = System.currentTimeMillis();
			response = ServiceCalls.makePostRequest(localizationServiceEndpoint, request, "UTF-8");
			long this_time = System.currentTimeMillis() - start;
			System.out.println("Response (" + this_time + " ms): " + response);
			total_time_ext += this_time / 1000.0;
			JSONObject jo = new JSONObject(response);
			loading_time += jo.getDouble("loading_time");
			localization_time += jo.getDouble("localization_time");
			encoding_time += jo.getDouble("encoding_time");
			// pngmask_time += jo.getDouble("pngmask_time");
			total_time_int += jo.getDouble("total_time");

			Thread.sleep(1000); // wait for 1 sec
		}
		System.out.println("Loading time: " + loading_time / numCalls);
		System.out.println("Localization time: " + localization_time / numCalls);
		System.out.println("Encoding time: " + encoding_time / numCalls);
		// System.out.println("Png mask time: " + pngmask_time / numIters);
		System.out.println("Total int time: " + total_time_int / numCalls);
		System.out.println("Total ext time: " + total_time_ext / numCalls);

		return response;
	}

	public static void testSkyDetectionService(String request, int numCalls) throws Exception {
		long waiting_time = 0, detection_time = 0, total_time_int = 0, total_time_ext = 0;
		for (int i = 0; i < numCalls; i++) { // test 10 times and record results
			long start = System.currentTimeMillis();
			String response = ServiceCalls.makePostRequest(detectionServiceEndpoint, request, "UTF-8");
			long this_time = System.currentTimeMillis() - start;
			System.out.println("Response (" + this_time + " ms): " + response);
			total_time_ext += this_time;
			JSONObject jo = new JSONObject(response);
			waiting_time += jo.getLong("waiting_time");
			detection_time += jo.getLong("detection_time");
			total_time_int += jo.getLong("total_time");
		}
		System.out.println("Waiting time: " + (double) waiting_time / numCalls);
		System.out.println("Detection time: " + (double) detection_time / numCalls);
		System.out.println("Total int time: " + (double) total_time_int / numCalls);
		System.out.println("Total ext time: " + (double) total_time_ext / numCalls);
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

	public static String encodeMask(String file, String encodedFile) throws Exception {

		BufferedReader in = new BufferedReader(new FileReader(file));
		String line;
		StringBuffer sb = new StringBuffer();
		int height = 0, width = 0;
		while ((line = in.readLine()) != null) {
			String[] parts = line.split(" ");
			String sparseRow = "";
			ArrayList<Integer> conseq = new ArrayList<Integer>();
			for (int i = 0; i < parts.length; i++) {
				if (parts[i].equals("1")) {
					conseq.add(i);
				}
				if (parts[i].equals("0") || (parts[i].equals("1") && (i == parts.length - 1))) {
					if (conseq.size() > 0) {
						String conseqString = listAsCompressedString(conseq);
						sparseRow += conseqString + ",";
						conseq.clear(); // empty list
					}
				}
			}
			// eat last "," if needed
			if (sparseRow.length() > 0) {
				sparseRow = sparseRow.substring(0, sparseRow.length() - 1);
			}
			sparseRow += "R";
			sb.append(sparseRow);
			height++;
			width = parts.length;
		}
		in.close();

		String encoded = height + "X" + width + "|" + sb.toString();

		if (encodedFile != null) {
			BufferedWriter out = new BufferedWriter(new FileWriter(new File(encodedFile)));
			out.write(encoded);
			out.close();
		}

		return encoded;
	}

	public static String listAsCompressedString(ArrayList<Integer> conseq) {
		if (conseq.size() == 1) {
			return conseq.get(0).toString();
		} else {
			return conseq.get(0).toString() + "-" + conseq.get(conseq.size() - 1);
		}
	}

}
