package com.github.skjolber.packing.visualizer.api.packaging;

public abstract class StackableVisualizer extends AbstractVisualizer {

	private String id;
	private String name;

	private int dx;
	private int dy;
	private int dz;

	public int getDx() {
		return dx;
	}
	public void setDx(int dx) {
		this.dx = dx;
	}
	public int getDy() {
		return dy;
	}
	public void setDy(int dy) {
		this.dy = dy;
	}
	public int getDz() {
		return dz;
	}
	public void setDz(int dz) {
		this.dz = dz;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

}
