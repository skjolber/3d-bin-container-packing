package com.github.skjolber.packing.packer.bruteforce;

import static com.github.skjolber.packing.packer.PackagerGoldenMasterTest.LOAD_IDENTICAL;
import static com.github.skjolber.packing.packer.PackagerGoldenMasterTest.LOAD_NONE;
import static com.github.skjolber.packing.packer.PackagerGoldenMasterTest.LOAD_WEIGHT;
import static com.github.skjolber.packing.packer.PackagerGoldenMasterTest.LOAD_WEIGHT_PRESSURE_COUNT;
import static com.github.skjolber.packing.packer.PackagerGoldenMasterTest.pack;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.BruteForcePointIteratorFilter;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.ClosestVolumeAndAreaPointFilter;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.MostPromisingPointFilter;
import com.github.skjolber.packing.packer.bruteforce.RecursiveBruteForceSearch.RecursiveBruteForcePackager;

/**
 * The brute-force search compared with the recursive reference ({@link RecursiveBruteForceSearch}): packing results
 * must be identical for random orders, with and without point filters, groups, load limits, box item orders and
 * skipping.
 *
 * <pre>
 *   seed --> random boxes --+--> brute-force packager ---------> checksum
 *                           |                                       ==
 *                           +--> packager with recursive search --> checksum
 * </pre>
 */
public class BruteForceSearchReferenceTest {

	private static final int SEEDS = 40;

	private static final int MAX_BOXES = 5;

	private static List<BruteForcePointIteratorFilter> pointFilters() {
		return Arrays.asList(null, new ClosestVolumeAndAreaPointFilter(), new MostPromisingPointFilter(2, FastBruteForcePackager.DEFAULT_POINT_COMPARATOR));
	}

	@Test
	public void searchMatchesRecursiveReference() {
		for(BruteForcePointIteratorFilter pointFilter : pointFilters()) {
			try (BruteForcePackager packager = new BruteForcePackager(new BruteForceIntermediatePackagerResultComparator(), pointFilter, false);
					BruteForcePackager reference = new RecursiveBruteForcePackager(pointFilter)) {
				for(int seed = 0; seed < SEEDS; seed++) {
					for(boolean groups : new boolean[] {false, true}) {
						for(Order order : Order.values()) {
							assertThat(pack(packager, seed, MAX_BOXES, 1, groups, LOAD_NONE, order))
									.as("seed %d, point filter %s, groups %s, order %s", seed, pointFilter, groups, order)
									.isEqualTo(pack(reference, seed, MAX_BOXES, 1, groups, LOAD_NONE, order));
						}
					}
				}
			}
		}
	}

	@Test
	public void loadSearchMatchesRecursiveReference() {
		for(BruteForcePointIteratorFilter pointFilter : pointFilters()) {
			try (BruteForcePackager packager = new BruteForcePackager(new BruteForceIntermediatePackagerResultComparator(), pointFilter, false);
					BruteForcePackager reference = new RecursiveBruteForcePackager(pointFilter)) {
				for(int seed = 0; seed < SEEDS; seed++) {
					for(int load : new int[] {LOAD_WEIGHT, LOAD_WEIGHT_PRESSURE_COUNT, LOAD_IDENTICAL}) {
						for(Order order : Order.values()) {
							assertThat(pack(packager, seed, MAX_BOXES, 1, false, load, order))
									.as("seed %d, point filter %s, load %d, order %s", seed, pointFilter, load, order)
									.isEqualTo(pack(reference, seed, MAX_BOXES, 1, false, load, order));
						}
					}
				}
			}
		}
	}
}
