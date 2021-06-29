package com.github.skjolber.packing.projection;

import java.util.ArrayList;
import java.util.List;

public class StackVisualization extends AbstractVisualization {

	private List<StackPlacementVisualization> placements = new ArrayList<>();

	public boolean add(StackPlacementVisualization e) {
		return placements.add(e);
	}
	
	public List<StackPlacementVisualization> getPlacements() {
		return placements;
	}
	
	public void setPlacements(List<StackPlacementVisualization> stackable) {
		this.placements = stackable;
	}

	
}
