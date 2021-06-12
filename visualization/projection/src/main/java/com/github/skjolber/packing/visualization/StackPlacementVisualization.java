package com.github.skjolber.packing.visualization;

public class StackPlacementVisualization extends AbstractVisualization {

	private int x;
	private int y;
	private int z;
	
	private StackableVisualization stackable;

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

	public void setStackable(StackableVisualization stackable) {
		this.stackable = stackable;
	}

	public StackableVisualization getStackable() {
		return stackable;
	}
}
