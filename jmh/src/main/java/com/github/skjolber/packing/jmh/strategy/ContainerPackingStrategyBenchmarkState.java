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
import com.github.skjolber.packing.cost.FixedContainerCostCalculator;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.strategy.allocation.FewestContainersFitContainerPackingStrategy;
import com.github.skjolber.packing.packer.strategy.allocation.LowestCostFitContainerPackingStrategy;
import com.github.skjolber.packing.packer.strategy.cost.LowestCostContainerPackingStrategy;
import com.github.skjolber.packing.packer.strategy.ordered.OrderedContainerPackingStrategy;
import com.github.skjolber.packing.packer.strategy.ordered.ParallelContainerPackingStrategy;
import com.github.skjolber.packing.jmh.BouwkampConverter;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;

/**
 * Shared inputs for container-candidate selection: the ordered and parallel strategies, the cost-aware strategy (which needs a
 * cost calculator on every container item) and the two allocation strategies (one of which needs the costs as well).
 */
@State(Scope.Benchmark)
public class ContainerPackingStrategyBenchmarkState {

	@Param({"2", "4", "6"})
	public int containerItemCount;

	private PlainPackager orderedPackager;
	private PlainPackager parallelPackager;
	private BruteForcePackager orderedBruteForcePackager;
	private BruteForcePackager parallelBruteForcePackager;
	private PlainPackager lowestCostPackager;
	private BruteForcePackager lowestCostBruteForcePackager;
	private PlainPackager fewestContainersFitPackager;
	private BruteForcePackager fewestContainersFitBruteForcePackager;
	private PlainPackager lowestCostFitPackager;
	private BruteForcePackager lowestCostFitBruteForcePackager;
	private ExecutorService executorService;
	private List<ContainerItem> containerItems;
	private List<ContainerItem> rotatedContainerItems;
	private List<ContainerItem> pricedContainerItems;
	private List<ContainerItem> pricedRotatedContainerItems;
	private List<BoxItem> boxItems;
	private List<BoxItem> bruteForceBoxItems;

