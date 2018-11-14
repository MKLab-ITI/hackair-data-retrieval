package downloader;

public class IdAndUrl {

	private String id;
	private String url;
	private String downloadSubfolder;

	public IdAndUrl(String id, String url) {
		this.id = id;
		this.url = url;
	}

	public IdAndUrl(String id, String url, String downloadSubFolder) {
		this.id = id;
		this.url = url;
		this.downloadSubfolder = downloadSubFolder;
	}

	public String getDownloadSubfolder() {
		return downloadSubfolder;
	}

	public String getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}

	public void setDownloadSubfolder(String downloadFolder) {
		this.downloadSubfolder = downloadFolder;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
