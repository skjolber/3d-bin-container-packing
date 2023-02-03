package com.github.skjolber.packing.visualizer.api.packaging;

import java.util.ArrayList;
import java.util.List;

public class StackPlacementVisualizer extends AbstractVisualizer {

	private int x;
	private int y;
	private int z;

	private StackableVisualizer stackable;

	private List<PointVisualizer> points = new ArrayList<>();

	public List<PointVisualizer> getPoints() {
		return points;
	}

	public void setPoints(List<PointVisualizer> points) {
		this.points = points;
	}

	public void add(PointVisualizer p) {
		this.points.add(p);
	}

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
