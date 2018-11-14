package gr.iti.mklab.utils;

import java.util.ArrayList;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import dcnn.DcnnPipeline;
import dcnn.classes.ConceptData;
import dcnn.classes.ImageConceptData;

public class ConceptDetectionThread extends Thread {

	public static final boolean verbose = true;

	private String rootFolder;
	private String dcnnPath;
	private MongoCollection<Document> collection;
	private ArrayList<Document> photoDocuments;
	private int detectionMaxBatchSize;

	public ConceptDetectionThread(String rootFolder, String dcnnPath, MongoCollection<Document> collection,
			ArrayList<Document> photoDocuments, int detectionMaxBatchSize) {
		this.rootFolder = rootFolder;
		this.dcnnPath = dcnnPath;
		this.collection = collection;
		this.photoDocuments = photoDocuments;
		this.detectionMaxBatchSize = detectionMaxBatchSize;
	}

	public void run() {
		performConceptDetection(rootFolder, dcnnPath, collection, photoDocuments, detectionMaxBatchSize);
	}

	/**
	 * Performs DCNN concept detection and update the mongo records.
	 * 
	 * @param photoDocuments
	 * @param collection
	 */
	private static void performConceptDetection(String rootFolder, String dcnnPath,
			MongoCollection<Document> collection, ArrayList<Document> photoDocuments,
			int detectionMaxBatchSize) {
		if (photoDocuments.size() == 0) {
			return;
		}
		int dcnnCounter = 0;
		int processedCounter = 0;
		int withSkyCounter = 0;
		ArrayList<String> imgPathsList = new ArrayList<String>();
		ArrayList<String> imageIdList = new ArrayList<String>();
		for (Document doc : photoDocuments) {
			dcnnCounter++;

			String imagePath = rootFolder + extractImagePath(doc);
			// System.out.println(imagePath);
			imgPathsList.add(imagePath);
			imageIdList.add(((Document) doc.get("source_info")).getString("id"));

			// every 100 images or if the end of the list is reached, send a batch to concept detector
			if (dcnnCounter % detectionMaxBatchSize == 0 || dcnnCounter == photoDocuments.size()) {
				Utils.outputMessage("Sending batch " + (processedCounter + 1) + "-" + dcnnCounter
						+ " to concept detector", verbose);

				ArrayList<ImageConceptData> imgCon = null;
				try {
					imgCon = (ArrayList<ImageConceptData>) DcnnPipeline.runPipeline(dcnnPath, imageIdList,
							imgPathsList);
				} catch (Exception e) {
					System.err.println("DCNN extraction failed for this batch!");
					// System.err.println("DCNN extraction failed for image " + imgPathsList.get(0));
					continue;
				}

				for (int i = 0; i < imgCon.size(); i++) {
					String imageId = imgCon.get(i).getImageName();
					// System.out.println(imageId);
					ArrayList<ConceptData> conData = (ArrayList<ConceptData>) imgCon.get(i).getConceptData();
					Document conceptDoc = new Document();
					boolean hasSky = false;
					for (int j = 0; j < conData.size(); j++) {
						String conceptName = conData.get(j).getConceptName();
						double conceptScore = conData.get(j).getConceptValue();
						conceptDoc.append(conceptName, conceptScore);
						if (conceptScore > 0.8) {
							hasSky = true;
						}
					}
					if (hasSky) {
						withSkyCounter++;
					}
					// Document correspondingDoc = photoDocsDownloaded.get(i);
					// correspondingDoc.append("concepts", conceptDoc);

					Document updateQuery = new Document();
					updateQuery.append("$set", new Document().append("source_info.concepts", conceptDoc));

					Document searchQuery = new Document();
					searchQuery.append("source_info.id", imageId);

					collection.updateOne(searchQuery, updateQuery);
				}
				Utils.outputMessage("Batch processing completed successfully!", verbose);
				// empty list to hold new batch
				imgPathsList = new ArrayList<String>();
				imageIdList = new ArrayList<String>();
				processedCounter = dcnnCounter;
			}

		}

		System.out.println("Images with sky: " + withSkyCounter + " out of " + photoDocuments.size());
	}

	public static String extractImagePath(Document photoDoc) {
		String photoId = (String) ((Document) photoDoc.get("source_info")).get("id");
		String dateStr = ((String) photoDoc.get("date_str")).split(" ")[0];
		String imagePath = dateStr + "/" + photoId + ".jpg";
		return imagePath;
	}

}
