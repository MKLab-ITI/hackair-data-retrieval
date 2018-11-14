package gr.iti.mklab.webcams.travel;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.CronScheduleBuilder;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;

import gr.iti.mklab.collector.AbstractImageDataCollector;
import gr.iti.mklab.utils.MongoConnection;
import gr.iti.mklab.utils.ServiceCalls;
import gr.iti.mklab.utils.Utils;

/**
 * Class for crawling specific webcams for which static image urls and geolocations are already available in a csv file.
 * The csv file should have the format: <webcam_id>,<webcam_url>,<lat>,<lon> This collector should in principle work
 * similarly to the AMOS collector, provided that a csv file with webcam details has been created for AMOS!
 * 
 * @author Eleftherios Spyromitros-Xioufis
 *
 */
@DisallowConcurrentExecution
public class WebcamsCollectionlJob extends AbstractImageDataCollector implements org.quartz.Job {

	public static String imagesBaseURL;
	public static boolean useCronTrigger;
	public static String webcamsIndexFile;

	/**
	 * Definition of when the job will be executed, the following definitions amounts to:<br>
	 * "run daily at 10:00,12:00,14:00,16:00" This default cron string can be overridden by the value in the crawl
	 * settings file
	 */
	public static String cronString = "0 0 10,12,14,16 * * ?";// run daily at four specific hours
	public static int crawlIntervalSecs = 3600;
	public static final int numDownloadThreads = 5;

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

		// define the job and tie it to the WebcamsCrawlJob class
		JobDetail job = newJob(WebcamsCollectionlJob.class).withIdentity("webcams_crawljob1", "group1").build();
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
		System.out.println(new Date() + ": Webcams crawl job is executing.");

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

			// parse the csv file to load webcam information
			// intentionally do this every time to allow updating of the csv while the process is running
			// for a small number of rows, the time should be negligible
			ArrayList<Document> webcamDocs = new ArrayList<Document>();
			BufferedReader in = new BufferedReader(new FileReader(new File(webcamsIndexFile)));
			String line = in.readLine(); // skip the header
			int numFailedWebcams = 0;
			while ((line = in.readLine()) != null) {
				SimpleWebcamInfo webcam = new SimpleWebcamInfo(line);
				try {
					// We assume that the static image is always up-to-date! Therefore, we try to immediately download
					// the image and consider the current timestamp as the image's timestamp.
					webcam.downloadAndUpdateInfo(imageDownloadFolder);
					// directly transform to mongo doc and add to list of docs
					webcamDocs.add(webcam.toMongoDoc());
				} catch (Exception e) {
					System.out.println("Webcam with id: " + webcam.getId() + " failed due to the following exception:");
					System.out.println(e.getMessage());
					// e.printStackTrace();
					numFailedWebcams++;
					System.out.println("Proceeding to next webcam.");
				}
			}
			in.close();
			Utils.outputMessage(
					webcamDocs.size() + " webcam photos loaded from index file, " + numFailedWebcams + " failed.",
					verbose);

			// +++ writing to mongo start +++
			// try to write the docs to mongo.
			// we do not need to check if the image is more fresh that the one we already have from this
			// webcam because the source_info.id field is named after the webcamId and timestamp and is unique
			// this means that it will not be written if the current photo is not newer!
			ArrayList<Document> photoDocsWritten = writeDocumentsToMongo(webcamDocs, collection, "source_info.id");
			// +++ writing to mongo end +++

			// +++ image analysis on downloaded images start +++
			if (!imageAnalysisEndpoint.equals("null") && photoDocsWritten.size() > 0) {
				int batchSize = 100;
				int numBatches = (int) Math.ceil((double) photoDocsWritten.size() / batchSize);

				for (int batchIndex = 0; batchIndex < numBatches; batchIndex++) {
					int fromIndex = batchIndex * batchSize;
					int toIndex = Math.min((batchIndex + 1) * batchSize, photoDocsWritten.size());
					System.out.println("Sending batch: " + fromIndex + "-" + toIndex + " to image analysis!");
					List<Document> batch = photoDocsWritten.subList(fromIndex, toIndex);
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

			Thread.sleep(5000);
		} catch (Exception e) {
			System.err.println("Exception thrown while executing the job!");
			System.err.println("Exception " + e.getCause() + ":" + e.getMessage());
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


}
