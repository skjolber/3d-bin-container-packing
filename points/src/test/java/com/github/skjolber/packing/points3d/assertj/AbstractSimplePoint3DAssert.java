package com.github.skjolber.packing.points3d.assertj;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.ep.points3d.XYPlanePoint3D;
import com.github.skjolber.packing.ep.points3d.XZPlanePoint3D;
import com.github.skjolber.packing.ep.points3d.YZPlanePoint3D;
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
			if(actual instanceof XYPlanePoint3D) {
				XYPlanePoint3D support = (XYPlanePoint3D)actual;

				failWithMessage("Expected xy support at " + x + "x" + y + ", was " + support.getSupportedXYPlaneMaxX() + "x" + support.getSupportedXYPlaneMaxY() + " for " + actual);

			} else {
				failWithMessage("Expected xy support at " + x + "x" + y + ", was none for " + actual);
			}
		}
		return myself;
	}

	public SELF isXZSupportAt(int x, int z) {
		isNotNull();
		if(!actual.isSupportedXZPlane(x, z)) {
			if(actual instanceof XZPlanePoint3D) {
				XZPlanePoint3D support = (XZPlanePoint3D)actual;

				failWithMessage("Expected xz support at " + x + "x" + z + ", was " + support.getSupportedXZPlaneMaxX() + "x" + support.getSupportedXZPlaneMaxZ() + " for " + actual);
			} else {
				failWithMessage("Expected xz support at " + x + "x" + z + ", was none for " + actual);
			}
		}
		return myself;
	}

	public SELF isYZSupportAt(int y, int z) {
		isNotNull();
		if(!actual.isSupportedYZPlane(y, z)) {
			if(actual instanceof YZPlanePoint3D) {
				YZPlanePoint3D support = (YZPlanePoint3D)actual;

				failWithMessage("Expected yz support at " + y + "x" + z + ", was " + support.getSupportedYZPlaneMaxY() + "x" + support.getSupportedYZPlaneMaxZ() + " for " + actual);

			} else {
				failWithMessage("Expected yz support at " + y + "x" + z + ", was none for " + actual);
			}
		}
		return myself;
	}

	public SELF isNoXYSupportAt(int x, int y) {
		isNotNull();
		if(actual.isSupportedXYPlane(x, y)) {
			if(actual instanceof XYPlanePoint3D) {
				XYPlanePoint3D support = (XYPlanePoint3D)actual;

				failWithMessage("Expected no xy support at " + x + "x" + y + ", was " + support.getSupportedXYPlaneMaxX() + "x" + support.getSupportedXYPlaneMaxY() + " for " + actual);
			}
		}
		return myself;
	}

	public SELF isNoXZSupportAt(int x, int z) {
		isNotNull();
		if(actual.isSupportedXZPlane(x, z)) {
			if(actual instanceof XZPlanePoint3D) {
				XZPlanePoint3D support = (XZPlanePoint3D)actual;

				failWithMessage("Expected no xz support at " + x + "x" + z + ", was " + support.getSupportedXZPlaneMaxX() + "x" + support.getSupportedXZPlaneMaxZ() + " for " + actual);
			}
		}
		return myself;
	}

	public SELF isNoYZSupportAt(int y, int z) {
		isNotNull();
		if(actual.isSupportedYZPlane(y, z)) {
			if(actual instanceof YZPlanePoint3D) {
				YZPlanePoint3D support = (YZPlanePoint3D)actual;

				failWithMessage("Expected no yz support at " + y + "x" + z + ", was " + support.getSupportedYZPlaneMaxY() + "x" + support.getSupportedYZPlaneMaxZ() + " for " + actual);
			}
		}
		return myself;
	}

}
