package gr.iti.mklab.misc;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import javax.imageio.ImageIO;

import org.jongo.Jongo;
import org.jongo.MongoCursor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;

import gr.iti.mklab.geonames.ReverseGeocoder;
import gr.iti.mklab.utils.MongoQueryUtils;
import gr.iti.mklab.utils.ServiceCalls;
import gr.mklab.GeneralFunctions;
import gr.mklab.SkyLocalizationAndRatios;
import gr.mklab.classes.RatioCalculationResults;
import pojo.mongo.HackairRecordFlickrWithIA;
import pojo.requests.LocalizationRequest;
import pojo.responses.LocalizationResponse;

public class ExportMongoDataToCSV {

	public static final String fieldSeparator = ";";
	// public static final String[] exportFields = { "name", "latitude", "longitude", "day", "hour", "minute",
	// "R/G", "G/B", "sky_pixels", "all_pixels", "sky", "clouds", "sun" };
	// public static final String[] exportFields = { "id", "latitude", "longitude", "day", "hour", "minute",
	// "R/G", "G/B", "sky_pixels", "all_pixels" };
	// public static final String[] exportFields = { "id", "source_type", "webcam_id", "datetime", "year", "month",
	// "dayofmonth", "hour", "dayofyear", "containsSky", "usableSky", "sky", "clouds", "sun", "sky_pixels",
	// "all_pixels", "R/G", "G/B" };
	// public static final String[] exportFields = { "id", "latitude", "longitude", "tags", "title",
	// "description" };
	public static final String[] exportFields = { "source_type", "name", "datetime" };

	private static final String localizationServiceEndpoint = "http://160.40.50.236:8080/SkyLocalizationFCN/post";

