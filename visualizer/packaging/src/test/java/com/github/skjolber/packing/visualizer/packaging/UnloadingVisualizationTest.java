package com.github.skjolber.packing.visualizer.packaging;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.Unloading;
import com.github.skjolber.packing.visualizer.api.packaging.PackagingResultVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.PackagingResultsVisualizer;

/**
 * Writes the same stack validated for both ways of unloading, to compare them in the viewer (key R).
 *
 * <pre>
 *  z
 *  20 +-------------------+
 *     |         2         |   weight 40, placed second, on box 1, overhanging
 *  10 +---------+---------+
 *     |    1    |    3    |   box 1 carries at most 30; box 3 is placed last, into the gap under the overhang
 *   0 +---------+---------+
 *     0        10        20  x
 * </pre>
 *
 * With {@link Unloading#ANY_ORDER}, box 3 may be unloaded first, so box 1 carries all of box 2: invalid. With
 * {@link Unloading#REVERSE_LOADING_ORDER}, boxes 1 and 3 share box 2: valid.
 */
public class UnloadingVisualizationTest extends AbstractPackagerTest {

	@Test
	void overhangForBothWaysOfUnloading() throws Exception {
		PackagingResultsVisualizer visualization = new PackagingResultsVisualizer();
		for (Unloading unloading : Unloading.values()) {
			PackagingResultVisualizer result = new DefaultPackagingResultVisualizerFactory(true, unloading).visualize(new PackagerResult(List.of(overhang()), 0, false));
			result.setName(unloading.name());
			visualization.add(result);
		}
		Files.writeString(OUTPUT.toPath(), visualization.toJson(), StandardCharsets.UTF_8);
	}

	private static Container overhang() {
		Box one = Box.newBuilder().withId("1").withSize(10, 10, 10).withWeight(10).withMaxLoadWeight(30).build();
		Box two = Box.newBuilder().withId("2").withSize(20, 10, 10).withWeight(40).build();
		Box three = Box.newBuilder().withId("3").withSize(10, 10, 10).withWeight(10).build();
		new BoxItem(one);
		new BoxItem(two);
		new BoxItem(three);

		Stack stack = new Stack();
		stack.add(new Placement(one.getStackValue(0), 0, 0, 0, 0));
		stack.add(new Placement(two.getStackValue(0), 0, 0, 0, 10));
		stack.add(new Placement(three.getStackValue(0), 0, 10, 0, 0));
		return Container.newBuilder().withId("container").withSize(20, 10, 20).withMaxLoadWeight(1000).withStack(stack).build();
	}
}
