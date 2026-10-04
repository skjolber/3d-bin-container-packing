package com.github.skjolber.packing.visualizer.packaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

	/** Sample output, also parsed by the viewer's tests. Regenerate with -Dvisualizer.updateSample=true. */
	private static final File SAMPLE = new File("../viewer/src/fixtures/containers.json");

	//
	//  z
	//  2 +---+
	//    | C |      C rests on A and B
	//  1 +---+---+
	//    | A | B |  A: max load weight 5, B: max load box count 1
	//  0 +---+---+
	//    0   1   2  x
	//
	private static Container sampleContainer() {
		Box a = Box.newBuilder().withId("A").withDescription("base").withSize(1, 1, 1).withWeight(2).withMaxLoadWeight(5).build();
		Box b = Box.newBuilder().withId("B").withSize(1, 1, 1).withWeight(3).withMaxLoadBoxCount(1).build();
		Box c = Box.newBuilder().withId("C").withSize(2, 1, 1).withWeight(4).build();
		new BoxItem(a);
		new BoxItem(b);
		new BoxItem(c);

		Stack stack = new Stack();
		stack.add(new Placement(a.getStackValue(0), 0, 0, 0, 0));
		stack.add(new Placement(b.getStackValue(0), 0, 1, 0, 0));
		stack.add(new Placement(c.getStackValue(0), 0, 0, 0, 1));
		return Container.newBuilder()
				.withId("container")
				.withDescription("sample")
				.withSize(2, 1, 2)
				.withMaxLoadWeight(100)
				.withStack(stack)
				.build();
	}

	@Test
	void writesTheSampleJson() throws Exception {
		String json = new DefaultPackagingResultVisualizerFactory(true).visualize(List.of(sampleContainer())).toJson().replace("\r\n", "\n") + "\n";
		if(Boolean.getBoolean("visualizer.updateSample")) {
			Files.writeString(SAMPLE.toPath(), json, StandardCharsets.UTF_8);
		}
		assertThat(Files.readString(SAMPLE.toPath(), StandardCharsets.UTF_8))
				.as("JSON format changed: update the viewer, then regenerate %s with -Dvisualizer.updateSample=true", SAMPLE)
				.isEqualTo(json);
	}

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
