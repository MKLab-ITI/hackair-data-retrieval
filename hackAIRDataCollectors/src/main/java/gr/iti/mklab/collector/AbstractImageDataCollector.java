package gr.iti.mklab.collector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mongodb.client.MongoCollection;

import gr.iti.mklab.utils.ServiceCalls;
import gr.iti.mklab.utils.Utils;

public abstract class AbstractImageDataCollector extends AbstractDataCollector {

	public static boolean downloadImages;
	public static String imageDownloadFolder;
	public static boolean deleteFailedDownloadsFromMongo;
	public static String imageAnalysisEndpoint;
	public static final int imageAnalysisBatchSize = 50;

	protected static void loadCrawlSettings(String crawlSettingsFile) throws Exception {
		// parse general data collection settings using the hyperclass method
		AbstractDataCollector.loadCrawlSettings(crawlSettingsFile);

		// them parse more specific image collecting settings in this method
		StringBuffer sb = new StringBuffer();
		String line;
		BufferedReader in = new BufferedReader(new FileReader(new File(crawlSettingsFile)));
		while ((line = in.readLine()) != null) {
			sb.append(line);
		}
		in.close();
		JSONObject json = new JSONObject(sb.toString());
		JSONObject crawlSettingsObj = (JSONObject) json.getJSONArray("crawl_settings").get(0);

		downloadImages = crawlSettingsObj.getBoolean("downloadImages");
		if (downloadImages) {
			deleteFailedDownloadsFromMongo = crawlSettingsObj.getBoolean("deleteFailedDownloadsFromMongo");
			// the download folder path should always be absolute for the concept detection to work
			imageDownloadFolder = crawlSettingsObj.getString("imageDownloadFolder");
			// transform it to absolute path!
			Path path = Paths.get(imageDownloadFolder);
			Path absolute = path.toAbsolutePath();
			imageDownloadFolder = absolute.toString() + "/";
		}

		imageAnalysisEndpoint = crawlSettingsObj.getString("imageAnalysisEndpoint");

	}

	/**
	 * Update the collection using the results of image analysis.
	 * 
	 * @param response
	 * @param collection
	 */
	protected static void updateCollectionFromImageAnalysis(String iaResponse, MongoCollection<Document> collection) {

		JSONArray iaResponseJA;
		try {
			iaResponseJA = new JSONObject(iaResponse).getJSONArray("images");
		} catch (JSONException e) {
			System.err.println("Image analysis response has problems!");
			return;
		}

		for (int i = 0; i < iaResponseJA.length(); i++) {
			JSONObject entry = iaResponseJA.getJSONObject(i);
			String id = entry.getString("id");
			entry.remove("id");
			entry.remove("path");

			Document doc = Document.parse(entry.toString());

			Document updateQuery = new Document();
			updateQuery.append("$set", new Document().append("source_info.ia", doc));

			Document searchQuery = new Document();
			searchQuery.append("source_info.id", id);

			try {
				try {
					collection.updateOne(searchQuery, updateQuery);
				} catch (Exception e) {
					// wait a bit and re-try
					Thread.sleep(500);
					collection.updateOne(searchQuery, updateQuery);
				}
			} catch (Exception e) {
				System.err.println("Updating entry: " + id + " failed!");
			}
		}
	}

	protected static String sendForImageAnalysis(List<Document> photoDocsDownloaded) {
		if (photoDocsDownloaded.size() == 0) {
			return null;
		}

		// process docs to make them suitable for the image analysis call
		JSONArray iaRequestJA = new JSONArray();
		for (Document doc : photoDocsDownloaded) {
			String path = ((Document) doc.get("source_info")).getString("path");
			String id = ((Document) doc.get("source_info")).getString("id");
			JSONObject entry = new JSONObject();
			entry.put("path", path);
			entry.put("id", id);
			iaRequestJA.put(entry);
		}
		JSONObject iaRequestJO = new JSONObject();
		iaRequestJO.put("images", iaRequestJA);

		String response = null;
		try {
			response = ServiceCalls.makePostRequestSSL(imageAnalysisEndpoint, iaRequestJO.toString(), "UTF-8", true);
			Utils.outputMessage("Response from Image Analysis:\n" + response, verbose);
		} catch (Exception e) {
			System.err.println("Call to Image Analysis failed with Exception: " + e.getMessage());
		}
		return response;
	}


}
