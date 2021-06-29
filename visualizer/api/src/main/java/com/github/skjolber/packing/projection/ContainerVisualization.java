package com.github.skjolber.packing.projection;

public class ContainerVisualization extends StackableVisualization {

	private int loadDx;
	private int loadDy;
	private int loadDz;
	
	private StackVisualization stack;
	
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

	public StackVisualization getStack() {
		return stack;
	}
	
	public void setStack(StackVisualization stack) {
		this.stack = stack;
	}

	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
}
