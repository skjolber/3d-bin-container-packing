package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.Version4Orders.Scenario;
import com.github.skjolber.packing.packer.Version4Orders.Summary;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;

/**
 * Compares a packager of 5.x with the same packager of version 4 (module shadowed-v4 in legacy/v4) on {@linkplain Version4Orders random orders},
 * and checks the aggregate contract of the comparison tests.
 * <p>
 * The contract is aggregate, not per order: 5.x enumerates candidate placements in a different order than 4.x, so where two candidates rank
 * exactly equal (for example the same box turned 2x8x7 or 8x2x7) the two versions pick different ones, and the layouts diverge, to the advantage of
 * either version. What can be required is that 5.x does not do worse than 4.x overall: of the compared orders it loses few
 * ({@linkplain #assertFewLosses(Tally)}) and, over many orders, wins at least as many as it loses ({@linkplain #assertWinsAtLeastLosses(Tally)}).
 * <p>
 * A result is better if all boxes are packed and the other is not, then if it uses fewer containers (see {@linkplain Summary#compareTo(Summary)}).
 */
public final class Version4Comparison {

	/** The most orders, in percent of the compared orders, which 5.x may lose */
	public static final int MAXIMUM_LOSS_PERCENT = 5;

	/** The number of seeds of orders which 5.x lost, kept in the tally */
	private static final int LOSS_SEEDS = 5;

	/** Packs an order with a packager of 5.x */
	@FunctionalInterface
	public interface Packing {

		PackagerResult pack(Scenario scenario);
	}

	/** Packs an order with a packager of version 4 */
	@FunctionalInterface
	public interface Version4Packing {

		com.github.skjolber.packing.v4.api.PackagerResult pack(Scenario scenario);
	}

	/**
	 * The outcome of comparing a range of orders.
	 *
	 * @param compared the number of orders which both versions packed
	 * @param wins the number of orders which 5.x packed better than 4.x
	 * @param losses the number of orders which 5.x packed worse than 4.x
	 * @param ties the number of orders which both versions packed equally well (including those with the same placements)
	 * @param identical the number of orders which both versions packed with the same placements
	 * @param version4Success the number of compared orders for which 4.x packed all boxes
	 * @param version4Failed the number of orders which are left out because 4.x failed (see {@linkplain #compare})
	 * @param lossSeeds the seeds of the first orders which 5.x lost
	 */
	public record Tally(int compared, int wins, int losses, int ties, int identical, int version4Success, int version4Failed, List<Long> lossSeeds) {
	}

	private Version4Comparison() {
	}

	/**
	 * Pack the orders of a range of seeds with both versions, check that the results of 5.x are valid, and tally which version did better.
	 *
	 * @param from the first seed
	 * @param to the seed after the last
	 * @param packing packs an order with the packager of 5.x
	 * @param version4Packing packs an order with the same packager of version 4
	 * @param version4Failure whether an exception of 4.x is a known failure of 4.x: that order is left out. Other exceptions are thrown.
	 * @return the outcome
	 */
	public static Tally compare(long from, long to, Packing packing, Version4Packing version4Packing, Predicate<NullPointerException> version4Failure) {
		int compared = 0;
		int wins = 0;
		int losses = 0;
		int identical = 0;
		int version4Success = 0;
		int version4Failed = 0;
		List<Long> lossSeeds = new ArrayList<>();
		for (long seed = from; seed < to; seed++) {
			Scenario scenario = Version4Orders.random(seed);

			Summary expected;
			try {
				expected = Version4Orders.summary(version4Packing.pack(scenario));
			} catch (NullPointerException e) {
				if(version4Failure.test(e)) {
					version4Failed++;
					continue;
				}
				throw e;
			}

			PackagerResult result = packing.pack(scenario);
			if(!result.getContainers().isEmpty()) {
				PackagerResultAssert.assertThat(result).isStackedWithinConstraints();
			}
			Summary actual = Version4Orders.summary(result);

			compared++;
			int comparison = actual.compareTo(expected);
			if(comparison > 0) {
				wins++;
			} else if(comparison < 0) {
				losses++;
				if(lossSeeds.size() < LOSS_SEEDS) {
					lossSeeds.add(seed);
				}
			}
			if(actual.equals(expected)) {
				identical++;
			}
			if(expected.success()) {
				version4Success++;
			}
		}
		return new Tally(compared, wins, losses, compared - wins - losses, identical, version4Success, version4Failed, lossSeeds);
	}

	/**
	 * The comparison is only meaningful if 4.x packs the orders: most orders are compared, and 4.x packs all boxes of many of them.
	 *
	 * @param tally the outcome
	 * @param orders the number of orders
	 */
	public static void assertMeaningful(Tally tally, int orders) {
		assertThat(tally.compared()).as("%s", tally).isGreaterThanOrEqualTo(orders * 9 / 10);
		assertThat(tally.version4Success()).as("%s", tally).isGreaterThanOrEqualTo(tally.compared() / 2);
	}

	/**
	 * 5.x loses at most {@linkplain #MAXIMUM_LOSS_PERCENT} percent of the compared orders.
	 *
	 * @param tally the outcome
	 */
	public static void assertFewLosses(Tally tally) {
		assertThat(tally.losses() * 100).as("losses of at most %s%% of the compared orders: %s", MAXIMUM_LOSS_PERCENT, tally)
				.isLessThanOrEqualTo(tally.compared() * MAXIMUM_LOSS_PERCENT);
	}

	/**
	 * 5.x wins at least as many orders as it loses; meaningful for many orders only, as a few can go either way.
	 *
	 * @param tally the outcome
	 */
	public static void assertWinsAtLeastLosses(Tally tally) {
		assertThat(tally.wins()).as("wins at least as many as losses: %s", tally).isGreaterThanOrEqualTo(tally.losses());
	}
}
