package gr.mklab.classes;

public class RatioCalculationResults {

	private String imageName;

	private String imageId;

	private String exceptionMessage;

	private int numAllPixels;

	private int numSkyPixels;

	private int numSkyPixelsBeforeOutlierRemoval;

	public int getNumSkyPixelsBeforeOutlierRemoval() {
		return numSkyPixelsBeforeOutlierRemoval;
	}

	public void setNumSkyPixelsBeforeOutlierRemoval(int numSkyPixelsBeforeOutlierRemoval) {
		this.numSkyPixelsBeforeOutlierRemoval = numSkyPixelsBeforeOutlierRemoval;
	}

	private float meanRG;

	private float meanGB;

	private int numMonotoneVerticalLines;
	private int numVerticalLinesChecked;

	public void setNumMonotoneVerticalLines(int numMonotoneVerticalLines) {
		this.numMonotoneVerticalLines = numMonotoneVerticalLines;
	}

	public int getNumMonotoneVerticalLines() {
		return numMonotoneVerticalLines;
	}

	public int getNumVerticalLinesChecked() {
		return numVerticalLinesChecked;
	}

	public void setNumVerticalLinesChecked(int numVerticalLinesChecked) {
		this.numVerticalLinesChecked = numVerticalLinesChecked;
	}

	public void setNumSkyPixels(int numSkyPixels) {
		this.numSkyPixels = numSkyPixels;
	}

	private long readingTime;
	private long conCompTime;
	private long monotonicityTime;
	private long maskWritingTime;
	private long totalTime;

	private String testThatFailed;

	public int getNumAllPixels() {
		return numAllPixels;
	}

	public void setNumAllPixels(int numAllPixels) {
		this.numAllPixels = numAllPixels;
	}

	public String getTestThatFailed() {
		return testThatFailed;
	}

	public void setTestThatFailed(String testThatFailed) {
		this.testThatFailed = testThatFailed;
	}

	public RatioCalculationResults() {
		// TODO Auto-generated constructor stub
	}

	public RatioCalculationResults(String exceptionMessage) {
		this.exceptionMessage = exceptionMessage;
	}

	public int getAllPixels() {
		return numAllPixels;
	}

	public long getConCompTime() {
		return conCompTime;
	}

	public String getExceptionMessage() {
		return exceptionMessage;
	}

	public String getImageId() {
		return imageId;
	}

	public String getImageName() {
		return imageName;
	}

	public long getMaskWritingTime() {
		return maskWritingTime;
	}

	public float getMeanGB() {
		return meanGB;
	}

	public float getMeanRG() {
		return meanRG;
	}

	public long getMonotonicityTime() {
		return monotonicityTime;
	}

	public int getNumSkyPixels() {
		return numSkyPixels;
	}

	public long getReadingTime() {
		return readingTime;
	}

	public long getTotalTime() {
		return totalTime;
	}

	public void setAllPixels(int allPixels) {
		this.numAllPixels = allPixels;
	}

	public void setConCompTime(long conCompTime) {
		this.conCompTime = conCompTime;
	}

	public void setExceptionMessage(String exceptionMessage) {
		this.exceptionMessage = exceptionMessage;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public void setImageName(String imageName) {
		this.imageName = imageName;
	}

	public void setMaskWritingTime(long maskWritingTime) {
		this.maskWritingTime = maskWritingTime;
	}

	public void setMeanGB(float meanGB) {
		this.meanGB = meanGB;
	}

	public void setMeanRG(float meanRG) {
		this.meanRG = meanRG;
	}

	public void setMonotonicityTime(long monotonicityTime) {
		this.monotonicityTime = monotonicityTime;
	}

	public void setNumUsableSkyPixels(int skyPixels) {
		this.numSkyPixels = skyPixels;
	}

	public void setReadingTime(long readingTime) {
		this.readingTime = readingTime;
	}

	public void setTotalTime(long totalTime) {
		this.totalTime = totalTime;
	}

	public String toString() {
		if (numSkyPixels == 0) {
			return "No usable sky: " + testThatFailed;
		} else {
			return "R/G:" + meanRG + "\tG/B:" + meanGB + "\t# sky pixels:" + numSkyPixels + "\t# all pixels:"
					+ numAllPixels;
		}
	}

	public void printTimes() {
		System.out.print("totalTime:" + totalTime + "\t");
		System.out.print("readingTime:" + readingTime + "\t");
		System.out.print("conCompTime:" + conCompTime + "\t");
		System.out.print("monotonicityTime:" + monotonicityTime + "\t");
		System.out.print("maskWritingTime:" + maskWritingTime + "\n");
	}
}
