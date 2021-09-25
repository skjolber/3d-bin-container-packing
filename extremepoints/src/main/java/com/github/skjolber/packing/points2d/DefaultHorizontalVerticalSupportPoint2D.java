package com.github.skjolber.packing.points2d;

public class DefaultHorizontalVerticalSupportPoint2D extends Point2D implements HorizontalSupportPoint2D, VerticalSupportPoint2D {

	/** range constrained to current minY */
	private final int horizontalSupportMinX;
	private final int horizontalSupportMaxX;
	
	/** range constrained to current minX */
	private int verticalSupportMinY;
	private int verticalSupportMaxY;
	
	public DefaultHorizontalVerticalSupportPoint2D(int minX, int minY, int maxX, int maxY, int horizontalSupportMinX, int horizontalSupportMaxX, int verticalSupportMinY, int verticalSupportMaxY) {
		super(minX, minY, maxX, maxY);
		
		if(minX < 0) {
			throw new RuntimeException();
		}
		if(minY < 0) {
			throw new RuntimeException();
		}
		if(maxX < 0) {
			throw new RuntimeException();
		}
		if(maxY < 0) {
			throw new RuntimeException();
		}
		if(horizontalSupportMaxX < 0) {
			throw new RuntimeException();
		}
		if(horizontalSupportMaxX < 0) {
			throw new RuntimeException();
		}
		if(verticalSupportMinY < 0) {
			throw new RuntimeException();
		}
		if(verticalSupportMaxY < 0) {
			throw new RuntimeException();
		}
		
		this.horizontalSupportMinX = horizontalSupportMinX;
		this.horizontalSupportMaxX = horizontalSupportMaxX;
		this.verticalSupportMinY = verticalSupportMinY;
		this.verticalSupportMaxY = verticalSupportMaxY;
	}

	@Override
	public boolean isVerticalSupport(int y) {
		return verticalSupportMinY <= y && y <= verticalSupportMaxY;
	}

	@Override
	public boolean isHorizontalSupport(int x) {
		return horizontalSupportMinX <= x && x <= horizontalSupportMaxX;
	}

	public int getHorizontalSupportMinX() {
		return horizontalSupportMinX;
	}

	public int getHorizontalSupportMaxX() {
		return horizontalSupportMaxX;
	}

	public int getVerticalSupportMinY() {
		return verticalSupportMinY;
	}
	
	public int getVeriftalSupportMaxY() {
		return verticalSupportMaxY;
	}

}
