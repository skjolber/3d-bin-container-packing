package com.github.skjolber.packing.api.project;

public class ProjectionCoordinate {

	private final int x;
	private final int y;
	private final int dx;
	private final int dy;
	
	private final int value;

	public ProjectionCoordinate(int x, int dx, int y, int dy, int value) {
		super();
		this.x = x;
		this.y = y;
		this.dx = dx;
		this.dy = dy;
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}

	public int getDx() {
		return dx;
	}
	
	public int getDy() {
		return dy;
	}
	
	public boolean intersects(int startX, int endX, int startY, int endY) {
		return intersects(startX, endX, startY, endY);
	}
	
	public boolean intersectsY(int startY, int endY) {
		if (startY <= y & y <= endY) {
			return true;
		}

		return startY <= y + dy - 1 &&
				y + dy - 1 <= endY;
	}

	public boolean intersectsX(int startX, int endX) {
		if (startX <= x && x <= endX) {
			return true;
		}

		return startX <= x + dx - 1 &&
				x + dx - 1 <= endX;
	}	
	
}
