package gr.iti.mklab.webcams.travel;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.quartz.CronScheduleBuilder;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;

import gr.iti.mklab.collector.AbstractImageDataCollector;
import gr.iti.mklab.download.IdAndUrl;
import gr.iti.mklab.download.ImageDownloader;
import gr.iti.mklab.utils.MongoConnection;
import gr.iti.mklab.utils.ServiceCalls;
import gr.iti.mklab.utils.Utils;
import pojo.webcams.travel.WebcamsTravelResponse;
import pojo.webcams.travel.WebcamsTravelResponse.Result.Webcam;

/**
 * Class for crawling the webcams.travel API dataset.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 *
 */
@DisallowConcurrentExecution
public class WebcamsTravelCollectionJob extends AbstractImageDataCollector implements org.quartz.Job {

	// The CET timezone should always be assumed
	public static final String DATE_FORMAT = "dd-MM-yyyy HH:mm:ss";
	public static final long oneDaySecs = 86400;
	public static final boolean bulkInserts = true;

	public static String imagesBaseURL;
	public static boolean useCronTrigger;
	public static int numWebcamsDE;
	public static int numWebcamsNO;
	public static int numWebcamsGR;
	public static int numWebcamsBE;

	/**
	 * Definition of when the job will be executed, the following definitions amounts to:<br>
	 * "run daily at 10:00,13:00,16:00" This default cron string can be overridden by the value in the crawl settings
	 * file
	 */
	public static String cronString = "0 0 10,13,16 * * ?";// run daily at specific hours
	public static int crawlIntervalSecs = 3600;
	public static final int numDownloadThreads = 5;

	public static final boolean getSources = false;
	public static final int webcamsTravelMaxLimit = 50;

	// public static final String wtEndpointBase = "https://webcamstravel.p.mashape.com/webcams/list/continent=EU";
	// public static int maxWebcamsPerCrawl = 2000; // >=50

	public static String[] wtEndpointBase;

	// "/webcams/list/bbox=38.3546,24.1672,37.599,23.0493" // Athens BB
	// "/webcams/list/bbox=50.94718,4.55658,50.742539,4.177551" // Brussels BB

	public static int[] maxWebcamsPerCrawl;

	public static final String wtOrderBy = "/orderby=popularity,desc";
	public static final String wtShow = "?show=webcams:basic,image,location,url";

	// The following HashMap will hold the source urls of all webcams in memory, so that we don't need to
	// fetch the same source url twice. This could be also done by querying mongo but it would be slower.
	private static HashMap<String, String> webcamIdToSource = new HashMap<String, String>();

