package com.github.skjolber.packing.validator.load;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
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
			BoxItem item = new BoxItem(box);
			placements.add(new Placement(item, box.getStackValue(0), 0, 0, 0, i));
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
	//    |   2   |   weight 4, on box 1, overhanging
	//  1 +---+---+
	//    | 1 | 3 |   box 1 carries at most 3; box 3 is in the gap under the overhang
	//  0 +---+---+
	//    0   1   2  x
	//
	@Test
	void boxUnderAnOverhangSharesTheLoad() {
		Box one = Box.newBuilder().withId("1").withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(3).build();
		Box two = Box.newBuilder().withId("2").withSize(2, 1, 1).withWeight(4).build();
		Box three = Box.newBuilder().withId("3").withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(1).build();
		Placement p1 = new Placement(one.getStackValue(0), 0, 0, 0, 0);
		Placement p2 = new Placement(two.getStackValue(0), 0, 0, 0, 1);
		Placement p3 = new Placement(three.getStackValue(0), 0, 1, 0, 0);

		// boxes 1 and 3 carry 2 each: box 1 is within its limit, box 3 is not
		List<ValidatorResultReason> reasons = new ArrayList<>();
		assertThat(new WeightLoadValidator().isValid(List.of(p1, p3, p2), reasons)).isFalse();
		assertThat(reasons).singleElement().isInstanceOf(ExcessiveLoadWeightReason.class);
		assertThat(((ExcessiveLoadWeightReason)reasons.get(0)).getPlacement()).isSameAs(p3);

		// box 2 is fully supported
		PlacementsAssert.assertThat(List.of(p1, p3, p2)).isAcceptedBy(new FullySupportedStabilityValidator());
	}
}
