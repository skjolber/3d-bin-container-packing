package com.github.skjolber.packing.test.assertj;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.ep.Point2D;
import com.github.skjolber.packing.api.ep.XSupportPoint2D;
import com.github.skjolber.packing.api.ep.YSupportPoint2D;

public abstract class AbstractPoint2DAssert<SELF extends AbstractPoint2DAssert<SELF, ACTUAL>, ACTUAL extends Point2D>
extends AbstractObjectAssert<SELF, ACTUAL> {

	protected AbstractPoint2DAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF isMinX(int x) {
		isNotNull();
		if (actual.getMinX() != x) {
			failWithMessage("Expected x at " + x);
		}
		return myself;
	}

	public SELF isMinY(int y) {
		isNotNull();
		if (actual.getMinY() != y) {
			failWithMessage("Expected y at " + y);
		}
		return myself;
	}

	public SELF isMin(int x, int y) {
		isNotNull();
		if (actual.getMinX() != x) {
			failWithMessage("Expected x " + x + ", not " + actual.getMinX() + " (was " + getCoordinates() + ")");
		}
		if (actual.getMinY() != y) {
			failWithMessage("Expected y " + y + ", not " + actual.getMinY()+ " (was " + getCoordinates() + ")");
		}
		return myself;
	}
	
	public SELF isMax(int x, int y) {
		isNotNull();
		if (actual.getMaxX() != x) {
			failWithMessage("Expected x " + x + ", not " + actual.getMaxX() + " (was " + getCoordinates() + ")");
		}
		if (actual.getMaxY() != y) {
			failWithMessage("Expected y " + y + ", not " + actual.getMaxY()+ " (was " + getCoordinates() + ")");
		}
		return myself;
	}
	
	public SELF isMaxX(int x) {
		isNotNull();
		if (actual.getMaxX() != x) {
			failWithMessage("Expected x " + x + ", not " + actual.getMaxX() + " (was " + getCoordinates() + ")");
		}
		return myself;
	}

	public SELF isMaxY(int y) {
		isNotNull();
		if (actual.getMaxY() != y) {
			failWithMessage("Expected y " + y + ", not " + actual.getMaxY()+ " (was " + getCoordinates() + ")");
		}
		return myself;
	}
	
	public SELF isYSupportAt(int y) {
		isNotNull();
		if (!actual.isYSupport(y)) {
			if(actual instanceof YSupportPoint2D) {
				YSupportPoint2D ySupportPoint2D = (YSupportPoint2D)actual;
				Placement2D support = ySupportPoint2D.getYSupport();
				
				if(support.getAbsoluteEndY() != y) {
					failWithMessage("Expected y support at " + y + ", was " + support.getAbsoluteEndY());
				}
			} else {
				failWithMessage("Expected y support at " + y);
			}
		}
		return myself;
	}

	public SELF isXSupportAt(int x) {
		isNotNull();
		if(actual instanceof XSupportPoint2D) {
			XSupportPoint2D ySupportPoint2D = (XSupportPoint2D)actual;
			Placement2D support = ySupportPoint2D.getXSupport();
			
			if(support.getAbsoluteEndX() != x) {
				failWithMessage("Expected x support at " + x + ", was " + support.getAbsoluteEndX());
			}
		} else {
			failWithMessage("Expected x support at " + x + ", was none for " + actual);
		}
		return myself;
	}

	public SELF isSupport(int x, int y) {
		isXSupportAt(x);
		isYSupportAt(y);
		return myself;
	}

	public SELF isYSupport(int y) {
		isNotNull();
		if(actual instanceof YSupportPoint2D) {
			YSupportPoint2D ySupportPoint2D = (YSupportPoint2D)actual;
			Placement2D ySupport = ySupportPoint2D.getYSupport();
			if(ySupport.getAbsoluteEndY() != y) {
				failWithMessage("Expected y support limit " + y + ", was " + ySupport.getAbsoluteEndY());
			}
		} else {
			failWithMessage("No y support, expected " + y + ", was none for " + actual);
		}
		return myself;
	}
	
	public SELF isNoYSupport() {
		isNotNull();
		if(actual instanceof YSupportPoint2D) {
			YSupportPoint2D ySupportPoint2D = (YSupportPoint2D)actual;
			Placement2D support = ySupportPoint2D.getYSupport();
			failWithMessage("Expected no y support, was " + support.getAbsoluteEndY());
		}
		return myself;
	}

	public SELF isNoXSupport() {
		isNotNull();
		if(actual instanceof XSupportPoint2D) {
			XSupportPoint2D xSupportPoint2D = (XSupportPoint2D)actual;
			Placement2D support = xSupportPoint2D.getXSupport();
			failWithMessage("Expected no x support, was " + support.getAbsoluteEndX());
		}
		return myself;
	}


	public SELF isXSupport(int x) {
		isNotNull();
		if(actual instanceof XSupportPoint2D) {
			XSupportPoint2D xSupportPoint2D = (XSupportPoint2D)actual;
			Placement2D ySupport = xSupportPoint2D.getXSupport();
			if(ySupport.getAbsoluteEndX() != x) {
				failWithMessage("Expected x support limit " + x + ", was " + ySupport.getAbsoluteEndX());
			}
		} else {
			failWithMessage("No x support, expected " + x);
		}
		return myself;
	}
			
	private String getEndCoordinates() {
		return actual.getMaxX() + "x" + actual.getMaxY();
	}

	private String getStartCoordinates() {
		return actual.getMinX() + "x" + actual.getMinY();
	}

	private String getCoordinates() {
		return getStartCoordinates() + " " + getEndCoordinates();
	}
	
	
	protected boolean isOverlapX(Point2D placement) {
		
		if(placement.getMinX() <= actual.getMinX() && actual.getMinX() <= placement.getMaxX()) {
			return true;
		}
		
		if(placement.getMinX() <= actual.getMaxX() && actual.getMaxX() <= placement.getMaxX()) {
			return true;
		}
		
		return false;
	}

	protected boolean isOverlapY(Point2D placement) {
		if(placement.getMinY() <= actual.getMinY() && actual.getMinY() <= placement.getMaxY()) {
			return true;
		}
		
		if(placement.getMinY() <= actual.getMaxY() && actual.getMaxY() <= placement.getMaxY()) {
			return true;
		}
		
		return false;
	}

	public SELF isAlongsideY(Point2D other) {
		isNotNull();
		
		if(!isOverlapX(other)) {
			failWithMessage("Not overlapping in x dimension");
		}
		if (actual.getMaxY() + 1 != other.getMinY() && other.getMaxY() + 1 != actual.getMinY()) {
			failWithMessage("Expected start y at " + (other.getMaxY() + 1) + " or end y at " + (other.getMinY() - 1));
		}
		return myself;
	}
	
	public SELF isAlongsideX(Point2D other) {
		isNotNull();
		
		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
		}

		if (actual.getMaxX() + 1 != other.getMinX() && other.getMaxX() + 1 != actual.getMinX()) {
			failWithMessage("Expected start x at " + (other.getMaxX() + 1) + " or end x at " + (other.getMinX() - 1));
		}
		return myself;
	}
	
	public SELF followsAlongsideX(Point2D other) {
		isNotNull();
		
		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
		}
		
		if (other.getMaxX() + 1 != actual.getMinX()) {
			failWithMessage("Expected start x at " + (other.getMaxX() + 1));
		}
		return myself;
	}
	
	public SELF followsAlongsideY(Point2D other) {
		isNotNull();
		
		if(!isOverlapX(other)) {
			failWithMessage("Not overlapping in x dimension");
		}
		
		if (other.getMaxY() + 1 != actual.getMinY()) {
			failWithMessage("Expected start y at " + (other.getMaxY() + 1));
		}
		return myself;
	}
	
	public SELF followsAlongsideZ(Point2D other) {
		isNotNull();
		
		if(!isOverlapX(other)) {
			failWithMessage("Not overlapping in x dimension");
		}
		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
		}
		
		return myself;
	}

	public SELF preceedsAlongsideY(Point2D other) {
		isNotNull();
		
		if(!isOverlapX(other)) {
			failWithMessage("Not overlapping in x dimension");
		}
		
		if (actual.getMaxY() + 1 != other.getMinY()) {
			failWithMessage("Expected end y at " + (other.getMinY() - 1));
		}
		return myself;
	}
	
	public SELF preceedsAlongsideX(Point2D other) {
		isNotNull();
		
		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
		}
		
		if (actual.getMaxX() + 1 != other.getMinX() ) {
			failWithMessage("Expected end x at " + (other.getMinX() - 1));
		}
		return myself;
	}
	
	public SELF preceedsAlongsideZ(Point2D other) {
		isNotNull();
		
		if(!isOverlapX(other)) {
			failWithMessage("Not overlapping in x dimension");
		}
		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
		}
		
		return myself;
	}
	
}
