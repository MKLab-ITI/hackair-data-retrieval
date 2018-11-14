package gr.iti.mklab.misc;

import java.util.Date;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import gr.iti.mklab.utils.MongoConnection;

public class CheckCollectionDates {

	public static void main(String[] args) throws Exception {
		MongoConnection mongo = new MongoConnection("hackair_v2", "flickr", "173.212.212.242");
		MongoCollection<Document> collection = mongo.getCollection();

		Document isFlickr = new Document("source_type", "flickr");

		MongoCursor<Document> cursor = collection.find(isFlickr).iterator();

		int counter = 0;
		while (cursor.hasNext()) {
			if (counter % 1000 == 0) {
				System.out.println(counter);
			}
			Document doc = cursor.next();
			Date taken_date = doc.getDate("datetime");
			Date upload_date = ((Document) doc.get("source_info")).getDate("date_uploaded");
			if (upload_date.getTime() > taken_date.getTime() + 24 * 3600 * 1000) {
				throw new Exception("Unexpected!");
			}

			counter++;
		}
	}

}
