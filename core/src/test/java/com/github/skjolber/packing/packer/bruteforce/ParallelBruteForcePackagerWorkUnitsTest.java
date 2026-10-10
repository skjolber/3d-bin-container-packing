package com.github.skjolber.packing.packer.bruteforce;

import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.packer.PackagerInput;

/**
 * The work units of a parallel search are only started when they are needed: a search which fits in a single work unit
 * does not start any, and a search which finds all boxes in one work unit does not start the work units after it. This
 * goes for boxes, and for box item groups (a single group, or the permutations of each order of a few groups).
 * <br>
 * The tasks are run on the calling thread, one at a time, so that the number of work units which were started is
 * deterministic.
 */
public class ParallelBruteForcePackagerWorkUnitsTest {

	private static final int PARALLELIZATION_COUNT = 16;

	/** Runs each task on the thread which submits it, counting the tasks */
	private static class CallingThreadExecutor extends AbstractExecutorService {

		private final AtomicInteger tasks = new AtomicInteger();

		@Override
		public void execute(Runnable command) {
			tasks.incrementAndGet();
			command.run();
		}

		@Override
		public void shutdown() {
		}

		@Override
		public List<Runnable> shutdownNow() {
			return List.of();
		}

		@Override
		public boolean isShutdown() {
			return false;
		}

		@Override
		public boolean isTerminated() {
			return false;
		}

		@Override
		public boolean awaitTermination(long timeout, TimeUnit unit) {
			return true;
		}

		int getTasks() {
			return tasks.get();
		}
	}

	private static BoxItem box(String id, int dx, int dy) {
		return new BoxItem(Box.newBuilder().withId(id).withSize(dx, dy, 1).withWeight(1).build());
	}

