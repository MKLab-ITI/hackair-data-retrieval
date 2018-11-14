package pojo.luftdaten;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import gr.iti.mklab.utils.DateManipulation;
import pojo.mongo.HackairRecordBase.Location;
import pojo.mongo.HackairRecordSensors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LuftDatenData {

	private long id;
	private String sampling_rate;
	private String timestamp;

	private LuftDatenLocation location;
	private LuftDatenSensor sensor;
	private ArrayList<LuftDatenSensorDataValue> sensordatavalues;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class LuftDatenLocation {

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

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

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}

		private long id;
		private String latitude;
		private String longitude;
		private String country;

	};

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class LuftDatenSensor {

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getPin() {
			return pin;
		}

		public void setPin(String pin) {
			this.pin = pin;
		}

		public SensorType getSensor_type() {
			return sensor_type;
		}

		public void setSensor_type(SensorType sensor_type) {
			this.sensor_type = sensor_type;
		}

		private long id;
		private String pin;
		private SensorType sensor_type;

		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class SensorType {
			private long id;
			private String name;

			public long getId() {
				return id;
			}

			public void setId(long id) {
				this.id = id;
			}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public String getManufacturer() {
				return manufacturer;
			}

			public void setManufacturer(String manufacturer) {
				this.manufacturer = manufacturer;
			}

			private String manufacturer;
		};

	};

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class LuftDatenSensorDataValue {

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getValue_type() {
			return value_type;
		}

		public void setValue_type(String value_type) {
			this.value_type = value_type;
		}

		private long id;
		private String value;
		private String value_type;

	};

	public HackairRecordSensors toHackairRecordSensors(String pollutant) throws Exception {

		if (!"pm10".equals(pollutant) && !"pm25".equals(pollutant)) {
			throw new Exception("Wrong pollutant parameter, use pm10 or pm25!");
		}

		if (!isNumeric(location.longitude) || !isNumeric(location.latitude)) {
			System.err.println("Either longitude or latitude is wrong!");
			return null;
		}

		String pollutantToSearch = "pm10".equals(pollutant) ? "P1" : "P2";

		for (LuftDatenSensorDataValue sdv : sensordatavalues) {
			if (pollutantToSearch.equals(sdv.value_type)) {
				// create a new mongo record
				// TODO: check the timestamp conversion!
				String StringTimestamp = timestamp;
				long timestamp = DateManipulation.convertStringDateToTimestamp(StringTimestamp, "UTC",
						DateManipulation.LUFTDATEN_DATE_FORMAT);

				HackairRecordSensors rec = new HackairRecordSensors("luftdaten", location.country,
						String.valueOf(sensor.id), pollutant, timestamp);
				rec.setLoc(new Location("Point", Double.parseDouble(location.longitude),
						Double.parseDouble(location.latitude)));
				// hrec.setCity(city);
				rec.setValue(Double.parseDouble(sdv.value));
				// hrec.setUnit(measurements[0].getUnit());
				rec.setSourceName(sensor.sensor_type.name);

				return rec;
			}
		}

		return null;

	}

	public ArrayList<HackairRecordSensors> toHackairRecordsSensors() throws Exception {

		ArrayList<HackairRecordSensors> records = new ArrayList<HackairRecordSensors>();

		for (LuftDatenSensorDataValue sdv : sensordatavalues) {
			// check if we are interested in any of the sensor values
			String pollutant = null;
			if ("P1".equals(sdv.value_type)) {
				pollutant = "pm10";
			} else if ("P2".equals(sdv.value_type)) {
				pollutant = "pm25";
			} else {
				// not interested in this value_type
				continue;
			}

			// create a new mongo record
			// TODO: check the timestamp conversion!
			String StringTimestamp = timestamp;
			long timestamp = DateManipulation.convertStringDateToTimestamp(StringTimestamp, "UTC",
					DateManipulation.LUFTDATEN_DATE_FORMAT);
			HackairRecordSensors rec = new HackairRecordSensors("luftdaten", location.country,
					String.valueOf(location.id), pollutant, timestamp);
			rec.setLoc(new Location("Point", Double.parseDouble(location.longitude),
					Double.parseDouble(location.latitude)));
			// hrec.setCity(city);
			rec.setValue(Double.parseDouble(sdv.value));
			// hrec.setUnit(measurements[0].getUnit());
			rec.setSourceName(sensor.sensor_type.name);

			records.add(rec);
		}

		return records;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getSampling_rate() {
		return sampling_rate;
	}

	public void setSampling_rate(String sampling_rate) {
		this.sampling_rate = sampling_rate;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public LuftDatenLocation getLocation() {
		return location;
	}

	public void setLocation(LuftDatenLocation location) {
		this.location = location;
	}

	public LuftDatenSensor getSensor() {
		return sensor;
	}

	public void setSensor(LuftDatenSensor sensor) {
		this.sensor = sensor;
	}

	public ArrayList<LuftDatenSensorDataValue> getSensordatavalues() {
		return sensordatavalues;
	}

	public void setSensordatavalues(ArrayList<LuftDatenSensorDataValue> sensordatavalues) {
		this.sensordatavalues = sensordatavalues;
	}

	public static boolean isNumeric(String str) {
		try {
			double d = Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

}
