package com.github.skjolber.packing.visualizer.api.packaging;

public class StackPlacementVisualizer extends AbstractVisualizer {

	private int x;
	private int y;
	private int z;
	
	private StackableVisualizer stackable;

	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public int getZ() {
		return z;
	}
	public void setZ(int z) {
		this.z = z;
	}

	public void setStackable(StackableVisualizer stackable) {
		this.stackable = stackable;
	}

	public StackableVisualizer getStackable() {
		return stackable;
	}
}
