package com.github.skjolber.packing.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.test.assertj.PlacementsAssert;
import com.github.skjolber.packing.validator.load.IdenticalBoxOnlyLoadValidator;
import com.github.skjolber.packing.validator.load.MaxBoxCountLoadValidator;
import com.github.skjolber.packing.validator.load.MaxPressureLoadValidator;
import com.github.skjolber.packing.validator.load.WeightLoadValidator;
import com.github.skjolber.packing.validator.load.reasons.NonIdenticalLoadBoxReason;
import com.github.skjolber.packing.validator.stability.CenterOfGravityStabilityValidator;

/**
 * Validators walk the support graph once per placement instead of once per path: with staggered
 * (brick) stacking, the number of paths grows exponentially with the height.
 *
 * <pre>
 *  row 2   |  2,0  |  2,1  |  2,2  |
 *  row 1 |  1,0  |  1,1  |  1,2  |  1,3  |
 *  row 0   |  0,0  |  0,1  |  0,2  |
 * </pre>
 */
public class SupportGraphValidatorsTest {

	private static final int ROWS = 40;
	private static final int WIDTH = 4;

	@Test
	void tallBrickWallIsValidatedQuickly() {
		List<Placement> wall = wall(ROWS);

		assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
			PlacementsAssert.assertThat(wall).isAcceptedBy(new WeightLoadValidator());
			PlacementsAssert.assertThat(wall).isAcceptedBy(new MaxPressureLoadValidator());
			PlacementsAssert.assertThat(wall).isAcceptedBy(new MaxBoxCountLoadValidator());
			PlacementsAssert.assertThat(wall).isAcceptedBy(new IdenticalBoxOnlyLoadValidator());
			new CenterOfGravityStabilityValidator().isValid(wall, new ArrayList<>());
		});
	}

	/**
	 * A box reached through two paths is reported once.
	 *
	 * <pre>
	 *  z |
	 *  3     +----------+
	 *        |  other   |        ← not identical to X: one reason
	 *  2 +---+------+---+------+
	 *    |    X     |    X     |
	 *  1 +---+------+---+------+
	 *        |  X (identical only)
	 *  0     +----------+
	 *    0   5     10  15     20  x
	 * </pre>
	 */
	@Test
	void nonIdenticalBoxReachedThroughTwoPathsIsReportedOnce() {
		BoxItem identical = new BoxItem(Box.newBuilder().withId("X").withSize(10, 10, 1).withWeight(1).withMaxLoadIdenticalBoxCount(-1).build());
		BoxItem other = new BoxItem(Box.newBuilder().withId("other").withSize(10, 10, 1).withWeight(1).build());

		Placement bottom = new Placement(identical.getBox().getStackValue(0), 0, 5, 0, 0);
		Placement left = new Placement(identical.getBox().getStackValue(0), 0, 0, 0, 1);
		Placement right = new Placement(identical.getBox().getStackValue(0), 0, 10, 0, 1);
		Placement top = new Placement(other.getBox().getStackValue(0), 0, 5, 0, 2);
		bottom.addLoad(left, 50, 1);
		bottom.addLoad(right, 50, 1);
		left.addLoad(top, 50, 0.5);
		right.addLoad(top, 50, 0.5);

		List<ValidatorResultReason> reasons = new ArrayList<>();
		new IdenticalBoxOnlyLoadValidator().isValid(List.of(bottom), reasons);

		assertThat(reasons).hasSize(1);
		assertThat(reasons.get(0)).isInstanceOf(NonIdenticalLoadBoxReason.class);
	}

	/** Bricks of 2x1x1 of the same box item; odd rows are shifted by half a brick. */
	private static List<Placement> wall(int rows) {
		Box box = Box.newBuilder()
				.withId("brick")
				.withSize(2, 1, 1)
				.withWeight(1)
				.withMaxLoadWeight(1_000_000)
				.withMaxLoadPressure(1_000_000)
				.withMaxLoadBoxCount(1_000)
				.build();
		new BoxItem(box);
		List<Placement> placements = new ArrayList<>();
		Placement[] previous = null;
		for(int row = 0; row < rows; row++) {
			boolean shifted = row % 2 == 1;
			int count = shifted ? WIDTH + 1 : WIDTH;
			Placement[] current = new Placement[count];
			for(int i = 0; i < count; i++) {
				int x = shifted ? Math.max(0, 2 * i - 1) : 2 * i;
				current[i] = new Placement(box.getStackValue(0), 0, x, 0, row);
				placements.add(current[i]);
			}
			if(previous != null) {
				for(Placement placement : current) {
					List<Placement> supporters = new ArrayList<>();
					for(Placement below : previous) {
						if(below.getAbsoluteX() <= placement.getAbsoluteEndX() && placement.getAbsoluteX() <= below.getAbsoluteEndX()) {
							supporters.add(below);
						}
					}
					for(Placement below : supporters) {
						below.addLoad(placement, 1, 1.0 / supporters.size());
					}
				}
			}
			previous = current;
		}
		return placements;
	}
}
