package com.github.skjolber.packing.packer.bruteforce;

import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.PackagerInput;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;
import com.github.skjolber.packing.validator.DefaultValidator;

/**
 * Box item groups with several container types. The attempts for the container types share the session's placements: a
 * result which is accepted, or which another container is checked against ({@link PackagerSession#peek(int, IntermediatePackagerResult)}),
 * after the attempt for another container must still hold the boxes and positions which it found.
 * <p>
 * Three groups of one box each, in containers which all have room for the volume of the boxes (top views):
 *
 * <pre>
 *   a, b: 2 x 2     c: 1 x 1
 *
 *   small  3 x 3     a and c fit, a and b do not (the result holds the groups a and c)
 *
 *     +-------+---+
 *     | a   a | c |
 *     | a   a |   |
 *     +-------+---+
 *     |           |
 *     +-----------+
 *
 *   medium 5 x 2     all three groups fit (the result holds all groups)
 *
 *     +-------+-------+---+
 *     | a   a | b   b | c |
 *     | a   a | b   b |   |
 *     +-------+-------+---+
 *
 *   large  6 x 2     all three groups fit
 * </pre>
 */
public class BruteForceBoxItemGroupsContainerTypesTest {

	private static List<BoxItemGroup> groups() {
		List<BoxItemGroup> groups = new ArrayList<>();
		groups.add(createGroup("a", 2, 2));
		groups.add(createGroup("b", 2, 2));
		groups.add(createGroup("c", 1, 1));
		return groups;
	}

	private static BoxItemGroup createGroup(String id, int dx, int dy) {
		return new BoxItemGroup(id, List.of(new BoxItem(Box.newBuilder().withId(id).withSize(dx, dy, 1).withWeight(1).build(), 1)));
	}

	private static List<ContainerItem> containers() {
		return List.of(
				new ContainerItem(Container.newBuilder().withId("small").withSize(3, 3, 1).withMaxLoadWeight(10).build(), 1),
				new ContainerItem(Container.newBuilder().withId("medium").withSize(5, 2, 1).withMaxLoadWeight(10).build(), 1),
				new ContainerItem(Container.newBuilder().withId("large").withSize(6, 2, 1).withMaxLoadWeight(10).build(), 1));
	}

