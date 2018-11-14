package pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Concepts {
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