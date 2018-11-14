package pojo.responses;

import java.util.ArrayList;

public class LocalizationResponse {

	private ArrayList<PathMask> images;

	private double total_time;
	private double encoding_time;
	private double localization_time;
	private double loading_time;
	private boolean debug;

	public double getTotal_time() {
		return total_time;
	}

	public void setTotal_time(double total_time) {
		this.total_time = total_time;
	}

	public double getEncoding_time() {
		return encoding_time;
	}

	public void setEncoding_time(double encoding_time) {
		this.encoding_time = encoding_time;
	}

	public double getLocalization_time() {
		return localization_time;
	}

	public void setLocalization_time(double localization_time) {
		this.localization_time = localization_time;
	}

	public double getLoading_time() {
		return loading_time;
	}

	public void setLoading_time(double loading_time) {
		this.loading_time = loading_time;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public ArrayList<PathMask> getImages() {
		return images;
	}

	public void setImages(ArrayList<PathMask> images) {
		this.images = images;
	}

}
