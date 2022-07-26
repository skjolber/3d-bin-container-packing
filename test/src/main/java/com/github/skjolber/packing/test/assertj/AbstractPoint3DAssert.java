package com.github.skjolber.packing.test.assertj;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.api.ep.XYPlanePoint3D;
import com.github.skjolber.packing.api.ep.XZPlanePoint3D;
import com.github.skjolber.packing.api.ep.YZPlanePoint3D;

@SuppressWarnings("rawtypes")
public abstract class AbstractPoint3DAssert<SELF extends AbstractPoint3DAssert<SELF, ACTUAL>, ACTUAL extends Point3D>
extends AbstractObjectAssert<SELF, ACTUAL> {

	protected AbstractPoint3DAssert(ACTUAL actual, Class<?> selfType) {
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
	
	public SELF isMinZ(int z) {
		isNotNull();
		if (actual.getMinZ() != z) {
			failWithMessage("Expected z at " + z);
		}
		return myself;
	}

	public SELF isMin(int x, int y, int z) {
		isNotNull();
		if (actual.getMinX() != x) {
			failWithMessage("Expected x " + x + ", not " + actual.getMinX() + " (was " + getCoordinates() + ")");
		}
		if (actual.getMinY() != y) {
			failWithMessage("Expected y " + y + ", not " + actual.getMinY()+ " (was " + getCoordinates() + ")");
		}
		if (actual.getMinZ() != z) {
			failWithMessage("Expected z " + z + ", not " + actual.getMinZ()+ " (was " + getCoordinates() + ")");
		}
		return myself;
	}
	
	public SELF isMax(int x, int y, int z) {
		isNotNull();
		if (actual.getMaxX() != x) {
			failWithMessage("Expected x " + x + ", not " + actual.getMaxX() + " (was " + getCoordinates() + ")");
		}
		if (actual.getMaxY() != y) {
			failWithMessage("Expected y " + y + ", not " + actual.getMaxY()+ " (was " + getCoordinates() + ")");
		}
		if (actual.getMaxZ() != z) {
			failWithMessage("Expected z " + y + ", not " + actual.getMaxZ()+ " (was " + getCoordinates() + ")");
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
	
	public SELF isMaxZ(int z) {
		isNotNull();
		if (actual.getMaxZ() != z) {
			failWithMessage("Expected z " + z + ", not " + actual.getMaxZ()+ " (was " + getCoordinates() + ")");
		}
		return myself;
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
		
	
	public SELF isMaxXYSupport(int x, int y) {
		isNotNull();
		if(actual instanceof XYPlanePoint3D) {
			XYPlanePoint3D support = (XYPlanePoint3D)actual;

			if(support.getSupportedXYPlaneMaxX() != x || support.getSupportedXYPlaneMaxY() != y) {
				failWithMessage("Expected xy support at " + x + "x" + y + ", was " + support.getSupportedXYPlaneMaxX() + "x" + support.getSupportedXYPlaneMaxY() + " for " + actual);
			}
			
		} else {
			failWithMessage("Expected xy support at " + x + "x" + y + ", was none for " + actual);
		}
		return myself;
	}

	public SELF isMaxXZSupport(int x, int z) {
		isNotNull();
		if(actual instanceof XZPlanePoint3D) {
			XZPlanePoint3D support = (XZPlanePoint3D)actual;

			if(support.getSupportedXZPlaneMaxX() != x || support.getSupportedXZPlaneMaxZ() != z) {
				failWithMessage("Expected xz support at " + x + "x" + z + ", was " + support.getSupportedXZPlaneMaxX() + "x" + support.getSupportedXZPlaneMaxZ() + " for " + actual);
			}
		} else {
			failWithMessage("Expected xz support at " + x + "x" + z + ", was none for " + actual);
		}
		return myself;
	}

	public SELF isMaxYZSupport(int y, int z) {
		isNotNull();
		if(actual instanceof YZPlanePoint3D) {
			YZPlanePoint3D support = (YZPlanePoint3D)actual;

			if(support.getSupportedYZPlaneMaxY() != y || support.getSupportedYZPlaneMaxZ() != z) {
				failWithMessage("Expected yz support at " + y + "x" + z + ", was " + support.getSupportedYZPlaneMaxY() + "x" + support.getSupportedYZPlaneMaxZ() + " for " + actual);
			}
			
		} else {
			failWithMessage("Expected yz support at " + y + "x" + z + ", was none for " + actual);
		}
		return myself;
	}
	
	
	public SELF isNoXYSupport() {
		isNotNull();
		if(actual instanceof XYPlanePoint3D) {
			XYPlanePoint3D support = (XYPlanePoint3D)actual;
			
			failWithMessage("Expected no xy support, was " + support.getSupportedXYPlaneMaxX() + "x" + support.getSupportedXYPlaneMaxY());
		}
		return myself;
	}

	public SELF isNoXZSupport() {
		isNotNull();
		if(actual instanceof XZPlanePoint3D) {
			XZPlanePoint3D support = (XZPlanePoint3D)actual;
			
			failWithMessage("Expected no xz support, was " + support.getSupportedXZPlaneMaxX() + "x" + support.getSupportedXZPlaneMaxZ());
		}
		return myself;
	}
	
	public SELF isNoYZSupport() {
		isNotNull();
		if(actual instanceof YZPlanePoint3D) {
			YZPlanePoint3D support = (YZPlanePoint3D)actual;
			
			failWithMessage("Expected no yz support, was " + support.getSupportedYZPlaneMaxY() + "x" + support.getSupportedYZPlaneMaxZ());
		}
		return myself;
	}
			
	private String getEndCoordinates() {
		return actual.getMaxX() + "x" + actual.getMaxY() + "x" + actual.getMaxZ();
	}

	private String getStartCoordinates() {
		return actual.getMinX() + "x" + actual.getMinY() + "x" + actual.getMinZ();
	}

	private String getCoordinates() {
		return getStartCoordinates() + " " + getEndCoordinates();
	}
	
	
	protected boolean isOverlapX(Point3D placement) {
		
		if(placement.getMinX() <= actual.getMinX() && actual.getMinX() <= placement.getMaxX()) {
			return true;
		}
		
		if(placement.getMinX() <= actual.getMaxX() && actual.getMaxX() <= placement.getMaxX()) {
			return true;
		}
		
		return false;
	}

	protected boolean isOverlapY(Point3D placement) {
		if(placement.getMinY() <= actual.getMinY() && actual.getMinY() <= placement.getMaxY()) {
			return true;
		}
		
		if(placement.getMinY() <= actual.getMaxY() && actual.getMaxY() <= placement.getMaxY()) {
			return true;
		}
		
		return false;
	}

	protected boolean isOverlapZ(Point3D placement) {
		if(placement.getMinZ() <= actual.getMinZ() && actual.getMinZ() <= placement.getMaxZ()) {
			return true;
		}
		
		if(placement.getMinZ() <= actual.getMaxZ() && actual.getMaxZ() <= placement.getMaxZ()) {
			return true;
		}
		
		return false;
	}

	public SELF isAlongsideY(Point3D other) {
		isNotNull();
		
		if(!isOverlapX(other)) {
			failWithMessage("Not overlapping in x dimension");
		}
		if(!isOverlapZ(other)) {
			failWithMessage("Not overlapping in z dimension");
		}

		if (actual.getMaxY() + 1 != other.getMinY() && other.getMaxY() + 1 != actual.getMinY()) {
			failWithMessage("Expected start y at " + (other.getMaxY() + 1) + " or end y at " + (other.getMinY() - 1));
		}
		return myself;
	}
	
	public SELF isAlongsideX(Point3D other) {
		isNotNull();
		
		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
		}
		if(!isOverlapZ(other)) {
			failWithMessage("Not overlapping in z dimension");
		}

		if (actual.getMaxX() + 1 != other.getMinX() && other.getMaxX() + 1 != actual.getMinX()) {
			failWithMessage("Expected start x at " + (other.getMaxX() + 1) + " or end x at " + (other.getMinX() - 1));
		}
		return myself;
	}
	
	public SELF followsAlongsideZ(Point3D other) {
		isNotNull();
		
		if(!isOverlapX(other)) {
			failWithMessage("Not overlapping in x dimension");
		}

		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
		}
		
		if (other.getMaxZ() + 1 != actual.getMinZ()) {
			failWithMessage("Expected start z at " + (other.getMaxZ() + 1));
		}
		return myself;
	}
	
	public SELF followsAlongsideY(Point3D other) {
		isNotNull();
		
		if(!isOverlapX(other)) {
			failWithMessage("Not overlapping in x dimension");
		}
		if(!isOverlapZ(other)) {
			failWithMessage("Not overlapping in z dimension");
		}
		
		if (other.getMaxY() + 1 != actual.getMinY()) {
			failWithMessage("Expected start y at " + (other.getMaxY() + 1));
		}
		return myself;
	}
	
	public SELF preceedsAlongsideY(Point3D other) {
		isNotNull();
		
		if(!isOverlapX(other)) {
			failWithMessage("Not overlapping in x dimension");
		}
		if(!isOverlapZ(other)) {
			failWithMessage("Not overlapping in z dimension");
		}

		if (actual.getMaxY() + 1 != other.getMinY()) {
			failWithMessage("Expected end y at " + (other.getMinY() - 1));
		}
		return myself;
	}
	
	public SELF preceedsAlongsideX(Point3D other) {
		isNotNull();
		
		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
		}
		if(!isOverlapZ(other)) {
			failWithMessage("Not overlapping in z dimension");
		}

		if (actual.getMaxX() + 1 != other.getMinX() ) {
			failWithMessage("Expected end x at " + (other.getMinX() - 1));
		}
		return myself;
	}
	
	public SELF preceedsAlongsideZ(Point3D other) {
		isNotNull();
		
		if(!isOverlapX(other)) {
			failWithMessage("Not overlapping in x dimension");
		}
		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
		}
		if (actual.getMaxZ() + 1 != other.getMinZ() ) {
			failWithMessage("Expected end z at " + (other.getMinZ() - 1));
		}
		
		return myself;
	}
	
}
