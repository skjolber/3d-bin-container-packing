package com.github.skjolber.packing.packer.strategy.ordered;

import static org.assertj.core.api.Assertions.assertThat;
import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.packer.DelegatingPackagerSession;
import com.github.skjolber.packing.packer.PackagerInput;
import com.github.skjolber.packing.packer.bruteforce.BruteForceIntermediatePackagerResultComparator;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;

/**
 * Sessions may return null instead of an empty result. With three or more container types, the ordered strategy
 * searches for the first container which holds all boxes (binary search); with fewer, it tries them in order.
 */
public class OrderedContainerPackingStrategyNullResultTest {

	/** A session which returns null for the first container type (from attempts, and from reusing other results). */
	private static class NullResultSession extends DelegatingPackagerSession {

		NullResultSession(PackagerSession delegate) {
			super(delegate);
		}

		@Override
		protected PackagerSession wrap(PackagerSession session) {
			return new NullResultSession(session);
		}

		@Override
		public IntermediatePackagerResult attempt(int containerIndex, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			if(containerIndex == 0) {
				return null;
			}
			return super.attempt(containerIndex, best, abortOnAnyBoxTooBig);
		}

		@Override
		public IntermediatePackagerResult peek(int containerIndex, IntermediatePackagerResult existing) {
			if(containerIndex == 0) {
				return null;
			}
			return super.peek(containerIndex, existing);
		}
	}

	private static class NullResultPackager extends FastBruteForcePackager {

		NullResultPackager() {
			super(new BruteForceIntermediatePackagerResultComparator(), DEFAULT_POINT_COMPARATOR);
		}

		@Override
		protected PackagerSession newSession(PackagerInput input, PackagerInterruptSupplier interrupt) {
			return new NullResultSession(super.newSession(input, interrupt));
		}
	}

	@Test
	public void searchesForTheFirstContainerWhichHoldsAllBoxes() {
		//  a [x][ ]   b [x][ ][ ]   c ...   (a returns null; b is the first which holds the box)
		// <figure>
		//   z                 z                 y                 z
		//                     1 +-------+       1 +-------+       1 +-------+
		//   | /-------|   y     |       |         |       |         |       |
		//   |/       /|         |  box  |         |  box  |         |  box  |
		// 1 |-------| | /       |       |         |       |         |       |
		//   |       | |/      0 +-------+       0 +-------+       0 +-------+
		//   |  box  | | 1       0       1   x     0       1   x     0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		// </figure>
		assertUsesTheSecondContainer(4);
	}

	@Test
	public void triesTheContainersInOrder() {
		// <figure>
		//   z                 z                 y                 z
		//                     1 +-------+       1 +-------+       1 +-------+
		//   | /-------|   y     |       |         |       |         |       |
		//   |/       /|         |  box  |         |  box  |         |  box  |
		// 1 |-------| | /       |       |         |       |         |       |
		//   |       | |/      0 +-------+       0 +-------+       0 +-------+
		//   |  box  | | 1       0       1   x     0       1   x     0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		// </figure>
		assertUsesTheSecondContainer(2);
	}

	private static void assertUsesTheSecondContainer(int containerCount) {
		ContainerItem[] containers = new ContainerItem[containerCount];
		for(int i = 0; i < containerCount; i++) {
			containers[i] = new ContainerItem(Container.newBuilder().withId("c" + i).withSize(2 + i, 1, 1).withMaxLoadWeight(100).build(), 1);
		}
		try (NullResultPackager packager = new NullResultPackager()) {
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(List.of(containers))
					.withBoxItems(new BoxItem(Box.newBuilder().withId("box").withSize(1, 1, 1).withWeight(1).build(), 1))
					.build();
			figure(result);

			assertThat(result.getContainers()).extracting(Container::getId).containsExactly("c1");
		}
	}
}
