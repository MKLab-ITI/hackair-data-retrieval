package pojo.mongo;

import java.util.Calendar;

import gr.iti.mklab.utils.DateManipulation;

public class HackairRecordFlickrWithIA extends HackairRecordBase {

	private SourceInfoFlickrWithIA source_info;

	public HackairRecordFlickrWithIA() {
		super("flickr");
	}

	public String getPath() {
		return source_info.getPath();
	}

	public String getId() {
		return source_info.getId();
	}

	private String getFieldValue(String fieldName) throws Exception {
		Calendar calendar = Calendar.getInstance();
		switch (fieldName) {
		case "id":
			return source_info.getId();
		case "name":
			return source_info.getPath();
		case "latitude":
			// return String.format(Locale.US, "%.8g", loc.getLatitude());
			return String.valueOf(loc.getLatitude());
		case "longitude":
			return String.valueOf(loc.getLongitude());
		case "datetime":
			return DateManipulation.convertTimestampToDateString(datetime.getTime(), "UTC",
					DateManipulation.FLICKR_DATE_FORMAT);
		case "year":
			calendar = Calendar.getInstance();
			calendar.setTime(datetime);
			int year = calendar.get(Calendar.YEAR);
			return String.valueOf(year);
		case "dayofmonth":
			calendar = Calendar.getInstance();
			calendar.setTime(datetime);
			int dayofmonth = calendar.get(Calendar.DAY_OF_MONTH);
			return String.valueOf(dayofmonth);
		case "dayofweek":
			calendar = Calendar.getInstance();
			calendar.setTime(datetime);
			int dayofweek = calendar.get(Calendar.DAY_OF_WEEK);
			return String.valueOf(dayofweek);
		case "hour":
			calendar = Calendar.getInstance();
			calendar.setTime(datetime);
			int hour = calendar.get(Calendar.HOUR_OF_DAY);
			return String.valueOf(hour);
		case "minute":
			calendar = Calendar.getInstance();
			calendar.setTime(datetime);
			int minute = calendar.get(Calendar.MINUTE);
			return String.valueOf(minute);
		case "dayofyear":
			calendar = Calendar.getInstance();
			calendar.setTime(datetime);
			int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
			return String.valueOf(dayOfYear);
		case "month":
			calendar = Calendar.getInstance();
			calendar.setTime(datetime);
			int month = calendar.get(Calendar.MONTH) + 1;
			return String.valueOf(month);
		case "R/G":
			return String.valueOf(source_info.getIa().getRG());
		case "G/B":
			return String.valueOf(source_info.getIa().getGB());
		case "sky_pixels":
			return String.valueOf(source_info.getIa().getSky_pixels());
		case "all_pixels":
			return String.valueOf(source_info.getIa().getAll_pixels());
		case "sky":
			return String.valueOf(source_info.getIa().getConcepts().getSky());
		case "clouds":
			return String.valueOf(source_info.getIa().getConcepts().getClouds());
		case "sun":
			return String.valueOf(source_info.getIa().getConcepts().getSun());
		case "title":
			return source_info.getTitle();
		case "tags":
			return source_info.getTags();
		case "description":
			return source_info.getDescription().replaceAll("\n", " ");
		case "containsSky":
			return String.valueOf(source_info.getIa().isContainsSky());
		case "usableSky":
			return String.valueOf(source_info.getIa().isUsableSky());
		case "webcam_id":
			return String.valueOf(source_info.getWebcam_id());
		case "url":
			return source_info.getUrl();
		case "source_type":
			return source_type;
		default:
			throw new Exception("No mapping found for field: " + fieldName);
		}

	}

	public SourceInfoFlickrWithIA getSource_info() {
		return source_info;
	}

	public void setSource_info(SourceInfoFlickrWithIA source_info) {
		this.source_info = source_info;
	}

	public String toCSV(String[] fields, String separator) throws Exception {
		StringBuilder sb = new StringBuilder();
		for (String field : fields) {
			sb.append(getFieldValue(field) + separator);
		}
		return sb.toString();
	}

}
