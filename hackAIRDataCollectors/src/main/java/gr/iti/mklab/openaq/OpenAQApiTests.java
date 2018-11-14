package gr.iti.mklab.openaq;

import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import gr.iti.mklab.utils.ServiceCalls;
import pojo.openaq.OpenAQResponseMeasurements;

public class OpenAQApiTests {

	public static void main(String[] args) throws Exception {

		// String openAqEndpoint = "https://api.openaq.org/v1/latest";
		// String openAqEndpoint = "https://api.openaq.org/v1/sources";
		// String openAqEndpoint = "https://api.openaq.org/v1/parameters";
		// String openAqEndpoint = "https://api.openaq.org/v1/locations?&country=DE";
		// String openAqEndpoint = "https://api.openaq.org/v1/countries";
		// String openAqEndpoint = "https://api.openaq.org/v1/cities?country=DE";

		// String openAqEndpoint = "https://api.openaq.org/v1/measurements";
		// ArrayList<String> parameters = new ArrayList<String>();
		// parameters.add("parameter[]=pm25&parameter[]=pm10");
		// long to = System.currentTimeMillis();
		// long from = to - 6 * 3600 * 1000; // last half hour;
		// String toString = DateManipulation.convertTimestampToDateString(to, "UTC",
		// DateManipulation.OPENAQ_DATE_FORMAT);
		// String fromString = DateManipulation.convertTimestampToDateString(from, "UTC",
		// DateManipulation.OPENAQ_DATE_FORMAT);
		//
		// parameters.add("date_from=" + fromString);
		// parameters.add("date_to=" + toString);
		// parameters.add("has_geo=true"); // ensures that only geolocated measurements will be returned
		// parameters.add("limit=2");
		// parameters.add("include_fields=[attribution,averagingPeriod,sourceName]");
		// // results are sorted by date descending by default
		// // I just add the parameters here in case we want to change it
		// parameters.add("order_by=date&sort=desc"); // this works!

		// for (int i = 0; i < parameters.size(); i++) {
		// if (i == 0) {// first param
		// openAqEndpoint += "?" + parameters.get(i);
		// } else {
		// openAqEndpoint += "&" + parameters.get(i);
		// }
		// }

		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, String> codeToCountry = OpenAQCollector.populateCountries();

		for (String code : codeToCountry.keySet()) {
			String openAqEndpoint = "https://api.openaq.org/v1/locations?&country=" + code;
			String responseString = ServiceCalls.makeGetRequest(openAqEndpoint);
			// Object json = mapper.readValue(responseString, Object.class);
			// System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
			OpenAQResponseMeasurements responseObj = mapper.readValue(responseString, OpenAQResponseMeasurements.class);
			int numStations = responseObj.getMeta().getFound();
			System.out.println(codeToCountry.get(code) + "," + numStations);

		}

	}

}
