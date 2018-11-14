package pojo.mongo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageAnalysis {
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

	private Concepts concepts;

	public static class Concepts {
		private double sky;
		private double sun;
		private double clouds;

		public Concepts() {

		}

		public double getSky() {
			return sky;
		}

		public void setSky(double sky) {
			this.sky = sky;
		}

		public double getSun() {
			return sun;
		}

		public void setSun(double sun) {
			this.sun = sun;
		}

		public double getClouds() {
			return clouds;
		}

		public void setClouds(double clouds) {
			this.clouds = clouds;
		}

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
