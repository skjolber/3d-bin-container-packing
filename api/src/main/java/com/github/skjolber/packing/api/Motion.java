package com.github.skjolber.packing.api;

public class Motion {

	private Acceleration x;
	private Acceleration y;
	private Acceleration z;
	
	public void setX(Acceleration x) {
		this.x = x;
	}
	
	public void setY(Acceleration y) {
		this.y = y;
	}
	
	public void setZ(Acceleration z) {
		this.z = z;
	}
	
	public Acceleration getX() {
		return x;
	}
	
	public Acceleration getY() {
		return y;
	}
	
	public Acceleration getZ() {
		return z;
	}
}
