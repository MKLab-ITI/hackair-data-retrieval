package gr.iti.mklab.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;

public class DuthDataParsing {

	public static void main(String[] args) throws Exception {
		String dataFolder = "D:/espyromi/Dropbox/hackair-mine/duth data - feedback/1st batch/";
		HashMap<String, String> modisTerraMap = new HashMap<String, String>();
		HashMap<String, String> modisAquaMap = new HashMap<String, String>();
		HashMap<String, String> aeronetMap = new HashMap<String, String>();
		HashMap<String, String> predictedMap = new HashMap<String, String>();

		String duthDataFileTerra = dataFolder + "data_thessaloniki_2010_2012_with_aq_estimation_comparison_TERRA.dat";
		String duthDataFileAqua = dataFolder + "data_thessaloniki_2010_2012_with_aq_estimation_comparison_AQUA.dat";

		String line;
		BufferedReader in = new BufferedReader(new FileReader(new File(duthDataFileTerra)));
		while ((line = in.readLine()) != null) {
			String path = line.split("\\s+")[0].split("\\.")[0] + ".jpg";
			String aeronet = line.split("\\s+")[7];
			String modis = line.split("\\s+")[8];
			String predicted = line.split("\\s+")[11];
			modisTerraMap.put(path, modis);
			if (aeronetMap.get(path) != null && !aeronetMap.get(path).equals(aeronet)) {
				throw new Exception("Unexpected 1!");
			}
			aeronetMap.put(path, aeronet);

			if (predictedMap.get(path) != null && !predictedMap.get(path).equals(predicted)) {
				throw new Exception("Unexpected 2!");
			}
			predictedMap.put(path, predicted);
		}
		in.close();

		in = new BufferedReader(new FileReader(new File(duthDataFileAqua)));
		while ((line = in.readLine()) != null) {
			String path = line.split("\\s+")[0].split("\\.")[0] + ".jpg";
			String aeronet = line.split("\\s+")[7];
			String modis = line.split("\\s+")[8];
			String predicted = line.split("\\s+")[11];
			modisAquaMap.put(path, modis);
			if (aeronetMap.get(path) != null && !aeronetMap.get(path).equals(aeronet)) {
				throw new Exception("Unexpected 1!");
			}
			aeronetMap.put(path, aeronet);

			if (predictedMap.get(path) != null && !predictedMap.get(path).equals(predicted)) {
				throw new Exception("Unexpected 2!");
			}
			predictedMap.put(path, predicted);
		}
		in.close();

		System.out.println("Distinct in AERONET: " + aeronetMap.size());
		System.out.println("Distinct in TERRA: " + modisTerraMap.size());
		System.out.println("Distinct in AQUA: " + modisAquaMap.size());
		System.out.println("Distinct in PREDICTED: " + predictedMap.size());

		// parse the initial files that we sent to DUTH and append the information that they provided

		String originalRatiosFile = "D:/espyromi/Dropbox/hackair-mine/duth data - feedback/1st batch/data_thessaloniki_2012.csv";
		String appendedRatiosFile = originalRatiosFile.replace(".csv", "_withgt.csv");
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(appendedRatiosFile)));

		in = new BufferedReader(new FileReader(new File(originalRatiosFile)));
		while ((line = in.readLine()) != null) {
			String path = line.split(";")[0];
			String aeronet = aeronetMap.get(path);
			String terra = modisTerraMap.get(path);
			String aqua = modisAquaMap.get(path);
			String predicted = predictedMap.get(path);

			line += ";" + aeronet + ";" + terra + ";" + aqua + ";" + predicted;
			out.write(line + "\n");
		}
		in.close();

		out.close();

	}

}
