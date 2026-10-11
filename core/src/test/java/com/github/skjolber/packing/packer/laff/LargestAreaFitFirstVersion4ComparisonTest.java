package com.github.skjolber.packing.packer.laff;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.Version4Comparison;
import com.github.skjolber.packing.packer.Version4Comparison.Tally;
import com.github.skjolber.packing.packer.Version4Orders;
import com.github.skjolber.packing.packer.Version4Orders.Scenario;

/**
 * The largest area fit first packagers against those of version 4 (module shadowed-v4 in legacy/v4) on random orders
 * ({@linkplain Version4Orders}): 5.x must do at least as well as 4.x overall. An order is won if 5.x packs all boxes where 4.x does not, or
 * into fewer containers; lost in the opposite case; otherwise tied. This test is a smoke test of {@value #SEEDS} orders: results must be valid
 * (stacked within the constraints), the orders must be meaningful (see {@linkplain Version4Comparison#assertMeaningful}), and 5.x loses at most
 * {@value Version4Comparison#MAXIMUM_LOSS_PERCENT} percent of the compared orders.
 * {@linkplain LargestAreaFitFirstVersion4ComparisonIT} adds many more orders, over which 5.x must also win at least as many orders as it loses.
 * <p>
 * The contract is aggregate, because "5.x is at least as good as 4.x on every order" is wrong: 5.x enumerates the candidate placements in a
 * different order than 4.x, and where two candidates rank exactly equal (for example the same box turned 2x8x7 or 8x2x7, which is where the layouts
 * of the two versions almost always first diverge) it picks a different one, which later is better or worse for the order as a whole. Over the
 * orders of seeds 300 to 10,299 against 4.2.5, the largest area fit first packager wins 824 and loses 11 orders, and the fast one wins 750 and
 * loses 9. Small samples mislead in either direction, so the smoke test only limits the share of losses.
 * <p>
 * 5.x is not identical to 4.x: when no box fits a new level, it raises the level below to the top of the container (see
 * {@linkplain RaisedLevelTest}), so it packs some orders into fewer containers, or packs them where 4.x does not. Most orders get the
 * same placements.
 * <p>
 * Version 4.2.4 of the fast packager threw a {@code NullPointerException} from its 2D point calculator for some orders (fixed in
 * 4.2.5); such orders are left out.
 */
class LargestAreaFitFirstVersion4ComparisonTest {

	/** Seeds of this test; {@linkplain LargestAreaFitFirstVersion4ComparisonIT} continues with the following seeds */
	static final int SEEDS = 300;

	@ParameterizedTest(name = "fast={0}")
	@ValueSource(booleans = { false, true })
	void doesNotLoseToVersion4(boolean fast) {
		Tally tally = compare(fast, 0, SEEDS);
		Version4Comparison.assertMeaningful(tally, SEEDS);
		Version4Comparison.assertFewLosses(tally);
	}

	/**
	 * @param fast whether to compare the fast packagers
	 * @param from the first seed
	 * @param to the seed after the last
	 * @return the outcome
	 */
	static Tally compare(boolean fast, long from, long to) {
		return Version4Comparison.compare(from, to, scenario -> pack(fast, scenario), scenario -> packVersion4(fast, scenario),
				e -> fast && isVersion4PointCalculatorFailure(e));
	}

	/**
	 * The fast packager of 4.2.4 can pass a point index which is out of range to its 2D point calculator. Once thrown often, the JVM
	 * throws the exception without a stack trace, so an exception without one is taken to be the same.
	 */
	private static boolean isVersion4PointCalculatorFailure(NullPointerException e) {
		StackTraceElement[] stack = e.getStackTrace();
		return stack.length == 0 || stack[0].getClassName().equals(com.github.skjolber.packing.v4.ep.points2d.DefaultPointCalculator2D.class.getName());
	}

	private static PackagerResult pack(boolean fast, Scenario scenario) {
		try (AbstractPackager<?> packager = fast ? FastLargestAreaFitFirstPackager.newBuilder().build() : LargestAreaFitFirstPackager.newBuilder().build()) {
			return Version4Orders.pack(packager, scenario);
		}
	}

	private static com.github.skjolber.packing.v4.api.PackagerResult packVersion4(boolean fast, Scenario scenario) {
		if(fast) {
			return Version4Orders.packVersion4(com.github.skjolber.packing.v4.packer.laff.FastLargestAreaFitFirstPackager.newBuilder().build(), scenario);
		}
		return Version4Orders.packVersion4(com.github.skjolber.packing.v4.packer.laff.LargestAreaFitFirstPackager.newBuilder().build(), scenario);
	}
}
