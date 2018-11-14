package gr.iti.mklab.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;

import gr.iti.mklab.utils.DateManipulation;
import gr.iti.mklab.utils.MongoConnection;


public class GeographicalQueriesTest {

	public static HashMap<String, BasicDBObject> countryToGeometry;

	public static void main(String[] args) throws Exception {

		// String mongoSettingsFile = "input/mongosettings.json";
		// MongoConnection mongo = new MongoConnection(mongoSettingsFile);
		MongoConnection mongo = new MongoConnection("flickr_hackair", "data_test");
		MongoCollection<Document> collection = mongo.getCollection();

		String path = "input/polygons/";
		String[] countries = { "germany", "norway", "greece" };
		boolean bbox = false;
		countryToGeometry = new HashMap<String, BasicDBObject>();
		for (String country : countries) {
			String countryGeometryFile = path + country + ".json";
			BasicDBObject geometry = extractGeometryDBObject(countryGeometryFile, bbox);
			countryToGeometry.put(country, geometry);
		}

		String startingDate = "2016-10-20 00:00:00";
		String zoneId = "CET";
		long minTimestamp = DateManipulation.convertStringDateToTimestamp(startingDate, zoneId, DateManipulation.FLICKR_DATE_FORMAT);
		long step = 3600 * 1 * 1000;
		long takenWindow = 3600 * 24 * 1000;
		long uploadedWindow = -1; // limits lower=0 higher = -1

		long maxTimestamp = System.currentTimeMillis() - 3600 * 1000;
		long curMaxTimestamp = minTimestamp;

		while (curMaxTimestamp < maxTimestamp) {
			for (String country : countries) {
				BasicDBObject geometry = countryToGeometry.get(country);
				long minTakenTimestamp = curMaxTimestamp - takenWindow;
				long maxTakenTimestamp = curMaxTimestamp;
				// System.out.println(minTakenTimestamp + " " + maxTakenTimestamp + " " +
				// maxUploadedTimestamp);
				BasicDBObject query = findWithinPolygon(geometry, minTakenTimestamp, maxTakenTimestamp,
						uploadedWindow);
				long numResults = 0;
				for (Document cur : collection.find(query)) {
					System.out.println(cur.toJson());
					numResults++;
				}
				// long numResults = collection.count(query);

				System.out.println(
						country + "," + DateManipulation.convertTimestampToDateString(curMaxTimestamp, zoneId, DateManipulation.FLICKR_DATE_FORMAT) + ","
								+ numResults + "," + bbox + "," + uploadedWindow);
			}

			BasicDBObject query = findWithinPolygon(null, curMaxTimestamp - takenWindow, curMaxTimestamp,
					uploadedWindow);
			// int numResults = 0;
			// for (Document cur : collection.find(query)) {
			// // System.out.println(cur.toJson());
			// numResults++;
			// }
			long numResults = collection.count(query);

			System.out
					.println("europe" + "," + DateManipulation.convertTimestampToDateString(curMaxTimestamp, zoneId, DateManipulation.FLICKR_DATE_FORMAT)
							+ "," + numResults + "," + bbox + "," + uploadedWindow);

			curMaxTimestamp += step;
		}

	}

	public static BasicDBObject findWithinPolygon(BasicDBObject geometry, long minTakenTimestamp,
			long maxTakenTimestamp, long uploadedWindow) {

		BasicDBObject dateUploadedQuery = null;
		if (uploadedWindow >= 0) {
			long maxUploadedTimestamp = maxTakenTimestamp + uploadedWindow;
			dateUploadedQuery = new BasicDBObject("source_info.date_uploaded",
					new BasicDBObject("$lte", new Date(maxUploadedTimestamp)));
			// System.out.println(dateUploadedQuery);
		}

		BasicDBObject locationQuery = null;
		if (geometry != null) {
			locationQuery = new BasicDBObject("loc", new BasicDBObject("$geoWithin", geometry));
			// System.out.println(locationQuery);
		}

		BasicDBObject dateTakenQuery = new BasicDBObject("datetime",
				new BasicDBObject("$gt", new Date(minTakenTimestamp)).append("$lte",
						new Date(maxTakenTimestamp)));
		
		ArrayList<BasicDBObject> extraQueries = new ArrayList<BasicDBObject>();
		BasicDBObject skyQuery = new BasicDBObject("concepts.sky",
				new BasicDBObject("$gt", 0.5));
		extraQueries.add(skyQuery);


		if (locationQuery != null) {
			extraQueries.add(locationQuery);
		}
		if (dateUploadedQuery != null) {
			extraQueries.add(dateUploadedQuery);
		}

		if (extraQueries.size() == 0) {
			return dateTakenQuery;
		} else {
			extraQueries.add(dateTakenQuery);
			BasicDBObject combined = new BasicDBObject("$and", extraQueries);
			System.out.println(combined);
			return combined;
		}

	}

	public static BasicDBObject extractGeometryDBObject(String countryPolygonFile, boolean bbox)
			throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(new File(countryPolygonFile)));
		StringBuffer sb = new StringBuffer();
		String line;
		while ((line = in.readLine()) != null) {
			sb.append(line);
		}
		in.close();
		String jsonString = sb.toString();

		Document countryPolygon = Document.parse(jsonString);
		Document features = (Document) ((ArrayList) countryPolygon.get("features")).get(0);
		Document geometry = (Document) features.get("geometry");
		ArrayList bboxList = (ArrayList) features.get("bbox");

		// System.out.println(geometry);
		// String type = ((String) geometry.get("type"));
		// System.out.println(type);
		// ArrayList coOrdinates = (ArrayList) geometry.get("coordinates");
		// System.out.println(coOrdinates);

		BasicDBObject geometryDBObejct;
		if (!bbox) {
			geometryDBObejct = new BasicDBObject("$geometry", new BasicDBObject()
					.append("type", geometry.get("type")).append("coordinates", geometry.get("coordinates")));
		} else {
			geometryDBObejct = new BasicDBObject("$box", bboxList);
		}

		return geometryDBObejct;
	}

}
