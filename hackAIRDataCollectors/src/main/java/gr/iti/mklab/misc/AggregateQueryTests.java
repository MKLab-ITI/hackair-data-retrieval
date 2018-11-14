package gr.iti.mklab.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mongodb.MongoClient;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;

public class AggregateQueryTests {

	public static void main(String[] args) throws Exception {
		String mongoHost = "localhost";
		String mongoDb = "hackair";
		String mongoCollection = "sensors";
		String placeGeometryFile = "input/polygons/germany.json";

		MongoCollection col = new MongoClient(mongoHost, 27017).getDatabase(mongoDb)
				.getCollection(mongoCollection);

		ArrayList<Document> pipeline = new ArrayList<Document>();

		// Document matchDoc = new Document("countryCode", "DE").append("pollutant", "pm10");
		Document matchDoc = new Document("pollutant", "pm10");
		Document geometryDoc = extractPlaceGeometryDocument(placeGeometryFile, false);
		matchDoc.put("loc", new Document("$geoWithin", geometryDoc));
		pipeline.add(new Document("$match", matchDoc));

		Document sortDoc = new Document("datetime", -1);
		pipeline.add(new Document("$sort", sortDoc));

		Document groupDoc = new Document("_id", "$location");
		String[] fieldsInOutput = { "datetime", "location", "countryCode", "pollutant", "value", "loc" };
		for (String field : fieldsInOutput) {
			groupDoc.put(field, new Document("$first", "$" + field));
		}
		pipeline.add(new Document("$group", groupDoc));

		long start = System.currentTimeMillis();
		AggregateIterable<Document> results = col.aggregate(pipeline);
		long end = System.currentTimeMillis();
		System.out.println("Query took: " + (end - start) + " ms");

		int counter = 0;

		JSONArray jsonArray = new JSONArray();
		for (Document result : results) {
			if (jsonArray.length() == 10) {
				break;
			}
			Date date = (Date) result.get("datetime");
			long time = date.getTime();
			result.replace("datetime", time);
			// System.out.println(time);\
			// System.out.println(date);

			jsonArray.put(result);
			System.out.println(result.toJson());
		}
		System.out.println("Total results: " + counter);

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("measurements", jsonArray);
		System.out.println(jsonObject.toString());
	}

	/**
	 * Extracts the bounding box or the geometry of a place from a geojson file.
	 * 
	 * @param placeGeometryFile
	 * @param bbox
	 * @return
	 * @throws Exception
	 */
	public static Document extractPlaceGeometryDocument(String placeGeometryFile, boolean bbox)
			throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(new File(placeGeometryFile)));
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

		Document geometryDBObejct;
		if (!bbox) {
			geometryDBObejct = new Document("$geometry", new Document("type", geometry.get("type"))
					.append("coordinates", geometry.get("coordinates")));
		} else {
			geometryDBObejct = new Document("$box", bboxList);
		}

		return geometryDBObejct;
	}

}
