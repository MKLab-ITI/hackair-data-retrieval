package gr.mklab;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import boofcv.struct.image.GrayU8;
import commonFunctions.PublicVariables;
import graph.washington.CCClass;

public class GeneralFunctions {

	// Generate output mask
	public static void generatingMask(int W, int H, BufferedImage bimg, String outputFile, int[][] map) {

		BufferedImage offImage = new BufferedImage(bimg.getWidth(), bimg.getHeight(), BufferedImage.OPAQUE);
		File outputImg = new File(outputFile);

		for (int x = 0; x < W; x++) {
			for (int y = 0; y < H / 2; y++) {
				if (map[x][y] > 2) {
					offImage.setRGB(x, y, bimg.getRGB(x, y));
				} else {
					offImage.setRGB(x, y, 0);
				}
			}
		}

		try {
			ImageIO.write(offImage, "jpg", outputImg);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// Generate output mask
	public static void generatingReverseMask(int W, int H, BufferedImage bimg, String outputFile, List<CCClass> ccc) {

		int i, j;

		BufferedImage offImage = new BufferedImage(bimg.getWidth(), bimg.getHeight(), BufferedImage.OPAQUE);
		File outputImg = new File(outputFile);

		for (int x = 0; x < W; x++) {
			for (int y = 0; y < H; y++) {
				offImage.setRGB(x, y, bimg.getRGB(x, y));
			}
		}

		for (i = 0; i < ccc.size(); i++) {
			List<String> cc = ccc.get(i).getXY();
			for (j = 0; j < cc.size(); j++) {
				String[] coords = cc.get(j).split(",");
				offImage.setRGB(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), 0);
			}
		}

		try {
			ImageIO.write(offImage, "jpg", outputImg);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void saveMaskToFileWithBorders(String originalImagePath, boolean[][] mask, String outFilePath)
			throws Exception {

		int width = mask[0].length;
		int height = mask.length;

		BufferedImage originalImage = ImageIO.read(new File(originalImagePath));

		BufferedImage maskImage = new BufferedImage(width, height, BufferedImage.OPAQUE);
		File outputImg = new File(outFilePath);
		int size = 2;

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (mask[y][x]) {
					// if any of the neighboring pixels is not part of the mask, this is a boundary pixel and
					// should be red
					boolean isMaskBoundary = false;
					if (x > size - 1 && y > size - 1 && x < width - size && y < height - size) {
						// check the neighborhood
						for (int i = 1; i <= size; i++) {
							if (!mask[y + i][x] || !mask[y - i][x] || !mask[y][x + i] || !mask[y][x - i]
									|| !mask[y - i][x - i] || !mask[y - i][x + i] || !mask[y + i][x - i]
									|| !mask[y + i][x + i]) {
								isMaskBoundary = true;
							}
						}
					}

					if (isMaskBoundary) {
						maskImage.setRGB(x, y, Color.RED.getRGB());
					} else {
						maskImage.setRGB(x, y, originalImage.getRGB(x, y));
					}
				} else {
					maskImage.setRGB(x, y, 0);
				}

			}
		}

		ImageIO.write(maskImage, "jpg", outputImg);
	}

	public static void saveMaskToFileSimple(String originalImagePath, boolean[][] mask, String outFilePath)
			throws Exception {

		int width = mask[0].length;
		int height = mask.length;

		BufferedImage originalImage = ImageIO.read(new File(originalImagePath));

		BufferedImage maskImage = new BufferedImage(width, height, BufferedImage.OPAQUE);
		File outputImg = new File(outFilePath);

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (mask[y][x]) {
					// maskImage.setRGB(x, y, originalImage.getRGB(x, y));
					maskImage.setRGB(x, y, Color.WHITE.getRGB());
				} else {
					maskImage.setRGB(x, y, Color.BLACK.getRGB());
				}

			}
		}

