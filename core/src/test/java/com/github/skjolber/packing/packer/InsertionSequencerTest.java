package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;

public class InsertionSequencerTest {

	private static Placement place(String id, int dx, int dy, int dz, int x, int y, int z) {
		Box box = Box.newBuilder().withId(id).withSize(dx, dy, dz).withWeight(1).build();
		return new Placement(box.getStackValue(0), 0, x, y, z);
	}

	private static Stack stack(Placement... placements) {
		Stack stack = new Stack();
		for (Placement placement : placements) {
			stack.add(placement);
		}
		return stack;
	}

	//
	//  z
	//  2 +-------+
	//    |   B   |   C was placed last, into the gap under the overhang of B
	//  1 +---+---+
	//    | A | C |
	//  0 +---+---+
	//    0   1   2  x
	//
	@Test
	void boxPlacedIntoAGapIsInsertedBeforeTheBoxAboveIt() {
		Placement a = place("A", 1, 1, 1, 0, 0, 0);
		Placement b = place("B", 2, 1, 1, 0, 0, 1);
		Placement c = place("C", 1, 1, 1, 1, 0, 0);
		Stack stack = stack(a, b, c);

		assertThat(InsertionSequencer.sequence(stack, ContainerAccess.ANY)).isTrue();
		assertThat(stack.getPlacements()).containsExactly(a, c, b);
		assertThat(stack.getPlacements()).extracting(Placement::getIndex).containsExactly(0, 1, 2);
	}

	//
	//  z
	//  2 +---+       +---+
	//    | B |       | D |   B rests on A; D rests on C, but was placed before it
	//  1 +---+       +---+
	//    | A |       | C |
	//  0 +---+       +---+
	//    0   1   2   3   4  x
	//
	@Test
	void boxesAreOrderedByHeightKeepingTheSearchOrder() {
		Placement a = place("A", 1, 1, 1, 0, 0, 0);
		Placement b = place("B", 1, 1, 1, 0, 0, 1);
		Placement d = place("D", 1, 1, 1, 3, 0, 1);
		Placement c = place("C", 1, 1, 1, 3, 0, 0);
		Stack stack = stack(a, b, d, c);

		assertThat(InsertionSequencer.sequence(stack, ContainerAccess.TOP)).isTrue();
		assertThat(stack.getPlacements()).containsExactly(a, c, b, d);
		// through a door, only what is needed is changed
		Stack front = stack(a, b, d, c);
		assertThat(InsertionSequencer.sequence(front, ContainerAccess.FRONT)).isTrue();
		assertThat(front.getPlacements()).containsExactly(a, b, c, d);
	}

	@Test
	void possibleOrderIsUnchanged() {
		Placement a = place("A", 1, 1, 1, 0, 0, 0);
		Placement c = place("C", 1, 1, 1, 1, 0, 0);
		Placement b = place("B", 2, 1, 1, 0, 0, 1);
		Stack stack = stack(a, c, b);

		assertThat(InsertionSequencer.sequence(stack, ContainerAccess.ANY)).isTrue();
		assertThat(stack.getPlacements()).containsExactly(a, c, b);
	}

	//
	//  side view (z up), door at x = 3
	//
	//  1 +---+---+---+
	//    | A | B | C |   door ->
	//  0 +---+---+---+
	//    0   1   2   3  x
	//
	@Test
	void boxesAreLoadedFromTheBackTowardsTheDoor() {
		Placement a = place("A", 1, 1, 1, 0, 0, 0);
		Placement b = place("B", 1, 1, 1, 1, 0, 0);
		Placement c = place("C", 1, 1, 1, 2, 0, 0);
		Stack stack = stack(c, a, b);

		assertThat(InsertionSequencer.sequence(stack, ContainerAccess.FRONT)).isTrue();
		assertThat(stack.getPlacements()).containsExactly(a, b, c);

		// from above, the order does not matter for boxes side by side
		Stack top = stack(c, a, b);
		assertThat(InsertionSequencer.sequence(top, ContainerAccess.TOP)).isTrue();
		assertThat(top.getPlacements()).containsExactly(c, a, b);
	}
}
