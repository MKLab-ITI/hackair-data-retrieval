package gr.iti.mklab.flickr;

import java.net.URLEncoder;

public class FlickrQuery {

	private String queryType;

	public String getType() {
		return queryType;
	}

	public String getWoeId() {
		return woeId;
	}

	public void setWoeId(String woeId) {
		this.woeId = woeId;
	}

	public String[] getBbox() {
		return bbox;
	}

	public String getLat() {
		return lat;
	}

	public String getLon() {
		return lon;
	}

	public String getRadius() {
		return radius;
	}

	private String name;
	private String woeId;
	private String[] bbox;
	private String lat;
	private String lon;
	private String radius;
	private String tags;

	public String getName() {
		return name;
	}

	public FlickrQuery(String definitionLine) throws Exception {
		name = definitionLine.split(",")[0];
		queryType = definitionLine.split(",")[1];
		switch (queryType) {
		case "woeid":
			woeId = definitionLine.split(",")[2];
			break;
		case "textual":
			if (definitionLine.lastIndexOf(",") == definitionLine.length() - 1) {
				tags = "";
			} else {
				tags = definitionLine.split(",")[2];
			}
			break;
		case "centroid":
			lat = definitionLine.split(",")[2];
			lon = definitionLine.split(",")[3];
			break;
		case "bbox":
			bbox = new String[4];
			for (int i = 0; i < 4; i++) {
				bbox[i] = definitionLine.split(",")[i + 2];
			}
			break;
		default:
			throw new Exception("Wrong query type: " + queryType + " in definition line: " + definitionLine);
		}
	}

	public String toQueryString() throws Exception {
		String queryString = "";
		switch (queryType) {
		case "woeid":
			queryString += "&woe_id=" + woeId;
			break;
		case "textual":
			// queryString += "&tags=" + tags + "&tag_mode=any";
			queryString += "&text=" + URLEncoder.encode(tags, "UTF-8") + "&tag_mode=any";
			queryString += "&has_geo=0"; // we do retrieve geotagged photos in textual queries
			break;
		case "centroid":
			queryString += "&lat=" + lat + "&lon=" + lon + "&radius=" + radius;
			break;
		case "bbox":
			String bboxString = String.valueOf(bbox[0]);
			for (int i = 1; i < 4; i++) {
				bboxString += "," + bbox[i];
			}
			queryString += "&bbox=" + bboxString;
			break;
		}
		return queryString;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public String toString() {
		return name + " - " + queryType;
	}
}
