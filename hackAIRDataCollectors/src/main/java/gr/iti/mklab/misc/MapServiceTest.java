package gr.iti.mklab.misc;

import org.json.JSONArray;
import org.json.JSONObject;

import gr.iti.mklab.utils.ServiceCalls;

public class MapServiceTest {

	public static void main(String[] args) throws Exception {
		long now = System.currentTimeMillis() + 2 * 3600 * 1000;
		long oneDayAgo = System.currentTimeMillis() - 5 * 3600 * 1000;
		String place = "europe";

		String since = String.valueOf(oneDayAgo / 1000);
		String until = String.valueOf(now / 1000);
		String callback = "12345";
		String clouds = "0.0";
		String sky = "0.0";
		String sun = "0.0";
		
		String endpoint = "http://173.212.212.242:8080/FlickrCrawlREST-0.0.1-SNAPSHOT/flickrCrawl/v1/";
		String parameters = "?place=" + place + "&callback=" + callback + "&since=" + since + "&until="
				+ until + "&clouds=" + clouds + "&sky=" + sky + "&sun=" + sun;

		String response = ServiceCalls.makeGetRequest(endpoint + parameters);
		System.out.println(response);

		JSONObject jo = new JSONObject(response);
		JSONArray ja = jo.getJSONArray("flickr_images");

		System.out.println(ja.length());

	}

}
