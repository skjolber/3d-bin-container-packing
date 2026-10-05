package com.github.skjolber.packing.visualizer.packaging;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.packer.InsertionSequencer;
import com.github.skjolber.packing.visualizer.api.packaging.PackagingResultVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.PackagingResultsVisualizer;

/**
 * Writes the same container in the order of a packager's search, and in insertion order, to compare them in the
 * viewer (key R; step through the boxes with A and D).
 *
 * <pre>
 *  side view (z up), door at x = 30
 *
 *  20 +-------------------+
 *     |         2         |   box 2 overhangs box 1; box 3 is in the gap under the overhang,
 *  10 +---------+---------+---------+   and box 4 is between box 3 and the door
 *     |    1    |    3    |    4    |
 *   0 +---------+---------+---------+
 *     0        10        20        30  x
 * </pre>
 *
 * In the search order (1, 2, 4, 3), box 3 is placed under box 2, which already rests on it, and behind box 4: not a
 * possible insertion order. In insertion order (1, 3, 2, 4), each box is inserted after the boxes it rests on, and
 * before the boxes between it and the door.
 */
public class InsertionOrderVisualizationTest extends AbstractPackagerTest {

	@Test
	void searchOrderAndInsertionOrder() throws Exception {
		DefaultPackagingResultVisualizerFactory factory = new DefaultPackagingResultVisualizerFactory(true);
		PackagingResultsVisualizer visualization = new PackagingResultsVisualizer();

		Container searchOrder = container();
		visualization.add(named(factory, searchOrder, "search order"));

		Container insertionOrder = container();
		InsertionSequencer.sequence(insertionOrder.getStack(), insertionOrder.getAccess());
		visualization.add(named(factory, insertionOrder, "insertion order"));

		Files.writeString(OUTPUT.toPath(), visualization.toJson(), StandardCharsets.UTF_8);
	}

	private static PackagingResultVisualizer named(DefaultPackagingResultVisualizerFactory factory, Container container, String name) {
		PackagingResultVisualizer result = factory.visualize(new PackagerResult(List.of(container), 0, false));
		result.setName(name);
		return result;
	}

	private static Container container() {
		Box one = Box.newBuilder().withId("1").withSize(10, 10, 10).withWeight(10).build();
		Box two = Box.newBuilder().withId("2").withSize(20, 10, 10).withWeight(10).build();
		Box three = Box.newBuilder().withId("3").withSize(10, 10, 10).withWeight(10).build();
		Box four = Box.newBuilder().withId("4").withSize(10, 10, 10).withWeight(10).build();
		for (Box box : List.of(one, two, three, four)) {
			new BoxItem(box);
		}
		Stack stack = new Stack();
		stack.add(new Placement(one.getStackValue(0), 0, 0, 0, 0));
		stack.add(new Placement(two.getStackValue(0), 0, 0, 0, 10));
		stack.add(new Placement(four.getStackValue(0), 0, 20, 0, 0));
		stack.add(new Placement(three.getStackValue(0), 0, 10, 0, 0));
		return Container.newBuilder()
				.withId("container")
				.withSize(30, 10, 20)
				.withMaxLoadWeight(1000)
				.withAccess(ContainerAccess.FRONT)
				.withStack(stack)
				.build();
	}
}
