package gr.iti.mklab.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class GenerateImageMasksFromCSV {

	public static void main(String[] args) throws Exception {

		String whichFolder = "thessaloniki_2012";
		String originalImagesRootThisPC = "//160.40.51.145/Data/Images_Aris/" + whichFolder + "/";
		String masksExportPath = originalImagesRootThisPC + "masks/";
		String localizationServiceEndpoint = "http://160.40.50.236:8080/SkyLocalizationFCN/post";

		// parse the csv file and extract the require fiedl from images with usable sky masks, i.e. ratios
		String csvFilePath = "D:/espyromi/Dropbox/hackair-mine/duth data - feedback/1st batch/data_thessaloniki_2012.csv";
		BufferedReader in = new BufferedReader(new FileReader(new File(csvFilePath)));
		String line = in.readLine(); // skip header
		int allCounter = 0;
		int withRatioCounter = 0;
		while ((line = in.readLine()) != null) {
			String path = line.split(";")[0];
			String id = path.substring(11, path.length() - 4);
			String originalImagePathThisPC = originalImagesRootThisPC + path;
			String relativeImagePathFCNPC = whichFolder + "/" + path;

			int numSkyPixels = Integer.parseInt(line.split(";")[8]);
			if (numSkyPixels > 0) {
				withRatioCounter++;
				MaskGenerationFunctions.generateAllMasks(originalImagePathThisPC, relativeImagePathFCNPC, masksExportPath,
						id, localizationServiceEndpoint);
			}
			allCounter++;
		}
		System.out.println(withRatioCounter + " with ratios out of " + allCounter);
		in.close();

	}

}
