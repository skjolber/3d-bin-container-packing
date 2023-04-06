package com.github.skjolber.packing.test.assertj;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.api.ep.Point2D;

@SuppressWarnings("rawtypes")
public abstract class AbstractPoint2DAssert<SELF extends AbstractPoint2DAssert<SELF, ACTUAL>, ACTUAL extends Point2D>
		extends AbstractObjectAssert<SELF, ACTUAL> {

	protected AbstractPoint2DAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF isMinX(int x) {
		isNotNull();
		if(actual.getMinX() != x) {
			failWithMessage("Expected x at " + x);
		}
		return myself;
	}

	public SELF isMinY(int y) {
		isNotNull();
		if(actual.getMinY() != y) {
			failWithMessage("Expected y at " + y);
		}
		return myself;
	}

	public SELF isMin(int x, int y) {
		isNotNull();
		if(actual.getMinX() != x) {
			failWithMessage("Expected x " + x + ", not " + actual.getMinX() + " (was " + getCoordinates() + ")");
		}
		if(actual.getMinY() != y) {
			failWithMessage("Expected y " + y + ", not " + actual.getMinY() + " (was " + getCoordinates() + ")");
		}
		return myself;
	}

	public SELF isMax(int x, int y) {
		isNotNull();
		if(actual.getMaxX() != x) {
			failWithMessage("Expected x " + x + ", not " + actual.getMaxX() + " (was " + getCoordinates() + ")");
		}
		if(actual.getMaxY() != y) {
			failWithMessage("Expected y " + y + ", not " + actual.getMaxY() + " (was " + getCoordinates() + ")");
		}
		return myself;
	}

	public SELF isMaxX(int x) {
		isNotNull();
		if(actual.getMaxX() != x) {
			failWithMessage("Expected x " + x + ", not " + actual.getMaxX() + " (was " + getCoordinates() + ")");
		}
		return myself;
	}

	public SELF isMaxY(int y) {
		isNotNull();
		if(actual.getMaxY() != y) {
			failWithMessage("Expected y " + y + ", not " + actual.getMaxY() + " (was " + getCoordinates() + ")");
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
		if(actual.getMaxY() + 1 != other.getMinY() && other.getMaxY() + 1 != actual.getMinY()) {
			failWithMessage("Expected start y at " + (other.getMaxY() + 1) + " or end y at " + (other.getMinY() - 1));
		}
		return myself;
	}

	public SELF isAlongsideX(Point2D other) {
		isNotNull();

		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
		}

		if(actual.getMaxX() + 1 != other.getMinX() && other.getMaxX() + 1 != actual.getMinX()) {
			failWithMessage("Expected start x at " + (other.getMaxX() + 1) + " or end x at " + (other.getMinX() - 1));
		}
		return myself;
	}

	public SELF followsAlongsideX(Point2D other) {
		isNotNull();

		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
		}

		if(other.getMaxX() + 1 != actual.getMinX()) {
			failWithMessage("Expected start x at " + (other.getMaxX() + 1));
		}
		return myself;
	}

	public SELF followsAlongsideY(Point2D other) {
		isNotNull();

		if(!isOverlapX(other)) {
			failWithMessage("Not overlapping in x dimension");
		}

		if(other.getMaxY() + 1 != actual.getMinY()) {
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

		if(actual.getMaxY() + 1 != other.getMinY()) {
			failWithMessage("Expected end y at " + (other.getMinY() - 1));
		}
		return myself;
	}

	public SELF preceedsAlongsideX(Point2D other) {
		isNotNull();

		if(!isOverlapY(other)) {
			failWithMessage("Not overlapping in y dimension");
		}

		if(actual.getMaxX() + 1 != other.getMinX()) {
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
