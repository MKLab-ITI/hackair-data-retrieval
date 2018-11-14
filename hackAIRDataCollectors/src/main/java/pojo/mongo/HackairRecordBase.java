package pojo.mongo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class HackairRecordBase {
	/** The timezone to use when converting Date to String */
	public static final String timezone = "Europe/Athens";
	protected Date datetime;
	protected Location loc;

	public HackairRecordBase() {

	}

	public HackairRecordBase(String source_type) {
		this.source_type = source_type;
	}

	public static class Location {
		private String type;
		private double[] coordinates;

		public Location() {

		}

		public Location(String type, double longitude, double latitude) {
			this.type = type;
			coordinates = new double[2];
			coordinates[0] = longitude;
			coordinates[1] = latitude;
		}

		@JsonIgnore
		public double getLongitude() {
			return coordinates[0];
		}

		@JsonIgnore
		public double getLatitude() {
			return coordinates[1];
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public double[] getCoordinates() {
			return coordinates;
		}

		public void setCoordinates(double[] coordinates) {
			this.coordinates = coordinates;
		}

	}

	protected String source_type;

	public MongoDate getDatetime() {
		MongoDate md = new MongoDate(datetime);
		return md;
	}

	public Location getLoc() {
		return loc;
	}

	public String getSource_type() {
		return source_type;
	}

	public void setDatetime(Date datetime) {
		this.datetime = datetime;
	}

	public void setLoc(Location loc) {
		this.loc = loc;
	}

	public void setSource_type(String source_type) {
		this.source_type = source_type;
	}

}
