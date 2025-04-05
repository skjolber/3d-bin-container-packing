package com.github.skjolber.packing.test.assertj;

import java.util.List;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;

public abstract class AbstractStackAssert<SELF extends AbstractStackAssert<SELF, ACTUAL>, ACTUAL extends Stack>
		extends AbstractObjectAssert<SELF, ACTUAL> {

	protected AbstractStackAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF isWithinLoadContraints(Container container) {
		isNotNull();
		isWithinLoadDimensions(container);
		isWithinLoadWeight(container);
		placementsDoNotIntersect();
		return myself;
	}

	public SELF placementsDoNotIntersect() {
		isNotNull();

		List<StackPlacement> entries = actual.getPlacements();

		for (StackPlacement stackPlacement1 : entries) {
			for (StackPlacement stackPlacement2 : entries) {
				if(stackPlacement1 != stackPlacement2) {
					if(stackPlacement1.intersects(stackPlacement2)) {
						failWithMessage(stackPlacement1 + " intersects " + stackPlacement2 + " for stack " + actual.getClass().getName());
					}
				}
			}
		}
		return myself;
	}

	public SELF isWithinLoadDimensions(Container container) {
		isNotNull();

		int loadDx = container.getLoadDx();
		int loadDy = container.getLoadDy();
		int loadDz = container.getLoadDz();

		for (StackPlacement stackPlacement : actual.getPlacements()) {

			if(stackPlacement.getAbsoluteX() < 0) {
				failWithMessage("Expected stacked x position >= 0, got " + stackPlacement.getAbsoluteX() + " for " + stackPlacement);
			}
			if(stackPlacement.getAbsoluteY() < 0) {
				failWithMessage("Expected stacked y position >= 0, got " + stackPlacement.getAbsoluteY() + " for " + stackPlacement);
			}
			if(stackPlacement.getAbsoluteZ() < 0) {
				failWithMessage("Expected stacked z position >= 0, got " + stackPlacement.getAbsoluteZ() + " for " + stackPlacement);
			}

			if(stackPlacement.getAbsoluteEndX() > loadDx) {
				failWithMessage("Expected stacked end x position <= " + loadDx + ", got " + stackPlacement.getAbsoluteEndX() + " for " + stackPlacement);
			}
			if(stackPlacement.getAbsoluteEndY() > loadDy) {
				failWithMessage("Expected stacked end y position <= " + loadDy + ", got " + stackPlacement.getAbsoluteEndY() + " for " + stackPlacement);
			}
			if(stackPlacement.getAbsoluteEndZ() > loadDz) {
				failWithMessage("Expected stacked end z position <= " + loadDz + ", got " + stackPlacement.getAbsoluteEndZ() + " for " + stackPlacement);
			}
		}

		return myself;
	}

	public SELF isWithinLoadWeight(Container container) {
		isNotNull();

		int loadWeight = 0;
		for (StackPlacement stackPlacement : actual.getPlacements()) {
			loadWeight += stackPlacement.getStackable().getWeight();
		}
		int maxLoadWeight = container.getMaxLoadWeight();

		if(loadWeight > maxLoadWeight) {
			failWithMessage("Expected stacked load weight <= " + maxLoadWeight + ", got " + loadWeight);
		}

		return myself;
	}

}
