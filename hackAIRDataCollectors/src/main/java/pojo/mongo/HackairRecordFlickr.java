package pojo.mongo;

public class HackairRecordFlickr extends HackairRecordBase {

	protected SourceInfoFlickr source_info;

	public HackairRecordFlickr() {
		super("flickr");
	}

	public SourceInfoFlickr getSource_info() {
		return source_info;
	}

	public void setSource_info(SourceInfoFlickr source_info) {
		this.source_info = source_info;
	}

}
