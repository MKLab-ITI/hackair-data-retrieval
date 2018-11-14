package gr.iti.mklab.collector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.bson.Document;
import org.json.JSONObject;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoWriteException;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.InsertManyOptions;

import gr.iti.mklab.utils.Utils;

/**
 * This class abstracts some common functionality of all data collection classes.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 *
 */
public abstract class AbstractDataCollector {

	// The CET timezone should always be assumed
	public static final String DATE_FORMAT = "dd-MM-yyyy HH:mm:ss";
	public static final long oneDaySecs = 86400;
	public static final boolean bulkInserts = true;

	public static boolean verbose;
	public static long crawlIntervalSecs;
	// The crawl start time in the CET time zone. Crawl starts now if null.
	public static String crawlStartString; // e.g. 31-12-2017 24:00:00
	// The crawl end time in the CET time zone. Defaults to 1 year from now if null.
	public static String crawlEndString; // e.g. 31-12-2017 24:00:00
	public static String mongoSettingsFile;

	protected static void loadCrawlSettings(String crawlSettingsFile) throws Exception {
		// initialize crawl settings from the settings file
		StringBuffer sb = new StringBuffer();
		String line;
		BufferedReader in = new BufferedReader(new FileReader(new File(crawlSettingsFile)));
		while ((line = in.readLine()) != null) {
			sb.append(line);
		}
		in.close();
		JSONObject json = new JSONObject(sb.toString());
		JSONObject mongoSettingsObj = (JSONObject) json.getJSONArray("crawl_settings").get(0);

		verbose = mongoSettingsObj.getBoolean("verbose");
		crawlIntervalSecs = mongoSettingsObj.getLong("crawlIntervalSecs");
		if (crawlIntervalSecs < 1800) {
			throw new Exception("Too small crawl interval, try something >= 1800 (s)!");
		}
		crawlStartString = mongoSettingsObj.getString("crawlStartString");
		crawlEndString = mongoSettingsObj.getString("crawlEndString");
		mongoSettingsFile = mongoSettingsObj.getString("mongoSettingsFile");

	}

	/**
	 * Attempts to write a given list of Documents to the given MongoCollection. Returns a list of the documents that
	 * were actually written (because e.g. duplicate documents are not written).
	 * 
	 * @param docs
	 * @param collection
	 * @return
	 */
	protected static ArrayList<Document> writeDocumentsToMongo(ArrayList<Document> docs,
			MongoCollection<Document> collection, String uniqueField) {
		if (docs.size() == 0) { // if the list is empty, just return it
			return docs;
		}
		long countBefore = collection.count();
		long start = System.currentTimeMillis();
		ArrayList<Document> docsWritten = writeToMongo(collection, docs, uniqueField, bulkInserts);
		long end = System.currentTimeMillis();
		Utils.outputMessage("Mongo update took " + (end - start) + " ms", verbose);
		long countAfter = countBefore + docsWritten.size();
		// long countAfterReal = collection.count();
		// if (countAfter != countAfterReal) {
		// System.err.println("Calculation of inserted ids was wrong!");
		// }
		long numDuplicates = docs.size() - docsWritten.size();
		Utils.outputMessage("Mongo updated. Previous size: " + countBefore + " Current size: " + countAfter + " ("
				+ numDuplicates + " of " + docs.size() + " docs were duplicates)", verbose);

		return docsWritten;
	}

	/**
	 * Writes a list of documents to mongo using either one-by-one or bulk insert.
	 * 
	 * @param collection
	 * @param docs
	 * @param bulkInsert
	 * @return
	 */
	private static ArrayList<Document> writeToMongo(MongoCollection<Document> collection, ArrayList<Document> docs,
			String uniqueFieldName, boolean bulkInsert) {

		ArrayList<Document> nonDuplicateDocs = new ArrayList<Document>();
		// long duplicateCounter = 0;
		if (!bulkInsert) {
			for (Document doc : docs) {
				// String photoId = (String) ((Document) doc.get("source_info_flickr")).get("id");
				try {
					collection.insertOne(doc);
				} catch (MongoWriteException e) {
					if (e.getCode() == 11000) {// a duplicate entry, do nothing and continue

					} else { // just print exception message and continue
						System.err.println(e.getMessage());
					}
					// whatever the cause of the exception, go to next document without adding to list
					continue;
				}
				nonDuplicateDocs.add(doc); // document was written successfully
			}
		} else {
			HashSet<String> duplicateIds = new HashSet<String>();
			try {
				collection.insertMany(docs, new InsertManyOptions().ordered(false));
			} catch (MongoBulkWriteException e) {
				List<BulkWriteError> bulkWriteErrors = e.getWriteErrors();
				for (BulkWriteError error : bulkWriteErrors) {
					int code = error.getCode();
					if (code == 11000) { // a duplicate entry, do nothing
						String message = error.getMessage();
						// System.out.println(message);
						String dupKey = message.split("dup key: \\{ : \"")[1].split("\"")[0];
						duplicateIds.add(dupKey);
					} else { // if any error is not related to duplicate key, re-throw the exception
						throw e;
					}
				}
			}
			for (Document doc : docs) {
				// String photoId = (String) ((Document) doc.get(uniqueFieldName)).get("id");
				String uniqueId = null;
				if (uniqueFieldName.contains(".")) {
					String subdocString = uniqueFieldName.split("\\.")[0];
					Document subDoc = (Document) doc.get(subdocString);
					uniqueId = (String) subDoc.get(uniqueFieldName.split("\\.")[1]);
				} else {
					uniqueId = (String) doc.get(uniqueFieldName);
				}
				if (!duplicateIds.contains(uniqueId)) {
					nonDuplicateDocs.add(doc);
				}
			}
		}
		return nonDuplicateDocs;

	}

}
