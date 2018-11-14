package gr.iti.mklab.misc;

import org.jongo.Jongo;
import org.jongo.MongoCursor;

import com.mongodb.MongoClient;

import pojo.mongo.HackairRecordFlickr;

public class CopyRecordsBetweenMongoDbs {

	public static void main(String[] args) throws Exception {

		// mongodb connection details
		String mongoHost = "173.212.212.242";
		String mongoDbRemote = "hackair_v2";
		String mongoCollectionRemote = "flickr";

		String mongoDbLocal = "flickr_hackair";
		String mongoCollectionLocal = "data_test";

		// JONGO (saves the day!)
		Jongo jongo = new Jongo(new MongoClient(mongoHost, 27017).getDB(mongoDbRemote));
		org.jongo.MongoCollection hackairCollectionRemoteJongo = jongo.getCollection(mongoCollectionRemote);

		Jongo jongo2 = new Jongo(new MongoClient("localhost", 27017).getDB(mongoDbLocal));
		org.jongo.MongoCollection hackairCollectionLocalJongo = jongo2.getCollection(mongoCollectionLocal);

		MongoCursor<HackairRecordFlickr> cursor = hackairCollectionRemoteJongo.find()
				.as(HackairRecordFlickr.class);

		// put selected records to an ArrayList
		int counter = 0;
		while (cursor.hasNext()) {
			if (counter % 1000 == 0) {
				System.out.println(counter + " records copied");
			}
			HackairRecordFlickr rec = cursor.next();
			hackairCollectionLocalJongo.insert(rec);
			counter++;
		}
		cursor.close();

	}

}
