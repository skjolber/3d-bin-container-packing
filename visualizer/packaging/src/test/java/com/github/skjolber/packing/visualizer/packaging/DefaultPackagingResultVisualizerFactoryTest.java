package com.github.skjolber.packing.visualizer.packaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.visualizer.api.packaging.BoxVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.PackagingResultVisualizer;

class DefaultPackagingResultVisualizerFactoryTest {

	@Test
	void assignsStableKeysToBoxItems() {
		Box firstBox = Box.newBuilder().withId("same-label").withSize(1, 1, 1).withWeight(1).build();
		Box secondBox = Box.newBuilder().withId("same-label").withSize(1, 1, 1).withWeight(1).build();
		new BoxItem(firstBox, 2);
		new BoxItem(secondBox);

		Stack stack = new Stack();
		stack.add(new Placement(firstBox.getStackValue(0), 0, 0, 0, 0));
		stack.add(new Placement(firstBox.getStackValue(0), 0, 1, 0, 0));
		stack.add(new Placement(secondBox.getStackValue(0), 0, 2, 0, 0));
		Container container = Container.newBuilder()
				.withSize(3, 1, 1)
				.withMaxLoadWeight(10)
				.withStack(stack)
				.build();

		PackagingResultVisualizer result = new DefaultPackagingResultVisualizerFactory(false).visualize(List.of(container));
		List<BoxVisualizer> boxes = result.getContainers().get(0).getStack().getPlacements().stream()
				.map(placement -> (BoxVisualizer)placement.getStackable())
				.toList();

		assertThat(boxes.get(0).getBoxItemKey()).isEqualTo(boxes.get(1).getBoxItemKey());
		assertThat(boxes.get(2).getBoxItemKey()).isNotEqualTo(boxes.get(0).getBoxItemKey());
	}
}