	public static void main(String[] args) throws Exception {

		boolean appendLocation = false;
		String exportFileName = "data_usablesky.txt";
		// mongo connection details
		String mongoHost = "173.212.212.242";
		String mongoDb = "hackair";
		String mongoCollection = "images";
		String sourceType = "flickr";
		String place = "greece";
		String placeGeometryFile = "input/polygons/" + place + ".json";
		long now = System.currentTimeMillis();
		long maxTimestamp = now;
		// System.out.println(maxTimestamp + " - ");
		long oneMonthSecs = 60 * 24 * 3600;
		long oneMonthMillis = oneMonthSecs * 1000;
		long minTimestamp = now - oneMonthMillis;
		// System.out.println(minTimestamp);

		boolean downloadImages = true;
		String imageDownloadFolder = "images/";
		boolean exportMasks = false;
		String imagesPath = "Z:/"; // remote webdav directory is mapped to this drive
		String maskExportPath = "masks/" + sourceType + "/";
		int seed = 1;
		Random rand = new Random(seed);
		int numToSelect = 1000; // use a large number, e.g. Integer.MAX_VALUE to avoid rejection
		int numToSkip = 0;

		// String exportFileName = "data_" + place + "_" + sourceType + "_" + System.currentTimeMillis()+
		// ".txt";

		ArrayList<BasicDBObject> queries = new ArrayList<BasicDBObject>();
		BasicDBObject geometry = MongoQueryUtils.extractPlaceGeometryBasicDBObject(placeGeometryFile, true);
		BasicDBObject geoQuery = new BasicDBObject("loc", new BasicDBObject("$geoWithin", geometry));

		// BasicDBObject dateQuery = MongoQueryGeneration.createDateQuery(minTimestamp, maxTimestamp);
		BasicDBObject dateQuery = MongoQueryUtils.createDateQuery("2017-10-01 00:00:00", "2017-11-01 00:00:00");

		// BasicDBObject sourceQuery = new BasicDBObject("source_type",
		// new BasicDBObject("$regex", "/^" + sourceType + "/"));
		BasicDBObject sourceQuery = new BasicDBObject("source_type", sourceType);
		BasicDBObject usableSky = new BasicDBObject("source_info.ia.usableSky", true);
		BasicDBObject hasTags = new BasicDBObject("source_info.tags", new BasicDBObject("$exists", true));
		BasicDBObject hasDesc = new BasicDBObject("source_info.description", new BasicDBObject("$exists", true));
		BasicDBObject hasIA = new BasicDBObject("source_info.ia", new BasicDBObject("$exists", true));
		BasicDBObject hasContainsSky = new BasicDBObject("source_info.ia.containsSky",
				new BasicDBObject("$exists", true));

		// String geoQuery =
		// "{loc:{$geoWithin:{$geometry:{type:\"MultiPolygon\",coordinates:[[[[23.69998,35.705004],[24.246665,35.368022],[25.025015,35.424996],[25.769208,35.354018],[25.745023,35.179998],[26.290003,35.29999],[26.164998,35.004995],[24.724982,34.919988],[24.735007,35.084991],[23.514978,35.279992],[23.69998,35.705004]]],[[[26.604196,41.562115],[26.294602,40.936261],[26.056942,40.824123],[25.447677,40.852545],[24.925848,40.947062],[23.714811,40.687129],[24.407999,40.124993],[23.899968,39.962006],[23.342999,39.960998],[22.813988,40.476005],[22.626299,40.256561],[22.849748,39.659311],[23.350027,39.190011],[22.973099,38.970903],[23.530016,38.510001],[24.025025,38.219993],[24.040011,37.655015],[23.115003,37.920011],[23.409972,37.409991],[22.774972,37.30501],[23.154225,36.422506],[22.490028,36.41],[21.670026,36.844986],[21.295011,37.644989],[21.120034,38.310323],[20.730032,38.769985],[20.217712,39.340235],[20.150016,39.624998],[20.615,40.110007],[20.674997,40.435],[20.99999,40.580004],[21.02004,40.842727],[21.674161,40.931275],[22.055378,41.149866],[22.597308,41.130487],[22.76177,41.3048],[22.952377,41.337994],[23.692074,41.309081],[24.492645,41.583896],[25.197201,41.234486],[26.106138,41.328899],[26.117042,41.826905],[26.604196,41.562115]]]]}}}}";
		// queries.add(geoQuery);
		queries.add(dateQuery);
		// queries.add(sourceQuery);
		// queries.add(hasTags);
		// queries.add(hasDesc);
		queries.add(hasContainsSky);
		queries.add(usableSky);

		String findQuery = "";
		if (queries.size() == 1) {
			findQuery = queries.get(0).toJson();
		} else { // combine queries with the AND operator
			BasicDBObject combined = new BasicDBObject("$and", queries);
			findQuery = combined.toJson();
		}

		// String findQuery = "{'source_type': '" + sourceType + "', 'source_info.ia.usableSky': {$eq:
		// true}}";
		// String combinedQuery = "{$and: [" + geoQuery + "," + findQuery + "]}";

		String sortQuery = "{datetime: 1}";
		// String sortQuery = "";

		// JONGO (saves the day!)
		DB db = new MongoClient(mongoHost, 27017).getDB(mongoDb);
		Jongo jongo = new Jongo(db);
		org.jongo.MongoCollection hackairCollection = jongo.getCollection(mongoCollection);
		// MongoCursor<HackairRecordFlickrWithIA> cursor = hackairCollection.find(findQuery).sort(sortQuery)
		// .as(HackairRecordFlickrWithIA.class);
		MongoCursor<HackairRecordFlickrWithIA> cursor = hackairCollection.find(findQuery)
				.as(HackairRecordFlickrWithIA.class);
		int totalNum = cursor.count();
		System.out.println("Query returned: " + totalNum + " results!");
		// System.exit(0);

		double selectionPct = Math.min((double) numToSelect / totalNum, 1);
		System.out.println(
				String.format(Locale.US, "%.3g", selectionPct * 100) + "% of the results will be randomly exported!");

		// create an ArrayList to store all selected records to avoid cursor expiration when generating masks
		ArrayList<HackairRecordFlickrWithIA> records = new ArrayList<HackairRecordFlickrWithIA>();

		int counter = 0;
		while (cursor.hasNext()) {
			if (counter % 1000 == 0) {
				System.out.println(counter + " of " + totalNum + " records processed");
			}
			HackairRecordFlickrWithIA rec = cursor.next();
			records.add(rec);
			counter++;
		}
		cursor.close();
		Collections.shuffle(records);
		List<HackairRecordFlickrWithIA> recordsSelected = records.subList(0, numToSelect);

		ReverseGeocoder rgeoService = null;
		if (appendLocation) {
			// load reverse geocoder!
			String dir = "D:/lefteris/geolocation";
			String citiesFile = dir + "/cities1000.txt";
			String countryInfoFile = dir + "/countryInfo.txt";
			rgeoService = new ReverseGeocoder(citiesFile, countryInfoFile);
		}

		BufferedWriter out = new BufferedWriter(new FileWriter(new File(exportFileName)));
		// write export details
		out.write("%% find query: " + findQuery + "\n");
		// out.write("%% sort query: " + sortQuery + "\n");
		// write csv header
		for (String field : exportFields) {
			out.write(field + fieldSeparator);
		}
		out.write("\n");

		for (int i = numToSkip; i < recordsSelected.size(); i++) {

			HackairRecordFlickrWithIA rec = recordsSelected.get(i);
			String path = rec.getPath();
			String id = rec.getId();
			if (downloadImages) {
				String downloadURl = "https://services.hackair.eu:8083/images/" + path;
				URL url = new URL(downloadURl);
				BufferedImage image = ImageIO.read(url);
				ImageIO.write(image, "jpg", new File(imageDownloadFolder + id + ".jpg"));
			}

			String csvRow = rec.toCSV(exportFields, fieldSeparator);

			if (appendLocation) {
				double lat = rec.getLoc().getLatitude();
				double lon = rec.getLoc().getLongitude();
				String[] ccc = rgeoService.getCityAndCountryAndContinentyByLatLon(lat, lon);
				csvRow += ccc[0] + fieldSeparator + ccc[1] + fieldSeparator + ccc[2];
			}

			if (exportMasks) {
				System.out.println("Generating masks for image: " + rec.getSource_info().getId());
				try {
					ArrayList<RatioCalculationResults> results = generateMasks(imagesPath, rec, maskExportPath,
							String.valueOf(i + 1));
					// append additional information to the csv row
					RatioCalculationResults kourtidis = results.get(0);
					RatioCalculationResults fcnKourtidis = results.get(1);
					csvRow += kourtidis.getMeanRG() + fieldSeparator + kourtidis.getMeanGB() + fieldSeparator
							+ kourtidis.getNumSkyPixels() + fieldSeparator + kourtidis.getAllPixels() + fieldSeparator
							+ kourtidis.getTestThatFailed();
					// csvRow += fcnKourtidis.getMeanRG() + fieldSeparator + fcnKourtidis.getMeanGB()
					// + fieldSeparator + fcnKourtidis.getNumSkyPixels() + fieldSeparator
					// + fcnKourtidis.getAllPixels();
				} catch (Exception e) {
					System.err.println("Exception thrown: " + e.getMessage()
							+ "\nGoing to next image (without increasing the counter!)");
					continue;
				}
			}

			out.write(csvRow + "\n");
			out.flush();

			// print as JSON
			// ObjectMapper mapper = new ObjectMapper();
			// String jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rec);
			// System.out.println(jsonInString);
		}

		out.close();

	}

