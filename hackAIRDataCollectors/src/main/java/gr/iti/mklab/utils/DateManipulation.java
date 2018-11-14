package gr.iti.mklab.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

public class DateManipulation {

	public static final String FLICKR_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	public static final String AMOS_DATE_FORMAT = "yyyyMMdd_HHmmss";
	public static final String OPENAQ_DATE_FORMAT_QUERY = "yyyy-MM-dd'T'HH:mm:ss";
	public static final String OPENAQ_DATE_FORMAT_RESPONSE = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	public static final String LUFTDATEN_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	public static String convertTimestampToDateString(long timeStamp, String zoneId, String DATE_FORMAT) {
		Date dateUploadedDate = new Date(timeStamp);

		DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		TimeZone gmtTime = TimeZone.getTimeZone(zoneId);
		dateFormat.setTimeZone(gmtTime);
		String dateUploadedStringGMT = dateFormat.format(dateUploadedDate);

		return dateUploadedStringGMT;
	}

	public static long convertStringDateToTimestamp(String dateString, String zoneId, String DATE_FORMAT) {
		if (dateString.contains("Z")) {
			// see https://www.mkyong.com/java/how-to-convert-string-to-date-java/
			dateString = dateString.replaceAll("Z$", "+0000");
		}
		LocalDateTime ldt = LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern(DATE_FORMAT));
		ZonedDateTime GMTZonedDateTime = ldt.atZone(ZoneId.of(zoneId));
		long timestamp = GMTZonedDateTime.toInstant().toEpochMilli();
		return timestamp;
	}

	public static void main(String args[]) {
		String date = "20170307_133700";
		String AMOS_DATE_FORMAT = "yyyyMMdd_HHmmss";

		long timestamp = convertStringDateToTimestamp(date, "CET", AMOS_DATE_FORMAT);

		String dateNew = convertTimestampToDateString(timestamp, "UTC", AMOS_DATE_FORMAT);
		System.out.println("Date new (UTC): " + dateNew);
		dateNew = convertTimestampToDateString(timestamp, "CET", AMOS_DATE_FORMAT);
		System.out.println("Date new (CET): " + dateNew);

		long now = System.currentTimeMillis();
		System.out.println("Difference: " + (now - timestamp) / (double) (1000 * 60 * 60));
	}

}
