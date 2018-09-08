package com.github.skjolberg.packing;

import net.jqwik.api.*;
import net.jqwik.api.arbitraries.IntegerArbitrary;
import net.jqwik.api.constraints.Size;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.jqwik.api.Arbitraries.integers;
import static org.assertj.core.api.Assertions.assertThat;

class BruteForcePropertyBasedTests {
  // The maximum number of different, random items which can be reliably packed with brute force
  // seems to be between 10 and 15
  @Property
  void bunchOfDifferentBoxesShouldFitInContainers(@ForAll @Size(min = 1, max = 11) List<BoxItem> items) {
    final Box empty = new Box(0, 0, 0, 0);

    final List<Container> containers =
      Stream.of(accumulateByDepth, accumulateByWidth, accumulateByHeight)
        .map(accumulator -> largeEnoughContainer(items, empty, accumulator))
        .collect(Collectors.toList());
    // only useful to debug when packaging fails
    System.out.printf("packing %d items into %s%n", items.size(), containers);
    System.out.println(items);
    final Container pack = new BruteForcePackager(containers).pack(items, System.currentTimeMillis() + 100);
    assertThat(pack).isNotNull();
  }

  private Container largeEnoughContainer(final List<BoxItem> items,
                                         final Box empty,
                                         final BiFunction<Box, BoxItem, Box> accumulator) {
    final Box largeEnough = items.stream().reduce(
      empty,
      accumulator,
      BruteForcePropertyBasedTests::add);
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
      .combine(sensiblePositiveNumber(), sensiblePositiveNumber(), sensiblePositiveNumber())
      .as(Dimension::new);
  }

  @Provide
  Arbitrary<BoxItem> boxItemGenerated() {
    return Combinators
      .combine(sensiblePositiveNumber(), dimensionGenerated(), integers().between(1, 1))
      .as((weight, dimension, count) -> new BoxItem(new Box(dimension, weight), count));
  }

  private IntegerArbitrary sensiblePositiveNumber() {
    return integers().between(1, 9999);
  }

}
