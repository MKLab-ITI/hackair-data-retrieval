package pojo.webcams.travel;

import java.util.ArrayList;
import java.util.Date;

import org.bson.Document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import gr.iti.mklab.utils.DateManipulation;
import gr.iti.mklab.webcams.travel.WebcamsTravelCollectionJob;

/**
 * Inner classes should be defined as static for Jackson mapping to work!!!
 * 
 * @author Eleftherios Spyromitros-Xioufis
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebcamsTravelResponse {

	private String status;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	private Result result;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Result {

		private int offset;
		private int limit;

		private ArrayList<Webcam> webcams;

		public int getOffset() {
			return offset;
		}

		public void setOffset(int offset) {
			this.offset = offset;
		}

		public int getLimit() {
			return limit;
		}

		public void setLimit(int limit) {
			this.limit = limit;
		}

		public int getTotal() {
			return total;
		}

		public void setTotal(int total) {
			this.total = total;
		}

		public ArrayList<Webcam> getWebcams() {
			return webcams;
		}

		public void setWebcams(ArrayList<Webcam> webcams) {
			this.webcams = webcams;
		}

		private int total;

		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class Webcam {

			private String id;
			private String status;
			private String title;

			/** This fields is not in the API response but can be obtained from the webcam's web page **/
			private String sourceUrl;

			public String getSourceUrl() {
				return sourceUrl;
			}

			public void setSourceUrl(String sourceUrl) {
				this.sourceUrl = sourceUrl;
			}

			public String getId() {
				return id;
			}

			public void setId(String id) {
				this.id = id;
			}

			public String getStatus() {
				return status;
			}

			public void setStatus(String status) {
				this.status = status;
			}

			public String getTitle() {
				return title;
			}

			public void setTitle(String title) {
				this.title = title;
			}

			public Image getImage() {
				return image;
			}

			public void setImage(Image image) {
				this.image = image;
			}

			public Location getLocation() {
				return location;
			}

			public void setLocation(Location location) {
				this.location = location;
			}

			public URL getUrl() {
				return url;
			}

			public void setUrl(URL url) {
				this.url = url;
			}

			private Image image;

			private Location location;
			private URL url;

			@JsonIgnoreProperties(ignoreUnknown = true)
			public static class Image {

				public ImageDetails getCurrent() {
					return current;
				}

				public void setCurrent(ImageDetails current) {
					this.current = current;
				}

				private ImageDetails current;

				private long update;

				public long getUpdate() {
					return update;
				}

				public void setUpdate(long update) {
					this.update = update;
				}

				@JsonIgnoreProperties(ignoreUnknown = true)
				public static class ImageDetails {
					public String getPreview() {
						return preview;
					}

					public void setPreview(String preview) {
						this.preview = preview;
					}

					private String preview;
				}
			}

			@JsonIgnoreProperties(ignoreUnknown = true)
			public static class Location {
				private String city;
				private String country;

				public String getCity() {
					return city;
				}

				public void setCity(String city) {
					this.city = city;
				}

				public String getCountry() {
					return country;
				}

				public void setCountry(String country) {
					this.country = country;
				}

				public double getLatitude() {
					return latitude;
				}

				public void setLatitude(double latitude) {
					this.latitude = latitude;
				}

				public double getLongitude() {
					return longitude;
				}

				public void setLongitude(double longitude) {
					this.longitude = longitude;
				}

				public String getTimezone() {
					return timezone;
				}

				public void setTimezone(String timezone) {
					this.timezone = timezone;
				}

				private double latitude;
				private double longitude;
				private String timezone;
			}

			@JsonIgnoreProperties(ignoreUnknown = true)
			public static class URL {

				private URLDetails current;

				public URLDetails getCurrent() {
					return current;
				}

				public void setCurrent(URLDetails current) {
					this.current = current;
				}

				@JsonIgnoreProperties(ignoreUnknown = true)
				public static class URLDetails {
					private String desktop;

					public String getDesktop() {
						return desktop;
					}

					public void setDesktop(String desktop) {
						this.desktop = desktop;
					}
				}
			}

			@JsonIgnoreProperties(ignoreUnknown = true)
			public static class User {
				private String id;
				private String name;

				public String getId() {
					return id;
				}

				public void setId(String id) {
					this.id = id;
				}

				public String getName() {
					return name;
				}

				public void setName(String name) {
					this.name = name;
				}

				public String getUrl() {
					return url;
				}

				public void setUrl(String url) {
					this.url = url;
				}

				private String url;
			}

			public Document toMongoDoc() {
				Document doc = new Document();

				Document location = new Document();
				location.append("type", "Point");
				ArrayList<Double> coordinates = new ArrayList<Double>(2);
				coordinates.add(this.location.getLongitude());
				coordinates.add(this.location.getLatitude());
				location.append("coordinates", coordinates);
				doc.append("loc", location);

				long lastUpdatedTimestamp = image.getUpdate() * 1000;
				doc.append("datetime", new Date(lastUpdatedTimestamp));

				String lastUpdatedDateString = DateManipulation.convertTimestampToDateString(lastUpdatedTimestamp,
						"CET", DateManipulation.FLICKR_DATE_FORMAT);
				doc.append("date_str", lastUpdatedDateString); // not needed anymore

				doc.append("source_type", "webcams-travel");

				String lastUpdatedDateStringForId = DateManipulation.convertTimestampToDateString(lastUpdatedTimestamp,
						"UTC", DateManipulation.AMOS_DATE_FORMAT);
				String imageId = id + "_" + lastUpdatedDateStringForId;

				Document webcamInfo = new Document();
				webcamInfo.append("id", imageId);
				webcamInfo.append("webcam_id", id);
				webcamInfo.append("webcam_url", url.getCurrent().getDesktop());
				String path = "webcams/travel/" + id + "/" + lastUpdatedDateStringForId.substring(0, 6) + "/" + imageId
						+ ".jpg";
				webcamInfo.append("path", path);
				webcamInfo.append("url_original", image.getCurrent().getPreview());
				webcamInfo.append("url", WebcamsTravelCollectionJob.imagesBaseURL + path);
				doc.append("source_info", webcamInfo);

				return doc;
			}

		}
	}

}
