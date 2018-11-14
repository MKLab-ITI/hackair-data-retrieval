package downloader;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;

/**
 * This class represents an image download task. It implements the Callable interface and can be used for
 * multi-threaded image download.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 * 
 */
public class ImageDownload implements Callable<ImageDownloadResult> {

	/**
	 * The URL where the image should be downloaded from.
	 */
	private String imageUrl;

	/**
	 * The image identifier.
	 */
	private String imageId;

	/**
	 * The directory where the image will be downloaded (temporarily or permanently).
	 */
	private String downloadFolder;

	/**
	 * Whether to follow redirects (note that when redirects are followed, a wrong image may be downloaded).
	 */
	private boolean followRedirects;

	/**
	 * The value to use in HttpURLConnection.setConnectTimeout()
	 */
	public static final int connectionTimeout = 5000;

	/**
	 * The value to use in HttpURLConnection.setReadTimeout()
	 */
	public static final int readTimeout = 5000;

	/**
	 * The number of connection retries.
	 */
	public static final int maxRetries = 0; // currently not used

	/**
	 * The size of the thumbnail in pixels.
	 */
	public static final int thumbnailSizeInPixels = 200 * 200;

	/**
	 * If set to true, debug output is displayed.
	 */
	public boolean debug = false;

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	/**
	 * Constructor with 3 arguments.
	 * 
	 * @param urlStr
	 *            The url where the image is downloaded from
	 * @param id
	 *            The image identifier
	 * @param downloadFolder
	 *            The folder where the image is temporarily downloaded
	 */
	public ImageDownload(String urlStr, String id, String downloadFolder) {
		this.imageUrl = urlStr;
		this.imageId = id;
		this.downloadFolder = downloadFolder;
		this.followRedirects = false;
	}

	/**
	 * Constructor with 6 arguments.
	 * 
	 * @param urlStr
	 *            The url where the image is downloaded from
	 * @param id
	 *            The image identifier (used to name the image file after download)
	 * @param downloadFolder
	 *            The folder where the image is downloaded
	 * @param followRedirects
	 *            Whether redirects should be followed
	 */
	public ImageDownload(String urlStr, String id, String downloadFolder, boolean followRedirects) {
		this.imageUrl = urlStr;
		this.imageId = id;
		this.downloadFolder = downloadFolder;
		this.followRedirects = followRedirects;
	}

	@Override
	/**
	 * Returns an ImageDownloadResult object from where the BufferedImage object and the image identifier can
	 * be obtained.
	 */
	public ImageDownloadResult call() {
		if (debug)
			System.out.println("Downloading image " + imageUrl + " started.");
		String exceptionMessage = downloadImage();
		if (exceptionMessage != null) {
			// exceptionMessage = "Exception thrown during download of image id: \"" + imageId + "\" at url: "
			// + imageUrl + "\nException message: " + exceptionMessage;
			exceptionMessage = "Exception thrown during download of image id: \"" + imageId + "\" at url: "
					+ imageUrl + " exception: " + exceptionMessage;
			System.err.println(exceptionMessage);
		}
		if (debug)
			System.out.println("Downloading image " + imageUrl + " completed.");
		return new ImageDownloadResult(imageId, imageUrl, exceptionMessage);
	}

	/**
	 * Download of an image by URL.
	 * 
	 * @return The image as a BufferedImage object.
	 * @throws Exception
	 */
	public String downloadImage() {
		String exceptionMessage = null;
		try {
			// try to recognize the type of the image from the URL so that the correct format and file
			// extension are used when saving the image.
			// In case that the URL does not contain a known image extension, the jpg extension is used.
			String[] splitted = imageUrl.split("\\.");
			String fileExtension = (splitted[splitted.length - 1]).toLowerCase();
			if (!fileExtension.equals("jpg") && !fileExtension.equals("jpeg") && !fileExtension.equals("png")
					&& !fileExtension.equals("bmp") && !fileExtension.equals("gif")) {
				fileExtension = "jpg";
			}
			// this name filename will be used for the saved image file
			String imageFilename = downloadFolder + imageId + "." + fileExtension;

			// initialize the url, checking that it is valid
			URL url = null;
			try {
				url = new URL(imageUrl);
			} catch (MalformedURLException e) {
				// System.err.println("Malformed url exception. Url: " + imageUrl);
				throw e;
			}

			HttpURLConnection conn = null;
			FileOutputStream fos = null;
			ReadableByteChannel rbc = null;
			InputStream in = null;
			boolean success = false;
			try {
				conn = (HttpURLConnection) url.openConnection();
				conn.setInstanceFollowRedirects(followRedirects);
				conn.setConnectTimeout(connectionTimeout); // TO DO: add retries when connections times out
				conn.setReadTimeout(readTimeout);
				conn.connect();
				success = true;
			} catch (Exception e) {
				// System.err.println("Connection related exception at url: " + imageUrl);
				throw e;
			} finally {
				if (!success) {
					conn.disconnect();
				}
			}

			success = false;
			try {
				in = conn.getInputStream();
				success = true;
			} catch (Exception e) {
				// System.err.println(
				// "Exception when getting the input stream from the connection at url: " + imageUrl);
				throw e;
			} finally {
				if (!success) {
					in.close();
				}
			}

			rbc = Channels.newChannel(in);

			// if an Exception is thrown in the following code, the file that was created must be deleted
			try {
				// just copy the file
				fos = new FileOutputStream(imageFilename);
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				fos.close();
				in.close();
				rbc.close();
				conn.disconnect();
			} catch (Exception e) { // in case of any other exception delete the image and re-throw the
									// exception
				// System.err.println(
				// "Exception: " + e.toString() + " | Image: " + imageFilename + " URL: " + imageUrl);
				throw (e);
			} finally {
				// close the open streams
				fos.close();
				in.close();
				rbc.close();
				conn.disconnect();
			}

			// downscale if needed!
			InputStream is = new FileInputStream(imageFilename);
			ImageInfo ii = new ImageInfo();
			ii.setInput(is); // in can be InputStream or RandomAccessFile
			if (!ii.check()) {
				throw new Exception("Image scaling failed!");
			}
			int size = ii.getWidth() * ii.getHeight();
			String formatName = ii.getFormatName();
			is.close();

			if (size > 500 * 500) {
				// System.out.println("++++++++++++++++Image requires downscaling!!!");
				ImageScaling imscale = new ImageScaling();
				imscale.setTargetSize(500 * 500);
				// read the image
				BufferedImage image;
				try { // first try reading with the default class
					image = ImageIO.read(new File(imageFilename));
				} catch (IllegalArgumentException e) { // if it fails retry with the corrected class
					// This exception is probably because of a grayscale jpeg image.
					System.out.println("Exception: " + e.getMessage() + " | Image: " + imageFilename);
					image = ImageIOGreyScale.read(new File(imageFilename));
				}

				image = imscale.maxPixelsScaling(image);
				ImageIO.write(image, formatName, new File(imageFilename));
			}

		} catch (Exception e) {
			exceptionMessage = e.getMessage();
		}
		return exceptionMessage;
	}

}
