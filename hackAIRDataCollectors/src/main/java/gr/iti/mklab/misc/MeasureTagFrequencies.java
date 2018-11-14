package gr.iti.mklab.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import gr.iti.mklab.utils.MongoConnection;

public class MeasureTagFrequencies {

	public static void main(String[] args) {

		MongoConnection mongo = new MongoConnection("hackair_v2", "flickr", "localhost");
		MongoCollection<Document> collection = mongo.getCollection();

		Document hasTags = new Document("source_info.tags", new Document("$exists", true));
		Document usableSky = new Document("source_info.ia.usableSky", true);
		Document hasSky = new Document("source_info.ia.containsSky", true);

		ArrayList<Document> queries = new ArrayList<Document>();
		queries.add(hasTags);
		queries.add(usableSky);

		Document hasTagsAndUsableSky = null;
		if (queries.size() == 1) {
			hasTagsAndUsableSky = queries.get(0);
		} else { // combine queries with the AND operator
			Document combined = new Document("$and", queries);
			hasTagsAndUsableSky = combined;
		}

		HashMap<String, Integer> tagToFreqAll = computeFreqs(collection, hasTags);
		HashMap<String, Integer> tagToFreqUsable = computeFreqs(collection, hasTagsAndUsableSky);

		HashMap<String, Double> tagToNfreq = new HashMap<String, Double>();

		for (String tag : tagToFreqUsable.keySet()) {
			int freq = tagToFreqUsable.get(tag);
			if (freq > 100) {
				int freqAll = tagToFreqAll.get(tag);
				double normalized = (double) freq / freqAll;
				tagToNfreq.put(tag, normalized);
			}
		}

		System.out.println(entriesSortedByValues(tagToFreqAll));
		System.out.println(entriesSortedByValues(tagToFreqUsable));
		System.out.println(entriesSortedByValues(tagToNfreq));

		// for (String term : tagToFreq.keySet()) {
		// System.out.println(term + ": " + tagToFreq.get(term));
		//
		// }
	}

	public static HashMap<String, Integer> computeFreqs(MongoCollection<Document> collection,
			Document findQuery) {

		MongoCursor<Document> cursor = collection.find(findQuery).iterator();

		int counter = 0;
		HashMap<String, Integer> tagToFreq = new HashMap<String, Integer>();
		try {
			while (cursor.hasNext()) {
				// if (counter == 10) {
				// break;
				// }
				Document doc = cursor.next();
				// System.out.println(doc.toJson());
				String tags = ((Document) doc.get("source_info")).getString("tags");
				String title = ((Document) doc.get("source_info")).getString("title");
				// System.out.println("tags: " + tags + "\ttitle: " + title);
				// System.out.print(((Document) doc.get("source_info")).getString("id") + ": ");
				if (tags.length() > 0) {
					String[] tagTerms = tags.split("\\s+");
					for (String term : tagTerms) {
						// System.out.print(term + "|");
						Integer freq = tagToFreq.get(term);
						if (freq == null) {
							tagToFreq.put(term, 1);
						} else {
							tagToFreq.put(term, freq + 1);
						}
					}
				}
				if (title.length() > 0) {
					String[] titleTerms = title.split("\\s+");
					for (String term : titleTerms) {
						// System.out.print(term + "|");
						Integer freq = tagToFreq.get(term);
						if (freq == null) {
							tagToFreq.put(term, 1);
						} else {
							tagToFreq.put(term, freq + 1);
						}
					}
				}
				// System.out.println();
				counter++;
			}
		} finally {
			cursor.close();
		}
		System.out.println("Total results: " + counter);
		return tagToFreq;
	}

	static <K, V extends Comparable<? super V>> List<Entry<K, V>> entriesSortedByValues(Map<K, V> map) {

		List<Entry<K, V>> sortedEntries = new ArrayList<Entry<K, V>>(map.entrySet());

		Collections.sort(sortedEntries, new Comparator<Entry<K, V>>() {
			@Override
			public int compare(Entry<K, V> e1, Entry<K, V> e2) {
				return e2.getValue().compareTo(e1.getValue());
			}
		});

		return sortedEntries;
	}

}
