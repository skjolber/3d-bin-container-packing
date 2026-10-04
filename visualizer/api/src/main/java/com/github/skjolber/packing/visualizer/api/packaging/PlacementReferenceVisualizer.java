package com.github.skjolber.packing.visualizer.api.packaging;

/**
 * Reference to a placement: the index of its container in the result, and its index in the container's stack.
 */
public class PlacementReferenceVisualizer {

	private int container;
	private int placement;

	public PlacementReferenceVisualizer() {
	}

	public PlacementReferenceVisualizer(int container, int placement) {
		this.container = container;
		this.placement = placement;
	}

	public int getContainer() {
		return container;
	}

	public void setContainer(int container) {
		this.container = container;
	}

	public int getPlacement() {
		return placement;
	}

	public void setPlacement(int placement) {
		this.placement = placement;
	}
}
