package pojo.requests;

import java.util.ArrayList;

public class LocalizationRequest {
	private boolean debug;
	private ArrayList<Path> images;

	public static class Path {
		private String path;

		public Path(String path) {
			this.path = path;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

	}

	public LocalizationRequest() {
		images = new ArrayList<Path>();
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public ArrayList<Path> getImages() {
		return images;
	}

	public void setImages(ArrayList<Path> images) {
		this.images = images;
	}

	public void addImage(String pathString) {
		Path path = new Path(pathString);
		images.add(path);
	}
}
