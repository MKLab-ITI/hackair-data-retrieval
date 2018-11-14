package pojo.mongo;

import java.util.Date;

public class SourceInfoFlickr {

	private String id;
	private String path;
	private String page_url;
	private String image_url;

	public String getWebcam_id() {
		return webcam_id;
	}

	public void setWebcam_id(String webcam_id) {
		this.webcam_id = webcam_id;
	}

	private String title;
	private String tags;
	private String description;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	private String webcam_id;
	private String url;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	private int views;
	private String username;
	private Date date_uploaded;

	public SourceInfoFlickr() {

	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getPage_url() {
		return page_url;
	}

	public void setPage_url(String page_url) {
		this.page_url = page_url;
	}

	public String getImage_url() {
		return image_url;
	}

	public void setImage_url(String image_url) {
		this.image_url = image_url;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getViews() {
		return views;
	}

	public void setViews(int views) {
		this.views = views;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public MongoDate getDate_uploaded() {
		MongoDate md = new MongoDate(date_uploaded);
		return md;
	}

	public void setDate_uploaded(Date date_uploaded) {
		this.date_uploaded = date_uploaded;
	}

}
