package pojo.openaq;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import gr.iti.mklab.utils.DateManipulation;
import pojo.mongo.HackairRecordBase.Location;
import pojo.mongo.HackairRecordSensors;

public class OpenAQResponseLatest {

	private Meta meta;
	private ArrayList<Result> results;

	public Meta getMeta() {
		return meta;
	}

	public void setMeta(Meta meta) {
		this.meta = meta;
	}

	public ArrayList<Result> getResults() {
		return results;
	}

	public void setResults(ArrayList<Result> results) {
		this.results = results;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Result {

		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class Measurement {
			private String parameter;
			private double value;

			public String getParameter() {
				return parameter;
			}

			public void setParameter(String parameter) {
				this.parameter = parameter;
			}

			public double getValue() {
				return value;
			}

			public void setValue(double value) {
				this.value = value;
			}

			public String getUnit() {
				return unit;
			}

			public void setUnit(String unit) {
				this.unit = unit;
			}

			public String getLastUpdated() {
				return lastUpdated;
			}

			public void setLastUpdate(String lastUpdate) {
				this.lastUpdated = lastUpdate;
			}

			public String getSourceName() {
				return sourceName;
			}

			public void setSourceName(String sourceName) {
				this.sourceName = sourceName;
			}

			private String unit;
			private String lastUpdated;
			private String sourceName;
		}

		private Measurement[] measurements;

		public Measurement[] getMeasurements() {
			return measurements;
		}

		public void setMeasurements(Measurement[] measurements) {
			this.measurements = measurements;
		}

		private String location;
		private Coordinates coordinates;
		private String country;
		private String city;

		public HackairRecordSensors toHackairRecordSensors() throws Exception {
			String dateStringUTC = measurements[0].getLastUpdated();
			long timestamp = DateManipulation.convertStringDateToTimestamp(dateStringUTC, "UTC",
					DateManipulation.OPENAQ_DATE_FORMAT_RESPONSE);
			HackairRecordSensors hrec = new HackairRecordSensors("openaq", country, location,
					measurements[0].getParameter(), timestamp);
			hrec.setLoc(new Location("Point", Double.parseDouble(coordinates.getLongitude()),
					Double.parseDouble(coordinates.getLatitude())));
			hrec.setCity(city);
			hrec.setValue(measurements[0].getValue());
			hrec.setUnit(measurements[0].getUnit());
			hrec.setSourceName(measurements[0].getSourceName());
			return hrec;
		}

		public String getLocation() {
			return location;
		}

		public void setLocation(String location) {
			this.location = location;
		}

		public Coordinates getCoordinates() {
			return coordinates;
		}

		public void setCoordinates(Coordinates coordinates) {
			this.coordinates = coordinates;
		}

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public class Coordinates {
			public String getLatitude() {
				return latitude;
			}

			public void setLatitude(String latitude) {
				this.latitude = latitude;
			}

			public String getLongitude() {
				return longitude;
			}

			public void setLongitude(String longitude) {
				this.longitude = longitude;
			}

			private String latitude;
			private String longitude;
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public class Date {
			public String getUtc() {
				return utc;
			}

			public void setUtc(String utc) {
				this.utc = utc;
			}

			public String getLocal() {
				return local;
			}

			public void setLocal(String local) {
				this.local = local;
			}

			private String utc;
			private String local;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Meta {
		private int page;
		private int limit;
		private int found;

		public int getPage() {
			return page;
		}

		public void setPage(int page) {
			this.page = page;
		}

		public int getLimit() {
			return limit;
		}

		public void setLimit(int limit) {
			this.limit = limit;
		}

		public int getFound() {
			return found;
		}

		public void setFound(int found) {
			this.found = found;
		}
	};
}
