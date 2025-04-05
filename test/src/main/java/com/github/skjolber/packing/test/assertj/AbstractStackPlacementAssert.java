package com.github.skjolber.packing.test.assertj;

import java.util.Objects;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.StackPlacement;

public abstract class AbstractStackPlacementAssert<SELF extends AbstractStackPlacementAssert<SELF, ACTUAL>, ACTUAL extends StackPlacement>
		extends AbstractObjectAssert<SELF, ACTUAL> {

	protected AbstractStackPlacementAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF isAtX(int x) {
		isNotNull();
		if(actual.getAbsoluteX() != x) {
			failWithExpectedXAt(x);
		}
		return myself;
	}

	private void failWithExpectedXAt(int x) {
		failWithMessage("Expected x at " + x);
	}

	public SELF isAtY(int y) {
		isNotNull();
		if(actual.getAbsoluteY() != y) {
			failWithExpectedYAt(y);
		}
		return myself;
	}

	private void failWithExpectedYAt(int y) {
		failWithMessage("Expected y at " + y);
	}

	public SELF isAtZ(int z) {
		isNotNull();
		if(actual.getAbsoluteZ() != z) {
			failWithExpectedZAt(z);
		}
		return myself;
	}

	private void failWithExpectedZAt(int z) {
		failWithMessage("Expected z at " + z);
	}

	public SELF isAt(int x, int y, int z) {
		isNotNull();
		if(actual.getAbsoluteX() != x && actual.getAbsoluteY() != y && actual.getAbsoluteZ() != z) {
			failWithMessage("Expected at " + x + "x" + y + "x" + z + ", not " + getStartCoordinates());
		}
		if(actual.getAbsoluteX() != x) {
			failWithMessage("Expected x " + x + ", not " + actual.getAbsoluteX() + " (was " + getCoordinates() + ")");
		}
		if(actual.getAbsoluteY() != y) {
			failWithMessage("Expected y " + y + ", not " + actual.getAbsoluteY() + " (was " + getCoordinates() + ")");
		}
		if(actual.getAbsoluteZ() != z) {
			failWithMessage("Expected z " + y + ", not " + actual.getAbsoluteZ() + " (was " + getCoordinates() + ")");
		}
		return myself;
	}

	public SELF isEndAt(int x, int y, int z) {
		isNotNull();
		if(actual.getAbsoluteEndX() != x && actual.getAbsoluteEndY() != y && actual.getAbsoluteEndZ() != z) {
			failWithMessage("Expected at " + x + "x" + y + "x" + z + ", not " + getEndCoordinates());
		}
		if(actual.getAbsoluteEndX() != x) {
			failWithMessage("Expected x " + x + ", not " + actual.getAbsoluteEndX() + " (was " + getCoordinates() + ")");
		}
		if(actual.getAbsoluteEndY() != y) {
			failWithMessage("Expected y " + y + ", not " + actual.getAbsoluteEndY() + " (was " + getCoordinates() + ")");
		}
		if(actual.getAbsoluteEndZ() != z) {
			failWithMessage("Expected z " + y + ", not " + actual.getAbsoluteEndZ() + " (was " + getCoordinates() + ")");
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

	protected boolean isOverlapX(StackPlacement placement) {

		if(placement.getAbsoluteX() <= actual.getAbsoluteX() && actual.getAbsoluteX() <= placement.getAbsoluteEndX()) {
			return true;
		}

		if(placement.getAbsoluteX() <= actual.getAbsoluteEndX() && actual.getAbsoluteEndX() <= placement.getAbsoluteEndX()) {
			return true;
		}

		return false;
	}

	protected boolean isOverlapY(StackPlacement placement) {
		if(placement.getAbsoluteY() <= actual.getAbsoluteY() && actual.getAbsoluteY() <= placement.getAbsoluteEndY()) {
			return true;
		}

		if(placement.getAbsoluteY() <= actual.getAbsoluteEndY() && actual.getAbsoluteEndY() <= placement.getAbsoluteEndY()) {
			return true;
		}

		return false;
	}

	protected boolean isOverlapZ(StackPlacement placement) {
		if(placement.getAbsoluteZ() <= actual.getAbsoluteZ() && actual.getAbsoluteZ() <= placement.getAbsoluteEndZ()) {
			return true;
		}

		if(placement.getAbsoluteZ() <= actual.getAbsoluteEndZ() && actual.getAbsoluteEndZ() <= placement.getAbsoluteEndZ()) {
			return true;
		}

		return false;
	}

	public SELF isAlongsideY(StackPlacement other) {
		isNotNull();

		if(!isOverlapX(other)) {
			failWithNotOverlappingInXDimension();
		}
		if(!isOverlapZ(other)) {
			failWithNotOverlappingInZDimension();
		}

		if(actual.getAbsoluteEndY() + 1 != other.getAbsoluteY() && other.getAbsoluteEndY() + 1 != actual.getAbsoluteY()) {
			failWithMessage("Expected start y at " + (other.getAbsoluteEndY() + 1) + " or end y at " + (other.getAbsoluteY() - 1));
		}
		return myself;
	}

	public SELF isAlongsideX(StackPlacement other) {
		isNotNull();

		if(!isOverlapY(other)) {
			failWithNotOverlappingInYDimension();
		}
		if(!isOverlapZ(other)) {
			failWithNotOverlappingInZDimension();
		}

		if(actual.getAbsoluteEndX() + 1 != other.getAbsoluteX() && other.getAbsoluteEndX() + 1 != actual.getAbsoluteX()) {
			failWithMessage("Expected start x at " + (other.getAbsoluteEndX() + 1) + " or end x at " + (other.getAbsoluteX() - 1));
		}
		return myself;
	}

	public SELF isAlongsideZ(StackPlacement other) {
		isNotNull();

		if(!isOverlapX(other)) {
			failWithNotOverlappingInXDimension();
		}
		if(!isOverlapY(other)) {
			failWithNotOverlappingInYDimension();
		}

		if(actual.getAbsoluteEndZ() + 1 != other.getAbsoluteZ() && other.getAbsoluteEndZ() + 1 != actual.getAbsoluteZ()) {
			failWithMessage("Expected start z at " + (other.getAbsoluteEndZ() + 1) + " or end z at " + (other.getAbsoluteZ() - 1));
		}
		return myself;
	}

	public SELF followsAlongsideX(StackPlacement other) {
		isNotNull();

		if(!isOverlapY(other)) {
			failWithNotOverlappingInYDimension();
		}
		if(!isOverlapZ(other)) {
			failWithNotOverlappingInZDimension();
		}

		if(other.getAbsoluteEndX() + 1 != actual.getAbsoluteX()) {
			failWithMessage("Expected start x at " + (other.getAbsoluteEndX() + 1));
		}
		return myself;
	}

	public SELF followsAlongsideY(StackPlacement other) {
		isNotNull();

		if(!isOverlapX(other)) {
			failWithNotOverlappingInXDimension();
		}
		if(!isOverlapZ(other)) {
			failWithNotOverlappingInZDimension();
		}

		if(other.getAbsoluteEndY() + 1 != actual.getAbsoluteY()) {
			failWithMessage("Expected start y at " + (other.getAbsoluteEndY() + 1));
		}
		return myself;
	}

	public SELF followsAlongsideZ(StackPlacement other) {
		isNotNull();

		if(!isOverlapX(other)) {
			failWithNotOverlappingInXDimension();
		}
		if(!isOverlapY(other)) {
			failWithNotOverlappingInYDimension();
		}

		if(other.getAbsoluteEndZ() + 1 != actual.getAbsoluteZ()) {
			failWithMessage("Expected start z at " + (other.getAbsoluteEndZ() + 1));
		}
		return myself;
	}

	public SELF preceedsAlongsideY(StackPlacement other) {
		isNotNull();

		if(!isOverlapX(other)) {
			failWithNotOverlappingInXDimension();
		}
		if(!isOverlapZ(other)) {
			failWithNotOverlappingInZDimension();
		}

		if(actual.getAbsoluteEndY() + 1 != other.getAbsoluteY()) {
			failWithMessage("Expected end y at " + (other.getAbsoluteY() - 1));
		}
		return myself;
	}

	public SELF preceedsAlongsideX(StackPlacement other) {
		isNotNull();

		if(!isOverlapY(other)) {
			failWithNotOverlappingInYDimension();
		}
		if(!isOverlapZ(other)) {
			failWithNotOverlappingInZDimension();
		}

		if(actual.getAbsoluteEndX() + 1 != other.getAbsoluteX()) {
			failWithMessage("Expected end x at " + (other.getAbsoluteX() - 1));
		}
		return myself;
	}

	public SELF preceedsAlongsideZ(StackPlacement other) {
		isNotNull();

		if(!isOverlapX(other)) {
			failWithNotOverlappingInXDimension();
		}
		if(!isOverlapY(other)) {
			failWithNotOverlappingInYDimension();
		}

		if(actual.getAbsoluteEndZ() + 1 != other.getAbsoluteZ()) {
			failWithMessage("Expected end z at " + (other.getAbsoluteZ() - 1));
		}
		return myself;
	}

	private void failWithNotOverlappingInZDimension() {
		failWithMessage("Not overlapping in z dimension");
	}

	private void failWithNotOverlappingInXDimension() {
		failWithMessage("Not overlapping in x dimension");
	}

	private void failWithNotOverlappingInYDimension() {
		failWithMessage("Not overlapping in y dimension");
	}

	/*
	public SELF isSupportedBy(StackPlacement ... others) {
		isNotNull();
		
		List<? extends StackPlacement> supports3d = actual.getSupports3D();
		for (StackPlacement other : others) {
			if(!supports3d.contains(other)) {
				failWithMessage("Not supported by " + other);
			}
		}
		return myself;
	}
	
	public SELF supports(StackPlacement ... others) {
		isNotNull();
		for (StackPlacement other : others) {
			List<? extends StackPlacement> list = other.getSupports3D();
			if(list == null || !list.contains(actual)) {
				failWithMessage(actual + " is not supporting " + other + ". Supporters are " + list);
			}
		}
		return myself;
	}
	*/
	
	public SELF hasStackableName(String name) {
		isNotNull();
		if(!Objects.equals(name, actual.getStackable().getDescription())) {
			failWithMessage("Expected stackable name " + name + ", not " + actual.getStackable().getDescription());
		}
		return myself;
	}

	public SELF hasStackValue(BoxStackValue stackValue) {
		isNotNull();
		if(!Objects.equals(stackValue, actual.getStackValue())) {
			failWithMessage("Expected stack value " + stackValue + ", not " + actual.getStackValue());
		}
		return myself;
	}

}
