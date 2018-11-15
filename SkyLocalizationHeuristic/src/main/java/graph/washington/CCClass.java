package graph.washington;

import java.util.ArrayList;
import java.util.List;

public class CCClass {

	int label;
	List<String> xy = new ArrayList<String>();
	List<Double> rgRatio = new ArrayList<Double>();
	List<Double> gbRatio = new ArrayList<Double>();

	public void setLabel(int label) {
		this.label = label;
	}

	public void setXY(List<String> xy) {
		this.xy = xy;
	}

	public void setRgRatio(List<Double> rgRatio) {
		this.rgRatio = rgRatio;
	}

	public void setGbRatio(List<Double> gbRatio) {
		this.gbRatio = gbRatio;
	}

	public int getLabel() {
		return label;
	}

	public List<String> getXY() {
		return xy;
	}

	public List<Double> getRgRatio() {
		return rgRatio;
	}

	public List<Double> getGbRatio() {
		return gbRatio;
	}

}
