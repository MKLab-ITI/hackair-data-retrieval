package gr.iti.mklab.flickr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;

import gr.iti.mklab.collector.AbstractImageDataCollector;
import gr.iti.mklab.download.IdAndUrl;
import gr.iti.mklab.download.ImageDownloader;
import gr.iti.mklab.geo.GeoCell;
import gr.iti.mklab.geonames.ReverseGeocoder;
import gr.iti.mklab.methods.LanguageModel;
import gr.iti.mklab.util.CellCoder;
import gr.iti.mklab.util.TextUtil;
import gr.iti.mklab.utils.DateManipulation;
import gr.iti.mklab.utils.MongoConnection;
import gr.iti.mklab.utils.Utils;

/**
 * Class for retrieving Flickr images using geographical & textual queries. Location estimation is employed to infer the
 * location in case of textual queries.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 *
 */
public class FlickrCollector extends AbstractImageDataCollector {

	public static String flickrApiKey;
	public static long crawlTimeSpanSecs;
	public static boolean pastCrawl;
	public static String flickrQueriesFile;
	public static String geotaggingFilesDir;
	public static final double locEstimationThreshold = 0.5;
	public static LanguageModel langModel = null;
	public static ReverseGeocoder revGeocoder = null;

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		String crawlSettingsFile = args[0]; // "input/crawlsettings.json";
		loadCrawlSettings(crawlSettingsFile);

		FlickrQueryManager fqm = new FlickrQueryManager(flickrApiKey);

		// Mongo connection
		MongoConnection mongo = new MongoConnection(mongoSettingsFile);
		MongoCollection<Document> collection = mongo.getCollection();

		// create a unique index on the id for the collection
		// this creates the index only if it didn't already exist
		collection.createIndex(new Document("source_info.id", 1), new IndexOptions().unique(true));
		collection.createIndex(new Document("datetime", 1));
		collection.createIndex(new Document("source_type", "hashed"));
		collection.createIndex(new Document("source_info.date_uploaded", 1));
		collection.createIndex(new Document("loc", "2dsphere"));
		collection.createIndex(new Document("source_info.path", "hashed"));

		// parse the queries file to load queries
		BufferedReader in = new BufferedReader(new FileReader(new File(flickrQueriesFile)));
		String line = in.readLine(); // skip file header
		ArrayList<FlickrQuery> queries = new ArrayList<FlickrQuery>();
		boolean containsTextualQuery = false;
		while ((line = in.readLine()) != null) {
			if (line.equals("")) { // stop parsing after an empty line!
				break;
			}
			FlickrQuery fq = new FlickrQuery(line);
			queries.add(fq);
			if (fq.getType().equals("textual")) {
				containsTextualQuery = true;
			}
		}
		in.close();
		Utils.outputMessage("Crawl will include: " + queries.toString(), verbose);

		long crawlStartTime;
		if (crawlStartString.equals("")) { // start crawl now
			crawlStartTime = Instant.now().getEpochSecond();
		} else { // convert crawlStartString to timestamp
			LocalDateTime ldt = LocalDateTime.parse(crawlStartString, DateTimeFormatter.ofPattern(DATE_FORMAT));
			ZonedDateTime CETZonedDateTime = ldt.atZone(ZoneId.of("CET"));
			Utils.outputMessage("Crawl will start at: " + CETZonedDateTime, verbose);
			crawlStartTime = CETZonedDateTime.toInstant().getEpochSecond();
		}

		long crawlEndTime;
		if (crawlEndString.equals("")) { // end crawl in 365 days from now
			crawlEndTime = Instant.now().getEpochSecond() + 365 * oneDaySecs;
		} else { // convert crawlEndString to timestamp
			LocalDateTime ldt = LocalDateTime.parse(crawlEndString, DateTimeFormatter.ofPattern(DATE_FORMAT));
			ZonedDateTime CETZonedDateTime = ldt.atZone(ZoneId.of("CET"));
			Utils.outputMessage("Crawl will end at: " + CETZonedDateTime, verbose);
			crawlEndTime = CETZonedDateTime.toInstant().getEpochSecond();
		}

		long nextCrawlTime = crawlStartTime;

