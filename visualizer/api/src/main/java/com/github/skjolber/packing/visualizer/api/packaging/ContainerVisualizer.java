package com.github.skjolber.packing.visualizer.api.packaging;

public class ContainerVisualizer extends StackableVisualizer {

	private int loadDx;
	private int loadDy;
	private int loadDz;

	private StackVisualizer stack;

	/** Centre of gravity of the load, or null if it weighs nothing */
	private Double centerOfGravityX;
	private Double centerOfGravityY;
	private Double centerOfGravityZ;

	public Double getCenterOfGravityX() {
		return centerOfGravityX;
	}

	public void setCenterOfGravityX(Double centerOfGravityX) {
		this.centerOfGravityX = centerOfGravityX;
	}

	public Double getCenterOfGravityY() {
		return centerOfGravityY;
	}

	public void setCenterOfGravityY(Double centerOfGravityY) {
		this.centerOfGravityY = centerOfGravityY;
	}

	public Double getCenterOfGravityZ() {
		return centerOfGravityZ;
	}

	public void setCenterOfGravityZ(Double centerOfGravityZ) {
		this.centerOfGravityZ = centerOfGravityZ;
	}

	private int emptyWeight;
	private int maxLoadWeight;
	private long loadWeight;
	private long maxLoadVolume;
	private long loadVolume;

	public int getEmptyWeight() {
		return emptyWeight;
	}

	public void setEmptyWeight(int emptyWeight) {
		this.emptyWeight = emptyWeight;
	}

	public int getMaxLoadWeight() {
		return maxLoadWeight;
	}

	public void setMaxLoadWeight(int maxLoadWeight) {
		this.maxLoadWeight = maxLoadWeight;
	}

	public long getLoadWeight() {
		return loadWeight;
	}

	public void setLoadWeight(long loadWeight) {
		this.loadWeight = loadWeight;
	}

	public long getMaxLoadVolume() {
		return maxLoadVolume;
	}

	public void setMaxLoadVolume(long maxLoadVolume) {
		this.maxLoadVolume = maxLoadVolume;
	}

	public long getLoadVolume() {
		return loadVolume;
	}

	public void setLoadVolume(long loadVolume) {
		this.loadVolume = loadVolume;
	}

	private String type = "container";

	public int getLoadDx() {
		return loadDx;
	}

	public void setLoadDx(int loadDx) {
		this.loadDx = loadDx;
	}

	public int getLoadDy() {
		return loadDy;
	}

	public void setLoadDy(int loadDy) {
		this.loadDy = loadDy;
	}

	public int getLoadDz() {
		return loadDz;
	}

	public void setLoadDz(int loadDz) {
		this.loadDz = loadDz;
	}

	public StackVisualizer getStack() {
		return stack;
	}

	public void setStack(StackVisualizer stack) {
		this.stack = stack;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
