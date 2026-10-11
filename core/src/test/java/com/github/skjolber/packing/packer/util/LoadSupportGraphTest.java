package com.github.skjolber.packing.packer.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;

/**
 * Box count checks, load propagation and relief visit each placement once instead of once per path,
 * which matters for staggered (brick) stacking where the number of paths grows exponentially with
 * the height.
 *
 * <pre>
 *  row 2   |  2,0  |  2,1  |  2,2  |
 *  row 1 |  1,0  |  1,1  |  1,2  |  1,3  |
 *  row 0   |  0,0  |  0,1  |  0,2  |
 *
 *  each brick rests on the two bricks below it (where present)
 * </pre>
 */
class LoadSupportGraphTest {

	private static final int WIDTH = 4;

	@Test
	void sameResultsAsRecursiveChecks() {
		Random random = new Random(1);
		for(int run = 0; run < 50; run++) {
			int rows = 2 + random.nextInt(9);
			Placement[][] wall = wall(rows, random, 1 + random.nextInt(rows + 1));
			TestUtility utility = utility(wall);
			for(Placement[] row : wall) {
				for(Placement placement : row) {
					// load propagated once per placement equals the sum over all paths
					assertThat(placement.getLoadWeight()).isCloseTo(accumulateWeight(placement), within(1e-9));
					for(int levels = 1; levels <= rows + 1; levels++) {
						assertThat(utility.isWithinMaxLoadBoxCount(placement, levels)).isEqualTo(placement.isWithinMaxLoadBoxCount(levels));
						assertThat(utility.isWithinSupporteeBoxCount(placement, levels)).isEqualTo(isWithinSupporteeBoxCount(placement, levels));
					}
				}
			}
		}
	}

	@Test
	void reliefSpreadOncePerPlacementEqualsSumOverPaths() {
		int rows = 8;
		Placement[][] wall = wall(rows, new Random(3), -1);
		TestUtility utility = utility(wall);
		int count = 0;
		for(Placement[] row : wall) {
			count += row.length;
		}
		double[] expected = new double[count];
		Placement top = wall[rows - 1][1];

		utility.resetReliefWeights();
		utility.calculateRelifWeight(top, 1.0);
		relief(top, 1.0, expected);

		for(Placement[] row : wall) {
			for(Placement placement : row) {
				assertThat(utility.reliefWeights[placement.getIndex()]).isCloseTo(expected[placement.getIndex()], within(1e-12));
			}
		}
	}

	/** Reference: relief added once per path. */
	private static void relief(Placement placement, double reliefWeight, double[] reliefWeights) {
		for(PlacementLoad link : placement.getSupporters()) {
			double r = reliefWeight * link.getArea() / placement.getSupportedArea();
			reliefWeights[link.getPlacement().getIndex()] += r;
			relief(link.getPlacement(), r, reliefWeights);
		}
	}

	@Test
	void tallBrickWallIsFast() {
		int rows = 40;
		// no limits; building the wall also propagates load down all paths
		Placement[][] wall = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> wall(rows, new Random(2), -1));
		TestUtility utility = utility(wall);

		assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
			// exponential (2^40 paths) without memoization
			assertThat(utility.isWithinMaxLoadBoxCount(wall[rows - 1][0], 1)).isTrue();
			assertThat(utility.isWithinSupporteeBoxCount(wall[0][1], rows + 1)).isTrue();
			assertThat(utility.isWithinSupporteeBoxCount(wall[0][1], rows - 1)).isFalse();
			utility.resetReliefWeights();
			utility.calculateRelifWeight(wall[rows - 1][0], 1.0);
		});
	}

	/**
	 * Reference: the load over all paths: for each supportee, the weight share of its link plus its
	 * own load in proportion to the contact area.
	 */
	private static double accumulateWeight(Placement placement) {
		double total = 0.0;
		for(PlacementLoad link : placement.getSupportees()) {
			Placement supportee = link.getPlacement();
			total += link.getWeight() + accumulateWeight(supportee) * link.getArea() / supportee.getSupportedArea();
		}
		return total;
	}

	/** Reference: the recursive check, which walks every path. */
	private static boolean isWithinSupporteeBoxCount(Placement candidate, int count) {
		if (count <= 0) {
			return false;
		}
		for (PlacementLoad supportee : candidate.getSupportees()) {
			if (!isWithinSupporteeBoxCount(supportee.getPlacement(), count - 1)) {
				return false;
			}
		}
		return true;
	}

	/** Bricks of 2x1x1; odd rows are shifted by half a brick and have one brick more. */
	private static Placement[][] wall(int rows, Random random, int maxLimit) {
		Placement[][] wall = new Placement[rows][];
		int index = 0;
		for(int row = 0; row < rows; row++) {
			boolean shifted = row % 2 == 1;
			int count = shifted ? WIDTH + 1 : WIDTH;
			wall[row] = new Placement[count];
			for(int i = 0; i < count; i++) {
				Box.Builder builder = Box.newBuilder().withSize(2, 1, 1).withWeight(1);
				if(maxLimit >= 0 && random.nextInt(3) == 0) {
					builder.withMaxLoadBoxCount(random.nextInt(maxLimit + 1));
				}
				int x = shifted ? 2 * i - 1 : 2 * i;
				Placement placement = new Placement(builder.build().getStackValue(0), 0, Math.max(0, x), 0, row);
				placement.setIndex(index++);
				wall[row][i] = placement;
			}
			if(row > 0) {
				for(Placement placement : wall[row]) {
					for(Placement below : wall[row - 1]) {
						if(below.getAbsoluteX() <= placement.getAbsoluteEndX() && placement.getAbsoluteX() <= below.getAbsoluteEndX()) {
							below.addLoad(placement, 1, 0.5);
						}
					}
				}
			}
		}
		return wall;
	}

	private static TestUtility utility(Placement[][] wall) {
		Stack stack = new Stack();
		int count = 0;
		for(Placement[] row : wall) {
			count += row.length;
		}
		TestUtility utility = new TestUtility(stack);
		utility.initialize(count);
		return utility;
	}

	private static class TestUtility extends WeightPressureCountLoadAwarePlacementUtility {
		TestUtility(Stack stack) {
			super(stack);
		}
	}
}
