package gr.iti.mklab.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;

import org.bson.Document;

import com.mongodb.BasicDBObject;

public class MongoQueryUtils {
	public static final String dateFieldName = "datetime";

	public static BasicDBObject createDateQuery(String dateStringFrom, String dateStringTo) throws Exception {
		if (dateStringFrom == null && dateStringTo == null) {
			throw new Exception("Either from or to should be != null.");
		}

		// transform dates to timestamps assuming CET timezone

		Long timeStampFrom = null;
		if (dateStringFrom != null) {
			timeStampFrom = DateManipulation.convertStringDateToTimestamp(dateStringFrom, "CET",
					DateManipulation.FLICKR_DATE_FORMAT);
		}

		Long timeStampTo = null;
		if (dateStringTo != null) {
			timeStampTo = DateManipulation.convertStringDateToTimestamp(dateStringTo, "CET",
					DateManipulation.FLICKR_DATE_FORMAT);
		}

		return createDateQuery(timeStampFrom, timeStampTo);

	}

	public static BasicDBObject createDateQuery(Long timeStampFrom, Long timeStampTo) throws Exception {
		BasicDBObject dateQuery = null;

		if (timeStampFrom == null && timeStampTo == null) {
			throw new Exception("Either from or to should be != null.");
		}

		if (timeStampFrom != null) {
			dateQuery = new BasicDBObject(dateFieldName, new BasicDBObject("$gte", new Date(timeStampFrom)));
		}

		if (timeStampTo != null) {
			if (dateQuery == null) {
				dateQuery = new BasicDBObject(dateFieldName,
						new BasicDBObject("$lte", new Date(timeStampTo)));
			} else {
				((BasicDBObject) dateQuery.get(dateFieldName)).append("$lte", new Date(timeStampTo));
			}
		}

		return dateQuery;

	}

	/**
	 * Extracts the bounding box or the geometry of a place from a geojson file.
	 * 
	 * @param placeGeometryFile
	 * @param bbox
	 * @return
	 * @throws Exception
	 */
	public static BasicDBObject extractPlaceGeometryBasicDBObject(String placeGeometryFile, boolean bbox)
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
