package gr.iti.mklab.flickr;


import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;

import gr.iti.mklab.utils.MongoConnection;
import gr.iti.mklab.utils.Utils;

/**
 * Deprecated!<br>
 * Helper class used to update the source_info field of existing collection items.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 *
 */
public class FlickrCollectorUpdateSourceInfo {

	// The CET timezone should always be assumed
	public static final String DATE_FORMAT = "dd-MM-yyyy HH:mm:ss";

	public static boolean verbose = true;
	// The crawl start time in the CET time zone. Crawl starts now if null.
	public static String crawlStartString = "01-04-2017 00:00:00"; // e.g. 31-12-2017 24:00:00
	// The crawl end time in the CET time zone. Defaults to 30 days from now if null.
	public static String crawlEndString = "01-05-2017 00:00:00"; // e.g. 31-12-2017 24:00:00

	public static String flickrApiKey = "adf14621cb57ec92f0161703c87ea38c";

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		// Mongo connection
		MongoConnection mongo = new MongoConnection("hackair_v2", "flickr", "localhost");
		MongoCollection<Document> collection = mongo.getCollection();

		String query = "europe,woeid,24865675";
		FlickrQuery fq = new FlickrQuery(query);
		Utils.outputMessage("Crawl for query: " + fq.toString(), verbose);

		LocalDateTime ldt = LocalDateTime.parse(crawlEndString, DateTimeFormatter.ofPattern(DATE_FORMAT));
		ZonedDateTime CETZonedDateTime = ldt.atZone(ZoneId.of("CET"));
		Utils.outputMessage("Crawl for images with max date: " + CETZonedDateTime, verbose);
		long max_taken_date = CETZonedDateTime.toInstant().getEpochSecond();

		ldt = LocalDateTime.parse(crawlStartString, DateTimeFormatter.ofPattern(DATE_FORMAT));
		CETZonedDateTime = ldt.atZone(ZoneId.of("CET"));
		Utils.outputMessage("Crawl for images with min date: " + CETZonedDateTime, verbose);
		long min_taken_date = CETZonedDateTime.toInstant().getEpochSecond();

		FlickrQueryManager fqm = new FlickrQueryManager(flickrApiKey);

		String flickrQuery = fqm.buildQuery(min_taken_date, max_taken_date, 1, fq);
		Utils.outputMessage("Generating calls for query: " + flickrQuery, verbose);
		ArrayList<String> calls = fqm.generateRequiredApiCalls(flickrQuery);

		Utils.outputMessage("Call generation completed, " + calls.size() + " calls will be performed.", verbose);

		// use an ArryList to store the results of all flickr calls
		ArrayList<JSONObject> flickrResults = new ArrayList<JSONObject>();

		int callCounter = 0;
		int numCallsFailed = 0;
		for (String call : calls) {
			Utils.outputMessage("Call " + (callCounter + 1) + "/" + calls.size(), verbose);
			// add extra parameters to the call
			call = FlickrQueryManager.addExtraParameter(call, "tags,description,license,date_upload,date_taken");

			String response;
			try {
				response = fqm.throttledGetApiResponse(call, FlickrQueryManager.maxCallRetries);
			} catch (Exception e) {
				System.err.println("Call failed with the following exception: ");
				e.printStackTrace();
				System.err.println("Continuing with the next call.");
				numCallsFailed++;
				continue;
			}
			callCounter++;

			JSONObject json = new JSONObject(response);

			JSONArray photoArr = json.getJSONObject("photos").getJSONArray("photo");
			for (int i = 0; i < photoArr.length(); i++) {
				flickrResults.add(photoArr.getJSONObject(i));
			}
		}
		if (numCallsFailed > 0) {
			Utils.outputMessage(numCallsFailed + " calls failed!", verbose);
		}

		Utils.outputMessage("Calls returned " + flickrResults.size() + " results for this query.", verbose);

		updateCollectionFromFlicrkCrawl(flickrResults, collection);

	}

	private static void updateCollectionFromFlicrkCrawl(ArrayList<JSONObject> flickrResults,
			MongoCollection<Document> collection) throws Exception {

		int numMatched = 0;
		int numMod = 0;
		for (JSONObject fr : flickrResults) {
			String id = fr.getString("id");
			String tags = fr.getString("tags");
			String desc = ((JSONObject) fr.get("description")).getString("_content");
			String license = fr.getString("license");

			// Document updateDoc = new Document("source_info.license", license).append("source_info.tags",
			// tags)
			// .append("source_info.description", desc);
			Document updateDoc = new Document("source_info.description", desc);

			Document updateQuery = new Document();
			updateQuery.append("$set", updateDoc);

			Document searchQuery = new Document();
			searchQuery.append("source_info.id", id);

			UpdateResult res = collection.updateOne(searchQuery, updateQuery);
			long matchedCount = res.getMatchedCount();
			long modCount = res.getModifiedCount();

			if (matchedCount > 1 || modCount > 1 || matchedCount < 0 || modCount < 0) {
				throw new Exception("Strange!");
			}

			System.out.println("Id: " + id + "\t" + matchedCount + "\t" + modCount);
			numMatched += matchedCount;
			numMod += modCount;

		}
		System.out.println("Total matched: " + numMatched + " of " + flickrResults.size());
		System.out.println("Total mod: " + numMod + " of " + flickrResults.size());

	}

}
