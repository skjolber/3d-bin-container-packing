package com.github.skjolberg.packing.model;

public class PlacementModel {

	private final String name;
	private final int x, y, z;

	private final int width;
	private final int length;
	private final int height;

	public PlacementModel(String name, int x, int y, int z, int width, int length, int height) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.width = width;
		this.length = length;
		this.height = height;
	}

	public String getName() {
		return name;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
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

	@Override
	public String toString() {
		return "PlacementModel{" +
			"name='" + name + '\'' +
			", x=" + x +
			", y=" + y +
			", z=" + z +
			", width=" + width +
			", length=" + length +
			", height=" + height +
			'}';
	}
}
