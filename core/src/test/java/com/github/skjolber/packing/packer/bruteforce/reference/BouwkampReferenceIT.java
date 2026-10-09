package com.github.skjolber.packing.packer.bruteforce.reference;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.reference.ReferenceComparison.Quality;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;

/**
 * Slow integration test (run with the {@code slow-tests} profile): the two simple perfect squared rectangles of order
 * nine, which the other tests leave out as too slow. Their nine squares are all different, so there are 9! = 362 880
 * permutations, and the squares tile the rectangle without a gap: all squares must be placed, by the 4.x search
 * (see {@link Version4Reference}) as well as by the 5.0 brute force packager, with and without skipping reverse
 * permutations, and also through the public API.
 * <p>
 * Each search takes about 10 seconds on one core (the reference and 5.0 alike).
 * <p>
 * All searches have an interrupt deadline, so that a regression cannot hang the build: it fails the test instead.
 */
class BouwkampReferenceIT {

	/** Interrupt of each search: 15 minutes, while a search takes seconds */
	private static final long INTERRUPT_MILLIS = 15 * 60_000;

	private static BouwkampCode code(String name) {
		for (BouwkampCodes codes : BouwkampCodeDirectory.getInstance().getSimplePerfectSquaredRectangles(9)) {
			for (BouwkampCode code : codes.getCodes()) {
				if(code.getName().equals(name)) {
					return code;
				}
			}
		}
		throw new IllegalStateException("No simple perfect squared rectangle of order 9 named " + name);
	}

	@ParameterizedTest
	@ValueSource(strings = { "33x32A", "69x61A" })
	void allSquaresArePlaced(String name) throws PackagerInterruptedException {
		BouwkampCode code = code(name);
		assertThat(code.getOrder()).isEqualTo(9);

		ReferenceScenario scenario = ReferenceScenario.bouwkamp(code);

		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build();
				BruteForcePackager skippingPackager = BruteForcePackager.newBuilder().withSkipReversePermutations(true).build()) {

			// the reference, 5.0, the reference which skips reverse permutations, and 5.0 skipping them
			Quality quality = ReferenceComparison.assertAgree(scenario, packager, skippingPackager, INTERRUPT_MILLIS);

			// the squares tile the rectangle
			assertThat(quality).as("%s", scenario).isEqualTo(new Quality((long)code.getWidth() * code.getDepth(), code.getOrder(), code.getOrder()));

			assertPublicApiPlacesAll(packager, scenario, code);
			assertPublicApiPlacesAll(skippingPackager, scenario, code);
		}
	}

	/**
	 * The packager through its public API, for one container: it must place all squares.
	 */
	private static void assertPublicApiPlacesAll(BruteForcePackager packager, ReferenceScenario scenario, BouwkampCode code) {
		PackagerResult result = packager.newResultBuilder()
				.withContainerItems(List.of(new ContainerItem(scenario.newContainer(), 1)))
				.withBoxItems(scenario.newBoxItems())
				.withMaxContainerCount(1)
				.withInterruptDuration(INTERRUPT_MILLIS)
				.build();

		assertThat(result.isTimeout()).as("%s", scenario).isFalse();
		assertThat(result.isSuccess()).as("%s", scenario).isTrue();
		assertThat(result.size()).as("%s", scenario).isEqualTo(1);
		Stack stack = result.get(0).getStack();
		assertThat(stack.size()).as("%s", scenario).isEqualTo(code.getOrder());
		assertThat(stack.getVolume()).as("%s", scenario).isEqualTo((long)code.getWidth() * code.getDepth());
	}

}