	public static ArrayList<RatioCalculationResults> generateMasks(String imagesRootFolder,
			HackairRecordFlickrWithIA rec, String maskExportPath, String masksName) throws Exception {

		ArrayList<RatioCalculationResults> results = new ArrayList<RatioCalculationResults>(2);

		String relativePath = rec.getSource_info().getPath();
		String originalImagePath = imagesRootFolder + relativePath;

		LocalizationRequest locReq = new LocalizationRequest();
		locReq.addImage(relativePath);

		// convert request object to json
		ObjectMapper mapper = new ObjectMapper();
		String locReqJson = mapper.writeValueAsString(locReq);

		String responseJson = ServiceCalls.makePostRequest(localizationServiceEndpoint, locReqJson, "UTF-8");

		// convert json response to object
		LocalizationResponse locRes = mapper.readValue(responseJson, LocalizationResponse.class);
		String encodedMaskString = locRes.getImages().get(0).getMask();
		// System.out.println("Mask: " + encodedMaskString);

		boolean[][] mask = decodeMask(encodedMaskString, null);
		String fcnMaskPath = maskExportPath + masksName + "-fcnmask.jpg";
		GeneralFunctions.saveMaskToFileWithBorders(originalImagePath, mask, fcnMaskPath);

		// call the method of Kourtidis on the FCN-masked image
		String kourtidisMaskPath = maskExportPath + masksName + "-kourtidismask.jpg";
		RatioCalculationResults rcr1 = SkyLocalizationAndRatios.processImage(imagesRootFolder + relativePath, null,
				kourtidisMaskPath);
		results.add(rcr1);
		String testThatFailed1 = rcr1.getTestThatFailed();
		double rg1 = rcr1.getMeanRG();
		double gb1 = rcr1.getMeanGB();
		System.out.println("Test that failed when using Kourtidis mask: " + testThatFailed1);
		System.out.println("Ratios: " + rg1 + " - " + gb1);

		// call the method of Kourtidis on the FCN-masked image
		String fcnKourtidisMaskPath = maskExportPath + masksName + "-fcn+kourtidismask.jpg";
		RatioCalculationResults rcr2 = SkyLocalizationAndRatios.processImage(imagesRootFolder + relativePath, mask,
				maskExportPath + masksName + "-fcn+kourtidismask.jpg");
		results.add(rcr2);

		String testThatFailed2 = rcr2.getTestThatFailed();
		double rg2 = rcr2.getMeanRG();
		double gb2 = rcr2.getMeanGB();
		System.out.println("Test that failed when using FCN+Kourtidis mask: " + testThatFailed2);
		System.out.println("Ratios: " + rg2 + " - " + gb2);

		// sanity check if existing results are the same with these
		double rg = rec.getSource_info().getIa().getRG();
		double gb = rec.getSource_info().getIa().getGB();
		if (Math.abs(rg - rg2) > 0.02 || Math.abs(gb - gb2) > 0.02) {
			throw new Exception("Ratios differ: " + rg + "!=" + rg2);
		}

		// join original image and masks into a single image
		String[] imagePaths = { originalImagePath, fcnMaskPath, kourtidisMaskPath, fcnKourtidisMaskPath };
		String[] imageCaptions = { "original", "fcn mask",
				"kourtidis mask - ratios: R/G=" + String.format(Locale.US, "%.3g", rg1) + " G/B="
						+ String.format(Locale.US, "%.3g", gb1),
				"fcn+kourtidis mask - ratios: R/G=" + String.format(Locale.US, "%.3g", rg) + " G/B="
						+ String.format(Locale.US, "%.3g", gb) };
		String joinedImagePath = maskExportPath + masksName + "-join.jpg";
		joinImages(imagePaths, imageCaptions, joinedImagePath);

		return results;
	}

