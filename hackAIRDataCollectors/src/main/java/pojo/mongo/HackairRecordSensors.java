package pojo.mongo;

import java.util.Date;

public class HackairRecordSensors extends HackairRecordBase {

	private String pollutant;
	private double value;
	private String unit;

	public String getUnit() {
		return unit;
	}

	private String id;
	private String countryCode;
	private String city;
	private String location;
	private String sourceName;

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public HackairRecordSensors(String source, String countryCode, String location, String pollutant, long timeStamp) {
		super(source);
		this.countryCode = countryCode;
		this.location = location;
		this.datetime = new Date(timeStamp);
		this.pollutant = pollutant;
		this.id = countryCode + "_" + location + "_" + pollutant + "_" + timeStamp;
	}

	public String getId() {
		return id;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getPollutant() {
		return pollutant;
	}

	public void setPollutant(String pollutant) throws Exception {
		if (!pollutant.equals("pm10") && !pollutant.equals("pm25")) {
			throw new Exception("Wrong pollutant: " + pollutant);
		}

		this.pollutant = pollutant;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

}
