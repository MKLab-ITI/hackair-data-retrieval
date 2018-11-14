package pojo.responses;

public class PathMask {
	public String getSl_error() {
		return sl_error;
	}

	public void setSl_error(String sl_error) {
		this.sl_error = sl_error;
	}

	private String path;
	private String mask;
	private String sl_error;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getMask() {
		return mask;
	}

	public void setMask(String mask) {
		this.mask = mask;
	}

}
