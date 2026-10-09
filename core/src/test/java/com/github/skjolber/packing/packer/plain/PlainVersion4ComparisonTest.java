package com.github.skjolber.packing.packer.plain;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.Version4Comparison;
import com.github.skjolber.packing.packer.Version4Comparison.Tally;
import com.github.skjolber.packing.packer.Version4Orders;
import com.github.skjolber.packing.packer.Version4Orders.Scenario;

/**
 * The plain packager against that of version 4 (module shadowed-v4 in legacy/v4) on random orders ({@linkplain Version4Orders}): 5.x must do
 * at least as well as 4.x overall. An order is won if 5.x packs all boxes where 4.x does not, or into fewer containers; lost in the opposite
 * case; otherwise tied. This test is a smoke test of {@value #SEEDS} orders: results must be valid (stacked within the constraints), the orders
 * must be meaningful (see {@linkplain Version4Comparison#assertMeaningful}), and 5.x loses at most
 * {@value Version4Comparison#MAXIMUM_LOSS_PERCENT} percent of the compared orders. {@linkplain PlainVersion4ComparisonIT} adds many more orders,
 * over which 5.x must also win at least as many orders as it loses.
 * <p>
 * The contract is aggregate, because "5.x is at least as good as 4.x on every order" is wrong: 5.x enumerates the candidate placements in a
 * different order than 4.x, and where two candidates rank exactly equal (for example the same box turned 2x8x7 or 8x2x7, which is where the layouts
 * of the two versions almost always first diverge) it picks a different one, which later is better or worse for the order as a whole. Small
 * samples mislead: over the orders of seeds 0 to 299 against 4.2.5 the plain packager wins 3 and loses 4 orders (within the tolerance of this
 * test), over those of seeds 300 to 10,299 it wins 229 and loses 89, and ties the remaining 9,682. Against 4.2.4, whose plain ranking had an
 * inverted z tiebreak which stacked towers, it was 2 against 8, and 160 against 120.
 * <p>
 * The 4.x plain packager throws no exceptions for these orders, so none are left out (the fast largest area fit first packager of
 * 4.2.4 did, see {@code LargestAreaFitFirstVersion4ComparisonTest}).
 */
class PlainVersion4ComparisonTest {

	/** Seeds of this test; {@linkplain PlainVersion4ComparisonIT} continues with the following seeds */
	static final int SEEDS = 300;

	@Test
	void doesNotLoseToVersion4() {
		Tally tally = compare(0, SEEDS);
		Version4Comparison.assertMeaningful(tally, SEEDS);
		Version4Comparison.assertFewLosses(tally);
	}

	/**
	 * @param from the first seed
	 * @param to the seed after the last
	 * @return the outcome
	 */
	static Tally compare(long from, long to) {
		return Version4Comparison.compare(from, to, PlainVersion4ComparisonTest::pack, PlainVersion4ComparisonTest::packVersion4, e -> false);
	}

	private static PackagerResult pack(Scenario scenario) {
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			return Version4Orders.pack(packager, scenario);
		}
	}

	private static com.github.skjolber.packing.v4.api.PackagerResult packVersion4(Scenario scenario) {
		return Version4Orders.packVersion4(com.github.skjolber.packing.v4.packer.plain.PlainPackager.newBuilder().build(), scenario);
	}
}
