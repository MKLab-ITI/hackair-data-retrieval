package gr.iti.mklab.misc;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Locale;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.databind.ObjectMapper;

import gr.iti.mklab.utils.ServiceCalls;
import gr.mklab.GeneralFunctions;
import gr.mklab.SkyLocalizationAndRatiosOld;
import gr.mklab.classes.RatioCalculationResults;
import pojo.requests.LocalizationRequest;
import pojo.responses.LocalizationResponse;

public class MaskGenerationFunctions {

	public static ArrayList<RatioCalculationResults> generateAllMasks(String originalImagePathThisPC,
			String relativeImagePathFCNPC, String masksExportPath, String masksFilestem,
			String localizationServiceEndpoint) throws Exception {

		ArrayList<RatioCalculationResults> results = new ArrayList<RatioCalculationResults>(2);

		LocalizationRequest locReq = new LocalizationRequest();
		locReq.addImage(relativeImagePathFCNPC);

		// convert request object to json
		ObjectMapper mapper = new ObjectMapper();
		String locReqJson = mapper.writeValueAsString(locReq);

		String responseJson = ServiceCalls.makePostRequest(localizationServiceEndpoint, locReqJson, "UTF-8");

		// convert json response to object
		LocalizationResponse locRes = mapper.readValue(responseJson, LocalizationResponse.class);
		String encodedMaskString = locRes.getImages().get(0).getMask();
		// System.out.println("Mask: " + encodedMaskString);

		boolean[][] mask = decodeMask(encodedMaskString, null);
		String fcnMaskPath = masksExportPath + masksFilestem + "-fcnmask.jpg";
		GeneralFunctions.saveMaskToFileWithBorders(originalImagePathThisPC, mask, fcnMaskPath);

		// call the method of Kourtidis on the FCN-masked image
		String kourtidisMaskPath = masksExportPath + masksFilestem + "-kourtidismask.jpg";
		RatioCalculationResults rcr1 = SkyLocalizationAndRatiosOld.processImage(originalImagePathThisPC, null,
				kourtidisMaskPath);
		results.add(rcr1);
		String testThatFailed1 = rcr1.getTestThatFailed();
		double rg1 = rcr1.getMeanRG();
		double gb1 = rcr1.getMeanGB();
		System.out.println("Test that failed when using Kourtidis mask: " + testThatFailed1);
		System.out.println("Ratios: " + rg1 + " - " + gb1);

		// call the method of Kourtidis on the FCN-masked image
		String fcnKourtidisMaskPath = masksExportPath + masksFilestem + "-fcn+kourtidismask.jpg";
		RatioCalculationResults rcr2 = SkyLocalizationAndRatiosOld.processImage(originalImagePathThisPC, mask,
				masksExportPath + masksFilestem + "-fcn+kourtidismask.jpg");
		results.add(rcr2);

		String testThatFailed2 = rcr2.getTestThatFailed();
		double rg2 = rcr2.getMeanRG();
		double gb2 = rcr2.getMeanGB();
		System.out.println("Test that failed when using FCN+Kourtidis mask: " + testThatFailed2);
		System.out.println("Ratios: " + rg2 + " - " + gb2);

		// join original image and masks into a single image
		String[] imagePaths = { originalImagePathThisPC, fcnMaskPath, kourtidisMaskPath, fcnKourtidisMaskPath };
		String[] imageCaptions = { "original", "fcn mask",
				"kourtidis mask - ratios: R/G=" + String.format(Locale.US, "%.3g", rg1) + " G/B="
						+ String.format(Locale.US, "%.3g", gb1),
				"fcn+kourtidis mask - ratios: R/G=" + String.format(Locale.US, "%.3g", rg2) + " G/B="
						+ String.format(Locale.US, "%.3g", gb2) };
		String joinedImagePath = masksExportPath + masksFilestem + "-join.jpg";
		joinImages(imagePaths, imageCaptions, joinedImagePath);

		return results;
	}

