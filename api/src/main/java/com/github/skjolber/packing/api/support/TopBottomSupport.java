package com.github.skjolber.packing.api.support;

import com.github.skjolber.packing.api.Placement;

public class TopBottomSupport {

	protected final Placement top;
	protected final Placement bottom;
	
	protected final int minX;
	protected final int minY;

	protected final int maxY;
	protected final int maxX;

	protected final long area;
	
	protected long weight;
	
	public TopBottomSupport(int minX, int minY, int maxX, int maxY, Placement top, Placement bottom) {
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;

		this.top = top;
		this.bottom = bottom;

		int dx = maxX - minX + 1;
		int dy = maxY - minY + 1;
		
		this.area = (long) dx * (long) dy;
	}
	
	public Placement getBottom() {
		return bottom;
	}
	
	public Placement getTop() {
		return top;
	}
	
	public long getArea() {
		return area;
	}
	
	public long getWeight() {
		return weight;
	}

	public void setWeight(long weight) {
		this.weight = weight;
	}
	
	public void redistributeWeight(long weight, long area, int factor) {
		this.weight = (weight * this.area * factor) / area;
	}
}
