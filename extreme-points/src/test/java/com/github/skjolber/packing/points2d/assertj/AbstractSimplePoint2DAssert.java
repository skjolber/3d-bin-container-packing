package com.github.skjolber.packing.points2d.assertj;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.ep.points2d.Point2D;
import com.github.skjolber.packing.ep.points2d.SimplePoint2D;
import com.github.skjolber.packing.ep.points2d.XSupportPoint2D;
import com.github.skjolber.packing.ep.points2d.YSupportPoint2D;

@SuppressWarnings("rawtypes")
public abstract class AbstractSimplePoint2DAssert<SELF extends AbstractSimplePoint2DAssert<SELF, ACTUAL>, ACTUAL extends SimplePoint2D>
		extends AbstractPoint2DAssert<SELF, ACTUAL> {

	protected AbstractSimplePoint2DAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}


	public SELF isYSupport(int y) {
		isNotNull();
		if(!actual.isYSupport(y)) {
			if(actual instanceof YSupportPoint2D) {
				YSupportPoint2D ySupportPoint2D = (YSupportPoint2D)actual;

				failWithMessage("Expected y support at " + y + ", was " + ySupportPoint2D.getSupportedMinY() + " to " + ySupportPoint2D.getSupportedMaxY());
			} else {
				failWithMessage("Expected y support at " + y + ", was none for " + actual);
			}
		}
		return myself;
	}

	public SELF isNoYSupport(int y) {
		isNotNull();
		if(actual.isYSupport(y)) {
			if(actual instanceof YSupportPoint2D) {
				YSupportPoint2D ySupportPoint2D = (YSupportPoint2D)actual;

				failWithMessage("Expected no y support at " + y + ", was " + ySupportPoint2D.getSupportedMinY() + " to " + ySupportPoint2D.getSupportedMaxY());
			}
		}
		return myself;
	}

	public SELF isXSupport(int x) {
		isNotNull();
		if(!actual.isXSupport(x)) {
			if(actual instanceof XSupportPoint2D) {
				XSupportPoint2D xSupport = (XSupportPoint2D)actual;

				failWithMessage("Expected x support at " + x + ", was " + xSupport.getSupportedMinX() + " to " + xSupport.getSupportedMaxX());
			} else {
				failWithMessage("Expected x support at " + x + ", was none for " + actual);
			}
		}
		return myself;
	}

	public SELF isNoXSupport(int x) {
		isNotNull();
		if(actual.isXSupport(x)) {
			if(actual instanceof XSupportPoint2D) {
				XSupportPoint2D xSupport = (XSupportPoint2D)actual;

				failWithMessage("Expected no x support at " + x + ", was " + xSupport.getSupportedMinX() + " to " + xSupport.getSupportedMaxX());
			}
		}
		return myself;
	}

	public SELF isSupport(int x, int y) {
		isXSupport(x);
		isYSupport(y);
		return myself;
	}

	public SELF isMaxYSupport(int y) {
		isNotNull();
		if(actual instanceof YSupportPoint2D) {
			YSupportPoint2D ySupportPoint2D = (YSupportPoint2D)actual;
			if(ySupportPoint2D.getSupportedMaxY() != y) {
				failWithMessage("Expected y support limit " + y + ", was " + ySupportPoint2D.getSupportedMaxY());
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
			failWithMessage("Expected no y support, was " + ySupportPoint2D.getSupportedMaxY());
		}
		return myself;
	}

	public SELF isNoXSupport() {
		isNotNull();
		if(actual instanceof XSupportPoint2D) {
			XSupportPoint2D xSupportPoint2D = (XSupportPoint2D)actual;
			failWithMessage("Expected no x support, was " +  xSupportPoint2D.getSupportedMaxX());
		}
		return myself;
	}

	public SELF isMaxXSupport(int x) {
		isNotNull();
		if(actual instanceof XSupportPoint2D) {
			XSupportPoint2D xSupportPoint2D = (XSupportPoint2D)actual;
			if(xSupportPoint2D.getSupportedMaxX() != x) {
				failWithMessage("Expected x support limit " + x + ", was " + xSupportPoint2D.getSupportedMaxX());
			}
		} else {
			failWithMessage("No x support, expected " + x);
		}
		return myself;
	}
}
