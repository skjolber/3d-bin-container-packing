package com.github.skjolber.packing.packer.bruteforce;

import static com.github.skjolber.packing.packer.PackagerGoldenMasterTest.LOAD_NONE;
import static com.github.skjolber.packing.packer.PackagerGoldenMasterTest.LOAD_WEIGHT;
import static com.github.skjolber.packing.packer.PackagerGoldenMasterTest.pack;
import static com.github.skjolber.packing.packer.PackagerGoldenMasterTest.packSummary;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.packer.AbstractPackager;

/**
 * The brute-force packagers compare results whose stacks are not built (they share reused placements until a result
 * is accepted): result comparators read their load volume, weight and box count. The default result comparator gives
 * the same results as a reference comparator of the same values which never reads the stacks (the parallel packager:
 * the same summaries, as it chooses among equal results in the order its threads finish). Box item groups, as the
 * search of the group orders compares the results of different orders, which share placements.
 */
public class BruteForceResultComparatorTest {

	private static final int SEEDS = 10;

	/** Compares like {@link DefaultIntermediatePackagerResultComparator}, by the results' values (never their stacks) */
	private static final IntermediatePackagerResultComparator REFERENCE = new IntermediatePackagerResultComparator() {

		@Override
		public boolean prefersHigherLoadVolume() {
			return true;
		}

		@Override
		public int compare(IntermediatePackagerResult r1, IntermediatePackagerResult r2) {
			int compare = Long.compare(r1.getLoadVolume(), r2.getLoadVolume());
			if(compare == 0) {
				compare = Long.compare(r1.getLoadWeight(), r2.getLoadWeight());
			}
			if(compare == 0) {
				compare = Integer.compare(r1.getBoxCount(), r2.getBoxCount());
			}
			if(compare != 0 || r1.getBoxCount() == 0) {
				return compare;
			}
			// smaller container, then lighter container
			Container c1 = r1.getContainerItem().getContainer();
			Container c2 = r2.getContainerItem().getContainer();
			compare = Long.compare(c2.getVolume(), c1.getVolume());
			if(compare == 0) {
				compare = Long.compare(c2.getEmptyWeight(), c1.getEmptyWeight());
			}
			return compare;
		}
	};

	@Test
	public void defaultComparatorGivesTheResultsOfTheBruteForceComparator() {
		List<Function<IntermediatePackagerResultComparator, AbstractPackager<?>>> packagers = List.of(
				comparator -> BruteForcePackager.newBuilder().withIntermediatePackagerResultComparator(comparator).build(),
				comparator -> FastBruteForcePackager.newBuilder().withIntermediatePackagerResultComparator(comparator).build(),
				comparator -> ParallelBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(4).withIntermediatePackagerResultComparator(comparator).build());
		for (Function<IntermediatePackagerResultComparator, AbstractPackager<?>> factory : packagers) {
			try (AbstractPackager<?> bruteForceComparator = factory.apply(REFERENCE);
					AbstractPackager<?> defaultComparator = factory.apply(new DefaultIntermediatePackagerResultComparator())) {
				for (int seed = 0; seed < SEEDS; seed++) {
					for (int load : new int[] { LOAD_NONE, LOAD_WEIGHT }) {
						// 2 to 4 box items of 1 or 2 boxes, in groups of two box items
						if(bruteForceComparator instanceof ParallelBruteForcePackager) {
							assertThat(packSummary(defaultComparator, seed, 4, 2, true, load))
									.as("%s seed %d load %d", bruteForceComparator.getClass().getSimpleName(), seed, load)
									.isEqualTo(packSummary(bruteForceComparator, seed, 4, 2, true, load));
						} else {
							assertThat(pack(defaultComparator, seed, 4, 2, true, load))
									.as("%s seed %d load %d", bruteForceComparator.getClass().getSimpleName(), seed, load)
									.isEqualTo(pack(bruteForceComparator, seed, 4, 2, true, load));
						}
					}
				}
			}
		}
	}
}
