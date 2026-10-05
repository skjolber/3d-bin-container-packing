package com.github.skjolber.packing.validator.load;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Unloading;
import com.github.skjolber.packing.test.assertj.PlacementsAssert;
import com.github.skjolber.packing.validator.load.reasons.ExcessiveLoadBoxCountReason;
import com.github.skjolber.packing.validator.load.reasons.ExcessiveLoadPressureReason;
import com.github.skjolber.packing.validator.load.reasons.ExcessiveLoadWeightReason;
import com.github.skjolber.packing.validator.load.reasons.NonIdenticalLoadBoxReason;
import com.github.skjolber.packing.validator.stability.FullySupportedStabilityValidator;
import com.github.skjolber.packing.validator.stability.reasons.InsufficientSupportAreaReason;

/**
 * The validators find which boxes rest on which from the placements' positions, so results from packagers which do not
 * record support links (for example packagers without load limits) are validated too.
 *
 * <pre>
 *  z
 *  3 +---+
 *    | 3 |
 *  2 +---+
 *    | 2 |
 *  1 +---+
 *    | 1 |   weight 5 each, no support links recorded
 *  0 +---+
 *    0   1  x
 * </pre>
 */
public class SupportFromGeometryTest {

	private static List<Placement> column(Box.Builder builder) {
		List<Placement> placements = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			Box box = builder.withId(Integer.toString(i + 1)).build();
			new BoxItem(box);
			placements.add(new Placement(box.getStackValue(0), 0, 0, 0, i));
		}
		return placements;
	}

	private static Box.Builder box() {
		return Box.newBuilder().withSize(1, 1, 1).withWeight(5);
	}

	@Test
	void weight() {
		// the bottom box carries 10
		PlacementsAssert.assertThat(column(box().withMaxLoadWeight(6))).isRejectedBy(new WeightLoadValidator(), ExcessiveLoadWeightReason.class);
		PlacementsAssert.assertThat(column(box().withMaxLoadWeight(10))).isAcceptedBy(new WeightLoadValidator());
	}

	@Test
	void pressure() {
		PlacementsAssert.assertThat(column(box().withMaxLoadPressure(6))).isRejectedBy(new MaxPressureLoadValidator(), ExcessiveLoadPressureReason.class);
		PlacementsAssert.assertThat(column(box().withMaxLoadPressure(10))).isAcceptedBy(new MaxPressureLoadValidator());
	}

	@Test
	void boxCount() {
		PlacementsAssert.assertThat(column(box().withMaxLoadBoxCount(1))).isRejectedBy(new MaxBoxCountLoadValidator(), ExcessiveLoadBoxCountReason.class);
		PlacementsAssert.assertThat(column(box().withMaxLoadBoxCount(2))).isAcceptedBy(new MaxBoxCountLoadValidator());
	}

	@Test
	void identicalBoxOnly() {
		// every box is its own box item: 1 carries 2 and 3, 2 carries 3
		PlacementsAssert.assertThat(column(box().withMaxLoadIdenticalBoxCount(-1))).isRejectedBy(new IdenticalBoxOnlyLoadValidator(),
				NonIdenticalLoadBoxReason.class, NonIdenticalLoadBoxReason.class, NonIdenticalLoadBoxReason.class);
	}

	//
	//  z
	//  2     +-------+
	//        |   2   |   half of box 2 overhangs
	//  1 +---+---+---+
	//    |   1   |
	//  0 +-------+
	//    0   1   2   3  x
	//
	@Test
	void support() {
		Box bottom = Box.newBuilder().withId("1").withSize(2, 1, 1).withWeight(1).build();
		Box top = Box.newBuilder().withId("2").withSize(2, 1, 1).withWeight(1).build();
		List<Placement> placements = List.of(new Placement(bottom.getStackValue(0), 0, 0, 0, 0), new Placement(top.getStackValue(0), 0, 1, 0, 1));

		PlacementsAssert.assertThat(placements).isRejectedBy(new FullySupportedStabilityValidator(), InsufficientSupportAreaReason.class);
	}

	//
	//  z
	//  2 +-------+
	//    |   2   |   placed second, on box 1, overhanging
	//  1 +---+---+---+
	//    | 1 | 3 |       box 3 is placed last, into the gap under the overhang:
	//  0 +---+---+       it touches box 2, so it carries half of it
	//    0   1   2  x
	//
	@Test
	void boxPlacedUnderAnOverhangCarriesTheBoxesAboveIt() {
		Box one = Box.newBuilder().withId("1").withSize(1, 1, 1).withWeight(1).build();
		Box two = Box.newBuilder().withId("2").withSize(2, 1, 1).withWeight(5).build();
		Box three = Box.newBuilder().withId("3").withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(1).build();
		List<Placement> placements = List.of(
				new Placement(one.getStackValue(0), 0, 0, 0, 0),
				new Placement(two.getStackValue(0), 0, 0, 0, 1),
				new Placement(three.getStackValue(0), 0, 1, 0, 0));

		// box 3 carries 2.5, regardless of the loading order
		PlacementsAssert.assertThat(placements).isRejectedBy(new WeightLoadValidator(), ExcessiveLoadWeightReason.class);
		PlacementsAssert.assertThat(List.of(placements.get(0), placements.get(2), placements.get(1))).isRejectedBy(new WeightLoadValidator(), ExcessiveLoadWeightReason.class);
	}

	//
	//  z
	//  2 +-------+
	//    |   2   |   weight 4, placed second, on box 1, overhanging
	//  1 +---+---+
	//    | 1 | 3 |   box 1 carries at most 3; box 3 is placed last, into the gap under the overhang
	//  0 +---+---+
	//    0   1   2  x
	//
	@Test
	void boxPlacedUnderAnOverhangRelievesOnlyWhenUnloadedInReverseOrder() {
		Box one = Box.newBuilder().withId("1").withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(3).build();
		Box two = Box.newBuilder().withId("2").withSize(2, 1, 1).withWeight(4).build();
		Box three = Box.newBuilder().withId("3").withSize(1, 1, 1).withWeight(1).build();
		List<Placement> placements = List.of(
				new Placement(one.getStackValue(0), 0, 0, 0, 0),
				new Placement(two.getStackValue(0), 0, 0, 0, 1),
				new Placement(three.getStackValue(0), 0, 1, 0, 0));

		// box 3 may be unloaded first: box 1 carries all of box 2
		PlacementsAssert.assertThat(placements).isRejectedBy(new WeightLoadValidator(Unloading.ANY_ORDER), ExcessiveLoadWeightReason.class);
		PlacementsAssert.assertThat(placements).isRejectedBy(new FullySupportedStabilityValidator(Unloading.ANY_ORDER), InsufficientSupportAreaReason.class);
		// box 3 stays until box 2 is unloaded: they share box 2, and box 2 is fully supported
		PlacementsAssert.assertThat(placements).isAcceptedBy(new WeightLoadValidator(Unloading.REVERSE_LOADING_ORDER));
		PlacementsAssert.assertThat(placements).isAcceptedBy(new FullySupportedStabilityValidator(Unloading.REVERSE_LOADING_ORDER));
	}
}
