package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;
import com.github.skjolber.packing.test.assertj.StackPlacementAssert;

class ParallelBoxItemBruteForcePackagerLoadTest extends AbstractLoadBruteForcePackagerTest {

	private ExecutorService executorService;

	@Override
	protected ParallelBoxItemBruteForcePackager createPackager() {
		executorService = Executors.newFixedThreadPool(2);
		return ParallelBoxItemBruteForcePackager.newBuilder()
				.withExecutorService(executorService)
				.withParallelizationCount(2)
				.build();
	}

	@Test
	void usesThePointFilterWithLoadConstraints() {
		CountingPointFilter pointFilter = new CountingPointFilter();
		executorService = Executors.newFixedThreadPool(2);
		try (ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder()
				.withExecutorService(executorService)
				.withParallelizationCount(2)
				.withPointFilter(pointFilter)
				.build()) {
			PackagerResultAssert.assertThat(packColumn(packager)).isSuccess();
		}
		assertThat(pointFilter.getCount()).isPositive();
	}


	@AfterEach
	void shutdownExecutor() {
		if(executorService != null) {
			executorService.shutdownNow();
		}
	}

	@Test
	void preservesLoadsWhenPermutationsAreSplitAcrossWorkers() {
		/*
		 * Five distinct boxes produce 5! permutations, forcing the work to be
		 * divided between workers (parallelization count is two).
		 *
		 * +---+ load 0
		 * +---+ load = weight above
		 * +---+ load = weights above
		 * +---+ load = weights above
		 * +---+ load = total weight minus own weight
		 */
		BoxItem[] items = new BoxItem[5];
		for(int i = 0; i < items.length; i++) {
			Box box = Box.newBuilder().withId("box-" + i).withSize(10, 10, 1).withWeight(i + 1)
					.withMaxLoadWeight(20).build();
			items[i] = new BoxItem(box);
		}

		PackagerResult result = pack(container(5), 1, 1, items);
		List<Placement> placements = result.getContainers().get(0).getStack().getPlacements().stream()
				.sorted(Comparator.comparingInt(Placement::getAbsoluteZ)).toList();

		PackagerResultAssert.assertThat(result).isSuccess();
		assertThat(placements).hasSize(5);
		long weightAbove = 0;
		for(int i = placements.size() - 1; i >= 0; i--) {
			Placement placement = placements.get(i);
			StackPlacementAssert.assertThat(placement).hasLoadWeight((double) weightAbove);
			weightAbove += placement.getWeight();
		}
	}
}
