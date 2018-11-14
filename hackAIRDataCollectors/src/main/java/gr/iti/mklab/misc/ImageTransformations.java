package gr.iti.mklab.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;

import gr.mklab.classes.RatioCalculationResults;

public class ImageTransformations {
	public static String localizationServiceEndpoint = "http://160.40.50.236:8080/SkyLocalizationFCN/post";

	public static void main(String[] args) throws Exception {
		List<Transformation> trasformations = new ArrayList<Transformation>();
		// resize, keeping aspect ratio!
		trasformations.add(new Transformation().width(400).crop("scale"));
		trasformations.add(new Transformation().width(300).crop("scale"));
		trasformations.add(new Transformation().width(200).crop("scale"));
		trasformations.add(new Transformation().width(100).crop("scale"));
		// Color balance and level effects - brightness (default: 80)
		trasformations.add(new Transformation().effect("brightness:20"));
		trasformations.add(new Transformation().effect("brightness:50"));
		trasformations.add(new Transformation().effect("brightness:80"));
		// Color balance and level effects - saturation (default: 80)
		trasformations.add(new Transformation().effect("saturation:20"));
		trasformations.add(new Transformation().effect("saturation:50"));
		trasformations.add(new Transformation().effect("saturation:80"));
		// Color balance and level effects - sepia
		trasformations.add(new Transformation().effect("sepia"));
		// improvement effects
		trasformations.add(new Transformation().effect("improve"));
		trasformations.add(new Transformation().effect("gamma"));
		trasformations.add(new Transformation().effect("auto_brightness"));
		trasformations.add(new Transformation().effect("auto_contrast"));
		trasformations.add(new Transformation().effect("auto_color"));
		// artistic filters
		trasformations.add(new Transformation().effect("art:al_dente"));
		trasformations.add(new Transformation().effect("art:eucalyptus"));
		trasformations.add(new Transformation().effect("art:incognito"));
		trasformations.add(new Transformation().effect("art:red_rock"));
		trasformations.add(new Transformation().effect("art:zorro"));
		trasformations.add(new Transformation().effect("art:primavera"));
		trasformations.add(new Transformation().effect("art:athena"));
		trasformations.add(new Transformation().effect("art:aurora"));

		String originalImagesRootFolder = "Z:/thessaloniki_2012/";
		String transformedImagesRootFolder = "Z:/image_transformations/";
		Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap("cloud_name", "lefman", "api_key", "197395863245799",
				"api_secret", "Y5B0TP4y8PX3OsUH3rxC9WzoNZE"));

		// parse the csv file with the resuls sent by Aris and find all images with perfect predictions
		// submit each image to cloudinary to get transformed versions
		String totalAgreementsFile = "D:/espyromi/Dropbox/hackair-mine/duth data - feedback/1st batch/total_agreements.txt";

		int lineToStart = 86;
		BufferedWriter out = new BufferedWriter(
				new FileWriter(new File(totalAgreementsFile.replace(".txt", "_" + lineToStart + "_tr.txt"))));
		BufferedReader in = new BufferedReader(new FileReader(new File(totalAgreementsFile)));
		in.readLine();// skip header
		String line;
		int lineCounter = 0;
		while ((line = in.readLine()) != null) {
			lineCounter++;
			if (lineCounter < lineToStart) {
				continue;
			}
			String relativeImagePath = line.split("\\s+")[0];
			double rg_original = Double.parseDouble(line.split("\\s+")[1]);
			double bg_original = Double.parseDouble(line.split("\\s+")[2]);

			String imageId = relativeImagePath.substring(11, relativeImagePath.length() - 4);
			String imageFilePath = originalImagesRootFolder + relativeImagePath;
			File toUpload = new File(imageFilePath);

			Map options = ObjectUtils.asMap("eager", trasformations, "public_id", imageId);

			Map uploadResult = cloudinary.uploader().upload(toUpload, options);
			System.out.println(uploadResult.toString());

			new File(transformedImagesRootFolder + imageId).mkdir();

			String originalImageURL = (String) uploadResult.get("url");
			System.out.println("original: " + originalImageURL);
			// copy original image as well
			String imagePathThisPC = transformedImagesRootFolder + imageId + "/" + imageId + "_original.jpg";
			String relativeImagePathFCNPC = "image_transformations/" + imageId + "/" + imageId + "_original.jpg";
			FileUtils.copyURLToFile(new URL(originalImageURL), new File(imagePathThisPC));
			Thread.sleep(500); // wait for copy before processing
			RatioCalculationResults rcr = MaskGenerationFunctions.generateRatiosFCNKourtidis(imagePathThisPC,
					relativeImagePathFCNPC, localizationServiceEndpoint);
			// compare with initial ratios, should be the same
			String problem = "";
			if (Math.abs(rg_original - rcr.getMeanRG()) > 0.02 || Math.abs(bg_original - rcr.getMeanGB()) > 0.02) {
				problem = "-problem";
				System.err.println("problem");
				// throw new Exception("Problem?");
			}
			String trLine = relativeImagePath + ";original;" + rg_original + problem + ";" + bg_original + problem
					+ "\n";
			out.write(trLine);
			out.flush();

			ArrayList<Map> eagerTransformation = (ArrayList) uploadResult.get("eager");

			// first copy the images
			for (Map transformation : eagerTransformation) {
				String trasformationName = ((String) transformation.get("transformation")).replaceAll(":", "_");
				String trasformedURL = (String) transformation.get("url");
				System.out.println(trasformationName + ": " + trasformedURL);
				String imagePathThisPCtr = transformedImagesRootFolder + imageId + "/" + imageId + "_"
						+ trasformationName + ".jpg";
				FileUtils.copyURLToFile(new URL(trasformedURL), new File(imagePathThisPCtr));
			}

			Thread.sleep(500); // wait for copy before processing

			for (Map transformation : eagerTransformation) {
				String trasformationName = ((String) transformation.get("transformation")).replaceAll(":", "_");
				String trasformedURL = (String) transformation.get("url");
				System.out.println(trasformationName + ": " + trasformedURL);
				String imagePathThisPCtr = transformedImagesRootFolder + imageId + "/" + imageId + "_"
						+ trasformationName + ".jpg";
				String relativeImagePathFCNPCtr = "image_transformations/" + imageId + "/" + imageId + "_"
						+ trasformationName + ".jpg";

				RatioCalculationResults rcrtr = MaskGenerationFunctions.generateRatiosFCNKourtidis(imagePathThisPCtr,
						relativeImagePathFCNPCtr, localizationServiceEndpoint);

				trLine = relativeImagePath + ";" + trasformationName + ";" + rcrtr.getMeanRG() + ";" + rcrtr.getMeanGB()
						+ "\n";
				out.write(trLine);
				out.flush();
			}
		}

		in.close();
		out.close();
	}

}
