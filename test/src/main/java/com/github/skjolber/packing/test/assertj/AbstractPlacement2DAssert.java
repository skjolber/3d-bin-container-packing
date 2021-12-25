package com.github.skjolber.packing.test.assertj;

import java.util.List;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.Placement3D;

public abstract class AbstractPlacement2DAssert<SELF extends AbstractPlacement2DAssert<SELF, ACTUAL>, ACTUAL extends Placement2D>
extends AbstractObjectAssert<SELF, ACTUAL> {

	protected AbstractPlacement2DAssert(ACTUAL actual, Class<?> selfType) {
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

	public SELF isAt(int x, int y) {
		isNotNull();
		if (actual.getAbsoluteX() != x) {
			failWithMessage("Expected x " + x + ", not " + actual.getAbsoluteX() + " (was " + getCoordinates() + ")");
		}
		if (actual.getAbsoluteY() != y) {
			failWithMessage("Expected y " + y + ", not " + actual.getAbsoluteY()+ " (was " + getCoordinates() + ")");
		}
		return myself;
	}
	
	public SELF isEndAt(int x, int y, int z) {
		isNotNull();
		if (actual.getAbsoluteEndX() != x && actual.getAbsoluteEndY() != y) {
			failWithMessage("Expected at " + x + "x" + y + "x" + z + ", not " + getEndCoordinates());
		}
		if (actual.getAbsoluteEndX() != x) {
			failWithMessage("Expected x " + x + ", not " + actual.getAbsoluteEndX() + " (was " + getCoordinates() + ")");
		}
		if (actual.getAbsoluteEndY() != y) {
			failWithMessage("Expected y " + y + ", not " + actual.getAbsoluteEndY()+ " (was " + getCoordinates() + ")");
		}
		return myself;
	}
	
	private String getEndCoordinates() {
		return actual.getAbsoluteEndX() + "x" + actual.getAbsoluteEndY();
	}

	private String getStartCoordinates() {
		return actual.getAbsoluteX() + "x" + actual.getAbsoluteY();
	}

	private String getCoordinates() {
		return getStartCoordinates() + " " + getEndCoordinates();
	}
	
	
	protected boolean isOverlapX(Placement2D placement) {
		
		if(placement.getAbsoluteX() <= actual.getAbsoluteX() && actual.getAbsoluteX() <= placement.getAbsoluteEndX()) {
			return true;
		}
		
		if(placement.getAbsoluteX() <= actual.getAbsoluteEndX() && actual.getAbsoluteEndX() <= placement.getAbsoluteEndX()) {
			return true;
		}
		
		return false;
	}

	protected boolean isOverlapY(Placement2D placement) {
		if(placement.getAbsoluteY() <= actual.getAbsoluteY() && actual.getAbsoluteY() <= placement.getAbsoluteEndY()) {
			return true;
		}
		
		if(placement.getAbsoluteY() <= actual.getAbsoluteEndY() && actual.getAbsoluteEndY() <= placement.getAbsoluteEndY()) {
			return true;
		}
		
		return false;
	}

	public SELF isAlongsideY(Placement2D other) {
		isNotNull();
		
		if(!isOverlapX(other)) {
			failWithMessage("Not overlapping in x dimension");
		}
		if (actual.getAbsoluteEndY() + 1 != other.getAbsoluteY() && other.getAbsoluteEndY() + 1 != actual.getAbsoluteY()) {
			failWithMessage("Expected start y at " + (other.getAbsoluteEndY() + 1) + " or end y at " + (other.getAbsoluteY() - 1));
		}
		return myself;
	}
	
	public SELF isAlongsideX(Placement2D other) {
		isNotNull();
		
		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
		}

		if (actual.getAbsoluteEndX() + 1 != other.getAbsoluteX() && other.getAbsoluteEndX() + 1 != actual.getAbsoluteX()) {
			failWithMessage("Expected start x at " + (other.getAbsoluteEndX() + 1) + " or end x at " + (other.getAbsoluteX() - 1));
		}
		return myself;
	}
	
	public SELF followsAlongsideX(Placement2D other) {
		isNotNull();
		
		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
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
		
		return myself;
	}

	public SELF preceedsAlongsideY(Placement3D other) {
		isNotNull();
		
		if(!isOverlapX(other)) {
			failWithMessage("Not overlapping in x dimension");
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
		
		return myself;
	}
	
	
	public SELF isSupportedBy(Placement3D ... others) {
		isNotNull();
		
		List<? extends Placement2D> supports3d = actual.getSupports2D();
		for (Placement3D other : others) {
			if(!supports3d.contains(other)) {
				failWithMessage("Not supported by " + other);
			}
		}
		return myself;
	}
	
	public SELF supports(Placement3D ... others) {
		isNotNull();
		for (Placement3D other : others) {
			List<? extends Placement3D> list = other.getSupports3D();
			if(list == null || !list.contains(actual)) {
				failWithMessage(actual + " is not supporting " + other + ". Supporters are " + list);
			}
		}
		return myself;
	}
	
}
