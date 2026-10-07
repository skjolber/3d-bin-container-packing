package com.github.skjolber.packing.visualizer.packaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.visualizer.api.packaging.BoxVisualizer;
import com.github.skjolber.packing.validator.DefaultValidator;
import com.github.skjolber.packing.visualizer.api.packaging.ContainerVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.PackagingResultVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.PlacementReferenceVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.StackPlacementVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.ValidationReasonVisualizer;

class DefaultPackagingResultVisualizerFactoryTest {

	/** Sample output, also parsed by the viewer's tests. Regenerate with -Dvisualizer.updateSample=true. */
	private static final File SAMPLE = new File("../viewer/src/fixtures/containers.json");

	//
	//  z
	//  2 +---+
	//    | C |      C (weight 4) rests on A and B
	//  1 +---+---+
	//    | A | B |  A: max load weight 1, so carrying half of C is invalid; B: max load box count 1
	//  0 +---+---+
	//    0   1   2  x
	//
	private static Container sampleContainer() {
		return sampleContainer(1);
	}

	private static Container sampleContainer(long maxLoadWeightOfA) {
		Box a = Box.newBuilder().withId("A").withDescription("base").withSize(1, 1, 1).withWeight(2).withMaxLoadWeight(maxLoadWeightOfA).build();
		Box b = Box.newBuilder().withId("B").withSize(1, 1, 1).withWeight(3).withMaxLoadBoxCount(1).build();
		Box c = Box.newBuilder().withId("C").withSize(2, 1, 1).withWeight(4).build();
		BoxItem itemA = new BoxItem(a);
		BoxItem itemB = new BoxItem(b);
		BoxItem itemC = new BoxItem(c);

		Stack stack = new Stack();
		stack.add(new Placement(itemA, a.getStackValue(0), 0, 0, 0, 0));
		stack.add(new Placement(itemB, b.getStackValue(0), 0, 1, 0, 0));
		stack.add(new Placement(itemC, c.getStackValue(0), 0, 0, 0, 1));
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
		Map<String, PackagerResult> results = new LinkedHashMap<>();
		results.put("sample", new PackagerResult(List.of(sampleContainer()), 12, false, 34));
		// a second result to compare with: nothing packed
		results.put("empty", new PackagerResult(List.of(), 5, true));
		String json = new DefaultPackagingResultVisualizerFactory(true).visualize(results, null).toJson().replace("\r\n", "\n") + "\n";
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
		BoxItem first = new BoxItem(firstBox, 2);
		BoxItem second = new BoxItem(secondBox);

		Stack stack = new Stack();
		stack.add(new Placement(first, firstBox.getStackValue(0), 0, 0, 0, 0));
		stack.add(new Placement(first, firstBox.getStackValue(0), 0, 1, 0, 0));
		stack.add(new Placement(second, secondBox.getStackValue(0), 0, 2, 0, 0));
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

	@Test
	void marksPlacementsWhichExceedTheirLoadLimits() {
		PackagingResultVisualizer result = new DefaultPackagingResultVisualizerFactory(false).visualize(List.of(sampleContainer()));

		assertThat(result.isValid()).isFalse();
		assertThat(result.getValidationReasons()).hasSize(1);
		ValidationReasonVisualizer reason = result.getValidationReasons().get(0);
		assertThat(reason.getType()).isEqualTo("ExcessiveLoadWeightReason");
		assertThat(reason.getPlacements()).extracting(PlacementReferenceVisualizer::getContainer, PlacementReferenceVisualizer::getPlacement).containsExactly(tuple(0, 0));

		List<StackPlacementVisualizer> placements = result.getContainers().get(0).getStack().getPlacements();
		assertThat(placements.get(0).getReasons()).containsExactly(0);
		assertThat(placements.get(1).getReasons()).isEmpty();
		assertThat(placements.get(2).getReasons()).isEmpty();
	}

	@Test
	void validatesAgainstTheInput() throws Exception {
		Container container = sampleContainer(100);
		List<BoxItem> boxItems = new ArrayList<>();
		for (Placement placement : container.getStack().getPlacements()) {
			boxItems.add(placement.getBoxItem());
		}
		List<ContainerItem> containerItems = List.of(new ContainerItem(Container.newBuilder()
				.withId("container")
				.withSize(2, 1, 2)
				.withMaxLoadWeight(100)
				.withStack(new Stack())
				.build(), 1));
		PackagerResult packagerResult = new PackagerResult(List.of(container), 0, false);

		DefaultPackagingResultVisualizerFactory factory = new DefaultPackagingResultVisualizerFactory(false);
		try (DefaultValidator validator = new DefaultValidator()) {
			PackagingResultVisualizer valid = factory.visualize(packagerResult, validator.newResultBuilder()
					.withContainerItems(containerItems)
					.withMaxContainerCount(1)
					.withBoxItems(boxItems));
			assertThat(valid.isValid()).isTrue();
			assertThat(valid.getValidationReasons()).isEmpty();

			// one more box than was packed
			List<BoxItem> moreBoxItems = new ArrayList<>(boxItems);
			moreBoxItems.add(new BoxItem(Box.newBuilder().withId("D").withSize(1, 1, 1).withWeight(1).build()));
			PackagingResultVisualizer invalid = factory.visualize(packagerResult, validator.newResultBuilder()
					.withContainerItems(containerItems)
					.withMaxContainerCount(1)
					.withBoxItems(moreBoxItems));
			assertThat(invalid.isValid()).isFalse();
			assertThat(invalid.getValidationReasons()).isNotEmpty();
			// the invalid result is still visualized
			assertThat(invalid.getContainers().get(0).getStack().getPlacements()).hasSize(3);
		}
	}

	@Test
	void calculatesSupportLoadsAndCenterOfGravity() {
		PackagingResultVisualizer result = new DefaultPackagingResultVisualizerFactory(false).visualize(List.of(sampleContainer()));

		ContainerVisualizer container = result.getContainers().get(0);
		List<StackPlacementVisualizer> placements = container.getStack().getPlacements();
		// A and B on the floor, C on both
		assertThat(placements).extracting(StackPlacementVisualizer::getSupportedArea).containsExactly(0L, 0L, 2L);
		// C (weight 4) rests half on A and half on B
		assertThat(placements).extracting(StackPlacementVisualizer::getLoadWeight).containsExactly(2.0, 2.0, 0.0);

		// weights 2, 3 and 4 at centres x 0.5, 1.5 and 1, z 0.5, 0.5 and 1.5
		assertThat(container.getCenterOfGravityX()).isCloseTo(9.5 / 9, within(1e-9));
		assertThat(container.getCenterOfGravityY()).isCloseTo(0.5, within(1e-9));
		assertThat(container.getCenterOfGravityZ()).isCloseTo(8.5 / 9, within(1e-9));
	}
}
