package gr.iti.mklab.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import gr.iti.mklab.flickr.FlickrCollector;
import gr.iti.mklab.utils.DateManipulation;

public class FlickResponseParsingTest {

	public static void main(String[] args) throws Exception {

		String message = "E11000 duplicate key error index: flickr_hackair.data_test.$image_name_1 dup key: { : \"30939203486\" }";
		String dupKey = message.split("dup key: \\{ : \"")[1].split("\"")[0];
		System.out.println(dupKey);

		System.exit(0);

		long dateUploadeTimestamp = 1478866289000L;
		String dateUploadedStringGMT = DateManipulation.convertTimestampToDateString(dateUploadeTimestamp, "GMT",
				DateManipulation.FLICKR_DATE_FORMAT);
		System.out.println(dateUploadedStringGMT);

		String dateUploadedStringCET = DateManipulation.convertTimestampToDateString(dateUploadeTimestamp, "CET",
				DateManipulation.FLICKR_DATE_FORMAT);
		System.out.println(dateUploadedStringCET);

		long timestamp1 = DateManipulation.convertStringDateToTimestamp(dateUploadedStringGMT, "GMT",
				DateManipulation.FLICKR_DATE_FORMAT);
		long timestamp2 = DateManipulation.convertStringDateToTimestamp(dateUploadedStringCET, "CET",
				DateManipulation.FLICKR_DATE_FORMAT);
		System.out.println(timestamp1);
		System.out.println(timestamp2);

		System.exit(0);

		String exampleFile = "sample_response.txt";
		boolean debug = true;
		BufferedReader in = new BufferedReader(new FileReader(new File(exampleFile)));
		String response = in.readLine();
		in.close();

		// Flickr fl = new Flickr("adf14621cb57ec92f0161703c87ea38c", null, null);
		// SearchParameters sp = new SearchParameters();
		// sp.setWoeId("24865675");
		// Date date = new Date(System.currentTimeMillis() - 3600 * 12);
		// sp.setMinTakenDate(date);
		// PhotoList<Photo> photos = fl.getPhotosInterface().search(sp, 250, 1);

		JSONObject json = new JSONObject(response);

		JSONArray photoArr = json.getJSONObject("photos").getJSONArray("photo");

		for (int i = 0; i < photoArr.length(); i++) {
			if (i >= 1) {
				break;
			}
			JSONObject photoObj = photoArr.getJSONObject(i);
			System.out.println(photoObj.toString(2));
			Document doc = FlickrCollector.transformToDocument(photoObj, debug);
			System.out.println(doc.toJson());
		}

	}

}
