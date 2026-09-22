package com.github.skjolber.packing.jmh.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.packer.EmptyIntermediatePackagerResult;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.strategy.ordered.OrderedContainerPackingStrategy;
import com.github.skjolber.packing.packer.strategy.ordered.ParallelContainerPackingStrategy;
import com.github.skjolber.packing.jmh.BouwkampConverter;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;

/** Shared inputs for serial and parallel container-candidate selection. */
@State(Scope.Benchmark)
public class ContainerStrategyBenchmarkState {

	@Param({"2", "4", "6"})
	public int containerItemCount;

	private PlainPackager orderedPackager;
	private PlainPackager parallelPackager;
	private BruteForcePackager orderedBruteForcePackager;
	private BruteForcePackager parallelBruteForcePackager;
	private ExecutorService executorService;
	private List<ContainerItem> containerItems;
	private List<ContainerItem> rotatedContainerItems;
	private List<BoxItem> boxItems;
	private List<BoxItem> bruteForceBoxItems;

	@Setup(Level.Trial)
	public void setup() {
		executorService = Executors.newFixedThreadPool(Math.min(containerItemCount,
				Math.max(1, Runtime.getRuntime().availableProcessors())));
		DefaultIntermediatePackagerResultComparator comparator = new DefaultIntermediatePackagerResultComparator();

		orderedPackager = PlainPackager.newBuilder().build();
		orderedPackager.setContainerPackingStrategyFactory((calculator, boxes, groups) ->
				new OrderedContainerPackingStrategy(comparator, () -> EmptyIntermediatePackagerResult.EMPTY));

		parallelPackager = PlainPackager.newBuilder().build();
		parallelPackager.setContainerPackingStrategyFactory((calculator, boxes, groups) ->
				new ParallelContainerPackingStrategy(executorService, comparator));

		orderedBruteForcePackager = BruteForcePackager.newBuilder().withSkipReversePermutations(true).build();
		orderedBruteForcePackager.setContainerPackingStrategyFactory((calculator, boxes, groups) ->
				new OrderedContainerPackingStrategy(comparator, () -> EmptyIntermediatePackagerResult.EMPTY));
		parallelBruteForcePackager = BruteForcePackager.newBuilder().withSkipReversePermutations(true).build();
		parallelBruteForcePackager.setContainerPackingStrategyFactory((calculator, boxes, groups) ->
				new ParallelContainerPackingStrategy(executorService, comparator));

		// Unlike 15x11A, all nine squares in 33x32A have distinct sizes.
		BouwkampCode bouwkamp = BouwkampCodeDirectory.getInstance().codesForCount(9, "33x32A");
		if(bouwkamp == null) {
			throw new IllegalStateException("Missing Bouwkamp input 33x32A");
		}
		containerItems = new ArrayList<>(containerItemCount);
		rotatedContainerItems = new ArrayList<>(containerItemCount);
		int[][] rotations = {
				{bouwkamp.getWidth(), bouwkamp.getDepth(), 1},
				{bouwkamp.getWidth(), 1, bouwkamp.getDepth()},
				{bouwkamp.getDepth(), bouwkamp.getWidth(), 1},
				{bouwkamp.getDepth(), 1, bouwkamp.getWidth()},
				{1, bouwkamp.getWidth(), bouwkamp.getDepth()},
				{1, bouwkamp.getDepth(), bouwkamp.getWidth()}
		};
		for(int i = 0; i < containerItemCount; i++) {
			Container container = Container.newBuilder().withId("container-" + i)
					.withSize(bouwkamp.getWidth(), bouwkamp.getDepth(), 1)
					.withMaxLoadWeight(bouwkamp.getWidth() * bouwkamp.getDepth()).build();
			containerItems.add(new ContainerItem(container, 1));

			int[] rotation = rotations[i % rotations.length];
			Container rotatedContainer = Container.newBuilder().withId("rotated-container-" + i)
					.withSize(rotation[0], rotation[1], rotation[2])
					.withMaxLoadWeight(bouwkamp.getWidth() * bouwkamp.getDepth()).build();
			rotatedContainerItems.add(new ContainerItem(rotatedContainer, 1));
		}
		Box box = Box.newBuilder().withId("box").withSize(1, 1, 1).withWeight(1).build();
		boxItems = List.of(new BoxItem(box, 1_000));
		bruteForceBoxItems = BouwkampConverter.getStackableItems3D(bouwkamp);
	}

	@TearDown(Level.Trial)
	public void tearDown() {
		orderedPackager.close();
		parallelPackager.close();
		orderedBruteForcePackager.close();
		parallelBruteForcePackager.close();
		executorService.shutdownNow();
	}

	public PlainPackager getOrderedPackager() {
		return orderedPackager;
	}

	public PlainPackager getParallelPackager() {
		return parallelPackager;
	}

	public BruteForcePackager getOrderedBruteForcePackager() {
		return orderedBruteForcePackager;
	}

	public BruteForcePackager getParallelBruteForcePackager() {
		return parallelBruteForcePackager;
	}

	public List<ContainerItem> getContainerItems() {
		return containerItems;
	}

	public List<ContainerItem> getRotatedContainerItems() {
		return rotatedContainerItems;
	}

	public List<BoxItem> getBoxItems() {
		return boxItems;
	}

	public List<BoxItem> getBruteForceBoxItems() {
		return bruteForceBoxItems;
	}
}
