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
import java.io.File;
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

import graph.washington.CCClass;
import graph.washington.ConnectComponent;

public class SkyImageDetectionWashington {

	double meanRG, meanGB;
	int[] xN = { -1, 1, -1, 0, 1, -1, 0, 1 };
	int[] yN = { 0, 0, -1, -1, -1, 1, 1, 1 };

	public static void main(String[] args) {

		// String imageDir = args[0];
		String imageDir = "D:\\ITI\\PROJECTS\\hackAIR\\WPs\\WP3\\T3.2\\Code\\java\\SkyLocalization\\data";

		SkyImageDetectionWashington skd = new SkyImageDetectionWashington();

		for (int i = 1; i < 11; i++) {
			String img = imageDir + "\\" + "sky-" + i + ".jpg";
			String outImg = imageDir + "\\" + "out-sky-" + i + ".jpg";
			String outTxtMaskFile = imageDir + "\\" + "out-sky-" + i + ".txt";
			try {
				long t0 = System.currentTimeMillis();
				boolean flag = skd.processImage(img, outImg, outTxtMaskFile);
				double secs = (System.currentTimeMillis() - t0) / 1000.0;
				System.out.println(flag);
				// System.out.println(" in " + secs + " sec\n\n");
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	public boolean processImage(String inputFile, String outputFile, String outTxtMaskFile)
			throws IOException {

		int i, j;
		GeneralFunctions gf = new GeneralFunctions();

		File inputImg = new File(inputFile);

		BufferedImage bimg = ImageIO.read(inputImg);

		int H = bimg.getHeight();
		int W = bimg.getWidth();
		int clusterSizeLimitValue = H * W / 400;
		int allClusterPixelsLimitValue = H * W / 100;
		Dimension d = new Dimension(W, H);

		ConnectComponent cc = new ConnectComponent();

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

				if (isSkyPixelCandidate(r, g, b)) {
					map[y * W + x] = 3;
					cccv = cccv + 1;
				} else {
					map[y * W + x] = 0;
				}
			}
		}

		List<CCClass> ccc = cc.labeling(bimg, map, d, true);

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
					sumPRG = sumPRG + Math.pow(rgRatio.get(j) - meanRG, 2);
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

					boolean monotoneFlag = decidingUponMonotonicity(gf, xValue, xCounts, yValueMax, xyCoords,
							gbRatio);
					flag.add(monotoneFlag);

					if (count > 20)
						break;

					count = count + 1;
				}

				int trueFlagCounter = countingFlagTrueValues(flag);

				if (trueFlagCounter >= (count * 0.9)) {

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

					setMeanRG(meanRG);
					setMeanGB(meanGB);

					return true;
				}

			} else {
				return false;
			}

		} else {
			// System.out.println("This image is not appropriate for providing AQ estimation");
			return false;
		}

		return false;
	}

	private Map<Integer, Integer> sortMapByValues(Map<Integer, Integer> aMap) {

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

	private static int[] createEmptyLineMap(int W, int H) {
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

		if ((rg >= 0.5 && rg <= 1) && (gb >= 0.5 && gb <= 1) && (br >= 1.25)) {
			return true;
		} else {
			return false;
		}
	}

	boolean decidingUponMonotonicity(GeneralFunctions gf, int xValue, int xCounts, int yValueMax,
			List<String> xyCoords, List<Double> gbRatio) {

		double[] seqNums = new double[xCounts];
		for (int i = 0; i < xCounts; i++) {
			int yValue = yValueMax - i;
			if (xyCoords.contains(xValue + "," + yValue)) {
				seqNums[i] = gbRatio.get(xyCoords.indexOf(xValue + "," + yValue));
			}
		}

		boolean flag = gf.checkIfNumSequenceIsMonotone(seqNums);
		boolean flagA = gf.checkIfNumSequenceIsCloseToMonotone(seqNums);

		return flagA;
	}

	private void setMeanRG(double meanRG) {
		this.meanRG = meanRG;
	}

	private void setMeanGB(double meanGB) {
		this.meanGB = meanGB;
	}

	public double getMeanRG() {
		return meanRG;
	}

	public double getMeanGB() {
		return meanGB;
	}

	int countingFlagTrueValues(List<Boolean> flag) {
		int i, trueCounter = 0;

		for (i = 0; i < flag.size(); i++) {
			if (flag.get(i) == true)
				trueCounter = trueCounter + 1;
		}

		return trueCounter;
	}

}
