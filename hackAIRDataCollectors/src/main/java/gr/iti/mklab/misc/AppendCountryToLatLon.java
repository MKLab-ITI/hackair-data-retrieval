package gr.iti.mklab.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import gr.iti.mklab.geonames.ReverseGeocoder;

public class AppendCountryToLatLon {
	public static void main(String args[]) throws Exception {
		String indexType = "travel"; // "AMOS" or "travel"
		// load reverse geocoder!
		String dir = "D:/lefteris/geolocation";
		String citiesFile = dir + "/cities1000.txt";
		String countryInfoFile = dir + "/countryInfo.txt";

		String latLonFile;
		if (indexType.equals("AMOS")) {
			latLonFile = "input/AMOS/webcams_index.txt";
		} else {
			latLonFile = "input/travel/webcams_index.txt";
		}
		ReverseGeocoder rgeoService = new ReverseGeocoder(citiesFile, countryInfoFile);
		BufferedReader br = new BufferedReader(new FileReader(new File(latLonFile)));
		BufferedWriter bw = new BufferedWriter(
				new FileWriter(new File(latLonFile.replace(".txt", "_withcountries.txt"))));

		String line;
		while ((line = br.readLine()) != null) {
			String id;
			double lat, lon;
			if (indexType.equals("AMOS")) {
				id = line.split(";")[0];
				lat = Double.parseDouble(line.split(";")[2].split(",")[0]);
				lon = Double.parseDouble(line.split(";")[2].split(",")[1]);

			} else {
				id = line.split("\t")[0];
				lat = Double.parseDouble(line.split("\t")[1]);
				lon = Double.parseDouble(line.split("\t")[2]);
			}
			String[] cityConuntryContinent = rgeoService.getCityAndCountryAndContinentyByLatLon(lat, lon);
			bw.write(id + "\t" + lat + "\t" + lon + "\t" + cityConuntryContinent[0] + "\t" + cityConuntryContinent[1]
					+ "\t" + cityConuntryContinent[2] + "\n");
		}
		bw.close();
		br.close();

	}
}
