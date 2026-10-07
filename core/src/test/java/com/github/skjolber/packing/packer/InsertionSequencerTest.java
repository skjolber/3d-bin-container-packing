package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.RemainingBoxItem;
import com.github.skjolber.packing.api.packager.RemainingBoxItemGroup;

public class InsertionSequencerTest {

	private static Placement place(String id, int dx, int dy, int dz, int x, int y, int z) {
		Box box = Box.newBuilder().withId(id).withSize(dx, dy, dz).withWeight(1).build();
		return new Placement(box.getStackValue(0), 0, x, y, z);
	}

	private static Placement place(String id, int x, int z, int extractionOrder) {
		Box box = Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build();
		BoxItem item = new BoxItem(box).withExtractionOrder(extractionOrder);
		return new Placement(item, box.getStackValue(0), 0, x, 0, z);
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

	//
	//  side view, door at x = 3; the number is the extraction order
	//
	//  1 +---+---+---+
	//    | 2 | 2 | 1 |   door ->
	//  0 +---+---+---+
	//    0   1   2   3  x
	//
	@Test
	void boxesExtractedLastAreInsertedFirst() {
		Placement a = place("A", 0, 0, 2);
		Placement b = place("B", 1, 0, 2);
		Placement c = place("C", 2, 0, 1);
		Stack stack = stack(c, b, a);

		assertThat(InsertionSequencer.sequence(stack, ContainerAccess.FRONT)).isTrue();
		assertThat(stack.getPlacements()).containsExactly(a, b, c);

		// from above, by extraction order, then in the order of the search
		Stack top = stack(c, b, a);
		assertThat(InsertionSequencer.sequence(top, ContainerAccess.TOP)).isTrue();
		assertThat(top.getPlacements()).containsExactly(b, a, c);
	}

	//
	//  z
	//  2 +---+
	//    | 2 |   B is extracted after A, but rests on it: A cannot be extracted first
	//  1 +---+
	//    | 1 |
	//  0 +---+
	//    0   1  x
	//
	@Test
	void boxBelowABoxExtractedLaterCannotBeSequenced() {
		Placement a = place("A", 0, 0, 1);
		Placement b = place("B", 0, 1, 2);
		Stack stack = stack(a, b);

		assertThat(InsertionSequencer.sequence(stack, ContainerAccess.ANY)).isFalse();
		assertThat(stack.getPlacements()).containsExactly(a, b);
	}

	private static Placement place(String id, int x, int z, RemainingBoxItemGroup group) {
		Box box = Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build();
		RemainingBoxItem item = new RemainingBoxItem(new BoxItem(box));
		group.getItems().add(item);
		item.setGroup(group);
		return new Placement(item, box.getStackValue(0), 0, x, 0, z);
	}

	private static RemainingBoxItemGroup group(String id, int index) {
		return new RemainingBoxItemGroup(new BoxItemGroup(id, new ArrayList<>()), new ArrayList<>(), index);
	}

	//
	//  z
	//  2 +---+
	//    |A2 |       by height, B1 would be inserted between A1 and A2;
	//  1 +---+---+   the boxes of group A are inserted together
	//    |A1 |B1 |
	//  0 +---+---+
	//    0   1   2  x
	//
	@Test
	void boxesOfAGroupAreInsertedTogether() {
		RemainingBoxItemGroup a = group("A", 0);
		RemainingBoxItemGroup b = group("B", 1);
		Placement a1 = place("A1", 0, 0, a);
		Placement a2 = place("A2", 0, 1, a);
		Placement b1 = place("B1", 1, 0, b);
		Stack stack = stack(a1, a2, b1);

		assertThat(InsertionSequencer.sequence(stack, ContainerAccess.ANY)).isTrue();
		assertThat(stack.getPlacements()).containsExactly(a1, a2, b1);

		// in the order in which the groups were placed
		Stack reversed = stack(b1, a1, a2);
		assertThat(InsertionSequencer.sequence(reversed, ContainerAccess.ANY)).isTrue();
		assertThat(reversed.getPlacements()).containsExactly(b1, a1, a2);
	}

	//
	//  z
	//  3 +---+
	//    |A2 |       B1 rests on A1, and A2 on B1: the groups cannot be inserted one at a time,
	//  2 +---+       so the boxes are inserted by height
	//    |B1 |
	//  1 +---+
	//    |A1 |
	//  0 +---+
	//    0   1  x
	//
	@Test
	void groupsWhichCannotBeInsertedTogetherAreInsertedByHeight() {
		RemainingBoxItemGroup a = group("A", 0);
		RemainingBoxItemGroup b = group("B", 1);
		Placement a1 = place("A1", 0, 0, a);
		Placement a2 = place("A2", 0, 2, a);
		Placement b1 = place("B1", 0, 1, b);
		Stack stack = stack(a1, a2, b1);

		assertThat(InsertionSequencer.sequence(stack, ContainerAccess.ANY)).isTrue();
		assertThat(stack.getPlacements()).containsExactly(a1, b1, a2);
		assertThat(List.of(a1, b1, a2)).extracting(p -> p.getRemainingBoxItem().getGroup().getId()).containsExactly("A", "B", "A");
	}
}