	/** @return the given number of boxes with a volume of 1 */
	private static List<BoxItem> boxes(int count) {
		List<BoxItem> products = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			products.add(box("box-" + i, 1, 1));
		}
		return products;
	}

	private static ContainerItem container(int dx, int dy) {
		return new ContainerItem(Container.newBuilder().withId("container").withSize(dx, dy, 1).withMaxLoadWeight(100).build(), 1);
	}

	private static PackagerResult pack(CallingThreadExecutor executor, List<BoxItem> boxes, ContainerItem container) throws Exception {
		try (ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withExecutorService(executor)
				.withParallelizationCount(PARALLELIZATION_COUNT)
				.build()) {
			return packager.newResultBuilder()
					.withContainerItem(container)
					.withBoxItems(boxes)
					.withMaxContainerCount(1)
					.build();
		}
	}

	@Test
	void fewPermutationsAreSearchedWithoutStartingWorkUnits() throws Exception {
		CallingThreadExecutor executor = new CallingThreadExecutor();

		// 3! = 6 permutations
		PackagerResult result = pack(executor, boxes(3), container(3, 1));

		// <figure>
		//   z                                 z                                 y                                 z
		//                                     1 +-------+-------+-------+       1 +-------+-------+-------+       1 +-------+
		//   | /-------/-------/-------|   y     |       |       |       |         |       |       |       |         |       |
		//   |/       /       /       /|         | box-0 | box-1 | box-2 |         | box-0 | box-1 | box-2 |         | box-2 |
		// 1 |-------|-------|-------| | /       |       |       |       |         |       |       |       |         |       |
		//   |       |       |       | |/      0 +-------+-------+-------+       0 +-------+-------+-------+       0 +-------+
		//   | box-0 | box-1 | box-2 | | 1       0       1       2       3   x     0       1       2       3   x     0       1   y
		//   |       |       |       |/
		// 0 |-------|-------|-------|-- x
		//   0       1       2       3
		// </figure>
		figure(result);

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers().get(0).getStack().size()).isEqualTo(3);
		assertThat(executor.getTasks()).isZero();
	}

	@Test
	void completeSearchDoesNotStartTheRemainingWorkUnits() throws Exception {
		CallingThreadExecutor executor = new CallingThreadExecutor();

		// 5! = 120 permutations, split into 16 work units. All boxes fit, so the first work unit finds all of them
		PackagerResult result = pack(executor, boxes(5), container(5, 1));

		// <figure>
		//   z                                                 z
		//                                                     1 +-------+-------+-------+-------+-------+
		//   | /-------/-------/-------/-------/-------|   y     |       |       |       |       |       |
		//   |/       /       /       /       /       /|         | box-0 | box-1 | box-2 | box-3 | box-4 |
		// 1 |-------|-------|-------|-------|-------| | /       |       |       |       |       |       |
		//   |       |       |       |       |       | |/      0 +-------+-------+-------+-------+-------+
		//   | box-0 | box-1 | box-2 | box-3 | box-4 | | 1       0       1       2       3       4       5   x
		//   |       |       |       |       |       |/
		// 0 |-------|-------|-------|-------|-------|-- x
		//   0       1       2       3       4       5
		//
		// y                                                 z
		// 1 +-------+-------+-------+-------+-------+       1 +-------+
		//   |       |       |       |       |       |         |       |
		//   | box-0 | box-1 | box-2 | box-3 | box-4 |         | box-4 |
		//   |       |       |       |       |       |         |       |
		// 0 +-------+-------+-------+-------+-------+       0 +-------+
		//   0       1       2       3       4       5   x     0       1   y
		// </figure>
		figure(result);

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers().get(0).getStack().size()).isEqualTo(5);
		assertThat(executor.getTasks()).isEqualTo(1);
	}

	@Test
	void incompleteSearchStartsAllWorkUnits() throws Exception {
		CallingThreadExecutor executor = new CallingThreadExecutor();

		// 7! = 5040 permutations, split into 16 work units. The boxes have a volume of 23 in a container of 25, but two
		// boxes of 3 x 3 do not fit side by side in 5 x 5, so no work unit finds all of the boxes
		List<BoxItem> boxes = boxes(5);
		boxes.add(box("large-1", 3, 3));
		boxes.add(box("large-2", 3, 3));
		PackagerResult result = pack(executor, boxes, container(5, 5));

		assertThat(result.isSuccess()).isFalse();
		assertThat(executor.getTasks()).isEqualTo(PARALLELIZATION_COUNT);
	}

	private static PackagerResult packGroups(CallingThreadExecutor executor, List<BoxItemGroup> groups, ContainerItem container) throws Exception {
		try (ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withExecutorService(executor)
				.withParallelizationCount(PARALLELIZATION_COUNT)
				.build()) {
			return packager.newResultBuilder()
					.withContainerItem(container)
					.withBoxItemGroups(groups)
					.withMaxContainerCount(1)
					.build();
		}
	}

	@Test
	void completeGroupSearchDoesNotStartTheRemainingWorkUnits() throws Exception {
		CallingThreadExecutor executor = new CallingThreadExecutor();

		// one group of 5! = 120 permutations, split into 16 work units. All boxes fit, so the first work unit finds all of them
		PackagerResult result = packGroups(executor, List.of(new BoxItemGroup("group", boxes(5))), container(5, 1));

		// <figure>
		//   z                                                 z
		//                                                     1 +-------+-------+-------+-------+-------+
		//   | /-------/-------/-------/-------/-------|   y     |       |       |       |       |       |
		//   |/       /       /       /       /       /|         | box-0 | box-1 | box-2 | box-3 | box-4 |
		// 1 |-------|-------|-------|-------|-------| | /       |       |       |       |       |       |
		//   |       |       |       |       |       | |/      0 +-------+-------+-------+-------+-------+
		//   | box-0 | box-1 | box-2 | box-3 | box-4 | | 1       0       1       2       3       4       5   x
		//   |       |       |       |       |       |/
		// 0 |-------|-------|-------|-------|-------|-- x
		//   0       1       2       3       4       5
		//
		// y                                                 z
		// 1 +-------+-------+-------+-------+-------+       1 +-------+
		//   |       |       |       |       |       |         |       |
		//   | box-0 | box-1 | box-2 | box-3 | box-4 |         | box-4 |
		//   |       |       |       |       |       |         |       |
		// 0 +-------+-------+-------+-------+-------+       0 +-------+
		//   0       1       2       3       4       5   x     0       1   y
		// </figure>
		figure(result);

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers().get(0).getStack().size()).isEqualTo(5);
		assertThat(executor.getTasks()).isEqualTo(1);
	}

	@Test
	void completeGroupOrderSearchDoesNotStartTheRemainingWorkUnits() throws Exception {
		CallingThreadExecutor executor = new CallingThreadExecutor();

		// two groups of 3! * 3! = 36 permutations in either order: few orders of many permutations, which are searched one at a time
		// with the permutations split into 16 work units. All boxes fit, so the first work unit of the first order finds all of them,
		// and no other order is searched
		List<BoxItem> first = boxes(3);
		List<BoxItem> second = new ArrayList<>();
		for (int i = 3; i < 6; i++) {
			second.add(box("box-" + i, 1, 1));
		}
		PackagerResult result = packGroups(executor, List.of(new BoxItemGroup("first", first), new BoxItemGroup("second", second)), container(6, 1));

		// <figure>
		//   z                                                         z
		//                                                             1 +-------+-------+-------+-------+-------+-------+
		//   | /-------/-------/-------/-------/-------/-------|   y     |       |       |       |       |       |       |
		//   |/       /       /       /       /       /       /|         | box-0 | box-1 | box-2 | box-3 | box-4 | box-5 |
		// 1 |-------|-------|-------|-------|-------|-------| | /       |       |       |       |       |       |       |
		//   |       |       |       |       |       |       | |/      0 +-------+-------+-------+-------+-------+-------+
		//   | box-0 | box-1 | box-2 | box-3 | box-4 | box-5 | | 1       0       1       2       3       4       5       6   x
		//   |       |       |       |       |       |       |/
		// 0 |-------|-------|-------|-------|-------|-------|-- x
		//   0       1       2       3       4       5       6
		//
		// y                                                         z
		// 1 +-------+-------+-------+-------+-------+-------+       1 +-------+
		//   |       |       |       |       |       |       |         |       |
		//   | box-0 | box-1 | box-2 | box-3 | box-4 | box-5 |         | box-5 |
		//   |       |       |       |       |       |       |         |       |
		// 0 +-------+-------+-------+-------+-------+-------+       0 +-------+
		//   0       1       2       3       4       5       6   x     0       1   y
		// </figure>
		figure(result);

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers().get(0).getStack().size()).isEqualTo(6);
		assertThat(executor.getTasks()).isEqualTo(1);
	}

	@Test
	void incompleteGroupSearchStartsAllWorkUnits() throws Exception {
		CallingThreadExecutor executor = new CallingThreadExecutor();

		// one group of 7! = 5040 permutations, split into 16 work units. The boxes have a volume of 23 in a container of 25, but two
		// boxes of 3 x 3 do not fit side by side in 5 x 5, so no work unit finds all of the boxes
		List<BoxItem> boxes = boxes(5);
		boxes.add(box("large-1", 3, 3));
		boxes.add(box("large-2", 3, 3));
		PackagerResult result = packGroups(executor, List.of(new BoxItemGroup("group", boxes)), container(5, 5));

		assertThat(result.isSuccess()).isFalse();
		assertThat(executor.getTasks()).isEqualTo(PARALLELIZATION_COUNT);
	}

	/**
	 * The groups of a container are packed in order, so the container holds the groups before the first which it excludes (because it
	 * does not fit). The first group has 5! = 120 permutations in 16 work units, and the second group (7 x 1) does not fit the small
	 * container of 5 x 1, only the large of 8 x 1.
	 */
	private static List<BoxItemGroup> groupsWithAnExcludedGroup(List<BoxItem> first) {
		return List.of(new BoxItemGroup("first", first), new BoxItemGroup("wide", List.of(box("wide", 7, 1))));
	}

	private static IntermediatePackagerResult attemptSmallContainer(CallingThreadExecutor executor, List<BoxItemGroup> groups, ContainerItem small, ContainerItem large,
			Container[] accepted) throws Exception {
		try (ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withExecutorService(executor)
				.withParallelizationCount(PARALLELIZATION_COUNT)
				.build()) {
			PackagerSession session = packager.createSession(new PackagerInput(null, groups, List.of(small, large), 2, Order.NONE), () -> false);
			IntermediatePackagerResult result = session.attempt(0, null, false);
			if (accepted != null) {
				accepted[0] = session.accept(result);
			}
			return result;
		}
	}

	@Test
	void completeSearchOfTheGroupsBeforeAnExcludedGroupDoesNotStartTheRemainingWorkUnits() throws Exception {
		CallingThreadExecutor executor = new CallingThreadExecutor();

		// all boxes of the first group fit the small container, so the first work unit finds all that the container can hold
		Container[] accepted = new Container[1];
		IntermediatePackagerResult result = attemptSmallContainer(executor, groupsWithAnExcludedGroup(boxes(5)), container(5, 1), container(8, 1), accepted);

		// <figure>
		//   z                                                 z
		//                                                     1 +-------+-------+-------+-------+-------+
		//   | /-------/-------/-------/-------/-------|   y     |       |       |       |       |       |
		//   |/       /       /       /       /       /|         | box-0 | box-1 | box-2 | box-3 | box-4 |
		// 1 |-------|-------|-------|-------|-------| | /       |       |       |       |       |       |
		//   |       |       |       |       |       | |/      0 +-------+-------+-------+-------+-------+
		//   | box-0 | box-1 | box-2 | box-3 | box-4 | | 1       0       1       2       3       4       5   x
		//   |       |       |       |       |       |/
		// 0 |-------|-------|-------|-------|-------|-- x
		//   0       1       2       3       4       5
		//
		// y                                                 z
		// 1 +-------+-------+-------+-------+-------+       1 +-------+
		//   |       |       |       |       |       |         |       |
		//   | box-0 | box-1 | box-2 | box-3 | box-4 |         | box-4 |
		//   |       |       |       |       |       |         |       |
		// 0 +-------+-------+-------+-------+-------+       0 +-------+
		//   0       1       2       3       4       5   x     0       1   y
		// </figure>
		figure(new PackagerResult(List.of(accepted[0]), 0, false));

		assertThat(result.getBoxCount()).isEqualTo(5);
		assertThat(executor.getTasks()).isEqualTo(1);
	}

	@Test
	void incompleteSearchOfTheGroupsBeforeAnExcludedGroupStartsAllWorkUnits() throws Exception {
		CallingThreadExecutor executor = new CallingThreadExecutor();

		// the first group has 7! = 5040 permutations, with a volume of 23 in a container of 25, but two boxes of 3 x 3 do not fit side by
		// side in 5 x 5, so no work unit finds all of the boxes of the group. The group which the container excludes (7 x 1) is no reason to stop
		List<BoxItem> boxes = boxes(5);
		boxes.add(box("large-1", 3, 3));
		boxes.add(box("large-2", 3, 3));
		IntermediatePackagerResult result = attemptSmallContainer(executor, groupsWithAnExcludedGroup(boxes), container(5, 5), container(8, 5), null);

		// not even the first group fits whole
		assertThat(result).isNull();
		assertThat(executor.getTasks()).isEqualTo(PARALLELIZATION_COUNT);
	}
}
