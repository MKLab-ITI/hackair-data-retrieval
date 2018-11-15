/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 * 
 * 
 * URL: https://courses.cs.washington.edu/courses/cse576/02au/homework/hw3/ConnectComponent.java
 */
package gr.mklab;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;

import gr.mklab.classes.RatioCalculationResults;
import graph.washington.CCClass;
import graph.washington.ConnectComponent;

public class SkyLocalizationAndRatiosOld {

	public static void main(String[] args) throws Exception {

		// String imagesTestFolderPath = args[0]; // used to test remotely from a jar
		String imagesFolder = "D:/masktests/sky";
		String masksFolder = imagesFolder;

		File dir = new File(imagesFolder);
		String[] imageNames = dir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".jpg") && !name.contains("mask");
			}
		});

		long startTotalTime = System.currentTimeMillis();
		int counter = 0;
		for (String imageName : imageNames) {
			if (counter++ > 10) {
				break;
			}
			boolean[][] fcnMask = null;
			System.out.print("Processing image: " + imageName + "\t");
			long start = System.currentTimeMillis();
			RatioCalculationResults rcr = SkyLocalizationAndRatiosOld.processImage(imagesFolder + imageName, fcnMask,
					masksFolder + imageName.replace(".jpg", "-mask.jpg"));
			System.out.println(" completed in " + (System.currentTimeMillis() - start) + " ms");
			System.out.println("Result: " + rcr);

		}
		System.out.println("Total processing time: " + (System.currentTimeMillis() - startTotalTime) + " ms");

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
		BufferedImage bimg = ImageIO.read(new File(imagePath));
		long readingTime = System.currentTimeMillis() - start;
		imgR.setReadingTime(readingTime);

		int H = bimg.getHeight();
		int W = bimg.getWidth();
		int totalPixels = H * W;
		imgR.setAllPixels(totalPixels);
		float clusterSizeLimitValue = (float) totalPixels / 400;
		float allClusterPixelsLimitValue = (float) totalPixels / 100;

		// === Steps 1, 2 of the algorithm ===
		// first iteration through image pixels just to check sky condition per pixel
		// int cccv = 1;
		int[] map = createEmptyLineMap(W, H);
		for (int colIndex = 0; colIndex < W; colIndex++) {
			for (int rowIndex = 0; rowIndex < H / 2; rowIndex++) {
				int rgb = bimg.getRGB(colIndex, rowIndex);
				Color c = new Color(rgb);
				int r = c.getRed();// (rgb & 0xFF0000)>>16;
				int g = c.getGreen(); // rgb & 0x00FF00)>>8;
				int b = c.getBlue(); // rgb & 0x0000FF;

				boolean fcnMaskResult = true;
				if (fcnMask != null) {
					fcnMaskResult = fcnMask[rowIndex][colIndex];
				}
				if (fcnMaskResult && isSkyPixelCandidate(r, g, b)) {
					map[rowIndex * W + colIndex] = 3;
					// cccv = cccv + 1;
				} else {
					map[rowIndex * W + colIndex] = 0;
				}
			}
		}
		// === Steps 1, 2 completed ===

		// === Step 3 (check of neighboring pixels) is skipped because it is redundant given step 4 (discovery
		// of connected components) ===

		// === Steps 4, 5, 6: discovery of connected components, discarding small components, counting sum of
		// pixels of large enough components ===
		start = System.currentTimeMillis();
		ConnectComponent cc = new ConnectComponent();
		Dimension dimension = new Dimension(W, H);
		List<CCClass> ccc = cc.labeling(bimg, map, dimension, true);
		int numComponents = ccc.size();
		// System.out.println("# components: " + numComponents);
		long conCompTime = System.currentTimeMillis() - start;
		imgR.setConCompTime(conCompTime);
		// Get number of pixels for all clusters greater that clusterSizeLimitValue
		int totalNumPixelsInLargeClusters = 0;
		Iterator<CCClass> itr = ccc.iterator();
		while (itr.hasNext()) {
			CCClass ccI = itr.next();
			if (ccI.getXY().size() < clusterSizeLimitValue) {
				itr.remove();
			} else {
				totalNumPixelsInLargeClusters = totalNumPixelsInLargeClusters + ccI.getXY().size();
			}
		}
		imgR.setNumSkyPixelsBeforeOutlierRemoval(totalNumPixelsInLargeClusters);
		// === Steps 4, 5, 6 completed ===

		// System.out.println(ccc.size() + " out of " + numComponents + " are large enough");

		// === Step 7 ===
		if (totalNumPixelsInLargeClusters < allClusterPixelsLimitValue) {
			testThatFailed = "step 7, no significant sky part";
			imgR.setTestThatFailed(testThatFailed);
			// if (outImgMaskPath != null) { // generate the mask nevertheless if asked
			// start = System.currentTimeMillis();
			// boolean[][] mask = GeneralFunctions.getMask(W, H, ccc);
			// GeneralFunctions.saveMaskToFile(imagePath, mask, outImgMaskPath);
			// long maskWritingTime = System.currentTimeMillis() - start;
			// imgR.setMaskWritingTime(maskWritingTime);
			// }
			long totalTime = System.currentTimeMillis() - startAll;
			imgR.setTotalTime(totalTime);
			return imgR;
		}
		// === Step 7 completed ===

		// === Step 8: removing pixels with extreme values (outliers) ===
		// Calculate for all pixels R/G and G/B ratio and meanRG and meanGB
		long monotonicityTime = 0;
		// Calculating mean value of RG ratio
		double sumRG = 0.0;
		for (int i = 0; i < ccc.size(); i++) {
			CCClass component = ccc.get(i);
			List<Double> rgRatio = component.getRgRatio();
			for (int j = 0; j < rgRatio.size(); j++) {
				sumRG = sumRG + rgRatio.get(j);
			}
		}
		double meanRG = sumRG / totalNumPixelsInLargeClusters;

		// Calculating Standard Deviation of RG ratio
		double sumPRG = 0.0;
		for (int i = 0; i < ccc.size(); i++) {
			CCClass component = ccc.get(i);
			List<Double> rgRatio = component.getRgRatio();
			for (int j = 0; j < rgRatio.size(); j++) {
				double diff = rgRatio.get(j) - meanRG;
				double diff2 = diff * diff;
				sumPRG += diff2;
			}
		}
		double stdRG = Math.sqrt(sumPRG / totalNumPixelsInLargeClusters);

		// remove pixels with values smaller/larger than (meanRG-+4*stdRG)
		int numberOfPixelsWithOutliersRemoved = 0;
		for (int i = 0; i < ccc.size(); i++) {
			CCClass component = ccc.get(i);
			Iterator<Double> itrRG = component.getRgRatio().iterator();
			Iterator<Double> itrGB = component.getGbRatio().iterator();
			Iterator<String> itrXY = component.getXY().iterator();
			while (itrRG.hasNext()) {
				double rg = itrRG.next();
				itrGB.next();
				itrXY.next();
				if ((rg > (meanRG + 4 * stdRG)) || (rg < (meanRG - 4 * stdRG))) {
					itrRG.remove();
					itrGB.remove();
					itrXY.remove();
				} else {
					numberOfPixelsWithOutliersRemoved++;
				}
			}
		}
		// System.out.println("# pixels no outliers: " + numberOfPixelsWithOutliersRemoved);
		// === Step 8 completed ===

		// Save the mask here (after step 8) in any case because it is the final one!
		if (outImgMaskPath != null) {
			start = System.currentTimeMillis();
			boolean[][] mask = GeneralFunctions.getMask(W, H, ccc);
			GeneralFunctions.saveMaskToFileSimple(imagePath, mask, outImgMaskPath);
			long maskWritingTime = System.currentTimeMillis() - start;
			imgR.setMaskWritingTime(maskWritingTime);
		}

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
		// Getting bigger connected component
		int maxComp = 0;
		int indexOfMaxComponent = 0;
		for (int i = 0; i < ccc.size(); i++) {
			CCClass component = ccc.get(i);
			if (i == 0) {
				maxComp = component.getXY().size();
			}
			if (component.getXY().size() > maxComp) {
				indexOfMaxComponent = i;
			}
		}

		Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
		Map<Integer, Integer> xy = new HashMap<Integer, Integer>();
		CCClass component = ccc.get(indexOfMaxComponent);
		List<String> xyCoords = component.getXY();
		List<Double> gbRatio = component.getGbRatio();
		for (int i = 0; i < xyCoords.size(); i++) {
			int x = Integer.parseInt(xyCoords.get(i).split(",")[0]);
			int y = Integer.parseInt(xyCoords.get(i).split(",")[1]);

			if (xy.containsKey(x)) {
				if (xy.get(x) == (y - 1)) {
					xy.replace(x, y);
					counts.replace(x, counts.get(x) + 1);
				} else {
					xy.remove(x);
					counts.remove(x);
				}
			} else {
				xy.put(x, y);
				counts.put(x, 1);
			}
		}

		Map<Integer, Integer> sortedCounts = sortMapByValues(counts);

		// printing values after sorting of map
		Integer[] keysReverseCount = sortedCounts.keySet().toArray(new Integer[sortedCounts.size()]);
		int count = 0;
		List<Boolean> flag = new ArrayList<Boolean>();
		for (int i = keysReverseCount.length - 1; i >= 0; i--) {
			if (count == 20) {
				break;
			}
			// System.out.println(keysReverseCount[i] + " "+counts.get(keysReverseCount[i]));

			int xValue = keysReverseCount[i];
			int xCounts = counts.get(keysReverseCount[i]);
			int yValueMax = xy.get(keysReverseCount[i]);

			boolean monotoneFlag = decidingUponMonotonicity(xValue, xCounts, yValueMax, xyCoords, gbRatio);
			flag.add(monotoneFlag);

			count++;
		}
		int trueFlagCounter = countingFlagTrueValues(flag);
		imgR.setNumMonotoneVerticalLines(trueFlagCounter);
		imgR.setNumVerticalLinesChecked(count);

		monotonicityTime = System.currentTimeMillis() - start;
		imgR.setMonotonicityTime(monotonicityTime);

		// System.out.println("Num monotone: " + trueFlagCounter + "/ " + 20);

		if (trueFlagCounter < (count * 0.9)) {
			testThatFailed = "step 10, no monotone rise in G/B ratio " + trueFlagCounter + "/" + count;
			imgR.setTestThatFailed(testThatFailed);
			long totalTime = System.currentTimeMillis() - startAll;
			imgR.setTotalTime(totalTime);
			return imgR;
		}
		// === Step 10 completed ===

		// === ALL TESTS PASSED! COMPUTING FINAL RATIOS! ===
		// Calculating mean value of RG ratio
		sumRG = 0.0;
		double sumGB = 0.0;
		for (int i = 0; i < ccc.size(); i++) {
			component = ccc.get(i);
			List<Double> rgRatioF = component.getRgRatio();
			List<Double> gbRatioF = component.getGbRatio();
			for (int j = 0; j < rgRatioF.size(); j++) {
				sumRG += rgRatioF.get(j);
				sumGB += gbRatioF.get(j);
			}
		}
		meanRG = sumRG / numberOfPixelsWithOutliersRemoved;
		double meanGB = sumGB / numberOfPixelsWithOutliersRemoved;

		imgR.setMeanGB((float) meanGB);
		imgR.setMeanRG((float) meanRG);
		imgR.setNumUsableSkyPixels(numberOfPixelsWithOutliersRemoved);

		long totalTime = System.currentTimeMillis() - startAll;
		imgR.setTotalTime(totalTime);

		// and save the mask!
		// Save the mask here (after step 8) in any case because it is the final one!
		// if (outImgMaskPath != null) {
		// start = System.currentTimeMillis();
		// boolean[][] mask = GeneralFunctions.getMask(W, H, ccc);
		// GeneralFunctions.saveMaskToFile(imagePath, mask, outImgMaskPath);
		// long maskWritingTime = System.currentTimeMillis() - start;
		// imgR.setMaskWritingTime(maskWritingTime);
		// }

		return imgR;
	}

	@Deprecated
	public static RatioCalculationResults processImage(String imageName, String imagePath, String txtMaskFromDcnnPath,
			String outImgMaskPath, String outTxtMaskPath) throws IOException {
		long startAll = System.currentTimeMillis();

		long maskWritingTime = 0;

		int i, j;
		RatioCalculationResults imgR = new RatioCalculationResults();

		long start = System.currentTimeMillis();
		BufferedImage bimg = ImageIO.read(new File(imagePath));
		long readingTime = System.currentTimeMillis() - start;
		imgR.setReadingTime(readingTime);

		int H = bimg.getHeight();
		int W = bimg.getWidth();
		int clusterSizeLimitValue = H * W / 400;
		int allClusterPixelsLimitValue = H * W / 100;
		Dimension d = new Dimension(W, H);

		ConnectComponent cc = new ConnectComponent();

		int[][] dcnnMask = new int[H][W];
		if (txtMaskFromDcnnPath != null) {
			dcnnMask = readMask(txtMaskFromDcnnPath, W, H);
		}

		// first iteration through image pixels just to check sky condition per pixel
		int cccv = 1;
		int[] map = createEmptyLineMap(W, H);
		for (int x = 0; x < W; x++) {
			for (int y = 0; y < H / 2; y++) {
				int rgb = bimg.getRGB(x, y);
				Color c = new Color(rgb);
				int r = c.getRed();// (rgb & 0xFF0000)>>16;
				int g = c.getGreen(); // rgb & 0x00FF00)>>8;
				int b = c.getBlue(); // rgb & 0x0000FF;

				if ((isSkyPixelCandidate(r, g, b) == true) && (dcnnMask[y][x] == 27 || dcnnMask[y][x] == 0)) {
					map[y * W + x] = 3;
					cccv = cccv + 1;
				} else {
					map[y * W + x] = 0;
				}
			}
		}

		List<CCClass> ccc = cc.labeling(bimg, map, d, true);
		if (outImgMaskPath != null) {
			// start = System.currentTimeMillis();
			// GeneralFunctions.generatingMask(W, H, bimg, outImgMaskPath, ccc);
			// extraTime += System.currentTimeMillis() - start;
		}
		// if (outTxtMaskFile != null)
		// GeneralFunctions.generatingReverseMaskTextFile(w, h, bimg, outTxtMaskFile, ccc);

		// Get number of pixels for all clusters greater that clusterSizeLimitValue
		int numberOfPixels = 0;
		Iterator<CCClass> itr = ccc.iterator();
		while (itr.hasNext()) {
			CCClass ccI = itr.next();
			if (ccI.getXY().size() < clusterSizeLimitValue) {
				itr.remove();
			} else {
				numberOfPixels = numberOfPixels + ccI.getXY().size();
			}
		}

		// if (outImgMaskPath != null) {
		// GeneralFunctions.generatingMask(W, H, bimg, outImgMaskPath, ccc);
		// }
		// if (outTxtMaskPath != null) {
		// GeneralFunctions.generatingReverseMaskTextFile(W, H, bimg, outTxtMaskPath, ccc);
		// }

		if (outImgMaskPath != null) {
			// start = System.currentTimeMillis();
			// GeneralFunctions.generatingMask(W, H, bimg, outImgMaskPath.replace(".jpg", "_cc.jpg"), ccc);
			// extraTime += System.currentTimeMillis() - start;
		}

		// Calculate for all pixels R/G and G/B ratio and meanRG and meanGB
		if (numberOfPixels >= allClusterPixelsLimitValue) {

			// Calculating mean value of RG ratio
			double sumRG = 0.0;
			for (i = 0; i < ccc.size(); i++) {
				CCClass component = ccc.get(i);
				List<Double> rgRatio = component.getRgRatio();
				for (j = 0; j < rgRatio.size(); j++) {
					sumRG = sumRG + rgRatio.get(j);
				}
			}
			double meanRG = sumRG / numberOfPixels;

			// Calculating Standard Deviation of RG ratio
			double sumPRG = 0.0;
			for (i = 0; i < ccc.size(); i++) {
				CCClass component = ccc.get(i);
				List<Double> rgRatio = component.getRgRatio();
				for (j = 0; j < rgRatio.size(); j++) {
					double diff = rgRatio.get(j) - meanRG;
					double diff2 = diff * diff;
					sumPRG += diff2;
				}
			}
			double stdRG = Math.sqrt(sumPRG / numberOfPixels);

			// Remove points with value smaller than (meanRG+4*stdRG) and greater than that (removing
			// outliers)
			int numberOfPixelsWithOutliersRemoved = 0;
			for (i = 0; i < ccc.size(); i++) {
				CCClass component = ccc.get(i);
				Iterator<Double> itrRG = component.getRgRatio().iterator();
				Iterator<Double> itrGB = component.getGbRatio().iterator();
				Iterator<String> itrXY = component.getXY().iterator();
				while (itrRG.hasNext()) {
					double rg = itrRG.next();
					double gb = itrGB.next();
					String xy = itrXY.next();
					if ((rg > (meanRG + 4 * stdRG)) || (rg < (meanRG - 4 * stdRG))) {
						itrRG.remove();
						itrGB.remove();
						itrXY.remove();
					} else {
						numberOfPixelsWithOutliersRemoved = numberOfPixelsWithOutliersRemoved + 1;
					}
				}

			}

			if (outImgMaskPath != null) {
				// long start = System.currentTimeMillis();
				// GeneralFunctions.generatingMask(W, H, bimg, outImgMaskPath.replace(".jpg", "_noo.jpg"),
				// ccc);
				// extraTime += System.currentTimeMillis() - start;
			}

			// Check if monotone rise applies to biggest segment
			if (numberOfPixelsWithOutliersRemoved >= allClusterPixelsLimitValue) {

				// Getting bigger connected component
				int maxComp = 0;
				int indexOfMaxComponent = 0;
				for (i = 0; i < ccc.size(); i++) {
					CCClass component = ccc.get(i);

					if (i == 0) {
						maxComp = component.getXY().size();
					}

					if (component.getXY().size() > maxComp) {
						indexOfMaxComponent = i;
					}
				}

				Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
				Map<Integer, Integer> xy = new HashMap<Integer, Integer>();
				CCClass component = ccc.get(indexOfMaxComponent);
				List<String> xyCoords = component.getXY();
				List<Double> gbRatio = component.getGbRatio();
				for (i = 0; i < xyCoords.size(); i++) {
					int x = Integer.parseInt(xyCoords.get(i).substring(0, xyCoords.get(i).indexOf(",")));
					int y = Integer.parseInt(xyCoords.get(i).substring(xyCoords.get(i).indexOf(",") + 1));

					if (xy.containsKey(x)) {
						if (xy.get(x) == (y - 1)) {
							xy.replace(x, y);
							counts.replace(x, counts.get(x) + 1);
						} else {
							xy.remove(x);
							counts.remove(x);
						}
					} else {
						xy.put(x, y);
						counts.put(x, 1);
					}
				}

				Map<Integer, Integer> sortedCounts = sortMapByValues(counts);

				// printing values after sorting of map
				Integer[] keysReverseCount = sortedCounts.keySet().toArray(new Integer[sortedCounts.size()]);
				int count = 1;
				List<Boolean> flag = new ArrayList<Boolean>();
				for (i = keysReverseCount.length - 1; i >= 0; i--) {
					// System.out.println(keysReverseCount[i] + " "+counts.get(keysReverseCount[i]));

					int xValue = keysReverseCount[i];
					int xCounts = counts.get(keysReverseCount[i]);
					int yValueMax = xy.get(keysReverseCount[i]);

					boolean monotoneFlag = decidingUponMonotonicity(xValue, xCounts, yValueMax, xyCoords, gbRatio);
					flag.add(monotoneFlag);

					if (count > 20)
						break;

					count = count + 1;
				}

				int trueFlagCounter = countingFlagTrueValues(flag);

				if (trueFlagCounter >= (count * 0.9)) {
					if (outImgMaskPath != null) {
						start = System.currentTimeMillis();
						GeneralFunctions.generatingMask(W, H, bimg, outImgMaskPath, ccc);
						maskWritingTime += System.currentTimeMillis() - start;
					}

					// Calculating mean value of RG ratio
					sumRG = 0.0;
					double sumGB = 0.0;
					for (i = 0; i < ccc.size(); i++) {
						component = ccc.get(i);
						List<Double> rgRatioF = component.getRgRatio();
						List<Double> gbRatioF = component.getGbRatio();
						for (j = 0; j < rgRatioF.size(); j++) {
							sumRG = sumRG + rgRatioF.get(j);
							sumGB = sumGB + gbRatioF.get(j);
						}
					}
					meanRG = sumRG / numberOfPixelsWithOutliersRemoved;
					double meanGB = sumGB / numberOfPixelsWithOutliersRemoved;

					imgR.setMeanGB((float) meanGB);
					imgR.setMeanRG((float) meanRG);
					imgR.setNumUsableSkyPixels(numberOfPixelsWithOutliersRemoved);
				}
			}
		}

		imgR.setAllPixels(H * W);
		imgR.setImageName(imageName);
		imgR.setMaskWritingTime(maskWritingTime);

		long totalTime = System.currentTimeMillis() - startAll;
		imgR.setTotalTime(totalTime);
		return imgR;
	}

	protected static Map<Integer, Integer> sortMapByValues(Map<Integer, Integer> aMap) {

		Set<Entry<Integer, Integer>> mapEntries = aMap.entrySet();

		// used linked list to sort, because insertion of elements in linked list is faster than an array
		// list.
		List<Entry<Integer, Integer>> aList = new LinkedList<Entry<Integer, Integer>>(mapEntries);

		// sorting the List
		Collections.sort(aList, new Comparator<Entry<Integer, Integer>>() {
			public int compare(Entry<Integer, Integer> ele1, Entry<Integer, Integer> ele2) {
				return ele1.getValue().compareTo(ele2.getValue());
			}
		});

		// Storing the list into Linked HashMap to preserve the order of insertion.
		Map<Integer, Integer> aMap2 = new LinkedHashMap<Integer, Integer>();
		for (Entry<Integer, Integer> entry : aList) {
			aMap2.put(entry.getKey(), entry.getValue());
		}

		return aMap2;

	}

	protected static int[] createEmptyLineMap(int W, int H) {
		int[] map = new int[W * H];
		Arrays.fill(map, 0);
		return map;
	}

	protected static boolean isSkyPixelCandidate(int[] rgb) {
		return isSkyPixelCandidate(rgb[0], rgb[1], rgb[2]);
	}

	protected static boolean isSkyPixelCandidate(int r, int g, int b) {

		float rg = (float) r / (float) g;
		float gb = (float) g / (float) b;
		float br = (float) b / (float) r;

		if ((rg >= 0.5 && rg <= 1) && (gb >= 0.5 && gb <= 1) && (br > 1.25)) {
			return true;
		} else {
			return false;
		}
	}

	protected static boolean decidingUponMonotonicity(int xValue, int xCounts, int yValueMax, List<String> xyCoords,
			List<Double> gbRatio) {

		double[] seqNums = new double[xCounts];
		for (int i = 0; i < xCounts; i++) {
			int yValue = yValueMax - i;
			if (xyCoords.contains(xValue + "," + yValue)) {
				seqNums[i] = gbRatio.get(xyCoords.indexOf(xValue + "," + yValue));
			}
		}

		// boolean flag = gf.checkIfNumSequenceIsMonotone(seqNums);
		boolean flagA = GeneralFunctions.checkIfNumSequenceIsCloseToMonotone(seqNums);

		return flagA;
	}

	protected static int countingFlagTrueValues(List<Boolean> flag) {
		int i, trueCounter = 0;

		for (i = 0; i < flag.size(); i++) {
			if (flag.get(i) == true)
				trueCounter = trueCounter + 1;
		}

		return trueCounter;
	}

	protected static int[][] readMask(String file, int width, int height) throws IOException {
		int[][] imageMask = new int[height][width];
		int line = 0;
		String thisLine = "";

		BufferedReader br = new BufferedReader(new FileReader(file));

		while ((thisLine = br.readLine()) != null) {
			String[] chunks = thisLine.split("\\s+");
			for (int col = 0; col < chunks.length; col++) {
				imageMask[line][col] = Integer.parseInt(chunks[col].trim());
			}
			line = line + 1;
		}
		br.close();

		return imageMask;
	}

}