	@Setup(Level.Trial)
	public void setup() {
		executorService = Executors.newFixedThreadPool(Math.min(containerItemCount,
				Math.max(1, Runtime.getRuntime().availableProcessors())));

		orderedPackager = PlainPackager.newBuilder()
				.withContainerPackingStrategyFactory((inventory, boxes, groups, comparator, emptyResult) -> new OrderedContainerPackingStrategy(comparator, emptyResult))
				.build();

		parallelPackager = PlainPackager.newBuilder()
				.withContainerPackingStrategyFactory((inventory, boxes, groups, comparator, emptyResult) -> new ParallelContainerPackingStrategy(executorService, comparator))
				.build();

		orderedBruteForcePackager = BruteForcePackager.newBuilder()
				.withSkipReversePermutations(true)
				.withContainerPackingStrategyFactory((inventory, boxes, groups, comparator, emptyResult) -> new OrderedContainerPackingStrategy(comparator, emptyResult))
				.build();
		parallelBruteForcePackager = BruteForcePackager.newBuilder()
				.withSkipReversePermutations(true)
				.withContainerPackingStrategyFactory((inventory, boxes, groups, comparator, emptyResult) -> new ParallelContainerPackingStrategy(executorService, comparator))
				.build();

		lowestCostPackager = PlainPackager.newBuilder()
				.withContainerPackingStrategyFactory((inventory, boxes, groups, comparator, emptyResult) -> new LowestCostContainerPackingStrategy(comparator))
				.build();
		lowestCostBruteForcePackager = BruteForcePackager.newBuilder()
				.withSkipReversePermutations(true)
				.withContainerPackingStrategyFactory((inventory, boxes, groups, comparator, emptyResult) -> new LowestCostContainerPackingStrategy(comparator))
				.build();

		fewestContainersFitPackager = PlainPackager.newBuilder()
				.withContainerPackingStrategyFactory((inventory, boxes, groups, comparator, emptyResult) -> new FewestContainersFitContainerPackingStrategy())
				.build();
		fewestContainersFitBruteForcePackager = BruteForcePackager.newBuilder()
				.withSkipReversePermutations(true)
				.withContainerPackingStrategyFactory((inventory, boxes, groups, comparator, emptyResult) -> new FewestContainersFitContainerPackingStrategy())
				.build();

		lowestCostFitPackager = PlainPackager.newBuilder()
				.withContainerPackingStrategyFactory((inventory, boxes, groups, comparator, emptyResult) -> new LowestCostFitContainerPackingStrategy())
				.build();
		lowestCostFitBruteForcePackager = BruteForcePackager.newBuilder()
				.withSkipReversePermutations(true)
				.withContainerPackingStrategyFactory((inventory, boxes, groups, comparator, emptyResult) -> new LowestCostFitContainerPackingStrategy())
				.build();

		// Unlike 15x11A, all nine squares in 33x32A have distinct sizes.
		BouwkampCode bouwkamp = BouwkampCodeDirectory.getInstance().codesForCount(9, "33x32A");
		if(bouwkamp == null) {
			throw new IllegalStateException("Missing Bouwkamp input 33x32A");
		}
		containerItems = new ArrayList<>(containerItemCount);
		rotatedContainerItems = new ArrayList<>(containerItemCount);
		pricedContainerItems = new ArrayList<>(containerItemCount);
		pricedRotatedContainerItems = new ArrayList<>(containerItemCount);
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
			// the costs differ, so that the choice of container matters: the later the container item, the more expensive
			long cost = 100 + 10L * i;
			pricedContainerItems.add(new ContainerItem(container, 1, new FixedContainerCostCalculator(cost, container.getMaxLoadVolume(), "container-" + i, 0)));

			int[] rotation = rotations[i % rotations.length];
			Container rotatedContainer = Container.newBuilder().withId("rotated-container-" + i)
					.withSize(rotation[0], rotation[1], rotation[2])
					.withMaxLoadWeight(bouwkamp.getWidth() * bouwkamp.getDepth()).build();
			rotatedContainerItems.add(new ContainerItem(rotatedContainer, 1));
			pricedRotatedContainerItems.add(new ContainerItem(rotatedContainer, 1, new FixedContainerCostCalculator(cost, rotatedContainer.getMaxLoadVolume(), "rotated-container-" + i, 0)));
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
		lowestCostPackager.close();
		lowestCostBruteForcePackager.close();
		fewestContainersFitPackager.close();
		fewestContainersFitBruteForcePackager.close();
		lowestCostFitPackager.close();
		lowestCostFitBruteForcePackager.close();
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

	public PlainPackager getLowestCostPackager() {
		return lowestCostPackager;
	}

	public BruteForcePackager getLowestCostBruteForcePackager() {
		return lowestCostBruteForcePackager;
	}

	public PlainPackager getFewestContainersFitPackager() {
		return fewestContainersFitPackager;
	}

	public BruteForcePackager getFewestContainersFitBruteForcePackager() {
		return fewestContainersFitBruteForcePackager;
	}

	public PlainPackager getLowestCostFitPackager() {
		return lowestCostFitPackager;
	}

	public BruteForcePackager getLowestCostFitBruteForcePackager() {
		return lowestCostFitBruteForcePackager;
	}

	public List<ContainerItem> getContainerItems() {
		return containerItems;
	}

	public List<ContainerItem> getRotatedContainerItems() {
		return rotatedContainerItems;
	}

	/** The container items with a cost calculator each, as required by the cost-aware strategies. */
	public List<ContainerItem> getPricedContainerItems() {
		return pricedContainerItems;
	}

	/** The rotated container items with a cost calculator each, as required by the cost-aware strategies. */
	public List<ContainerItem> getPricedRotatedContainerItems() {
		return pricedRotatedContainerItems;
	}

	public List<BoxItem> getBoxItems() {
		return boxItems;
	}

	public List<BoxItem> getBruteForceBoxItems() {
		return bruteForceBoxItems;
	}
}
