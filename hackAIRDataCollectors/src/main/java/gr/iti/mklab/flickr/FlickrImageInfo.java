package gr.iti.mklab.flickr;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import org.bson.Document;

import gr.iti.mklab.utils.DateManipulation;

public class FlickrImageInfo {

	private String flickrId;
	private String owner;
	private String pageUrl;
	private String imageUrl;
	private long uploadedTimestamp;
	/** We can only assume that date taken is in CET on average */
	private String dateTaken;
	private Double lat;
	private Double lon;
	private int views;
	private String tags;
	private String description;
	private Double locEstimationConf;

	public Double getLocEstimationConf() {
		return locEstimationConf;
	}

	public void setLocEstimationConf(Double locEstimationConf) {
		this.locEstimationConf = locEstimationConf;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	private String license;

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public String getLicense() {
		return license;
	}

	public void setLicense(String licence) {
		this.license = licence;
	}

	private String queryTags;

	// public String getWoeid() {
	// return woeid;
	// }
	//
	// public void setQuery(String query) {
	// this.query = query;
	// }

	public String getQueryTags() {
		return queryTags;
	}

	public void setQueryTags(String queryTags) {
		this.queryTags = queryTags;
	}

	private String username;
	private String title;
	// private String woeid;
	// private String query;

	public Document toMongoDoc() throws ParseException {
		Document observation = new Document();

		// if (!(lon == 0 && lat == 0)) {
		Document location = new Document();
		location.append("type", "Point");
		ArrayList<Double> coordinates = new ArrayList<Double>(2);
		coordinates.add(lon);
		coordinates.add(lat);
		location.append("coordinates", coordinates);
		observation.append("loc", location);
		// } else {
		// // non-geolocated image!
		// }
		// following code throws missing codec exception when trying to transformt the document into json..
		// observation.append("loc", new Point(new Position(lon, lat)));

		// convert String date to timestamp, assuming CET timezone
		long takenTimestamp = DateManipulation.convertStringDateToTimestamp(dateTaken, "CET",
				DateManipulation.FLICKR_DATE_FORMAT);
		observation.append("datetime", new Date(takenTimestamp));

		// observation.append("date_str", dateTaken.split(" ")[0]);
		observation.append("date_str", dateTaken);

		// observation.append("image_name", flickrId);

		Document flickrInfo = new Document();
		flickrInfo.append("id", flickrId);
		flickrInfo.append("path", "flickr/" + extractImagePath());
		flickrInfo.append("page_url", pageUrl);
		flickrInfo.append("image_url", imageUrl);
		flickrInfo.append("title", title);
		flickrInfo.append("views", views);
		flickrInfo.append("username", username);
		flickrInfo.append("date_uploaded", new Date(uploadedTimestamp));
		flickrInfo.append("license", license);
		flickrInfo.append("tags", tags);
		flickrInfo.append("description", description);

		if (queryTags != null) {
			observation.append("source_type", "flickr-textual");
			flickrInfo.append("query_tags", queryTags);
			flickrInfo.append("locEstimationConf", locEstimationConf);
		} else {
			observation.append("source_type", "flickr");
		}

		// flickrInfo.append("date_uploaded_str", uploadedTimestamp);
		// flickrInfo.append("woeid", woeid);
		// flickrInfo.append("query", query);

		observation.append("source_info", flickrInfo);

		// // Put date object
		// JSONObject dateAsObj = new JSONObject();
		// Date dateT = new Date(timestamp * 1000);
		// String dateStr = sdf.format(dateT);
		// dateAsObj.put("_type", "Date");
		// dateAsObj.put("iso", dateStr);
		// obj.put("datetime", dateAsObj);

		return observation;
	}

	// public String getQuery() {
	// return woeid;
	// }
	//
	// public void setWoeid(String woeid) {
	// this.woeid = woeid;
	// }

	public String getFlickrId() {
		return flickrId;
	}

	public void setFlickrId(String flickrId) {
		this.flickrId = flickrId;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getPageUrl() {
		return pageUrl;
	}

	public void setPageUrl(String pageUrl) {
		this.pageUrl = pageUrl;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public long getUploadedTimestamp() {
		return uploadedTimestamp;
	}

	public void setUploadedTimestamp(long dateUploaded) {
		this.uploadedTimestamp = dateUploaded;
	}

	public String getDateTaken() {
		return dateTaken;
	}

	public void setDateTaken(String dateTaken) {
		this.dateTaken = dateTaken;
	}

	public Double getLat() {
		return lat;
	}

	public void setLat(Double lat) {
		this.lat = lat;
	}

	public Double getLon() {
		return lon;
	}

	public void setLon(Double lon) {
		this.lon = lon;
	}

	public int getViews() {
		return views;
	}

	public void setViews(int views) {
		this.views = views;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String extractImagePath() {
		String dateStr = dateTaken.split(" ")[0];
		String imagePath = dateStr + "/" + flickrId + ".jpg";
		return imagePath;
	}

}
