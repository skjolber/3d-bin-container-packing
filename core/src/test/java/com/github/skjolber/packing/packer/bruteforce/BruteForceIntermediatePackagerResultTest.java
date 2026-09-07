package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.ControlledContainerItem;

class BruteForceIntermediatePackagerResultTest {

	@Test
	void comparesCachedLoadMetricsWithoutMaterializingStacks() {
		Container container = Container.newBuilder().withSize(10, 10, 10).withMaxLoadWeight(1000).build();
		Stack smallStack = new Stack();
		Stack largeStack = new Stack();

		BruteForceIntermediatePackagerResult small = result(container, smallStack, box("small", 2, 2, 2, 100));
		BruteForceIntermediatePackagerResult large = result(container, largeStack, box("large", 3, 3, 3, 1));

		assertThat(new BruteForceIntermediatePackagerResultComparator().compare(small, large)).isNegative();
		assertThat(smallStack).isEmpty();
		assertThat(largeStack).isEmpty();
		assertThat(small.getLoadVolume()).isEqualTo(8L);
		assertThat(small.getLoadWeight()).isEqualTo(100);
		assertThat(large.getLoadVolume()).isEqualTo(27L);
		assertThat(large.getLoadWeight()).isEqualTo(1);

		assertThat(large.getStack()).hasSize(1);
	}

	@Test
	void recalculatesCachedLoadMetricsWhenTrimmed() {
		Box first = box("first", 2, 2, 2, 3);
		Box second = box("second", 3, 3, 3, 5);
		DefaultBoxItemPermutationRotationIterator iterator = iterator(first, second);
		BruteForceIntermediatePackagerResult result = new BruteForceIntermediatePackagerResult(
				new ControlledContainerItem(Container.newBuilder().withSize(10, 10, 10).withMaxLoadWeight(1000).build(), 1),
				new Stack(), 0, iterator);
		List<Point> points = new ArrayList<>(List.of(
				new DefaultPoint3D(0, 0, 0, 9, 9, 9),
				new DefaultPoint3D(2, 0, 0, 9, 9, 9)));
		result.setState(points, iterator.getState(), List.of(new Placement(), new Placement()));

		result.trimToSize(1);

		assertThat(result.getLoadVolume()).isEqualTo(first.getVolume());
		assertThat(result.getLoadWeight()).isEqualTo(first.getWeight());
	}

	@Test
	void rebuildsLoadsIndependentOfPlacementOrder() {
		Placement bottom = placement("bottom", 2, 0);
		Placement middle = placement("middle", 3, 1);
		Placement top = placement("top", 5, 2);
		BruteForceIntermediatePackagerResult result = new BruteForceIntermediatePackagerResult(null, null, 0, null);

		result.rebuildLoads(List.of(middle, top, bottom));

		assertThat(top.getLoadWeight()).isZero();
		assertThat(middle.getLoadWeight()).isEqualTo(5.0);
		assertThat(bottom.getLoadWeight()).isEqualTo(8.0);
		assertThat(middle.getSupporters()).singleElement()
				.extracting(load -> load.getPlacement()).isSameAs(bottom);
	}

	private static Placement placement(String id, int weight, int z) {
		Box box = box(id, 10, 10, 1, weight);
		new BoxItem(box);
		return new Placement(box.getStackValues()[0], 0, 0, 0, z);
	}

	private static BruteForceIntermediatePackagerResult result(Container container, Stack stack, Box box) {
		DefaultBoxItemPermutationRotationIterator iterator = iterator(box);
		BruteForceIntermediatePackagerResult result = new BruteForceIntermediatePackagerResult(
				new ControlledContainerItem(container, 1), stack, 0, iterator);
		result.setState(List.of(new DefaultPoint3D(0, 0, 0, 9, 9, 9)), iterator.getState(), List.of(new Placement()));
		return result;
	}

	private static DefaultBoxItemPermutationRotationIterator iterator(Box... boxes) {
		List<BoxItem> items = new ArrayList<>(boxes.length);
		for(Box box : boxes) {
			items.add(new BoxItem(box));
		}
		return DefaultBoxItemPermutationRotationIterator.newBuilder()
				.withLoadSize(10, 10, 10)
				.withMaxLoadWeight(1000)
				.withBoxItems(items)
				.build();
	}

	private static Box box(String id, int dx, int dy, int dz, int weight) {
		return Box.newBuilder().withId(id).withSize(dx, dy, dz).withWeight(weight).build();
	}
}
