package gr.iti.mklab.webcams.travel;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

import javax.imageio.ImageIO;

import org.bson.Document;

import gr.iti.mklab.download.ImageScaling;
import gr.iti.mklab.utils.DateManipulation;

public class SimpleWebcamInfo {
	public static final String separator = ";";
	public static final int maxImgSize = 500 * 500;

	private String webcamId;
	private String staticImageUrl;
	private String webcamPageUrl;
	private double lat;
	private double lon;
	private long timestamp;
	private String path;
	private String imageId;

	/**
	 * Constructs a webcam object based on the information of the index file. Throws an exception is anything fails.
	 * 
	 * @param csvLine
	 * @throws Exception
	 */
	public SimpleWebcamInfo(String csvLine) {
		String[] parts = csvLine.split(separator);
		webcamId = parts[0];
		lat = Double.parseDouble(parts[1]);
		lon = Double.parseDouble(parts[2]);
		staticImageUrl = parts[3];
		webcamPageUrl = parts[4];
	}

	public void downloadAndUpdateInfo(String imageDownloadFolder) throws Exception {
		URL url = new URL(this.staticImageUrl);
		BufferedImage img = ImageIO.read(url);
		// since the image has been downloaded, compute its timestamp
		timestamp = System.currentTimeMillis();
		// the transform the timestamp to a string that will be attached to the image's id to make it unique and will
		// also be used to compute the path where the image will be saved
		String lastUpdatedDateStringForId = DateManipulation.convertTimestampToDateString(timestamp, "CET",
				DateManipulation.AMOS_DATE_FORMAT);
		imageId = webcamId + "_" + lastUpdatedDateStringForId;
		String subfolder = "webcams-extra/" + webcamId + "/" + lastUpdatedDateStringForId.substring(0, 6) + "/";
		path = subfolder + imageId + ".jpg";
		// create any non-existing subdirectories first
		new File(imageDownloadFolder + subfolder).mkdirs();
		File file = new File(imageDownloadFolder + path);
		// IMPORTANT!!! Downscale the image if it is larger than 500x500 pixels in total
		int imgSize = img.getHeight() * img.getWidth();
		if (imgSize > maxImgSize) {
			ImageScaling imscale = new ImageScaling();
			imscale.setTargetSize(maxImgSize);
			BufferedImage scaled = imscale.maxPixelsScaling(img);
			ImageIO.write(scaled, "jpg", file);
		} else {
			ImageIO.write(img, "jpg", file);
		}
	}

	public String getId() {
		return webcamId;
	}

	public void setId(String id) {
		this.webcamId = id;
	}

	public String getUrl() {
		return staticImageUrl;
	}

	public void setUrl(String url) {
		this.staticImageUrl = url;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public Document toMongoDoc() {
		Document doc = new Document();

		Document location = new Document();
		location.append("type", "Point");
		ArrayList<Double> coordinates = new ArrayList<Double>(2);
		coordinates.add(lon);
		coordinates.add(lat);
		location.append("coordinates", coordinates);
		doc.append("loc", location);

		doc.append("datetime", new Date(timestamp));
		doc.append("source_type", "webcams-extra");

		Document webcamInfo = new Document();
		webcamInfo.append("id", imageId);
		webcamInfo.append("webcam_id", webcamId);
		webcamInfo.append("webcam_url", webcamPageUrl);
		webcamInfo.append("url", WebcamsCollectionlJob.imagesBaseURL + path);
		webcamInfo.append("url-original", staticImageUrl);
		webcamInfo.append("path", path);
		doc.append("source_info", webcamInfo);

		return doc;
	}

}
