package gr.iti.mklab.webcams.travel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import gr.iti.mklab.utils.ServiceCalls;
import pojo.webcams.travel.WebcamsTravelResponse;
import pojo.webcams.travel.WebcamsTravelResponse.Result.Webcam;

public class WebcamsTravelAPITests {
	public static final int maxLimit = 2;
	public static final int maxNumResults = 1000;

	public static void main(String[] args) throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File("input/travel/webcams_index.txt")));

		int offset = 0;
		int totalResults = maxLimit;// just an initial value
		while (offset < totalResults && offset < maxNumResults) {
			String endpoint = "https://webcamstravel.p.mashape.com/webcams/list/continent=EU/orderby=popular,desc/limit="
					+ maxLimit + "," + offset + WebcamsTravelCollectionJob.webcamsTravelMaxLimit;

			System.out.println("Call: " + offset + "-" + (offset + maxLimit));
			HashMap<String, String> headerElems = new HashMap<String, String>();
			headerElems.put("X-Mashape-Key", "zsguCALa1QmshGgCzycuDlSuPmKap1D3ntyjsnlilrY4bArsMb");
			String response = ServiceCalls.makeGetRequest(endpoint, headerElems, true);

			WebcamsTravelResponse wtr = mapper.readValue(response, WebcamsTravelResponse.class);
			totalResults = wtr.getResult().getTotal();
			System.out.println("Results in this call: " + wtr.getResult().getWebcams().size());

			for (Webcam webcam : wtr.getResult().getWebcams()) {
				String id = webcam.getId();
				String status = webcam.getStatus();
				long lastUpdated = webcam.getImage().getUpdate() * 1000;
				long now = System.currentTimeMillis();
				double hours = (now - lastUpdated) / (1000.0 * 3600);
				String url = webcam.getUrl().getCurrent().getDesktop();

				bw.write(id + "\t" + webcam.getLocation().getLatitude() + "\t" + webcam.getLocation().getLongitude()
						+ "\n");
				// get the source url from webcam's page on webcams.travel
				// Document doc = Jsoup.connect(url).get();
				// String webcamSource = doc.select("a[title=Source]").get(0).attr("href");
				// System.out.println("Webcam " + id + " is " + status + " last updated: " + hours + " hours ago! url: "
				// + url + " source url: " + webcamSource);
			}

			// String responseJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(wtr);
			// System.out.println("Response original: " + responseJson);

			offset += maxLimit;
		}

		bw.close();
	}

}
