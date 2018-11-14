package gr.iti.mklab.misc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import org.bson.Document;
import org.json.JSONObject;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.InsertManyOptions;

import gr.iti.mklab.utils.MongoConnection;

public class MongoDbUsageTests {

	public static void main(String[] args) throws Exception {
		Random rand = new Random(1);
		String indexType = "unique"; // no, std, unique
		int docsToGenerate = 100000;

		// Mongo connection
		String mongoSettingsFile = "input/mongosettings.json";
		MongoConnection mongo = new MongoConnection(mongoSettingsFile);
		MongoDatabase db = mongo.getDb();
		MongoCollection<Document> collection = mongo.getCollection();

		collection.drop();

		// create a unique index on the id for the collection
		if (indexType.equals("no")) {
			//
		} else if (indexType.equals("unique")) {
			Document keys = new Document("id", 1);
			collection.createIndex(keys, new IndexOptions().unique(true));
		} else if (indexType.equals("std")) {
			collection.createIndex(new Document("id", 1));
		} else {
			throw new Exception("Wrong index type!");
		}

		for (String coll : db.listCollectionNames()) {
			System.out.println(coll);
		}

		// check if multiple entries can be inserted at once!
		long start = System.currentTimeMillis();
		HashSet<String> uniqueIds = new HashSet<String>();
		ArrayList<Document> docs = new ArrayList<Document>();
		for (int i = 0; i < docsToGenerate; i++) {
			String id = String.valueOf(rand.nextInt(docsToGenerate));
			uniqueIds.add(id);
			JSONObject obj = new JSONObject();
			obj.put("id", id);
			obj.put("name", "D.J.TRUMP");

			// Insert json data into mongo
			String jsonS = obj.toString();
			// System.out.println(jsonS);
			Document doc = Document.parse(jsonS);

			docs.add(doc);

			// if (!recordExists(collection, String.valueOf(id))) {
			// collection.insertOne(doc);
			// }

			// try {
			// collection.insertOne(doc);
			// } catch (MongoWriteException e) {
			// if (e.getCode() == 11000) {
			// // a duplicate entry, do nothing
			// } else { // rethrow exception
			// throw e;
			// }
			// }
		}
		long countBefore = collection.count();
		try {
			collection.insertMany(docs, new InsertManyOptions().ordered(false));
		} catch (MongoBulkWriteException e) {
			// System.err.println(message);
			if (e.getMessage().contains("duplicate key error")) {
				// a duplicate entry, do nothing
			} else { // re-throw exception
				throw e;
			}
		}

		long duplicateCounter = docsToGenerate - (collection.count() - countBefore);
		long end = System.currentTimeMillis();
		System.out.println("Insertion took " + (end - start) + " ms");
		System.out.println("All docs: " + docsToGenerate);
		System.out.println("Unique documents " + uniqueIds.size());
		System.out.println("Duplicates: " + duplicateCounter);
		long startCount = System.currentTimeMillis();
		System.out.println("Collection size " + collection.count());
		long endCount = System.currentTimeMillis();
		System.out.println("Count took " + (endCount - startCount) + " ms");

	}

	public static boolean recordExists(MongoCollection<Document> collection, String id) {
		boolean flag = false;
		FindIterable<Document> dd = collection.find(com.mongodb.client.model.Filters.eq("id", id))
				.projection(com.mongodb.client.model.Projections
						.fields(com.mongodb.client.model.Projections.include("id")))
				.limit(1);
		Iterator<Document> iter = dd.iterator();
		while (iter.hasNext()) {
			flag = true;
			break;
		}

		return flag;
	}

}