	@Test
	public void bruteForceAcceptsAResultAfterAnAttemptForAnotherContainer() throws PackagerInterruptedException {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			assertAcceptsAResultAfterAnAttemptForAnotherContainer(packager);
		}
	}

	@Test
	public void fastBruteForceAcceptsAResultAfterAnAttemptForAnotherContainer() throws PackagerInterruptedException {
		try (FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build()) {
			assertAcceptsAResultAfterAnAttemptForAnotherContainer(packager);
		}
	}

	@Test
	public void parallelBruteForceAcceptsAResultAfterAnAttemptForAnotherContainer() throws PackagerInterruptedException {
		try (ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build()) {
			assertAcceptsAResultAfterAnAttemptForAnotherContainer(packager);
		}
	}

	/**
	 * The container packing strategy attempts the medium container (all groups), then the small container (the groups a and c),
	 * and accepts the result for the medium container. The small container's result holds a and c in the first two
	 * placements, so the medium container's result must not read them.
	 */
	private static void assertAcceptsAResultAfterAnAttemptForAnotherContainer(AbstractPackager<?> packager) throws PackagerInterruptedException {
		PackagerSession session = packager.createSession(new PackagerInput(null, groups(), containers(), 2, Order.NONE), () -> false);

		IntermediatePackagerResult medium = session.attempt(1, null, false);
		assertThat(ids(medium)).containsExactlyInAnyOrder("a", "b", "c");

		IntermediatePackagerResult small = session.attempt(0, null, false);
		assertThat(ids(small)).containsExactlyInAnyOrder("a", "c");

		Container container = session.accept(medium);
		assertThat(container.getId()).isEqualTo("medium");
		assertThat(container.getStack().getPlacements()).extracting(p -> p.getStackValue().getBox().getId()).containsExactlyInAnyOrder("a", "b", "c");
		assertThat(session.countRemainingBoxItemGroups()).isZero();
		assertThat(session.countRemainingBoxes()).isZero();
	}

	@Test
	public void bruteForcePacksGroupsIntoOneOfSeveralContainerTypes() {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			// <figure>
			//   z   /---------------/---------------|           y   z
			//      /               /               /|               1 +---------------+---------------+-------+
			//   | /               /               /-------|   /       |               |               |       |
			//   |/               /               /       /|  /        |       a       |       b       |   c   |
			// 1 |---------------|---------------|-------| | / 2       |               |               |       |
			//   |               |               |       | |/        0 +---------------+---------------+-------+
			//   |       a       |       b       |   c   | | 1         0               2               4       5   x
			//   |               |               |       |/
			// 0 |---------------|---------------|-------|-- x
			//   0               2               4       5
			//
			// y                                                 z
			// 2 +---------------+---------------+               1 +-------+-------+
			//   |               |               |                 |       |       |
			//   |               |               |                 |   c   |   b   |
			//   |               |               |                 |       |       |
			// 1 |       a       |       b       +-------+       0 +-------+-------+
			//   |               |               |       |         0       1       2   y
			//   |               |               |   c   |
			//   |               |               |       |
			// 0 +---------------+---------------+-------+
			//   0               2               4       5   x
			// </figure>
			assertPacksGroupsIntoOneOfSeveralContainerTypes(packager);
		}
	}

	@Test
	public void fastBruteForcePacksGroupsIntoOneOfSeveralContainerTypes() {
		try (FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build()) {
			// <figure>
			//   z   /---------------/---------------|           y   z
			//      /               /               /|               1 +---------------+---------------+-------+
			//   | /               /               /-------|   /       |               |               |       |
			//   |/               /               /       /|  /        |       a       |       b       |   c   |
			// 1 |---------------|---------------|-------| | / 2       |               |               |       |
			//   |               |               |       | |/        0 +---------------+---------------+-------+
			//   |       a       |       b       |   c   | | 1         0               2               4       5   x
			//   |               |               |       |/
			// 0 |---------------|---------------|-------|-- x
			//   0               2               4       5
			//
			// y                                                 z
			// 2 +---------------+---------------+               1 +-------+-------+
			//   |               |               |                 |       |       |
			//   |               |               |                 |   c   |   b   |
			//   |               |               |                 |       |       |
			// 1 |       a       |       b       +-------+       0 +-------+-------+
			//   |               |               |       |         0       1       2   y
			//   |               |               |   c   |
			//   |               |               |       |
			// 0 +---------------+---------------+-------+
			//   0               2               4       5   x
			// </figure>
			assertPacksGroupsIntoOneOfSeveralContainerTypes(packager);
		}
	}

	@Test
	public void parallelBruteForcePacksGroupsIntoOneOfSeveralContainerTypes() {
		try (ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build()) {
			assertPacksGroupsIntoOneOfSeveralContainerTypes(packager);
		}
	}

	/**
	 * All groups fit the medium container, the first container type which is large enough for the volume of the boxes and
	 * holds them all, so one container is enough.
	 */
	private static void assertPacksGroupsIntoOneOfSeveralContainerTypes(AbstractPackager<?> packager) {
		PackagerResult result = packager.newResultBuilder()
				.withContainerItems(containers())
				.withBoxItemGroups(groups())
				.withMaxContainerCount(2)
				.withInterruptDuration(10_000)
				.build();
		// parallel brute force picks between equally good packings by thread timing, so its figure would change between runs
		if (!(packager instanceof ParallelBruteForcePackager)) {
			figure(result);
		}

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(1);
		assertThat(result.get(0).getId()).isEqualTo("medium");
		assertThat(result.get(0).getStack().getPlacements()).extracting(p -> p.getStackValue().getBox().getId()).containsExactlyInAnyOrder("a", "b", "c");
		try (DefaultValidator validator = new DefaultValidator()) {
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containers())
					.withMaxContainerCount(2)
					.withBoxItemGroups(groups()));
		} catch (java.io.IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Test
	public void bruteForcePeeksAtTheBoxesOfAResultAfterAnAttemptForAnotherContainer() throws PackagerInterruptedException {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			assertPeeksAtTheBoxesOfAResultAfterAnAttemptForAnotherContainer(packager);
		}
	}

	@Test
	public void fastBruteForcePeeksAtTheBoxesOfAResultAfterAnAttemptForAnotherContainer() throws PackagerInterruptedException {
		try (FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build()) {
			assertPeeksAtTheBoxesOfAResultAfterAnAttemptForAnotherContainer(packager);
		}
	}

	@Test
	public void parallelBruteForcePeeksAtTheBoxesOfAResultAfterAnAttemptForAnotherContainer() throws PackagerInterruptedException {
		try (ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build()) {
			assertPeeksAtTheBoxesOfAResultAfterAnAttemptForAnotherContainer(packager);
		}
	}

	/**
	 * Whether the boxes of a result fit another container is decided by their positions, so the positions must be those
	 * of the result, also after an attempt for another container. Groups: a long box (4 x 1) and a unit box; containers
	 * (top views):
	 *
	 * <pre>
	 *   wide   4 x 2     both groups: [a a a a]       the result for the wide container
	 *                                 [b . . .]
	 *
	 *   narrow 3 x 2     the long box does not fit:   the result for the narrow container
	 *                    [b . .]
	 *                    [. . .]
	 * </pre>
	 *
	 * The result for the narrow container is placed in the first position of the result for the wide container; the
	 * second position still holds the unit box beside the long box, which is also within the narrow container. So the
	 * long box looks like it fits, if the positions of the result for the wide container are not placed again.
	 */
	private static void assertPeeksAtTheBoxesOfAResultAfterAnAttemptForAnotherContainer(AbstractPackager<?> packager) throws PackagerInterruptedException {
		List<BoxItemGroup> groups = List.of(createGroup("a", 4, 1), createGroup("b", 1, 1));
		List<ContainerItem> containers = List.of(
				new ContainerItem(Container.newBuilder().withId("narrow").withSize(3, 2, 1).withMaxLoadWeight(10).build(), 1),
				new ContainerItem(Container.newBuilder().withId("wide").withSize(4, 2, 1).withMaxLoadWeight(10).build(), 1));
		PackagerSession session = packager.createSession(new PackagerInput(null, groups, containers, 2, Order.NONE), () -> false);

		IntermediatePackagerResult wide = session.attempt(1, null, false);
		assertThat(ids(wide)).containsExactlyInAnyOrder("a", "b");

		IntermediatePackagerResult narrow = session.attempt(0, null, false);
		assertThat(ids(narrow)).containsExactly("b");

		assertThat(session.peek(0, wide)).isNull();
	}

	private static List<String> ids(IntermediatePackagerResult result) {
		List<String> ids = new ArrayList<>();
		for (Placement placement : result.getStack().getPlacements()) {
			ids.add(placement.getStackValue().getBox().getId());
		}
		return ids;
	}
}
