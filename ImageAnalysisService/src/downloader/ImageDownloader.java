package downloader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * This class implements multi-threaded image downloading.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 * 
 */
public class ImageDownloader {

	private ExecutorService downloadExecutor;

	private CompletionService<ImageDownloadResult> pool;

	/** The current number of tasks whose termination is pending. **/
	private int numPendingTasks;

	/**
	 * The maximum allowable number of pending tasks, used to limit the memory usage.
	 */
	private final int maxNumPendingTasks;

	/**
	 * The folder where the original image and/or its thumbnail should be saved.
	 **/
	private String downloadFolder;

	/**
	 * Whether redirects should be followed.
	 */
	private boolean followRedirects;

	/**
	 * Constructor of the multi-threaded download class.
	 * 
	 * @param numThreads
	 *            the number of download threads to use
	 * @param downloadFolder
	 *            the download folder
	 */
	public ImageDownloader(String downloadFolder, int numThreads) {
		this.downloadFolder = downloadFolder;
		followRedirects = false;

		downloadExecutor = Executors.newFixedThreadPool(numThreads);
		pool = new ExecutorCompletionService<ImageDownloadResult>(downloadExecutor);
		numPendingTasks = 0;
		maxNumPendingTasks = numThreads * 10;

	}

	/**
	 * Submits a new image download task.
	 * 
	 * @param URL
	 *            The url of the image
	 * @param id
	 *            The id of the image (used to name the image file after download)
	 */
	public void submitImageDownloadTask(String URL, String id) {
		Callable<ImageDownloadResult> call = new ImageDownload(URL, id, downloadFolder, followRedirects);
		pool.submit(call);
		numPendingTasks++;
	}

	/**
	 * Gets an image download results from the pool.
	 * 
	 * @return the download result, or null in no results are ready
	 * @throws Exception
	 *             for a failed download task
	 */
	public ImageDownloadResult getImageDownloadResult() throws Exception {
		Future<ImageDownloadResult> future = pool.poll();
		if (future == null) { // no completed tasks in the pool
			return null;
		} else {
			try {
				ImageDownloadResult imdr = future.get();
				return imdr;
			} catch (Exception e) {
				throw e;
			} finally {
				// in any case (Exception or not) the numPendingTask should be reduced
				numPendingTasks--;
			}
		}
	}

	/**
	 * Gets an image download result from the pool, waiting if necessary.
	 * 
	 * @return the download result
	 * @throws Exception
	 *             for a failed download task
	 */
	public ImageDownloadResult getImageDownloadResultWait() {
		ImageDownloadResult imdr = null;
		try {
			imdr = pool.take().get();
		} catch (ExecutionException | InterruptedException e) {
			System.err.println(e.getMessage());
		}
		numPendingTasks--;
		return imdr;
	}

	/**
	 * Returns true if the number of pending tasks is smaller than the maximum allowable number.
	 * 
	 * @return
	 */
	public boolean canAcceptMoreTasks() {
		if (numPendingTasks < maxNumPendingTasks) {
			return true;
		} else {
			return false;
		}
	}

	public void setFollowRedirects(boolean followRedirects) {
		this.followRedirects = followRedirects;
	}

	/**
	 * Shuts the download executor down, waiting for up to 60 seconds for the remaining tasks to complete. See
	 * http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html
	 * 
	 */
	public void shutDown() {
		downloadExecutor.shutdown(); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!downloadExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
				downloadExecutor.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!downloadExecutor.awaitTermination(60, TimeUnit.SECONDS))
					System.err.println("Pool did not terminate");
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			downloadExecutor.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Multi-threaded image download of an ArrayList of URLs.
	 * 
	 * @param dowloadFolder
	 *            Full path to the folder where the images are downloaded
	 * @param urls
	 * @return
	 */
	public static HashSet<String> downloadImages(String dowloadFolder, ArrayList<IdAndUrl> urls) {
		// Random rand = new Random();
		long start = System.currentTimeMillis();
		HashSet<String> failedIds = new HashSet<String>();
		int numThreads = 10;
		// System.out.println("Download folder:" + dowloadFolder);
		ImageDownloader downloader = new ImageDownloader(dowloadFolder, numThreads);
		int submittedCounter = 0;
		int completedCounter = 0;
		int failedCounter = 0;
		int reportingStep = 10;
		int nextReportingPoint = 0;
		while (true) {
			String url;
			String id = "";
			// if there are more task to submit and the downloader can accept more tasks then submit
			while (submittedCounter < urls.size() && downloader.canAcceptMoreTasks()) {
				url = urls.get(submittedCounter).getUrl();
				id = urls.get(submittedCounter).getId();
				downloader.submitImageDownloadTask(url, id);
				submittedCounter++;
			}
			// if there are submitted tasks that are pending completion ,try to consume
			if (completedCounter + failedCounter < submittedCounter) {
				ImageDownloadResult imdr = downloader.getImageDownloadResultWait();
				if (imdr.getExceptionMessage() == null) {
					completedCounter++;
				} else {
					failedCounter++;
					// System.err.println(failedCounter + "/" + idsAndurls.size() + " downloads failed!");
					String exceptionMessage = imdr.getExceptionMessage();
					String failedImageId = exceptionMessage.split("id: \"")[1].split("\"")[0];
					failedIds.add(failedImageId);
				}
				// System.out.println(completedCounter + " downloads completed!");

				double currentProgress = (double) (completedCounter + failedCounter) / urls.size() * 100;
				if (currentProgress >= nextReportingPoint) {
					System.out.print(nextReportingPoint + "% ");
					nextReportingPoint += reportingStep;
				}
			}
			// if all tasks have been consumed then break;
			if (completedCounter + failedCounter == urls.size()) {
				downloader.shutDown();
				break;
			}
		}
		long end = System.currentTimeMillis();
		System.out.println("Total download time: " + (end - start) + " ms");
		// System.out.println("Downloaded images: " + completedCounter);
		// System.out.println("Failed images: " + failedCounter);
		return failedIds;
	}

}