	public static void joinImages(String[] imagePaths, String[] imageCaptions, String joinedImagePath)
			throws Exception {
		BufferedImage[] images = new BufferedImage[imagePaths.length];
		for (int i = 0; i < images.length; i++) {
			images[i] = ImageIO.read(new File(imagePaths[i]));
		}
		int horizonallMargin = 10;
		int captionheight = 20;
		int totalWidth = images[0].getWidth() * images.length + horizonallMargin * (images.length - 1);
		int totalHeight = images[0].getHeight() + captionheight;

		BufferedImage join = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB);
		Graphics g = join.getGraphics();

		int x = 0;
		for (int i = 0; i < images.length; i++) {
			g.drawImage(images[i], x, 0, null);

			g.setFont(g.getFont().deriveFont(16f));
			// int captionXPosition = (int) (x + images[0].getWidth() / 2.0);
			int captionXPosition = x + horizonallMargin;
			g.drawString(imageCaptions[i], captionXPosition, totalHeight - 2);
			// g.dispose();

			x += images[i].getWidth() + horizonallMargin;
		}

		ImageIO.write(join, "jpg", new File(joinedImagePath));
	}

	public static boolean[][] decodeMask(String encodedString, String decodedFile) throws Exception {
		boolean[][] decoded = null;
		int rowIndex = 0;
		try {
			String[] parts = encodedString.split("\\|", 2);
			String size = parts[0];
			String mask = parts[1];
			int height = Integer.parseInt(size.split("X")[0]);
			int width = Integer.parseInt(size.split("X")[1]);
			decoded = new boolean[height][];

			ArrayList<Character> rowCharsList = new ArrayList<Character>();
			for (int i = 0; i < mask.length(); i++) {
				char c = mask.charAt(i);
				if (c != 'R') { // append row
					rowCharsList.add(c);
				} else { // new row
					decoded[rowIndex] = new boolean[width];
					// decode previous row
					if (rowCharsList.size() > 0) { // previous row is not empty
						// transform arraylist of characters to String for easier manipulation
						char[] rowCharsArray = new char[rowCharsList.size()];
						for (int j = 0; j < rowCharsList.size(); j++) {
							rowCharsArray[j] = rowCharsList.get(j);
						}
						String row = String.valueOf(rowCharsArray);
						String[] segments = row.split(",");
						for (String segment : segments) {
							if (!segment.contains("-")) {
								int index = Integer.parseInt(segment);
								decoded[rowIndex][index] = true;
							} else {
								int startIndex = Integer.parseInt(segment.split("-", 2)[0]);
								int endIndex = Integer.parseInt(segment.split("-", 2)[1]);
								for (int j = startIndex; j <= endIndex; j++) {
									decoded[rowIndex][j] = true;
								}
							}
						}
						// empty previous row
						rowCharsList.clear();
					}
					rowIndex++;
				}
			}

			if (decodedFile != null) {
				BufferedWriter out = new BufferedWriter(new FileWriter(new File(decodedFile)));
				for (int i = 0; i < decoded.length; i++) {
					StringBuffer sb = new StringBuffer();
					for (int j = 0; j < decoded[i].length; j++) {
						if (decoded[i][j]) {
							sb.append("1 ");
						} else {
							sb.append("0 ");
						}
					}
					String row = sb.toString();
					out.write(row.substring(0, row.length() - 1) + "\n");
				}
				out.close();
			}
		} catch (Exception e) {
			System.out.println("Problem at row: " + (rowIndex + 1));
			throw e;
		}
		return decoded;
	}

}
