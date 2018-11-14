package pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageAnalysis {
	private Concepts concepts;

	private String cd_error;
	private String sl_error;

	public String getCd_error() {
		return cd_error;
	}

	public void setCd_error(String cd_error) {
		this.cd_error = cd_error;
	}

	public String getSl_error() {
		return sl_error;
	}

	public void setSl_error(String sl_error) {
		this.sl_error = sl_error;
	}

	private boolean containsSky;
	private boolean usableSky;
	private int all_pixels;
	private int sky_pixels;

	@JsonProperty("R/G")
	private double RG;

	@JsonProperty("G/B")
	private double GB;

	public ImageAnalysis() {

	}

	public Concepts getConcepts() {
		return concepts;
	}

	public void setConcepts(Concepts concepts) {
		this.concepts = concepts;
	}

	public boolean isContainsSky() {
		return containsSky;
	}

	public void setContainsSky(boolean containsSky) {
		this.containsSky = containsSky;
	}

	public boolean isUsableSky() {
		return usableSky;
	}

	public void setUsableSky(boolean usableSky) {
		this.usableSky = usableSky;
	}

	public int getAll_pixels() {
		return all_pixels;
	}

	public void setAll_pixels(int all_pixels) {
		this.all_pixels = all_pixels;
	}

	public int getSky_pixels() {
		return sky_pixels;
	}

	public void setSky_pixels(int sky_pixels) {
		this.sky_pixels = sky_pixels;
	}

	public double getRG() {
		return RG;
	}

	public void setRG(double rG) {
		RG = rG;
	}

	public double getGB() {
		return GB;
	}

	public void setGB(double gB) {
		GB = gB;
	}

}