	public static RatioCalculationResults generateRatiosFCNKourtidis(String originalImagePathThisPC,
			String relativeImagePathFCNPC, String localizationServiceEndpoint) throws Exception {

		LocalizationRequest locReq = new LocalizationRequest();
		locReq.addImage(relativeImagePathFCNPC);

		// convert request object to json
		ObjectMapper mapper = new ObjectMapper();
		String locReqJson = mapper.writeValueAsString(locReq);

		String responseJson = ServiceCalls.makePostRequest(localizationServiceEndpoint, locReqJson, "UTF-8");

		// convert json response to object
		LocalizationResponse locRes = mapper.readValue(responseJson, LocalizationResponse.class);
		String encodedMaskString = locRes.getImages().get(0).getMask();
		System.out.println("Mask: " + encodedMaskString);

		boolean[][] mask = decodeMask(encodedMaskString, null);

		// call the method of Kourtidis on the FCN-masked image
		RatioCalculationResults rcr = SkyLocalizationAndRatiosOld.processImage(originalImagePathThisPC, mask, "");

		return rcr;
	}

	public static void joinImages(String[] imagePaths, String[] imageCaptions, String joinedImagePath)
			throws Exception {
		BufferedImage[] images = new BufferedImage[imagePaths.length];
		for (int i = 0; i < images.length; i++) {
			images[i] = ImageIO.read(new File(imagePaths[i]));
		}
		int horizonallMargin = 10;
		int captionheight = 20;
		int totalWidth = images[0].getWidth() * images.length + horizonallMargin * (images.length - 1);
		int totalHeight = images[0].getHeight() + captionheight;

		BufferedImage join = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB);
		Graphics g = join.getGraphics();

		int x = 0;
		for (int i = 0; i < images.length; i++) {
			g.drawImage(images[i], x, 0, null);

			g.setFont(g.getFont().deriveFont(16f));
			// int captionXPosition = (int) (x + images[0].getWidth() / 2.0);
			int captionXPosition = x + horizonallMargin;
			g.drawString(imageCaptions[i], captionXPosition, totalHeight - 2);
			// g.dispose();

			x += images[i].getWidth() + horizonallMargin;
		}

		ImageIO.write(join, "jpg", new File(joinedImagePath));
	}

	public static boolean[][] decodeMask(String encodedString, String decodedFile) throws Exception {
		boolean[][] decoded = null;
		int rowIndex = 0;
		try {
			String[] parts = encodedString.split("\\|", 2);
			String size = parts[0];
			String mask = parts[1];
			int height = Integer.parseInt(size.split("X")[0]);
			int width = Integer.parseInt(size.split("X")[1]);
			decoded = new boolean[height][];

			ArrayList<Character> rowCharsList = new ArrayList<Character>();
			for (int i = 0; i < mask.length(); i++) {
				char c = mask.charAt(i);
				if (c != 'R') { // append row
					rowCharsList.add(c);
				} else { // new row
					decoded[rowIndex] = new boolean[width];
					// decode previous row
					if (rowCharsList.size() > 0) { // previous row is not empty
						// transform arraylist of characters to String for easier manipulation
						char[] rowCharsArray = new char[rowCharsList.size()];
						for (int j = 0; j < rowCharsList.size(); j++) {
							rowCharsArray[j] = rowCharsList.get(j);
						}
						String row = String.valueOf(rowCharsArray);
						String[] segments = row.split(",");
						for (String segment : segments) {
							if (!segment.contains("-")) {
								int index = Integer.parseInt(segment);
								decoded[rowIndex][index] = true;
							} else {
								int startIndex = Integer.parseInt(segment.split("-", 2)[0]);
								int endIndex = Integer.parseInt(segment.split("-", 2)[1]);
								for (int j = startIndex; j <= endIndex; j++) {
									decoded[rowIndex][j] = true;
								}
							}
						}
						// empty previous row
						rowCharsList.clear();
					}
					rowIndex++;
				}
			}

			if (decodedFile != null) {
				BufferedWriter out = new BufferedWriter(new FileWriter(new File(decodedFile)));
				for (int i = 0; i < decoded.length; i++) {
					StringBuffer sb = new StringBuffer();
					for (int j = 0; j < decoded[i].length; j++) {
						if (decoded[i][j]) {
							sb.append("1 ");
						} else {
							sb.append("0 ");
						}
					}
					String row = sb.toString();
					out.write(row.substring(0, row.length() - 1) + "\n");
				}
				out.close();
			}
		} catch (Exception e) {
			System.out.println("Problem at row: " + (rowIndex + 1));
			throw e;
		}
		return decoded;
	}
}
