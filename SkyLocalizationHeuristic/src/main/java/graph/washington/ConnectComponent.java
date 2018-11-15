package graph.washington;

/**
 * Use row-by-row labeling algorithm to label connected components
 * The algorithm makes two passes over the image: one pass to record
 * equivalences and assign temporary labels and the second to replace each
 * temporary label by the label of its equivalence class.
 * [Reference]
 * Linda G. Shapiro, Computer Vision: Theory and Applications.  (3.4 Connected
 * Components Labeling)
 * Rosenfeld and Pfaltz (1966)
 */
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConnectComponent {
	final int MAX_LABELS = 3500000;
	int next_label = 1;

	/**
	 * return the max label in the labeling process. the range of labels is [0..max_label]
	 */
	public int getMaxLabel() {
		return next_label;
	}

	/**
	 * Label the connect components If label 0 is background, then label 0 is untouched; If not, label 0 may
	 * be reassigned [Requires] 0 is treated as background
	 * 
	 * @param image
	 *            data
	 * @param d
	 *            dimension of the data
	 * @param zeroAsBg
	 *            label 0 is treated as background, so be ignored
	 * @throws IOException
	 */
	public List<CCClass> labeling(BufferedImage bimg, int[] image, Dimension d, boolean zeroAsBg)
			throws IOException {
		int w = d.width, h = d.height;
		int[] rst = new int[w * h];
		int[] parent = new int[MAX_LABELS];
		int[] labels = new int[MAX_LABELS];

		// region label starts from 1;
		// this is required as union-find data structure
		int next_region = 1;
		for (int y = 0; y < h / 2; ++y) {

			for (int x = 0; x < w; ++x) {
				if (image[y * w + x] == 0 && zeroAsBg) {
					continue;
				}
				int k = 0;

				boolean connected = false;

				// if connected to the left
				if (x > 0 && image[y * w + x - 1] == image[y * w + x]) {
					k = rst[y * w + x - 1];
					connected = true;
				}

				// if connected to the up
				if (y > 0 && image[(y - 1) * w + x] == image[y * w + x]
						&& (connected = false || image[(y - 1) * w + x] < k)) {
					k = rst[(y - 1) * w + x];
					connected = true;
				}

				if (!connected) {
					k = next_region;
					next_region++;

				}

				if (k >= MAX_LABELS) {
					System.err
							.println("Maximum number of labels reached - Increase MAX_LABELS and recompile.");
					System.exit(1);
				}

				rst[y * w + x] = k;

				// if connected, but with different label, then do union
				if (x > 0 && image[y * w + x - 1] == image[y * w + x] && rst[y * w + x - 1] != k) {
					uf_union(k, rst[y * w + x - 1], parent);
				}

				if (y > 0 && image[(y - 1) * w + x] == image[y * w + x] && rst[(y - 1) * w + x] != k) {
					uf_union(k, rst[(y - 1) * w + x], parent);
				}

			}

		}

		// Begin the second pass. Assign the new labels
		// if 0 is reserved for background, then the first available label is 1
		next_label = 1;
		for (int y = 0; y < h / 2; ++y) {
			for (int x = 0; x < w; ++x) {
				if (image[y * w + x] != 0 || !zeroAsBg) {
					rst[y * w + x] = uf_find(rst[y * w + x], parent, labels);
					if (!zeroAsBg)
						rst[y * w + x]--;
				}
			}
		}
		next_label--; // next_label records the max label

		if (!zeroAsBg)
			next_label--;

		// System.out.println(next_label+" regions");

		// Storing connected components
		List<CCClass> ccc = new ArrayList<CCClass>();
		// int cccc = 0;
		for (int i = 1; i <= next_label; i++) {

			CCClass cc = new CCClass();

			List<String> xy = new ArrayList<String>();
			List<Double> rgRatio = new ArrayList<Double>();
			List<Double> gbRatio = new ArrayList<Double>();
			for (int y = 0; y < h / 2; ++y) {
				for (int x = 0; x < w; ++x) {
					if (rst[y * w + x] == i) {
						int rgb = bimg.getRGB(x, y);
						Color c = new Color(rgb);
						int r = c.getRed(); // (rgb & 0xFF0000)>>16;
						int g = c.getGreen(); // (rgb & 0x00FF00)>>8;
						int b = c.getBlue(); // rgb & 0x0000FF;

						xy.add(x + "," + y);
						rgRatio.add(1.0 * r / g);
						gbRatio.add(1.0 * g / b);
						// cccc = cccc + 1;
					}
				}
			}

			cc.setLabel(i);
			cc.setXY(xy);
			cc.setGbRatio(gbRatio);
			cc.setRgRatio(rgRatio);

			ccc.add(cc);
		}

		return ccc;
	}

	void uf_union(int x, int y, int[] parent) {
		while (parent[x] > 0)
			x = parent[x];
		while (parent[y] > 0)
			y = parent[y];
		if (x != y) {
			if (x < y)
				parent[x] = y;
			else
				parent[y] = x;
		}
	}

	/**
	 * This function is called to return the root label Returned label starts from 1 because label array is
	 * inited to 0 as first
	 * 
	 * label array records the new label for every root
	 */
	int uf_find(int x, int[] parent, int[] label)

	{
		while (parent[x] > 0)
			x = parent[x];
		if (label[x] == 0)
			label[x] = next_label++;
		return label[x];
	}

}
