package com.github.skjolber.packing.visualizer.api.packaging;

import java.util.ArrayList;
import java.util.List;

public class StackPlacementVisualizer extends AbstractVisualizer {

	private int x;
	private int y;
	private int z;

	private StackableVisualizer stackable;

	private List<PointVisualizer> points = new ArrayList<>();
	/** Area resting on boxes which count as support (the floor is not counted) */
	private long supportedArea;
	/** Total weight resting on the box, as calculated by the validators */
	private double loadWeight;

	public long getSupportedArea() {
		return supportedArea;
	}

	public void setSupportedArea(long supportedArea) {
		this.supportedArea = supportedArea;
	}

	public double getLoadWeight() {
		return loadWeight;
	}

	public void setLoadWeight(double loadWeight) {
		this.loadWeight = loadWeight;
	}

	/** Indexes of the validation reasons which concern this placement */
	private List<Integer> reasons = new ArrayList<>();

	public List<Integer> getReasons() {
		return reasons;
	}

	public void setReasons(List<Integer> reasons) {
		this.reasons = reasons;
	}

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
