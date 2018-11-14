package gr.iti.mklab.download;

public class IdAndUrl {

	private String id;
	private String url;
	private String downloadSubfolder;

	public IdAndUrl(String id, String url, String downloadSubFolder) {
		this.id = id;
		this.url = url;
		this.downloadSubfolder = downloadSubFolder;
	}

	public String getId() {
		return id;
	}

	public String getDownloadSubfolder() {
		return downloadSubfolder;
	}

	public void setDownloadSubfolder(String downloadFolder) {
		this.downloadSubfolder = downloadFolder;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
