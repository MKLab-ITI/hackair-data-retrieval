package pojo;

import java.net.MalformedURLException;
import java.net.URL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Image {

	private String path;

	public boolean pathIsURL() {
		boolean isURL = false;
		try {
			new URL(path); // just to check if valid url
			isURL = true;
		} catch (MalformedURLException e) { // not a URL
			// e.printStackTrace();
		}
		return isURL;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	private String id;

	private Concepts concepts;

	private boolean containsSky;
	private boolean usableSky;
	private int all_pixels;
	private int sky_pixels;

	@JsonProperty("R/G")
	private double RG;

	@JsonProperty("G/B")
	private double GB;

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
