package com.github.skjolber.packing.points3d.assertj;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.test.assertj.AbstractPoint3DAssert;

@SuppressWarnings("rawtypes")
public abstract class AbstractSimplePoint3DAssert<SELF extends AbstractSimplePoint3DAssert<SELF, ACTUAL>, ACTUAL extends SimplePoint3D>
		extends AbstractPoint3DAssert<SELF, ACTUAL> {

	protected AbstractSimplePoint3DAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF isXYSupportAt(int x, int y) {
		isNotNull();
		if(!actual.isSupportedXYPlane(x, y)) {
			if(actual instanceof DefaultPoint3D support && support.isSupportedXYPlane()) {

				failWithMessage("Expected xy support at " + x + "x" + y + ", was " + support.getXYPlane().getAbsoluteEndX() + "x" + support.getXYPlane().getAbsoluteEndY() + " for " + actual);

			} else {
				failWithMessage("Expected xy support at " + x + "x" + y + ", was none for " + actual);
			}
		}
		return myself;
	}

	public SELF isXZSupportAt(int x, int z) {
		isNotNull();
		if(!actual.isSupportedXZPlane(x, z)) {
			if(actual instanceof DefaultPoint3D support && support.isSupportedXZPlane()) {

				failWithMessage("Expected xz support at " + x + "x" + z + ", was " + support.getXZPlane().getAbsoluteEndX() + "x" + support.getXZPlane().getAbsoluteEndZ() + " for " + actual);
			} else {
				failWithMessage("Expected xz support at " + x + "x" + z + ", was none for " + actual);
			}
		}
		return myself;
	}

	public SELF isYZSupportAt(int y, int z) {
		isNotNull();
		if(!actual.isSupportedYZPlane(y, z)) {
			if(actual instanceof DefaultPoint3D support && support.isSupportedYZPlane()) {

				failWithMessage("Expected yz support at " + y + "x" + z + ", was " + support.getYZPlane().getAbsoluteEndY() + "x" + support.getYZPlane().getAbsoluteEndZ() + " for " + actual);

			} else {
				failWithMessage("Expected yz support at " + y + "x" + z + ", was none for " + actual);
			}
		}
		return myself;
	}

	public SELF isNoXYSupportAt(int x, int y) {
		isNotNull();
		if(actual.isSupportedXYPlane(x, y)) {
			if(actual instanceof DefaultPoint3D support && support.isSupportedXYPlane()) {

				failWithMessage("Expected no xy support at " + x + "x" + y + ", was " + support.getXYPlane().getAbsoluteEndX() + "x" + support.getXYPlane().getAbsoluteEndY() + " for " + actual);
			}
		}
		return myself;
	}

	public SELF isNoXZSupportAt(int x, int z) {
		isNotNull();
		if(actual.isSupportedXZPlane(x, z)) {
			if(actual instanceof DefaultPoint3D support && support.isSupportedXZPlane()) {

				failWithMessage("Expected no xz support at " + x + "x" + z + ", was " + support.getXZPlane().getAbsoluteEndX() + "x" + support.getXZPlane().getAbsoluteEndZ() + " for " + actual);
			}
		}
		return myself;
	}

	public SELF isNoYZSupportAt(int y, int z) {
		isNotNull();
		if(actual.isSupportedYZPlane(y, z)) {
			if(actual instanceof DefaultPoint3D support && support.isSupportedYZPlane()) {

				failWithMessage("Expected no yz support at " + y + "x" + z + ", was " + support.getYZPlane().getAbsoluteEndY() + "x" + support.getYZPlane().getAbsoluteEndZ() + " for " + actual);
			}
		}
		return myself;
	}

}