		if (containsTextualQuery) { // initialized and load geotagging module in memory
			/*
			 * Initialization of the language model and the load of the term-cell probabilities. The generated map
			 * allocate a significant amount of memory so it is recommended to be stored as a local variable instead of
			 * a global/static variable, in order to be easier manageable from the system.
			 */
			String termCellProbsFile = geotaggingFilesDir + "term_cell_probs_extended.txt";
			String citiesFile = geotaggingFilesDir + "cities1000.txt";
			String countryInfoFile = geotaggingFilesDir + "countryInfo.txt";

			langModel = new LanguageModel(termCellProbsFile, 0.1, 100);
			revGeocoder = new ReverseGeocoder(citiesFile, countryInfoFile);
		}

		// sleep until next crawl time
		long diff;
		if ((diff = nextCrawlTime - Instant.now().getEpochSecond()) > 0) {
			TimeUnit.SECONDS.sleep(diff);
		}

		while (true) {
			if (!pastCrawl) {
				if (nextCrawlTime > crawlEndTime) {
					System.out.println("Stopping crawl, end time reached!");
					break;
				}
			}

			// we will fetch photos taken between the time of the current crawl (max) and up to
			// crawlTimeSpanSecs secs in the past (min)
			long max_taken_date, min_taken_date;
			if (pastCrawl) {
				max_taken_date = crawlEndTime;
				min_taken_date = crawlStartTime;
			} else {
				max_taken_date = nextCrawlTime;
				min_taken_date = nextCrawlTime - crawlTimeSpanSecs;
			}

			Utils.outputMessage("Performing crawl with min-max date taken: " + min_taken_date + "-" + max_taken_date,
					verbose);

			// store only unique photos from each crawl cycle, to reduce db overhead
			HashSet<String> uniqueIds = new HashSet<String>();
			ArrayList<Document> photoDocs = new ArrayList<Document>();

			int numQueriesFailed = 0;
			for (FlickrQuery fq : queries) {
				String queryTags = fq.getTags();
				String query = fqm.buildQuery(min_taken_date, max_taken_date, 1, fq);
				Utils.outputMessage("Generating calls for: " + fq.getName(), verbose);
				Utils.outputMessage("API query: " + query, verbose);
				ArrayList<String> calls = new ArrayList<String>(); // create an empty list
				try {
					calls = fqm.generateRequiredApiCalls(query);
				} catch (Exception e) {
					System.err.println("Call generation for this query failed with the following exception: ");
					e.printStackTrace();
					System.err.println("Continuing with the next place.");
					numQueriesFailed++;
					continue;
				}
				String failureMessage = "";
				if (numQueriesFailed > 0) {
					failureMessage += " (" + numQueriesFailed + " queries failed during call generation!)";
				}
				Utils.outputMessage(
						"Call generation completed, " + calls.size() + " calls will be performed." + failureMessage,
						verbose);

				// use a HashMap to store all unique results (images) from this set of calls.
				HashMap<String, JSONObject> idToPhotoObject = new HashMap<String, JSONObject>();

				int callCounter = 0;
				int numCallsFailed = 0;
				for (String call : calls) {
					Utils.outputMessage("Call " + (callCounter + 1) + "/" + calls.size(), verbose);
					// add extra parameters to the call
					call = FlickrQueryManager.addExtraParameters(call);
					String response;
					try {
						response = fqm.throttledGetApiResponse(call, FlickrQueryManager.maxCallRetries);
					} catch (Exception e) {
						System.err.println("Call failed with the following exception: " + e.getMessage());
						System.err.println("Continuing with the next call.");
						// e.printStackTrace();
						numCallsFailed++;
						continue;
					}

					try {
						JSONObject json = new JSONObject(response);
						JSONArray photoArr = json.getJSONObject("photos").getJSONArray("photo");
						for (int i = 0; i < photoArr.length(); i++) {
							String id = photoArr.getJSONObject(i).getString("id");
							idToPhotoObject.put(id, photoArr.getJSONObject(i));
						}
					} catch (JSONException e) {
						System.err.println("Response JSON was not as expected!");
						System.err.println("Response:" + response);
						System.err.println("Continuing to next call");
						numCallsFailed++;
						continue;
					}
					callCounter++;

				}
				if (numCallsFailed > 0) {
					Utils.outputMessage(numCallsFailed + " calls failed!", verbose);
				}

				// int notInOtherQueryCounter = 0; // used only in development
				int usablePhotoCounter = 0;
				for (Map.Entry<String, JSONObject> e : idToPhotoObject.entrySet()) {
					String id = e.getKey();
					if (uniqueIds.add(id)) {
						JSONObject photoObject = e.getValue();
						photoObject.put("query_tags", queryTags);
						Document photoDoc;
						try {
							photoDoc = transformToDocument(photoObject, false);
						} catch (Exception e1) {
							System.err.println("Document transformation failed: " + e1.getMessage());
							System.err.println("Continuing with the next document.");
							// e1.printStackTrace();
							continue;
						}
						if (photoDoc != null) {
							photoDocs.add(photoDoc);
							usablePhotoCounter++;
						}
						// notInOtherQueryCounter++;
					} else {
						// System.err.println("Same id found in other query!");
					}
				}
				Utils.outputMessage("Calls returned " + idToPhotoObject.size() + " unique photos for this query, "
						+ usablePhotoCounter + " usable", verbose);
			}

			// +++ writing to mongo start +++
			Utils.outputMessage(
					"All queries generated " + photoDocs.size() + " usable photos that should be written to mongo",
					verbose);
			ArrayList<Document> photoDocsWritten = writeDocumentsToMongo(photoDocs, collection, "source_info.id");
			// +++ writing to mongo end +++

			// +++ multi-threaded download of written docs (non-duplicates) start +++
			ArrayList<Document> photoDocsDownloaded = new ArrayList<Document>();
			if (downloadImages) {
				photoDocsDownloaded = downloadImages(photoDocsWritten, collection);
			}
			// +++ multi-threaded download of written docs (non-duplicates) end +++

			// +++ perform image analysis on downloaded images start +++
			if (!imageAnalysisEndpoint.equals("null") && photoDocsDownloaded.size() > 0) {
				int numBatches = (int) Math.ceil((double) photoDocsDownloaded.size() / imageAnalysisBatchSize);

				for (int batchIndex = 0; batchIndex < numBatches; batchIndex++) {
					int fromIndex = batchIndex * imageAnalysisBatchSize;
					int toIndex = Math.min((batchIndex + 1) * imageAnalysisBatchSize, photoDocsDownloaded.size());
					System.out.println("Sending batch: " + fromIndex + "-" + toIndex + " to image analysis!");
					List<Document> batch = photoDocsDownloaded.subList(fromIndex, toIndex);
					String response = null;
					int numRetries = 0;
					while (response == null) {
						if (numRetries > 0) {
							Utils.outputMessage("Sleeping for 60 sec before retry #" + numRetries, verbose);
							TimeUnit.SECONDS.sleep(60);
						}
						response = sendForImageAnalysis(batch);
						numRetries++;
					}
					System.out.println("Updating collection!");
					updateCollectionFromImageAnalysis(response, collection);

				}

			}
			// +++ image analysis on downloaded images end +++

			nextCrawlTime += crawlIntervalSecs;

			// sleeping until next crawl time
			long now = Instant.now().getEpochSecond();
			long waitTime = (nextCrawlTime - now);
			if (waitTime > 0) {
				Utils.outputMessage("Next crawl will take place at " + new Date((now + waitTime) * 1000) + " (in "
						+ waitTime + " secs), sleeping for now..", verbose);
				TimeUnit.SECONDS.sleep(waitTime);
			} else {
				throw new Exception("Previous crawl batch took longer than expected!");
			}

		}

	}

	/**
	 * Tries to downloads the supplied list of images and returns a list with the images that were successfully
	 * downloaded. If the deleteFailedDownloadsFromMongo flag is true, then images that failed to download are also
	 * deleted from the database.
	 * 
	 * @param photoDocuments
	 * @param collection
	 * @return
	 */
	private static ArrayList<Document> downloadImages(ArrayList<Document> photoDocuments,
			MongoCollection<Document> collection) {
		if (photoDocuments.size() == 0) { // if the list is empty, just return it
			return photoDocuments;
		}
		if (downloadImages) {// create root download folder if it does not exist already
			new File(imageDownloadFolder).mkdirs();
		}
		// transforming list of documents to list of IdAndUrl objects that will be used by the downloader
		ArrayList<IdAndUrl> idsWritten = new ArrayList<IdAndUrl>();
		HashSet<String> uniqueDates = new HashSet<String>(); // store them to create the download dirs
		for (Document doc : photoDocuments) {
			String photoId = (String) ((Document) doc.get("source_info")).get("id");
			String photoUrl = (String) ((Document) doc.get("source_info")).get("image_url");
			String dateStr = ((String) doc.get("date_str")).split(" ")[0];
			idsWritten.add(new IdAndUrl(photoId, photoUrl, dateStr));
			uniqueDates.add(dateStr);
		}
		// create download sub-folders
		for (String date : uniqueDates) {
			new File(imageDownloadFolder + date + "/").mkdirs();
		}

		// TODO implement maximum call rate in ImageDownloader.downloadImages to avoid sending too many
		// requests to Flickr (see UrlIndexingMt class of the multimedia-indexing project).
		HashSet<String> failedDownloadIds = ImageDownloader.downloadImages(imageDownloadFolder, idsWritten, 10);
		Utils.outputMessage("Image download completed, " + failedDownloadIds.size() + " downloads failed!", verbose);

		// == move only images that could be downloaded to the photoDocsDownloaded list that will be
		// further processed. also delete failed images from mongo if flag is true ==
		// TODO check if bulk delete is possible/faster
		ArrayList<Document> photoDocsDownloaded = new ArrayList<Document>();
		int deletedCounter = 0;
		for (Document doc : photoDocuments) {
			String photoId = (String) ((Document) doc.get("source_info")).get("id");
			if (failedDownloadIds.contains(photoId)) {
				if (deleteFailedDownloadsFromMongo) {
					// DeleteResult delres = collection.deleteOne(Filters.eq("source_info.id", photoId));
					collection.deleteOne(Filters.eq("source_info.id", photoId));
					deletedCounter++;
				}
			} else {
				photoDocsDownloaded.add(doc);
			}
		}
		if (deleteFailedDownloadsFromMongo) {
			Utils.outputMessage(deletedCounter + " documents that failed donwload were deleted", verbose);
		}

		return photoDocsDownloaded;
	}

	public static void loadCrawlSettings(String crawlSettingsFile) throws Exception {
		// parse general data collection settings using the hyperclass method
		AbstractImageDataCollector.loadCrawlSettings(crawlSettingsFile);

		// then parse more specific image collecting settings in this method
		StringBuffer sb = new StringBuffer();
		String line;
		BufferedReader in = new BufferedReader(new FileReader(new File(crawlSettingsFile)));
		while ((line = in.readLine()) != null) {
			sb.append(line);
		}
		in.close();
		JSONObject json = new JSONObject(sb.toString());
		JSONObject mongoSettingsObj = (JSONObject) json.getJSONArray("crawl_settings").get(0);

		flickrApiKey = mongoSettingsObj.getString("flickrApiKey");
		flickrQueriesFile = mongoSettingsObj.getString("queriesFile");
		geotaggingFilesDir = mongoSettingsObj.getString("geotaggingFilesDir");

		pastCrawl = mongoSettingsObj.getBoolean("pastCrawl");
		if (!pastCrawl) {
			crawlTimeSpanSecs = (int) (mongoSettingsObj.getDouble("crawlTimeSpanDays") * oneDaySecs);
		}

	}

	/**
	 * Transforms the JSONObject returned from Flickr to a mongo document!
	 * 
	 * @param photoObject
	 * @param verbose
	 * @return
	 * @throws Exception
	 */
	public static Document transformToDocument(JSONObject photoObject, boolean verbose) throws Exception {

		// System.out.println(photoObject);

		// == Do several checks to see if the image represented by the photoObject is suitable ==
		String flickrId = photoObject.getString("id");
		String owner = photoObject.getString("owner");
		String pageUrl = "https://www.flickr.com/photos/" + owner + "/" + flickrId;

		// == 1. Check datetakengranularity, if >=4, the photo should not be taken into account ==
		int datetakengranularity = photoObject.getInt("datetakengranularity");
		if (datetakengranularity >= 4) {
			if (verbose)
				System.err.println("Date granularity is too coarse!");
			return null;
		}

		// == 2. Compare datetaken with dateupload, if the same, then datetaken might be wrong ==
		// The 'taken' date represents the time at which the photo has taken. This is extracted from EXIF data
		// if available, else set to the time of upload.
		Long uploadedTimestamp = Long.parseLong(photoObject.getString("dateupload")) * 1000;
		// convert String date to timestamp, assuming CET timezone
		String dateTaken = photoObject.getString("datetaken");
		long takenTimestamp = DateManipulation.convertStringDateToTimestamp(dateTaken, "CET",
				DateManipulation.FLICKR_DATE_FORMAT);

		// == 3. Reject images where takenTimestamp >= dateUploadedTimestamp, i.e. something is wrong
		// taken timestamp might be wrong because we do not know the timezone and we assume CET
		// thus, the real taken timestamp could be +-3 hours from the calculated
		if (takenTimestamp - 3 * 3600 * 1000 >= uploadedTimestamp) {
			if (verbose) {
				System.err.println("date taken >= date uploaded! id: " + pageUrl);
			}
			return null;
		}

		// == 3. also reject images that have been uploaded more than 24h after taken!
		if (uploadedTimestamp > takenTimestamp + 24 * 3600 * 1000) {
			if (verbose) {
				System.err.println("date uploaded > date taken + 24h! id: " + pageUrl);
			}
			return null;
		}

		// == 4. Then check if this photoObject has been retrieved from a textual query and estimate location
		String title = photoObject.getString("title"); // needed below
		String tags = photoObject.getString("tags"); // needed below
		String desc = ((JSONObject) photoObject.get("description")).getString("_content"); // needed below
		Double lat = null; // needed below
		Double lon = null; // needed below
		double locEstimationConf = 0;

		String queryTags = null; // this field is populated only for photos retrieved from textual queries
		try {
			queryTags = photoObject.getString("query_tags");
		} catch (Exception e) {
			// do nothing
		}
		if (queryTags != null) { // a textual query
			// estimate location
			// first combine the terms in title, description, tags
			Set<String> tagTerms = TextUtil.cleanText(tags);
			Set<String> titleTerms = TextUtil.cleanText(title);
			Set<String> descTerms = TextUtil.cleanText(desc);
			Set<String> unionTerms = new HashSet<String>();
			unionTerms.addAll(tagTerms);
			unionTerms.addAll(titleTerms);
			unionTerms.addAll(descTerms);
			// this should be equivalent with above
			// Set<String> allTerms = TextUtil.cleanText(tags + " " + title + " " + desc);
			// if (!allTerms.equals(unionTerms)) {
			// throw new Exception("Unexpected!");
			// }

			GeoCell mlc = langModel.calculateLanguageModel(unionTerms);
			if (mlc == null) { // null means estimation not possible -> image unusable
				return null;
			} else { // estimation was made!
				// 1. Check that confidence is above threshold
				locEstimationConf = mlc.getConfidence();
				if (locEstimationConf < locEstimationThreshold) {
					return null;
				}
				// 2. Check that estimated location is in Europe
				double[] result = CellCoder.cellDecoding(mlc.getID());
				String continent = revGeocoder.getCityAndCountryAndContinentyByLatLon(result[0], result[1])[2];
				if (!continent.equals("EU")) {
					return null;
				}
				// if everything was successful, update the location of the image with the estimation
				lat = result[0];
				lon = result[1];
			}
		}

		// get geolocation of geolocated image
		if (lat == null && lon == null) {
			try {
				String latString = photoObject.getString("latitude");
				lat = Double.parseDouble(latString);
			} catch (JSONException e) { // sometime latitude is a double..
				lat = photoObject.getDouble("latitude");
			}

			try {
				String latString = photoObject.getString("longitude");
				lon = Double.parseDouble(latString);
			} catch (JSONException e) { // sometime longitude is a double..
				lon = photoObject.getDouble("longitude");
			}
		}

		// get additional details about the image
		int views = photoObject.getInt("views");
		String username = photoObject.getString("ownername");
		String license = photoObject.getString("license");

		// get the image_url (the medium size, 500 on longest side)
		String imageUrl = "https://farm" + photoObject.getInt("farm") + ".staticflickr.com/"
				+ photoObject.getString("server") + "/" + photoObject.getString("id") + "_"
				+ photoObject.getString("secret") + ".jpg";

		FlickrImageInfo fIm = new FlickrImageInfo();
		fIm.setDateTaken(dateTaken);
		fIm.setUploadedTimestamp(uploadedTimestamp);
		fIm.setFlickrId(flickrId);
		fIm.setImageUrl(imageUrl);
		fIm.setLat(lat);
		fIm.setLon(lon);
		fIm.setOwner(owner);
		fIm.setPageUrl(pageUrl);
		fIm.setTitle(title);
		fIm.setUsername(username);
		fIm.setViews(views);
		fIm.setTags(tags);
		fIm.setDescription(desc);
		fIm.setLicense(license);
		fIm.setQueryTags(queryTags);
		fIm.setLocEstimationConf(locEstimationConf);

		// fIm.setWoeid(photoObject.getString("woeid"));
		// fIm.setQuery("woeid_" + woeid);

		Document doc = fIm.toMongoDoc();

		return doc;
	}

}