		ImageIO.write(maskImage, "jpg", outputImg);
	}

	public static void saveMaskToFileSimple(GrayU8 binaryFinal, String outFilePath) throws Exception {

		int width = binaryFinal.getWidth();
		int height = binaryFinal.getHeight();

		// BufferedImage maskImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		BufferedImage maskImage = new BufferedImage(width, height, BufferedImage.OPAQUE);
		File outputImg = new File(outFilePath);

		// int whiteTrasparentRGB = 0x00FFFFFF & Color.WHITE.getRGB();
		// int blackOpaqueRGB = Color.WHITE.getRGB() | 0xFF000000;

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (binaryFinal.get(x, y) == 1) {
					maskImage.setRGB(x, y, 0);
				} else {
					maskImage.setRGB(x, y, -16777216);
				}

			}
		}

		ImageIO.write(maskImage, "jpg", outputImg);
	}

	// Generate output mask
	public static void generatingMask(int W, int H, BufferedImage bimg, String outputFile, List<CCClass> ccc) {

		BufferedImage offImage = new BufferedImage(bimg.getWidth(), bimg.getHeight(), BufferedImage.OPAQUE);
		File outputImg = new File(outputFile);

		for (int x = 0; x < W; x++) {
			for (int y = 0; y < H; y++) {
				offImage.setRGB(x, y, 0);
			}
		}

		for (int i = 0; i < ccc.size(); i++) {
			List<String> cc = ccc.get(i).getXY();
			for (int j = 0; j < cc.size(); j++) {
				String[] coords = cc.get(j).split(",");
				int x = Integer.parseInt(coords[0]);
				int y = Integer.parseInt(coords[1]);
				offImage.setRGB(x, y, bimg.getRGB(x, y));
			}
		}

		try {
			ImageIO.write(offImage, "jpg", outputImg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean[][] getMask(int W, int H, List<CCClass> ccc) {
		boolean[][] mask = new boolean[H][];
		for (int i = 0; i < mask.length; i++) {
			mask[i] = new boolean[W];
		}

		for (int i = 0; i < ccc.size(); i++) {
			List<String> cc = ccc.get(i).getXY();
			for (int j = 0; j < cc.size(); j++) {
				String[] coords = cc.get(j).split(",");
				int x = Integer.parseInt(coords[0]);
				int y = Integer.parseInt(coords[1]);
				mask[y][x] = true;
			}
		}
		return mask;

	}

	// Generate output mask
	public static void generatingReverseMaskTextFile(int W, int H, BufferedImage bimg, String outputFile,
			List<CCClass> ccc) throws IOException {

		int i, j;

		int[][] outputArray = new int[W][H];

		for (int x = 0; x < W; x++) {
			for (int y = 0; y < H; y++) {
				outputArray[x][y] = 1;
			}
		}

		for (i = 0; i < ccc.size(); i++) {
			List<String> cc = ccc.get(i).getXY();
			for (j = 0; j < cc.size(); j++) {
				String[] coords = cc.get(j).split(",");
				outputArray[Integer.parseInt(coords[0])][Integer.parseInt(coords[1])] = 27;
			}
		}

		File fi = new File(outputFile);
		fi.createNewFile();
		BufferedWriter buffWriter = new BufferedWriter(new FileWriter(fi));

		for (int x = 0; x < W; x++) {
			StringBuffer sb = new StringBuffer();
			for (int y = 0; y < H; y++) {
				sb.append(outputArray[x][y] + " ");
			}
			String line = sb.toString().trim();
			buffWriter.write(line + PublicVariables.newLine);
			sb.setLength(0);
		}

		buffWriter.close();

	}

	public boolean checkIfNumSequenceIsMonotone(double[] sq) {
		boolean flag = false;

		double[] sq_tmp = Arrays.copyOf(sq, sq.length);
		Arrays.sort(sq_tmp);

		if (Arrays.equals(sq_tmp, sq)) {
			flag = true;
		}

		return flag;
	}

	public static boolean checkIfNumSequenceIsCloseToMonotone(double[] sq) {
		boolean flag = false;

		boolean[] data = new boolean[sq.length];
		for (int i = 0; i < sq.length - 1; i++) {
			if ((sq[i] <= sq[i + 1]) || (0.97 * sq[i] <= sq[i + 1]))
				// if((sq[i] <= sq[i+1]) )
				data[i] = true;
			else
				data[i] = false;
		}

		int counter = countTrueFlags(data);

		if (counter >= ((sq.length - 1) * 0.97))
			flag = true;

		return flag;
	}

	public static boolean checkIfNumSequenceIsCloseToMonotone(float[] sq) {
		boolean flag = false;

		boolean[] data = new boolean[sq.length];
		for (int i = 0; i < sq.length - 1; i++) {
			if ((sq[i] <= sq[i + 1]) || (0.97 * sq[i] <= sq[i + 1]))
				// if((sq[i] <= sq[i+1]) )
				data[i] = true;
			else
				data[i] = false;
		}

		int counter = countTrueFlags(data);

		if (counter >= ((sq.length - 1) * 0.97))
			flag = true;

		return flag;
	}

	public static int countTrueFlags(boolean[] flags) {
		int count = 0;
		for (int i = 0; i < flags.length; i++) {
			if (flags[i] == true)
				count = count + 1;
		}
		return count;
	}

}
