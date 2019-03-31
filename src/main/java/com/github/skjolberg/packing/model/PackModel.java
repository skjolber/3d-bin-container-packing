package com.github.skjolberg.packing.model;

import java.util.List;

public class PackModel {

	private final List<PlacementModel> placement;
	private final int weight;
	private final int width;
	private final int length;
	private final int height;
	private final long volume;
	private final String name;

	public PackModel(List<PlacementModel> placement, int weight, int width, int length, int height, long volume, String name) {
		this.placement = placement;
		this.weight = weight;
		this.width = width;
		this.length = length;
		this.height = height;
		this.volume = volume;
		this.name = name;
	}

	public List<PlacementModel> getPlacement() {
		return placement;
	}

	public int getWeight() {
		return weight;
	}

	public int getWidth() {
		return width;
	}

	public int getLength() {
		return length;
	}

	public int getHeight() {
		return height;
	}

	public long getVolume() {
		return volume;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "PackResult{" +
			", placement=" + placement +
			", weight=" + weight +
			", width=" + width +
			", length=" + length +
			", height=" + height +
			", volume=" + volume +
			", name='" + name + '\'' +
			'}';
	}
}
