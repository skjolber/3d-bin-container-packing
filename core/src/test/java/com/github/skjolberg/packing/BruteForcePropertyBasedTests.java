package com.github.skjolberg.packing;

import net.jqwik.api.*;
import net.jqwik.api.arbitraries.IntegerArbitrary;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static net.jqwik.api.Arbitraries.integers;
import static org.assertj.core.api.Assertions.assertThat;

class BruteForcePropertyBasedTests {
	// The maximum number of different, random items which can be reliably packed with brute force
	// seems to be between 10 and 15
	@Property
	void bunchOfDifferentBoxesShouldFitInContainers(@ForAll("boxItems") @Size(min = 1, max = 6) List<BoxItem> items) {
		final Box empty = new Box(0, 0, 0, 0);

		final List<Container> containers =
				Stream.of(accumulateByDepth, accumulateByWidth, accumulateByHeight)
				.map(accumulator -> largeEnoughContainer(items, empty, accumulator))
				.collect(Collectors.toList());
		// print a message here so that the build server does not think the build has failed
		System.out.printf("packing %d items into %s containers %n", items.size(), containers.size());
		final Container pack = new BruteForcePackager(containers).pack(items, System.currentTimeMillis() + 300);
		assertThat(pack).isNotNull();
	}

	@Property(tries = 10)
	void identicalBoxesShouldFitInContainers(@ForAll("boxItem") BoxItem item, @ForAll @IntRange(min = 1, max = 2) int countBySide) {
		final BoxItem repeatedItems = new BoxItem(item.getBox(), countBySide * countBySide * countBySide);
		//TODO: we could also randomly rotate the items
		final List<Container> containers = largeEnoughContainers(item, countBySide);
		System.out.printf("Fit %d repeated items into one of %d large enough containers\n", repeatedItems.getCount(), containers.size());
		final Container pack = new BruteForcePackager(containers).pack(singletonList(repeatedItems), Long.MAX_VALUE);
		assertThat(pack).isNotNull();
	}

	/**
	 * Prepare containers which are just the right size for the items by stacking them on 1, 2 or 3 directions.
	 */
	private List<Container> largeEnoughContainers(final BoxItem item, final int countBySide) {
		final int totalCount = countBySide * countBySide * countBySide;
		final Box box = item.getBox();
		Container threeDim = new Container(
				box.getWidth() * countBySide,
				box.getDepth() * countBySide,
				box.getHeight() * countBySide,
				box.getWeight() * totalCount);
		Container twoDim = new Container(
				box.getWidth() * countBySide * countBySide,
				box.getDepth() * countBySide,
				box.getHeight(),
				box.getWeight() * totalCount);
		Container oneDim = new Container(
				box.getWidth() * countBySide * countBySide * countBySide,
				box.getDepth(),
				box.getHeight(),
				box.getWeight() * totalCount);
		return Stream
				.of(threeDim, twoDim, oneDim)
				.flatMap(Container::rotationsStream)
				.collect(Collectors.toList());
	}


	private Container largeEnoughContainer(final List<BoxItem> items,
			final Box empty,
			final BiFunction<Box, BoxItem, Box> accumulator) {
		final Box largeEnough = items.stream().reduce(empty, accumulator, BruteForcePropertyBasedTests::add);
		return new Container(largeEnough, largeEnough.getWeight());
	}

	private BiFunction<Box, BoxItem, Box> accumulateByDepth = (acc, boxItem) -> new Box(
			acc.getWidth() + boxItem.getBox().getWidth(),
			acc.getHeight() + boxItem.getBox().getHeight(),
			acc.getDepth() + boxItem.getBox().getDepth() * boxItem.getCount(),
			acc.getWeight() + boxItem.getBox().getWeight() * boxItem.getCount());

	private BiFunction<Box, BoxItem, Box> accumulateByWidth = (acc, boxItem) -> new Box(
			acc.getWidth() + boxItem.getBox().getWidth() * boxItem.getCount(),
			acc.getHeight() + boxItem.getBox().getHeight(),
			acc.getDepth() + boxItem.getBox().getDepth(),
			acc.getWeight() + boxItem.getBox().getWeight() * boxItem.getCount());

	private BiFunction<Box, BoxItem, Box> accumulateByHeight = (acc, boxItem) -> new Box(
			acc.getWidth() + boxItem.getBox().getWidth(),
			acc.getHeight() + boxItem.getBox().getHeight() * boxItem.getCount(),
			acc.getDepth() + boxItem.getBox().getDepth(),
			acc.getWeight() + boxItem.getBox().getWeight() * boxItem.getCount());


	private static Box add(Box b1, Box b2) {
		return new Box(
				b1.getWidth() + b2.getWidth(),
				b1.getDepth() + b2.getDepth(),
				b1.getHeight() + b2.getHeight(),
				b1.getWeight() + b2.getWeight());
	}


	@Provide
	Arbitrary<Dimension> dimensionGenerated() {
		return Combinators
				.combine(countBySize(), countBySize(), countBySize())
				.as(Dimension::new);
	}

	@Provide
	Arbitrary<BoxItem> boxItem() {
		return Combinators
				.combine(countBySize(), dimensionGenerated(), integers().between(1, 1))
				.as((weight, dimension, count) -> new BoxItem(new Box(dimension, weight), count));
	}
	
	@Provide("boxItems")
	Arbitrary<List<BoxItem>> boxItems() {
		return Combinators
				.combine(countBySize(), dimensionGenerated(), integers().between(1, 1))
				.as((weight, dimension, count) -> new BoxItem(new Box(dimension, weight), count)).list();
	}

	private Arbitrary<Integer> countBySize() {
		return integers().between(1, 100);
	}

}
