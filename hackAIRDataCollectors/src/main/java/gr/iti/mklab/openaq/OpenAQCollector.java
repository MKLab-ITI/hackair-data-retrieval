package gr.iti.mklab.openaq;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.bson.Document;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;

import gr.iti.mklab.collector.AbstractDataCollector;
import gr.iti.mklab.utils.MongoConnection;
import gr.iti.mklab.utils.ServiceCalls;
import gr.iti.mklab.utils.Utils;
import pojo.mongo.HackairRecordSensors;
import pojo.openaq.OpenAQResponseLatest;

/**
 * Class for retrieving air quality measurements from the OpenAQ API (https://docs.openaq.org).
 * 
 * @author Eleftherios Spyromitros-Xioufis
 *
 */
public class OpenAQCollector extends AbstractDataCollector {

	public static String openAqEndpoint = "https://api.openaq.org/v1/latest";
	public static final String[] pollutants = { "pm25", "pm10" };

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		String crawlSettingsFile = args[0]; // "input/openaq/crawlsettings.json";
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

		HashMap<String, String> countries = populateCountries();
		ObjectMapper mapper = new ObjectMapper();

		while (true) {
			if (nextCrawlTime > crawlEndTime) {
				System.out.println("Stopping crawl, end time reached!");
				break;
			}

			try {
				// sanity check, normally, nextCrawlTime should be about now
				if (Math.abs(Instant.now().getEpochSecond() - nextCrawlTime) > 10) {
					throw new Exception("Sanity check failed!");
				}

				// We will ask for all measurements that entered the openaq system between now and the time when
				// we last asked for measurements. To be more sure that we will not lose measurements we could
				// perhaps extend the interval to e.g. 1 more hour in the past
				int extraSafetyTimeInSecs = 3600;
				// date_to is normally not needed but we specify it to ensure that the results will not contain
				// measurements with wrong dates, i.e. in the future
				// long date_to = nextCrawlTime * 1000; // also convert to millis
				// long date_from = (nextCrawlTime - crawlTimeSpanSecs - extraSafetyTimeInSecs) * 1000;

				// date parameters in openaq should be formatted in OPENAQ_DATE_FORMAT and the timezone is UTC!!!
				// String toString = DateManipulation.convertTimestampToDateString(date_to, "UTC",
				// DateManipulation.OPENAQ_DATE_FORMAT_QUERY);
				// String fromString = DateManipulation.convertTimestampToDateString(date_from, "UTC",
				// DateManipulation.OPENAQ_DATE_FORMAT_QUERY);

				ArrayList<String> apiCallParameters = new ArrayList<String>();
				// apiCallParameters.add("date_from=" + fromString);
				// apiCallParameters.add("date_to=" + toString);
				apiCallParameters.add("has_geo=true"); // ensure that only geolocated measurements are returned
				// 1000 is the maximum number of results per page. If the results are more, then more pages should
				// be fetched
				apiCallParameters.add("limit=1000");

				String openAqEndpointAndParamsBase = openAqEndpoint;
				for (int i = 0; i < apiCallParameters.size(); i++) {
					if (i == 0) {// first param
						openAqEndpointAndParamsBase += "?" + apiCallParameters.get(i);
					} else {
						openAqEndpointAndParamsBase += "&" + apiCallParameters.get(i);
					}
				}

				for (String pollutant : pollutants) {
					Utils.outputMessage("Asking latest " + pollutant + " measurents!", verbose);

					String openAqEndpointAndParams = openAqEndpointAndParamsBase + "&parameter=" + pollutant;

					// Utils.outputMessage("Asking measurements uploaded in: " + fromString + " - " + toString,
					// verbose);

					// ArrayList<Document> docs = new ArrayList<Document>();
					ArrayList<Document> docsh = new ArrayList<Document>();
					for (String countryCode : countries.keySet()) {
						Utils.outputMessage("Asking measurents in country: " + countries.get(countryCode), verbose);

						boolean moreResultPages = true;
						int resultsProcessed = 0;
						int pageIndex = 1;
						while (moreResultPages) {
							String callString = openAqEndpointAndParams + "&country=" + countryCode + "&page="
									+ pageIndex;
							// System.out.println(callString);

							String responseString = ServiceCalls.makeGetRequest(callString);

							// Sometimes, the response from openaq might not be as expected below due to e.g. server
							// errors. In such cases, an Exception will be thrown be the following code
							// Object responseObj = mapper.readValue(responseString, Object.class);
							OpenAQResponseLatest responseObj = mapper.readValue(responseString,
									OpenAQResponseLatest.class);
							int found = responseObj.getMeta().getFound();
							int resultsSize = responseObj.getResults().size();
							System.out.println(found + " measurements found!");
							System.out.println(resultsSize + " measurements in this page!");

							// if (found < 1000) {
							// if (found != resultsSize) {
							// throw new Exception("Unexpected!");
							// }
							// }

							for (OpenAQResponseLatest.Result res : responseObj.getResults()) {
								// Object responseObj = mapper.readValue(responseString, Object.class);

								// String resJson = mapper.writeValueAsString(res);
								// Document doc = Document.parse(resJson);
								// docs.add(doc);

								HackairRecordSensors hrec = res.toHackairRecordSensors();
								String hrecJson = mapper.writeValueAsString(hrec);
								Document doch = Document.parse(hrecJson);
								docsh.add(doch);

							}

							resultsProcessed += resultsSize;
							if (resultsProcessed >= found) {
								moreResultPages = false;
							} else {
								pageIndex++;
							}

							TimeUnit.SECONDS.sleep(1); // to ensure that openAQ's rate limit is respected
						}
					}

					// +++ writing to mongo start +++
					Utils.outputMessage(
							"All queries generated " + docsh.size() + " usable photos that should be written to mongo",
							verbose);
					ArrayList<Document> photoDocsWritten = writeDocumentsToMongo(docsh, collection, "id");

				}

			} catch (JsonParseException | JsonMappingException e) {
				// if an exception of the above types is thrown while trying to fetch the results from OpenAQ, it means
				// that the response from OpenAQ is not as expected (i.e. as usual) due to server errors
				// in that case, ignore the Exception and just proceed below to wait until the next crawl cycle.
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

	public static HashMap<String, String> populateCountries() {
		HashMap<String, String> populateCountries = new HashMap<String, String>();
		populateCountries.put("AT", "Austria");
		populateCountries.put("BA", "Bosnia and Herzegovina");
		populateCountries.put("BE", "Belgium");
		populateCountries.put("CH", "Switzerland");
		populateCountries.put("CZ", "Czech Republic");
		populateCountries.put("DE", "Germany");
		populateCountries.put("DK", "Denmark");
		populateCountries.put("ES", "Spain");
		populateCountries.put("FI", "Finland");
		populateCountries.put("FR", "France");
		populateCountries.put("GB", "United Kingdom");
		populateCountries.put("HR", "Croatia");
		populateCountries.put("GI", "Gibraltar");
		populateCountries.put("HU", "Hungary");
		populateCountries.put("IE", "Ireland");
		populateCountries.put("LT", "Lithuania");
		populateCountries.put("LU", "Luxembourg");
		populateCountries.put("LV", "Latvia");
		populateCountries.put("MK", "Macedonia, the Former Yugoslav Republic of");
		populateCountries.put("MT", "Malta");
		populateCountries.put("NL", "Netherlands");
		populateCountries.put("NO", "Norway");
		populateCountries.put("PL", "Poland");
		populateCountries.put("PT", "Portugal");
		populateCountries.put("SE", "Sweden");
		populateCountries.put("IT", "Italy");
		populateCountries.put("RU", "Russian Federation");
		populateCountries.put("RS", "Serbia");
		populateCountries.put("SI", "Slovenia");
		populateCountries.put("SK", "Slovakia");
		populateCountries.put("SE", "Sweden");

		return populateCountries;
	}

}