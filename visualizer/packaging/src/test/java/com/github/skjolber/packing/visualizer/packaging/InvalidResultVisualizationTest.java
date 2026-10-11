package com.github.skjolber.packing.visualizer.packaging;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;

/**
 * Writes an invalid result, to see how the viewer shows validation reasons: the boxes are outlined in red,
 * and the reasons are listed in the summary and box panels.
 *
 * <pre>
 *   z
 *   4 +---+
 *     | 4 |
 *   3 +---+
 *     | 3 |
 *   2 +---+
 *     | 2 |
 *   1 +---+
 *     | 1 |   every box weighs 5 and carries at most 6:
 *   0 +---+   boxes 1 and 2 are overloaded (15 and 10)
 *     0   1  x
 * </pre>
 */
public class InvalidResultVisualizationTest extends AbstractPackagerTest {

	@Test
	void overloadedColumn() throws Exception {
		Stack stack = new Stack();
		for (int i = 0; i < 4; i++) {
			Box box = Box.newBuilder().withId(Integer.toString(i + 1)).withSize(1, 1, 1).withWeight(5).withMaxLoadWeight(6).build();
			new BoxItem(box);
			stack.add(new Placement(box.getStackValue(0), 0, 0, 0, i));
		}
		Container container = Container.newBuilder()
				.withDescription("column")
				.withSize(1, 1, 4)
				.withMaxLoadWeight(100)
				.withStack(stack)
				.build();

		write(new PackagerResult(List.of(container), 0, false));
	}
}
