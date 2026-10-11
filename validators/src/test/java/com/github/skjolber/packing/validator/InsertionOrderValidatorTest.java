package com.github.skjolber.packing.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.reasons.BlockedInsertionReason;
import com.github.skjolber.packing.validator.reasons.InsertedBeforeSupporterReason;

public class InsertionOrderValidatorTest {

	private final InsertionOrderValidator validator = new InsertionOrderValidator();

	private static Placement place(String id, int dx, int dy, int dz, int x, int y, int z) {
		Box box = Box.newBuilder().withId(id).withSize(dx, dy, dz).withWeight(1).build();
		return new Placement(box.getStackValue(0), 0, x, y, z);
	}

	private List<ValidatorResultReason> validate(ContainerAccess access, Placement... placements) {
		List<ValidatorResultReason> reasons = new ArrayList<>();
		validator.validate(List.of(placements), access, reasons);
		return reasons;
	}

	//
	//  z
	//  2 +-------+
	//    |   B   |   B rests on A
	//  1 +---+---+
	//    | A |
	//  0 +---+
	//    0   1   2  x
	//
	@Test
	void boxIsInsertedAfterTheBoxesItRestsOn() {
		Placement a = place("A", 1, 1, 1, 0, 0, 0);
		Placement b = place("B", 2, 1, 1, 0, 0, 1);

		assertThat(validate(ContainerAccess.ANY, a, b)).isEmpty();
		assertThat(validate(ContainerAccess.ANY, b, a)).singleElement().isInstanceOf(InsertedBeforeSupporterReason.class);
	}

	//
	//  z
	//  2 +-------+
	//    |   B   |   C is placed into the gap under the overhang of B
	//  1 +---+---+
	//    | A | C |
	//  0 +---+---+
	//    0   1   2  x
	//
	@Test
	void boxPlacedIntoAGapUnderAnOverhangMustBeInsertedFirst() {
		Placement a = place("A", 1, 1, 1, 0, 0, 0);
		Placement b = place("B", 2, 1, 1, 0, 0, 1);
		Placement c = place("C", 1, 1, 1, 1, 0, 0);

		assertThat(validate(ContainerAccess.ANY, a, b, c)).singleElement().isInstanceOf(InsertedBeforeSupporterReason.class);
		assertThat(validate(ContainerAccess.ANY, a, c, b)).isEmpty();
	}

	//
	//  side view (z up), door at x = 2
	//
	//  1 +---+---+
	//    | A | B |   door ->
	//  0 +---+---+
	//    0   1   2  x
	//
	@Test
	void boxNearerTheDoorBlocksTheBoxesBehindIt() {
		Placement a = place("A", 1, 1, 1, 0, 0, 0);
		Placement b = place("B", 1, 1, 1, 1, 0, 0);

		assertThat(validate(ContainerAccess.FRONT, a, b)).isEmpty();
		List<ValidatorResultReason> reasons = validate(ContainerAccess.FRONT, b, a);
		assertThat(reasons).singleElement().isInstanceOf(BlockedInsertionReason.class);
		assertThat(((BlockedInsertionReason)reasons.get(0)).getPlacement()).isSameAs(a);
		assertThat(((BlockedInsertionReason)reasons.get(0)).getBlocking()).isSameAs(b);
		// from above, side by side boxes do not block each other
		assertThat(validate(ContainerAccess.TOP, b, a)).isEmpty();
	}

	//
	//  z
	//  2     +---+
	//        | B |   B is above A, without touching it
	//  1     +---+
	//
	//  0 +---+
	//    | A |       (A rests on the floor; the gap is between them)
	//    0   1   2  x
	//
	@Test
	void boxAboveBlocksInsertionFromTheTop() {
		Placement a = place("A", 2, 1, 1, 0, 0, 0);
		Placement b = place("B", 1, 1, 1, 1, 0, 2);

		assertThat(validate(ContainerAccess.TOP, a, b)).isEmpty();
		assertThat(validate(ContainerAccess.TOP, b, a)).singleElement().isInstanceOf(BlockedInsertionReason.class);
		assertThat(validate(ContainerAccess.ANY, b, a)).isEmpty();
	}
}
