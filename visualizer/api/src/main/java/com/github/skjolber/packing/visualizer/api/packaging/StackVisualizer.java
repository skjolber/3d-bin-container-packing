package com.github.skjolber.packing.visualizer.api.packaging;

import java.util.ArrayList;
import java.util.List;

public class StackVisualizer extends AbstractVisualizer {

	private List<StackPlacementVisualizer> placements = new ArrayList<>();

	public boolean add(StackPlacementVisualizer e) {
		return placements.add(e);
	}
	
	public List<StackPlacementVisualizer> getPlacements() {
		return placements;
	}
	
	public void setPlacements(List<StackPlacementVisualizer> stackable) {
		this.placements = stackable;
	}

	
}
