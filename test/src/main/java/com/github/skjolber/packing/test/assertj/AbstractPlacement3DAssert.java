package com.github.skjolber.packing.test.assertj;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.api.Placement3D;

public abstract class AbstractPlacement3DAssert<SELF extends AbstractPlacement3DAssert<SELF, ACTUAL>, ACTUAL extends Placement3D>
extends AbstractObjectAssert<SELF, ACTUAL> {

	protected AbstractPlacement3DAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF isAtX(int x) {
		isNotNull();
		if (actual.getAbsoluteX() != x) {
			failWithMessage("Expected x at " + x);
		}
		return myself;
	}

	public SELF isAtY(int y) {
		isNotNull();
		if (actual.getAbsoluteY() != y) {
			failWithMessage("Expected y at " + y);
		}
		return myself;
	}
	
	public SELF isAtZ(int z) {
		isNotNull();
		if (actual.getAbsoluteZ() != z) {
			failWithMessage("Expected z at " + z);
		}
		return myself;
	}

	public SELF isAt(int x, int y, int z) {
		isNotNull();
		if (actual.getAbsoluteX() != x && actual.getAbsoluteY() != y  && actual.getAbsoluteZ() != z ) {
			failWithMessage("Expected at " + x + "x" + y + "x" + z + ", not " + getStartCoordinates());
		}
		if (actual.getAbsoluteX() != x) {
			failWithMessage("Expected x " + x + ", not " + actual.getAbsoluteX() + " (was " + getCoordinates() + ")");
		}
		if (actual.getAbsoluteY() != y) {
			failWithMessage("Expected y " + y + ", not " + actual.getAbsoluteY()+ " (was " + getCoordinates() + ")");
		}
		if (actual.getAbsoluteZ() != z) {
			failWithMessage("Expected z " + y + ", not " + actual.getAbsoluteZ()+ " (was " + getCoordinates() + ")");
		}
		return myself;
	}
	
	public SELF isEndAt(int x, int y, int z) {
		isNotNull();
		if (actual.getAbsoluteEndX() != x && actual.getAbsoluteEndY() != y && actual.getAbsoluteEndZ() != z ) {
			failWithMessage("Expected at " + x + "x" + y + "x" + z + ", not " + getEndCoordinates());
		}
		if (actual.getAbsoluteEndX() != x) {
			failWithMessage("Expected x " + x + ", not " + actual.getAbsoluteEndX() + " (was " + getCoordinates() + ")");
		}
		if (actual.getAbsoluteEndY() != y) {
			failWithMessage("Expected y " + y + ", not " + actual.getAbsoluteEndY()+ " (was " + getCoordinates() + ")");
		}
		if (actual.getAbsoluteEndZ() != z) {
			failWithMessage("Expected z " + y + ", not " + actual.getAbsoluteEndZ()+ " (was " + getCoordinates() + ")");
		}
		return myself;
	}
	
	private String getEndCoordinates() {
		return actual.getAbsoluteEndX() + "x" + actual.getAbsoluteEndY() + "x" + actual.getAbsoluteEndZ();
	}

	private String getStartCoordinates() {
		return actual.getAbsoluteX() + "x" + actual.getAbsoluteY() + "x" + actual.getAbsoluteZ();
	}

	private String getCoordinates() {
		return getStartCoordinates() + " " + getEndCoordinates();
	}
	
	
	protected boolean isOverlapX(Placement3D placement) {
		
		if(placement.getAbsoluteX() <= actual.getAbsoluteX() && actual.getAbsoluteX() <= placement.getAbsoluteEndX()) {
			return true;
		}
		
		if(placement.getAbsoluteX() <= actual.getAbsoluteEndX() && actual.getAbsoluteEndX() <= placement.getAbsoluteEndX()) {
			return true;
		}
		
		return false;
	}

	protected boolean isOverlapY(Placement3D placement) {
		if(placement.getAbsoluteY() <= actual.getAbsoluteY() && actual.getAbsoluteY() <= placement.getAbsoluteEndY()) {
			return true;
		}
		
		if(placement.getAbsoluteY() <= actual.getAbsoluteEndY() && actual.getAbsoluteEndY() <= placement.getAbsoluteEndY()) {
			return true;
		}
		
		return false;
	}
	
	protected boolean isOverlapZ(Placement3D placement) {
		if(placement.getAbsoluteZ() <= actual.getAbsoluteZ() && actual.getAbsoluteZ() <= placement.getAbsoluteEndZ()) {
			return true;
		}
		
		if(placement.getAbsoluteZ() <= actual.getAbsoluteEndZ() && actual.getAbsoluteEndZ() <= placement.getAbsoluteEndZ()) {
			return true;
		}
		
		return false;
	}

	public SELF isAlongsideY(Placement3D other) {
		isNotNull();
		
		if(!isOverlapX(other)) {
			failWithMessage("Not overlapping in x dimension");
		}
		if(!isOverlapZ(other)) {
			failWithMessage("Not overlapping in z dimension");
		}
		
		if (actual.getAbsoluteEndY() + 1 != other.getAbsoluteY() && other.getAbsoluteEndY() + 1 != actual.getAbsoluteY()) {
			failWithMessage("Expected start y at " + (other.getAbsoluteEndY() + 1) + " or end y at " + (other.getAbsoluteY() - 1));
		}
		return myself;
	}
	
	public SELF isAlongsideX(Placement3D other) {
		isNotNull();
		
		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
		}
		if(!isOverlapZ(other)) {
			failWithMessage("Not overlapping in z dimension");
		}

		if (actual.getAbsoluteEndX() + 1 != other.getAbsoluteX() && other.getAbsoluteEndX() + 1 != actual.getAbsoluteX()) {
			failWithMessage("Expected start x at " + (other.getAbsoluteEndX() + 1) + " or end x at " + (other.getAbsoluteX() - 1));
		}
		return myself;
	}
	
	public SELF isAlongsideZ(Placement3D other) {
		isNotNull();
		
		if(!isOverlapX(other)) {
			failWithMessage("Not overlapping in x dimension");
		}
		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
		}
		
		if (actual.getAbsoluteEndZ() + 1 != other.getAbsoluteZ() && other.getAbsoluteEndZ() + 1 != actual.getAbsoluteZ()) {
			failWithMessage("Expected start z at " + (other.getAbsoluteEndZ() + 1) + " or end z at " + (other.getAbsoluteZ() - 1));
		}
		return myself;
	}	
	
	public SELF followsAlongsideX(Placement3D other) {
		isNotNull();
		
		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
		}
		if(!isOverlapZ(other)) {
			failWithMessage("Not overlapping in z dimension");
		}
		
		if (other.getAbsoluteEndX() + 1 != actual.getAbsoluteX()) {
			failWithMessage("Expected start x at " + (other.getAbsoluteEndX() + 1));
		}
		return myself;
	}
	
	public SELF followsAlongsideY(Placement3D other) {
		isNotNull();
		
		if(!isOverlapX(other)) {
			failWithMessage("Not overlapping in x dimension");
		}
		if(!isOverlapZ(other)) {
			failWithMessage("Not overlapping in z dimension");
		}
		
		if (other.getAbsoluteEndY() + 1 != actual.getAbsoluteY()) {
			failWithMessage("Expected start y at " + (other.getAbsoluteEndY() + 1));
		}
		return myself;
	}
	
	public SELF followsAlongsideZ(Placement3D other) {
		isNotNull();
		
		if(!isOverlapX(other)) {
			failWithMessage("Not overlapping in x dimension");
		}
		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
		}
		
		if (other.getAbsoluteEndZ() + 1 != actual.getAbsoluteZ()) {
			failWithMessage("Expected start z at " + (other.getAbsoluteEndZ() + 1));
		}
		return myself;
	}

	public SELF preceedsAlongsideY(Placement3D other) {
		isNotNull();
		
		if(!isOverlapX(other)) {
			failWithMessage("Not overlapping in x dimension");
		}
		if(!isOverlapZ(other)) {
			failWithMessage("Not overlapping in z dimension");
		}
		
		if (actual.getAbsoluteEndY() + 1 != other.getAbsoluteY()) {
			failWithMessage("Expected end y at " + (other.getAbsoluteY() - 1));
		}
		return myself;
	}
	
	public SELF preceedsAlongsideX(Placement3D other) {
		isNotNull();
		
		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
		}
		if(!isOverlapZ(other)) {
			failWithMessage("Not overlapping in z dimension");
		}
		
		if (actual.getAbsoluteEndX() + 1 != other.getAbsoluteX() ) {
			failWithMessage("Expected end x at " + (other.getAbsoluteX() - 1));
		}
		return myself;
	}
	
	public SELF preceedsAlongsideZ(Placement3D other) {
		isNotNull();
		
		if(!isOverlapX(other)) {
			failWithMessage("Not overlapping in x dimension");
		}
		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
		}
		
		if (actual.getAbsoluteEndZ() + 1 != other.getAbsoluteZ()) {
			failWithMessage("Expected end z at " + (other.getAbsoluteZ() - 1));
		}
		return myself;
	}	
}
