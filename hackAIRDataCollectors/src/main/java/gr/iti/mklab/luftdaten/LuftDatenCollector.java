package gr.iti.mklab.luftdaten;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bson.Document;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;

import gr.iti.mklab.collector.AbstractDataCollector;
import gr.iti.mklab.utils.MongoConnection;
import gr.iti.mklab.utils.ServiceCalls;
import gr.iti.mklab.utils.Utils;
import pojo.luftdaten.LuftDatenData;
import pojo.mongo.HackairRecordSensors;

/**
 * Class for retrieving sensor data from luftdaten.info (https://api.luftdaten.info/v1/).
 * 
 * @author Eleftherios Spyromitros-Xioufis
 *
 */
public class LuftDatenCollector extends AbstractDataCollector {

	public static final String luftdatenEndpoint = "http://api.luftdaten.info/static/v1/data.json";

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		String crawlSettingsFile = args[0]; // "input/luftdaten/crawlsettings.json";
		loadCrawlSettings(crawlSettingsFile);

		// Mongo connection
		MongoConnection mongoCon = new MongoConnection(mongoSettingsFile);
		MongoCollection<Document> collection = mongoCon.getCollection();

		// create a unique index on the id for the collection
		// this creates the index only if it didn't already exist
		collection.createIndex(new Document("id", 1), new IndexOptions().unique(true));
		collection.createIndex(new Document("datetime", 1));
		collection.createIndex(new Document("location", "hashed"));
		collection.createIndex(new Document("source_type", "hashed"));
		collection.createIndex(new Document("pollutant", "hashed"));
		collection.createIndex(new Document("loc", "2dsphere"));

		long crawlEndTime;
		if (crawlEndString.equals("")) { // end crawl in 365 days from now
			crawlEndTime = Instant.now().getEpochSecond() + 365 * oneDaySecs;
		} else { // convert crawlEndString to timestamp
			LocalDateTime ldt = LocalDateTime.parse(crawlEndString, DateTimeFormatter.ofPattern(DATE_FORMAT));
			ZonedDateTime CETZonedDateTime = ldt.atZone(ZoneId.of("CET"));
			Utils.outputMessage("Crawl will start at: " + CETZonedDateTime, verbose);
			crawlEndTime = CETZonedDateTime.toInstant().getEpochSecond();
		}

		long crawlStartTime;
		if (crawlStartString.equals("")) { // start crawl now
			crawlStartTime = Instant.now().getEpochSecond();
		} else { // convert crawlStartString to timestamp
			LocalDateTime ldt = LocalDateTime.parse(crawlStartString, DateTimeFormatter.ofPattern(DATE_FORMAT));
			ZonedDateTime CETZonedDateTime = ldt.atZone(ZoneId.of("CET"));
			Utils.outputMessage("Crawl will end at: " + CETZonedDateTime, verbose);
			crawlStartTime = CETZonedDateTime.toInstant().getEpochSecond();
		}

		long nextCrawlTime = crawlStartTime;

		// sleep until next crawl time
		long diff;
		if ((diff = nextCrawlTime - Instant.now().getEpochSecond()) > 0) {
			TimeUnit.SECONDS.sleep(diff);
		}

		ObjectMapper mapper = new ObjectMapper();
		// the following was added to avoid an Exception being thrown whenever the json
		// returned by luftdaten does not properly quote:
		// 1. control chars
		mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
		// 2. field names
		mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

		while (true) {
			if (nextCrawlTime > crawlEndTime) {
				System.out.println("Stopping crawl, end time reached!");
				break;
			}

			// catch any unhandled Exception thrown during the execution of the following code and
			// re-try collecting data in the next collection cycle
			try {
				Utils.outputMessage("Retrieving latest sensor measurents from luftdate.info!", verbose);

				ArrayList<Document> docs = new ArrayList<Document>();

				String responseString = null;
				try {
					responseString = ServiceCalls.makeGetRequest(luftdatenEndpoint);
				} catch (Exception e) {
					System.err.println("Exception thrown while calling the service, re-trying in 60 sec!");
					// sleep for some seconds and retry
					TimeUnit.SECONDS.sleep(60);
					continue;
				}

				List<LuftDatenData> listLuftDatenData = null;
				try {
					listLuftDatenData = mapper.readValue(responseString, new TypeReference<List<LuftDatenData>>() {
					});
				} catch (Exception e) {
					System.err.println("Luftdaten response is probably invalid and could not be parsed!");
					throw e;
				}

				Utils.outputMessage(listLuftDatenData.size() + " sensors with measurements returned!", verbose);

				int pm10Counter = 0;
				int pm25Counter = 0;

				for (LuftDatenData rec : listLuftDatenData) {

					HackairRecordSensors recPM10 = null;
					HackairRecordSensors recPM25 = null;
					try {
						recPM10 = rec.toHackairRecordSensors("pm10");
						recPM25 = rec.toHackairRecordSensors("pm25");

						if (recPM10 != null) {
							pm10Counter++;
							String recJson = mapper.writeValueAsString(recPM10);
							Document doc = Document.parse(recJson);
							docs.add(doc);
						}

						if (recPM25 != null) {
							pm25Counter++;
							String recJson = mapper.writeValueAsString(recPM25);
							Document doc = Document.parse(recJson);
							docs.add(doc);
						}

					} catch (Exception e) {
						System.err.print("Exception parsing record:");
						System.err.println(mapper.writeValueAsString(rec));
						throw e;
					}

				}

				Utils.outputMessage(pm10Counter + "/" + pm25Counter + " sensors with PM10/PM25 measurements!", verbose);

				// +++ writing to mongo start +++
				Utils.outputMessage(docs.size() + " sensor measurements to be written to mongo", verbose);
				ArrayList<Document> docsWritten = writeDocumentsToMongo(docs, collection, "id");
				Utils.outputMessage(docsWritten.size() + " sensor measurements were written to mongo", verbose);
				// +++ writing to mongo end +++


			} catch (Exception e) {
				System.err.println("An unhandled Exception was thrown during the previous collection cycle.");
				System.err.println("Exception message: " + e.getMessage());
				System.err.println("Exception stack trace:");
				e.printStackTrace();
				System.err.println("Collection will be attempted again in the next collection cycle.");
			}

			nextCrawlTime += crawlIntervalSecs;

			// sleeping until next crawl time
			long now = Instant.now().getEpochSecond();
			long waitTime = (nextCrawlTime - now);
			if (waitTime > 0) {
				Utils.outputMessage("Next retrieval will take place at " + new Date((now + waitTime) * 1000) + " (in "
						+ waitTime + " secs), sleeping for now..", verbose);
				TimeUnit.SECONDS.sleep(waitTime);
			} else {
				// throw new Exception("Previous retrieval took longer than expected!");
				System.err.println("Previous retrieval took longer than expected!");
				System.err.println("Starting next retrieval immediately!");
				// reset nextCrawlTime to now
				nextCrawlTime = now;
			}
		}
	}

}