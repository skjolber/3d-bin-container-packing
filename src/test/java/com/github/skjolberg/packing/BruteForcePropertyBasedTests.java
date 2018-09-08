package com.github.skjolberg.packing;

import net.jqwik.api.*;
import net.jqwik.api.arbitraries.IntegerArbitrary;
import net.jqwik.api.constraints.Size;

import java.util.Collections;
import java.util.List;

import static net.jqwik.api.Arbitraries.integers;
import static org.assertj.core.api.Assertions.assertThat;

class BruteForcePropertyBasedTests {

  // The maximum number of different, random items which can be reliably packed with brute force
  // seems to be between 10 and 15
  @Property
  void bunchOfDifferentBoxesShouldFitInContainer(@ForAll @Size(min = 1, max = 11) List<BoxItem> items) {
    final Box empty = new Box(0, 0, 0, 0);
    final Box largeEnough = items.stream().reduce(empty, BruteForcePropertyBasedTests::accumulate, BruteForcePropertyBasedTests::add);
    final Container container = new Container(largeEnough, largeEnough.getWeight());
    // only useful to debug when packaging fails
    // System.out.printf("packing %d items into %s%n", items.size(), container);
    // System.out.println(items);
    final Container pack = new BruteForcePackager(Collections.singletonList(container)).pack(items, System.currentTimeMillis() + 100);
    assertThat(pack).isNotNull();
  }

  private static Box accumulate(Box acc, BoxItem boxItem) {
    return new Box(
      acc.getWidth() + boxItem.getBox().getWidth(),
      acc.getHeight() + boxItem.getBox().getHeight(),
      acc.getDepth() + boxItem.getBox().getDepth() * boxItem.getCount(),
      acc.getWeight() + boxItem.getBox().getWeight() * boxItem.getCount());
  }

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