	/**
	 * From:<br>
	 * Quartz documentation:
	 * http://www.quartz-scheduler.org/documentation/quartz-2.2.x/tutorials/tutorial-lesson-06.html<br>
	 * and<br>
	 * Mkyong tutorial: https://www.mkyong.com/java/quartz-2-scheduler-tutorial/
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		loadCrawlSettings(args[0]);

		wtEndpointBase = new String[4];
		wtEndpointBase[0] = "https://webcamstravel.p.mashape.com/webcams/list/country=DE";
		wtEndpointBase[1] = "https://webcamstravel.p.mashape.com/webcams/list/country=NO";
		wtEndpointBase[2] = "https://webcamstravel.p.mashape.com/webcams/list/country=GR";
		wtEndpointBase[3] = "https://webcamstravel.p.mashape.com/webcams/list/country=BE";
		maxWebcamsPerCrawl = new int[4];
		maxWebcamsPerCrawl[0] = numWebcamsDE;
		maxWebcamsPerCrawl[1] = numWebcamsNO;
		maxWebcamsPerCrawl[2] = numWebcamsGR;
		maxWebcamsPerCrawl[3] = numWebcamsBE;

		// define the job and tie it to our WebcamsTravelCrawlJob class
		JobDetail job = newJob(WebcamsTravelCollectionJob.class).withIdentity("webcams.travel_job1", "group1").build();
		Trigger jobTrigger = null;
		if (useCronTrigger) {
			jobTrigger = newTrigger().withIdentity("cronTrigger", "group1")
					.withSchedule(CronScheduleBuilder.cronSchedule(cronString).inTimeZone(TimeZone.getTimeZone("CET")))
					.build();
		} else {
			// A simple trigger used for testing: runs the job now, and then repeats every intervalSecs
			jobTrigger = newTrigger().withIdentity("simpleTrigger", "group1").startNow()
					.withSchedule(simpleSchedule().withIntervalInSeconds(crawlIntervalSecs).repeatForever()).build();

		}

		// Grab the Scheduler instance from the Factory
		Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
		scheduler.start(); // and start it off
		scheduler.scheduleJob(job, jobTrigger); // Tell quartz to schedule the job using our trigger

	}

	public void execute(JobExecutionContext context) throws JobExecutionException {
		System.out.println(new Date() + ": Webcams.travel crawl job is executing.");

		MongoConnection mongo = null;
		try {
			// Mongo connection
			mongo = new MongoConnection(mongoSettingsFile);
			MongoCollection<Document> collection = mongo.getCollection();
			// create the necessary indexes if they don't exist already
			collection.createIndex(new Document("source_info.id", 1), new IndexOptions().unique(true));
			collection.createIndex(new Document("datetime", 1));
			collection.createIndex(new Document("source_type", "hashed"));
			collection.createIndex(new Document("loc", "2dsphere"));
			collection.createIndex(new Document("source_info.path", "hashed"));

			ArrayList<Webcam> allUsableWebcams = new ArrayList<Webcam>();
			for (int i = 0; i < wtEndpointBase.length; i++) {
				System.out.println("Collecting top " + maxWebcamsPerCrawl[i] + " webcams for query:\n"
						+ wtEndpointBase[i] + wtOrderBy);
				// query the webcams.travel API to collect details from maxWebcamsPerCrawl active webcams
				int offset = 0;
				int maxOffset = maxWebcamsPerCrawl[i];// just an initial value
				while (offset < maxOffset) {
					ArrayList<Webcam> usableWebcamsThisOffset = new ArrayList<Webcam>();
					int numOffsetRetries = 0;
					try {
						// System.out.println("Call: " + offset + "-" + (offset + webcamsTravelMaxLimit));
						// === Creating API call start ===
						HashMap<String, String> headerElems = new HashMap<String, String>();
						headerElems.put("X-Mashape-Key", "zsguCALa1QmshGgCzycuDlSuPmKap1D3ntyjsnlilrY4bArsMb");
						String limit = "/limit=" + webcamsTravelMaxLimit + "," + offset;
						String callString = wtEndpointBase[i] + wtOrderBy + limit + wtShow;

						String response = null;
						while (response == null || response.equals("Too many requests. Please try again.")) {
							response = ServiceCalls.makeGetRequest(callString, headerElems, true);
							if (response.equals("Too many requests. Please try again.")) { // wait on such a response,
								// before retrying
								Thread.sleep(60 * 3600); // wait 1 minute
							}
						}
						// === Creating API call end ===

						// === Parsing API response start ===
						ObjectMapper mapper = new ObjectMapper();
						WebcamsTravelResponse wtr = mapper.readValue(response, WebcamsTravelResponse.class);
						int totalNumResults = wtr.getResult().getTotal();
						// updating the max offset based on the # results of the last call
						maxOffset = Math.min(totalNumResults, maxWebcamsPerCrawl[i]);
						// System.out.println("Results in this call: " + wtr.getResult().getWebcams().size());
						for (Webcam webcam : wtr.getResult().getWebcams()) {
							if (!"active".equals(webcam.getStatus())) { // webcam is not active
								continue;
							}

							long lastUpdatedTime = webcam.getImage().getUpdate() * 1000;
							long now = System.currentTimeMillis();
							double hoursSinceLastUpdate = (now - lastUpdatedTime) / (3600 * 1000.0);
							if (hoursSinceLastUpdate < 0) {
								System.err.println("Unexpected!");
								continue;
							}
							if (hoursSinceLastUpdate > 24) { // reject images that are older than 24h
								continue;
							}

							// if we are here, we have an active webcam with a fresh image!

							// try to obtain the source url of this webcam if needed
							if (getSources) {
								String webcamId = webcam.getId();
								String sourceUrl;
								if (webcamIdToSource.containsKey(webcamId)) {
									sourceUrl = webcamIdToSource.get(webcamId);
								} else { // get the source url from webcam's page on webcams.travel
									String url = webcam.getUrl().getCurrent().getDesktop();
									org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
									sourceUrl = doc.select("a[title=Source]").get(0).attr("href");
									webcamIdToSource.put(webcamId, sourceUrl);
								}
								webcam.setSourceUrl(sourceUrl);
							}

							// add the webcam to the list of active webcams in this offset
							usableWebcamsThisOffset.add(webcam);
						}
						// === Parsing API response end ===

					} catch (Exception e) {// if an exception was thrown at any point during the call/response
											// processing
						System.out.println("Exception during offset processing, # retries: " + numOffsetRetries);
						System.err.println(e.getMessage()); // print exception
						e.printStackTrace();

						if (numOffsetRetries < 2) { // retry this offset
							numOffsetRetries++; // update retries counter
							Thread.sleep(5 * 1000); // wait 5 sec
							continue; // and retry this offset
						} else {
							// do nothing, will go to next offset
						}

					}

					// if everything completed successfully, prepare for the next call
					allUsableWebcams.addAll(usableWebcamsThisOffset); // add webcams
					offset += webcamsTravelMaxLimit; // update offset
					Thread.sleep(5 * 1000); // wait 5 sec before making the next call
				}
			}

			Utils.outputMessage("Crawl found " + allUsableWebcams.size() + " usable webcam images", verbose);

			// transform all active webcams to mongo documents that we will try to insert in mongo
			// we do not need to check if the image is more fresh that the one we already have from this
			// webcam because the source_info.id field is named after the webcamId and timestamp and is unique
			// this means that it will not be written if the current photo is not newer!
			ArrayList<Document> mongoDocs = new ArrayList<Document>();
			for (Webcam webcam : allUsableWebcams) {
				mongoDocs.add(webcam.toMongoDoc());
			}

			// +++ writing to mongo start +++
			Utils.outputMessage("Active webcams generated " + mongoDocs.size()
					+ " usable photos that will be attempted to be written to mongo", verbose);
			// TODO: check if we already have a webcam located in about the same location
			ArrayList<Document> photoDocsWritten = writeDocumentsToMongo(mongoDocs, collection, "source_info.id");
			// +++ writing to mongo end +++

			// +++ multi-threaded download of written docs (non-duplicates) start +++
			// TODO: reduce the number of threads when fetching from webcam.travel!
			ArrayList<Document> photoDocsDownloaded = new ArrayList<Document>();
			if (downloadImages) {
				photoDocsDownloaded = downloadImages(photoDocsWritten, collection);
			}
			// +++ multi-threaded download of written docs (non-duplicates) end +++

			// +++ image analysis on downloaded images start +++
			if (!imageAnalysisEndpoint.equals("null") && photoDocsDownloaded.size() > 0) {
				int batchSize = 100;
				int numBatches = (int) Math.ceil((double) photoDocsDownloaded.size() / batchSize);

				for (int batchIndex = 0; batchIndex < numBatches; batchIndex++) {
					int fromIndex = batchIndex * batchSize;
					int toIndex = Math.min((batchIndex + 1) * batchSize, photoDocsDownloaded.size());
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

		} catch (Exception e) {
			System.err.println("Exception thrown while executing the job!");
			System.err.println("Exception " + e.getCause() + ":" + e.getMessage());
			e.printStackTrace();
			JobExecutionException jee = new JobExecutionException(e);
			// Quartz will automatically unschedule
			// all triggers associated with this job
			// so that it does not run again
			jee.setUnscheduleAllTriggers(true);
			throw jee;
		} finally {
			mongo.closeConnectionToMongo();
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
		HashSet<String> uniqueSubFolders = new HashSet<String>(); // store them to create the download dirs
		for (Document doc : photoDocuments) {
			String photoId = (String) ((Document) doc.get("source_info")).get("id");
			String photoUrl = (String) ((Document) doc.get("source_info")).get("url_original");
			String imagePath = (String) ((Document) doc.get("source_info")).get("path");
			String path = imagePath.substring(0, imagePath.lastIndexOf("/"));
			idsWritten.add(new IdAndUrl(photoId, photoUrl, path));
			uniqueSubFolders.add(path);
		}
		// create download sub-folders
		for (String subFolder : uniqueSubFolders) {
			new File(imageDownloadFolder + subFolder + "/").mkdirs();
		}

		// TODO implement maximum call rate in ImageDownloader.downloadImages to avoid sending too many
		// requests to Flickr (see UrlIndexingMt class of the multimedia-indexing project).
		HashSet<String> failedDownloadIds = ImageDownloader.downloadImages(imageDownloadFolder, idsWritten,
				numDownloadThreads);
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

		imagesBaseURL = crawlSettingsObj.getString("imagesBaseURL");
		useCronTrigger = crawlSettingsObj.getBoolean("useCronTrigger");
		if (useCronTrigger) {
			if (crawlSettingsObj.has("cronString")) {
				cronString = crawlSettingsObj.getString("cronString");
			} else {
				// use the default
			}
		} else {
			if (crawlSettingsObj.has("intervalSecs")) {
				crawlIntervalSecs = crawlSettingsObj.getInt("intervalSecs");
			} else {
				// use the default
			}
		}

		numWebcamsDE = crawlSettingsObj.getInt("numWebcamsDE");
		numWebcamsNO = crawlSettingsObj.getInt("numWebcamsNO");
		numWebcamsGR = crawlSettingsObj.getInt("numWebcamsGR");
		numWebcamsBE = crawlSettingsObj.getInt("numWebcamsBE");

	}

}
