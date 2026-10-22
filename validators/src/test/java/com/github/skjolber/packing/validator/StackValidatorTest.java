package com.github.skjolber.packing.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.reasons.BoxesIntersectReason;
import com.github.skjolber.packing.validator.reasons.ValidatorInterruptedException;

class StackValidatorTest {

	private static final StackValidator VALIDATOR = new StackValidator();

	@Test
	void firstCollisionReasonMatchesOldAllPairsOrder() throws ValidatorInterruptedException {
		// Same y/z; overlapping x footprints:
		// A, B, C: [0--1]
		// D:          [1--2]
		// isolated:              [4--5]
		List<Placement> placements = List.of(
				placement("isolated", 4, 0),
				placement("A", 0, 0),
				placement("B", 0, 0),
				placement("C", 0, 0),
				placement("D", 1, 0));

		assertCollisionMatchesOldOracle(placements);
		List<Placement> reversed = new ArrayList<>(placements);
		Collections.reverse(reversed);
		assertCollisionMatchesOldOracle(reversed);
	}

	@Test
	void acceptsDisjointPlacements() throws ValidatorInterruptedException {
		// x: [ A A ][ B B ][ C C ] (all at y=z=0)
		List<Placement> placements = List.of(
				placement("A", 0, 0),
				placement("B", 2, 0),
				placement("C", 4, 0));
		List<ValidatorResultReason> reasons = new ArrayList<>();

		assertThat(VALIDATOR.validate(List.of(new ValidatorContainerItem(container(placements, 6), 1)), result(placements, 6), () -> false, reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	private static void assertCollisionMatchesOldOracle(List<Placement> placements) throws ValidatorInterruptedException {
		String expected = oldAllPairsReason(placements);
		List<ValidatorResultReason> reasons = new ArrayList<>();
		boolean valid = VALIDATOR.validate(List.of(new ValidatorContainerItem(container(placements, 6), 1)), result(placements, 6), () -> false, reasons);

		assertThat(valid).isFalse();
		assertThat(reasons).singleElement().isInstanceOf(BoxesIntersectReason.class).extracting(ValidatorResultReason::getMessage).isEqualTo(expected);
	}

	private static String oldAllPairsReason(List<Placement> placements) {
		for(int i = 0; i < placements.size(); i++) {
			for(int k = 0; k < placements.size(); k++) {
				if(i == k) {
					continue;
				}
				Placement first = placements.get(i);
				Placement second = placements.get(k);
				if(first.intersects(second)) {
					return first.getBox().getId() + " intersects " + second.getBox().getId();
				}
			}
		}
		throw new AssertionError("Expected intersecting placements");
	}

	private static Placement placement(String id, int x, int y) {
		Box box = Box.newBuilder()
				.withId(id)
				.withSize(2, 1, 1)
				.withWeight(1)
				.build();
		return new Placement(box.getStackValue(0), 0, x, y, 0);
	}

	private static Container container(List<Placement> placements, int width) {
		Stack stack = new Stack();
		stack.addAll(placements);
		return Container.newBuilder()
				.withId("container")
				.withSize(width, 2, 1)
				.withMaxLoadWeight(1000)
				.withStack(stack)
				.build();
	}

	private static PackagerResult result(List<Placement> placements, int width) {
		return new PackagerResult(List.of(container(placements, width)), 0L, false, -1);
	}
}
