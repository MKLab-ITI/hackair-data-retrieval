package gr.mklab;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import commonFunctions.ImageTransparency;
import georegression.struct.point.Point2D_I32;
import gr.mklab.classes.RatioCalculationResults;

/**
 * New, more efficient implementation based on BoofCV's blod detection implementation.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 *
 */
public class SkyLocalizationAndRatios {

	public static final float montonicityValueThresholdPct = 0.95f;
	public static final float montonicityErrorsPerLinePct = 0.95f;
	public static final float montonicityErrorsPct = 0.95f;
	public static final int numStdsExtreme = 2;

	public static final float allPixelsPct = 0.01f;
	public static final float componentPixelsPct = 0.0025f;

	public static void main(String[] args) throws Exception {

		// String imagesTestFolderPath = args[0]; // used to test remotely from a jar
		String imagesFolder = "D:/masktests/sky/";
		String masksFolder = imagesFolder;

		File dir = new File(imagesFolder);
		String[] imageNames = dir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".jpg") && !name.contains("mask");
			}
		});

		long startTotalTimeNew = System.currentTimeMillis();
		BufferedWriter out1 = new BufferedWriter(new FileWriter(new File("test-new.txt")));
		int counter = 0;
		for (String imageName : imageNames) {
			if (counter++ > 20) {
				break;
			}
			boolean[][] fcnMask = null;
			// System.out.print("Processing image: " + imageName + "\t");
			long start = System.currentTimeMillis();
			int index = imageName.lastIndexOf(".");
			String maskName = imageName.substring(0, index) + "_mask.png";
			RatioCalculationResults rcr = SkyLocalizationAndRatios.processImage(imagesFolder + imageName, fcnMask,
					masksFolder + maskName);
			out1.write(imageName + "\t" + (float) rcr.getNumSkyPixels() / rcr.getNumSkyPixelsBeforeOutlierRemoval()
					+ "\t" + (float) rcr.getNumMonotoneVerticalLines() / rcr.getNumVerticalLinesChecked() + "\t"
					+ rcr.getMeanRG() + "\t" + rcr.getMeanGB() + "\t" + (System.currentTimeMillis() - start) + "\t"
					+ rcr.getTestThatFailed() + "\n");
			rcr.printTimes();

		}
		out1.close();
		System.out.println("Total processing time: " + (System.currentTimeMillis() - startTotalTimeNew) + " ms");

		// long startTotalTime = System.currentTimeMillis();
		// BufferedWriter out = new BufferedWriter(new FileWriter(new File("test-existing.txt")));
		// int counter2 = 0;
		// for (String imageName : imageNames) {
		// if (counter2++ > 20) {
		// break;
		// }
		// boolean[][] fcnMask = null;
		// // System.out.print("Processing image: " + imageName + "\t");
		// long start = System.currentTimeMillis();
		// RatioCalculationResults rcr = SkyLocalizationAndRatiosNew.processImage(imagesFolder + imageName, fcnMask,
		// masksFolder + imageName.replace(".jpg", "-mask.jpg"));
		// out.write(imageName + "\t" + (float) rcr.getNumSkyPixels() / rcr.getNumSkyPixelsBeforeOutlierRemoval()
		// + "\t" + (float) rcr.getNumMonotoneVerticalLines() / rcr.getNumVerticalLinesChecked() + "\t"
		// + rcr.getMeanRG() + "\t" + rcr.getMeanGB() + "\t" + (System.currentTimeMillis() - start) + "\t"
		// + rcr.getTestThatFailed() + "\n");
		// rcr.printTimes();
		//
		// }
		// out.close();
		// System.out.println("Total processing time: " + (System.currentTimeMillis() - startTotalTime) + " ms");

	}

	/**
	 * Calculates (if appropriate) the R/G, G/B ratios of the image. Only the pixels selected by the fcnMask mask are
	 * considered. If the last 2 arguments are non-null, the computed mask is also written in png/jpeg or txt format
	 * respectively.
	 * 
	 * @param imagePath
	 * @param fcnMask
	 * @param outImgMaskPath
	 * @return
	 * @throws Exception
	 */
	public static RatioCalculationResults processImage(String imagePath, boolean[][] fcnMask, String outImgMaskPath)
			throws Exception {
		long startAll = System.currentTimeMillis();

		String testThatFailed = "";

		RatioCalculationResults imgR = new RatioCalculationResults();

		long start = System.currentTimeMillis();
		BufferedImage im = ImageIO.read(new File(imagePath));
		long readingTime = System.currentTimeMillis() - start;
		imgR.setReadingTime(readingTime);

		int width = im.getWidth();
		int height = im.getHeight();
		int totalPixels = width * height;
		imgR.setAllPixels(totalPixels);

		// === Steps 1, 2 of the algorithm ===
		// first iteration through image pixels just to check sky condition per pixel
		// store R/G and G/B values of all points for quick ref
		float[][] xyRG = new float[width][];
		float[][] xyGB = new float[width][];

		GrayU8 binary = new GrayU8(width, height);
		for (int x = 0; x < width; x++) {
			xyRG[x] = new float[height / 2];
			xyGB[x] = new float[height / 2];
			for (int y = 0; y < height / 2; y++) {
				// if no existing mask is given or the given mask includes that pixel then proceed to the test
				if (fcnMask == null || fcnMask[y][x]) {
					int rgb = im.getRGB(x, y);
					Color c = new Color(rgb);
					int r = c.getRed();// (rgb & 0xFF0000)>>16;
					int g = c.getGreen(); // rgb & 0x00FF00)>>8;
					int b = c.getBlue(); // rgb & 0x0000FF;

					float rg = (float) r / g;
					float gb = (float) g / b;
					float br = (float) b / r;

					xyRG[x][y] = rg;
					xyGB[x][y] = gb;

					if ((rg >= 0.5 && rg <= 1) && (gb >= 0.5 && gb <= 1) && (br > 1.25)) {
						binary.set(x, y, 1);
					}

				}
			}
		}
		// === Steps 1, 2 completed ===

		// === Step 3 (check of neighboring pixels) is skipped because it is redundant given step 4 (discovery
		// of connected components) ===

		// === Steps 4, 5, 6: discovery of connected components, discarding small components, counting sum of
		// pixels of large enough components ===
		start = System.currentTimeMillis();
		GrayS32 labeledImage = new GrayS32(binary.width, binary.height);
		// Detect blobs inside the image using an 4/8-connect rule
		List<Contour> contours = BinaryImageOps.contour(binary, ConnectRule.FOUR, labeledImage);
		int numBlobs = contours.size();
		// System.out.println("# components: " + numBlobs);
		List<List<Point2D_I32>> clusters = BinaryImageOps.labelToClusters(labeledImage, numBlobs, null);
		long conCompTime = System.currentTimeMillis() - start;
		imgR.setConCompTime(conCompTime);
		// Get number of pixels for all clusters greater that clusterSizeLimitValue
		// discard cluster that are not large enough
		float clusterSizeLimitValue = (float) totalPixels * componentPixelsPct;
		float allClusterPixelsLimitValue = (float) totalPixels * allPixelsPct;
		List<List<Point2D_I32>> bigEnoughClusters = new ArrayList<List<Point2D_I32>>();
		int sumPixelsInLarge = 0;
		for (List<Point2D_I32> cluster : clusters) {
			int clusterSize = cluster.size();
			if (clusterSize >= clusterSizeLimitValue) {
				bigEnoughClusters.add(cluster);
				sumPixelsInLarge += clusterSize;
			}
		}
		imgR.setNumSkyPixelsBeforeOutlierRemoval(sumPixelsInLarge);
		// System.out.println(bigEnoughClusters.size() + " out of " + numBlobs + " are large enough");
		// === Steps 4, 5, 6 completed ===

		// === Step 7 ===
		if (sumPixelsInLarge < allClusterPixelsLimitValue) {
			testThatFailed = "step 7, no significant sky part";
			imgR.setTestThatFailed(testThatFailed);
			long totalTime = System.currentTimeMillis() - startAll;
			imgR.setTotalTime(totalTime);
			return imgR;
		}
		// === Step 7 completed ===

		// === Step 8: removing pixels with extreme values (outliers) ===
		// Calculate for all pixels R/G and G/B ratio and meanRG and meanGB

		// Calculating mean value of RG ratio
		// remove outliers after computing mean and std in large clusters
		// === Step 8: removing pixels with extreme values (outliers) ===
		// Calculating mean value of RG ratio
		float meanRG = 0;
		for (List<Point2D_I32> cluster : bigEnoughClusters) {
			for (Point2D_I32 point : cluster) {
				float rg = xyRG[point.x][point.y];
				meanRG += rg;
			}
		}
		meanRG /= sumPixelsInLarge;

		// Calculating Standard Deviation of RG ratio
		float stdRG = 0;
		for (List<Point2D_I32> cluster : bigEnoughClusters) {
			for (Point2D_I32 point : cluster) {
				float rg = xyRG[point.x][point.y];
				float diff = rg - meanRG;
				float diffSquared = diff * diff;
				stdRG += diffSquared;
			}
		}
		stdRG = (float) Math.sqrt(stdRG / sumPixelsInLarge);

		// remove pixels with values smaller/larger than (meanRG-+4*stdRG)
		float upperThres = meanRG + numStdsExtreme * stdRG;
		float lowerThres = meanRG - numStdsExtreme * stdRG;

		int numberOfPixelsWithOutliersRemoved = 0;
		List<List<Point2D_I32>> bigEnoughClustersFiltered = new ArrayList<List<Point2D_I32>>();
		for (List<Point2D_I32> cluster : bigEnoughClusters) {
			List<Point2D_I32> clusterFiltered = new ArrayList<Point2D_I32>();
			for (Point2D_I32 point : cluster) {
				float rg = xyRG[point.x][point.y];
				if (!(rg > upperThres) || (rg < lowerThres)) { // not outlier
					clusterFiltered.add(point);
					numberOfPixelsWithOutliersRemoved++;
				}
			}
			bigEnoughClustersFiltered.add(clusterFiltered);
		}
		imgR.setNumUsableSkyPixels(numberOfPixelsWithOutliersRemoved);
		// System.out.println("# pixels no outliers: " + numberOfPixelsWithOutliersRemoved);
		// === Step 8 completed ===

		// === Step 9 ===
		if (numberOfPixelsWithOutliersRemoved < allClusterPixelsLimitValue) {
			testThatFailed = "step 9, no significant sky part (after outlier removal)";
			imgR.setTestThatFailed(testThatFailed);
			long totalTime = System.currentTimeMillis() - startAll;
			imgR.setTotalTime(totalTime);
			return imgR;
		}
		// === Step 9 completed ===

		// === Step 10: checking if monotone rise applies to biggest cluster ===
		start = System.currentTimeMillis();
		// check monotonicity
		// calculate and print the largest and smallest y for each x in the connected components
		// use a tree maps to ensure that x s are y s sorted
		TreeMap<Integer, TreeMap<Integer, Float>> xToYGBs = new TreeMap<Integer, TreeMap<Integer, Float>>();
		ArrayList<Integer> allXs = new ArrayList<Integer>();
		for (List<Point2D_I32> cluster : bigEnoughClustersFiltered) {
			for (Point2D_I32 point : cluster) {
				int x = point.x;
				int y = point.y;
				float gb = xyGB[x][y];
				TreeMap<Integer, Float> yToGBs = xToYGBs.get(x);
				if (yToGBs == null) {
					yToGBs = new TreeMap<Integer, Float>();
					allXs.add(x);
				}
				yToGBs.put(y, gb);
				xToYGBs.put(x, yToGBs);
			}
		}
		// System.out.println("Size:" + xToYGBs.size() + " " + allXs.size());

		// random shuffling is used in case we do not examine all vertical lines
		Collections.shuffle(allXs, new Random(1));
		int numMonotone = 0;
		for (int i = 0; i < allXs.size(); i++) {

			TreeMap<Integer, Float> yToGB = xToYGBs.get(allXs.get(i));
			// System.out.println("Checking vertical line " + allXs.get(i) + " - size: " + yToGB.size());

			// convert to sequence of numbers
			float[] seq = new float[yToGB.size()];
			int index = 0;
			for (Float gb : yToGB.values()) {
				seq[index] = gb;
				index++;
			}
			boolean monotone = checkIfSequenceIsCloseToMonotone(seq, montonicityValueThresholdPct,
					montonicityErrorsPerLinePct);
			if (monotone) {
				numMonotone++;
			}
		}
		imgR.setNumVerticalLinesChecked(allXs.size());
		imgR.setNumMonotoneVerticalLines(numMonotone);

		// System.out.println("Num monotone: " + numMonotone + "/ " + 20);
		long monotonicityTime = System.currentTimeMillis() - start;
		imgR.setMonotonicityTime(monotonicityTime);

		if (numMonotone < allXs.size() * montonicityErrorsPct) {
			testThatFailed = "step 10, no monotone rise in G/B ratio: " + numMonotone + "/" + allXs.size();
			imgR.setTestThatFailed(testThatFailed);
			long totalTime = System.currentTimeMillis() - startAll;
			imgR.setTotalTime(totalTime);
			return imgR;
		}
		// === Step 10 completed ===

		// === ALL TESTS PASSED! COMPUTING FINAL RATIOS! ===
		// Calculating mean value of RG ratio
		float finalRG = 0;
		float finalGB = 0;
		for (List<Point2D_I32> cluster : bigEnoughClustersFiltered) {
			for (Point2D_I32 point : cluster) {
				int x = point.x;
				int y = point.y;
				float rg = xyRG[x][y];
				float gb = xyGB[x][y];
				finalRG += rg;
				finalGB += gb;
			}
		}

		finalRG /= numberOfPixelsWithOutliersRemoved;
		finalGB /= numberOfPixelsWithOutliersRemoved;

		imgR.setMeanRG(finalRG);
		imgR.setMeanGB(finalGB);

		// if all previous steps completed successfully write the final mask
		if (outImgMaskPath != null) {
			start = System.currentTimeMillis();
			GrayU8 binaryFinal = new GrayU8(width, height);
			BinaryImageOps.clusterToBinary(bigEnoughClustersFiltered, binaryFinal);
			BufferedImage visualBinary = VisualizeBinaryData.renderBinary(binaryFinal, false, null);
			// GeneralFunctions.saveMaskToFileSimple(binaryFinal, outImgMaskPath);
			// ImageIO.write(visualBinary, "PNG", new File(outImgMaskPath));
			// transform to transparent png image
			Image imageWithTransparency = ImageTransparency.makeColorTransparent(visualBinary, Color.WHITE);
			BufferedImage transparentImage = ImageTransparency.imageToBufferedImage(imageWithTransparency);
			ImageIO.write(transparentImage, "PNG", new File(outImgMaskPath));
			long maskWritingTime = System.currentTimeMillis() - start;
			imgR.setMaskWritingTime(maskWritingTime);
		}

		long totalTime = System.currentTimeMillis() - startAll;
		imgR.setTotalTime(totalTime);

		return imgR;
	}

	public static boolean checkIfSequenceIsCloseToMonotone(float[] sq, float valuePct, float errorsPct) {
		int numOk = 0;
		for (int i = 0; i < sq.length - 1; i++) {
			if (sq[i + 1] >= valuePct * sq[i]) {
				numOk++;
			}
		}

		if (numOk >= (sq.length - 1) * errorsPct) {
			return true;
		} else {
			return false;
		}
	}

}
