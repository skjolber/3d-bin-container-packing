package com.github.skjolber.packing.visualization;

import java.util.ArrayList;
import java.util.List;

public class ContainerVisualization extends AbstractVisualization {

	private List<BoxVisualization> boxes = new ArrayList<>();

	public boolean add(BoxVisualization e) {
		return boxes.add(e);
	}
	
	public List<BoxVisualization> getBoxes() {
		return boxes;
	}
	
	public void setBoxes(List<BoxVisualization> boxes) {
		this.boxes = boxes;
	}
}
