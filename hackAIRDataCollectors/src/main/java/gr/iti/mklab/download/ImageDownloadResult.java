package gr.iti.mklab.download;

/**
 * This class represents the result of an image download task.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 * 
 */
public class ImageDownloadResult {

	private String imageId;

	private String imageUrl;

	private String exceptionMessage;

	public ImageDownloadResult(String imageId, String imageUrl, String exceptionMessage) {
		this.imageId = imageId;
		this.imageUrl = imageUrl;
		this.exceptionMessage = exceptionMessage;
	}

	public String getExceptionMessage() {
		return exceptionMessage;
	}

	public String getImageId() {
		return imageId;
	}

	public String getImageUrl() {
		return imageUrl;
	}

}
