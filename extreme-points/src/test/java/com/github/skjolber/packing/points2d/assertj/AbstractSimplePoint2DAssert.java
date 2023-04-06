package com.github.skjolber.packing.points2d.assertj;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.ep.Point2D;
import com.github.skjolber.packing.ep.points2d.SimplePoint2D;
import com.github.skjolber.packing.ep.points2d.XSupportPoint2D;
import com.github.skjolber.packing.ep.points2d.YSupportPoint2D;
import com.github.skjolber.packing.test.assertj.AbstractPoint2DAssert;

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
				Placement2D support = ySupportPoint2D.getYSupport();

				failWithMessage("Expected y support at " + y + ", was " + support.getAbsoluteY() + " to " + support.getAbsoluteEndY());
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
				Placement2D support = ySupportPoint2D.getYSupport();

				failWithMessage("Expected no y support at " + y + ", was " + support.getAbsoluteY() + " to " + support.getAbsoluteEndY());
			}
		}
		return myself;
	}

	public SELF isXSupport(int x) {
		isNotNull();
		if(!actual.isXSupport(x)) {
			if(actual instanceof XSupportPoint2D) {
				XSupportPoint2D ySupportPoint2D = (XSupportPoint2D)actual;
				Placement2D support = ySupportPoint2D.getXSupport();

				failWithMessage("Expected x support at " + x + ", was " + support.getAbsoluteX() + " to " + support.getAbsoluteEndX());
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
				XSupportPoint2D ySupportPoint2D = (XSupportPoint2D)actual;
				Placement2D support = ySupportPoint2D.getXSupport();

				failWithMessage("Expected no x support at " + x + ", was " + support.getAbsoluteX() + " to " + support.getAbsoluteEndX());
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

	public SELF isMaxXSupport(int x) {
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

}
