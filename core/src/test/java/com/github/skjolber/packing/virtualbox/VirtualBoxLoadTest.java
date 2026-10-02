package com.github.skjolber.packing.virtualbox;

import static org.assertj.core.api.Assertions.assertThat;
import static com.github.skjolber.packing.virtualbox.VirtualBoxLayoutTest.container;
import static com.github.skjolber.packing.virtualbox.VirtualBoxLayoutTest.item;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.validator.load.IdenticalBoxOnlyLoadValidator;
import com.github.skjolber.packing.validator.load.MaxBoxCountLoadValidator;
import com.github.skjolber.packing.validator.load.MaxPressureLoadValidator;
import com.github.skjolber.packing.validator.load.WeightLoadValidator;
import com.github.skjolber.packing.virtualbox.VirtualBoxPackagerTest.RecordingPackager;

class VirtualBoxLoadTest {
	/*
	 *             +-------+    top carries 0
	 *             |   A   |
	 *             +-------+    middle carries 1
	 *             |   A   |
	 *             +-------+    bottom carries 2
	 *             |   A   |
	 *             +-------+
	 *
	 * The delegate gets three real boxes and builds the physical load graph itself.
	 */
	@Test
	void preservesTheDelegatesPhysicalLoadGraph() throws IOException {
		BoxItem original = new BoxItem(Box.newBuilder()
				.withSize(1, 1, 1)
				.withWeight(1)
				.withMaxLoadWeight(2)
				.withMaxLoadPressure(2)
				.withMaxLoadIdenticalBoxCount(2)
				.build(), 3);
		try(PlainPackager delegate = PlainPackager.newBuilder().build();
				RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			PackagerResult result = wrapper.newResultBuilder()
					.withBoxItems(original)
					.withContainerItems(new ContainerItem(container(1, 1, 3), 1))
					.build();
			assertThat(recording.counts).containsExactly(3);
			assertThat(result.isSuccess()).isTrue();
			assertThat(result.get(0).getStack().getPlacements()).hasSize(3);
			assertLoads(result);
			for(Placement placement : result.get(0).getStack().getPlacements()) {
				assertThat(placement.getLoadWeight()).isEqualTo(2 - placement.getAbsoluteZ());
			}
			assertThat(original.getCount()).isEqualTo(3);
		}
	}

	/*
	 *       +-------+-------+-------+-------+
	 *       |   U   |   U   |   U   |   C   |
	 *       +-------+-------+-------+-------+
	 *
	 * A constrained singleton C prevents aggregation of unconstrained U too:
	 * otherwise U's envelope could conceal its physical load on C.
	 */
	@Test
	void everyLoadConstraintBypassesTheWholeOperation() throws IOException {
		List<Box> boxes = new ArrayList<>(constrainedBoxes());
		boxes.add(Box.newBuilder()
				.withSize(1, 1, 1)
				.withWeight(1)
				.withMaxLoadIdenticalBoxCount(-1)
				.build());
		for(Box box : boxes) {
			BoxItem unconstrained = item(1, 1, 1, 3);
			BoxItem constrained = new BoxItem(box);
			try(PlainPackager delegate = PlainPackager.newBuilder().build();
					RecordingPackager recording = new RecordingPackager(delegate);
					VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
				VirtualBoxPackagerResultBuilder operation = new VirtualBoxPackagerResultBuilder(recording, wrapper.scheduler) {
					@Override
					protected VirtualBoxPlan prepare(PackagerInterruptSupplier stop) {
						throw new AssertionError("Load-constrained operations must not build virtual boxes");
					}
				};
				PackagerResult result = operation
						.withBoxItems(unconstrained, constrained)
						.withContainerItems(new ContainerItem(container(4, 1, 1), 1))
						.withMaxDelegateBoxes(1)
						.withCompareUngrouped(true)
						.build();
				assertThat(recording.counts).containsExactly(4);
				assertThat(result.isSuccess()).isTrue();
				assertThat(result.get(0).getStack().getPlacements()).hasSize(4);
				assertLoads(result);
				assertThat(unconstrained.getCount()).isEqualTo(3);
				assertThat(constrained.getCount()).isEqualTo(1);
			}
		}
	}

	/*
	 *       +-------+
	 *       |   A   |      Three-high packing exceeds weight/pressure/depth 1.
	 *       +-------+
	 *       |   A   |      The delegate must reject it during placement.
	 *       +-------+
	 *       |   A   |      There is no post-pack rejection or wrapper retry.
	 *       +-------+
	 */
	@Test
	void delegateRejectsImpossibleLoadsWithoutWrapperRetries() throws IOException {
		for(Box box : constrainedBoxes()) {
			try(PlainPackager delegate = PlainPackager.newBuilder().build();
					RecordingPackager recording = new RecordingPackager(delegate);
					VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
				PackagerResult result = wrapper.newResultBuilder()
						.withBoxItems(new BoxItem(box, 3))
						.withContainerItems(new ContainerItem(container(1, 1, 3), 1))
						.withCompareUngrouped(true)
						.build();
				assertThat(result.isSuccess()).as("weight=%s pressure=%s count=%s", box.isMaxLoadWeight(), box.isMaxLoadPressure(), box.isMaxLoadBoxCount()).isFalse();
				assertThat(recording.counts).containsExactly(3);
			}
		}
	}

	/* Standalone grid construction also excludes internally overloaded towers. */
	@Test
	void gridRejectsEachInternalLoadViolation() {
		for(Box box : constrainedBoxes()) {
			assertThat(new GridVirtualBoxLayoutGenerator().generate(new BoxItem(box, 3),
					List.of(container(1, 1, 3)), 8, () -> false)).isEmpty();
		}
	}

	protected static List<Box> constrainedBoxes() {
		return List.of(
				Box.newBuilder()
						.withSize(1, 1, 1)
						.withWeight(1)
						.withMaxLoadWeight(1)
						.build(),
				Box.newBuilder()
						.withSize(1, 1, 1)
						.withWeight(1)
						.withMaxLoadPressure(1)
						.build(),
				Box.newBuilder()
						.withSize(1, 1, 1)
						.withWeight(1)
						.withMaxLoadBoxCount(1)
						.build());
	}

	protected static void assertLoads(PackagerResult result) {
		for(Container container : result.getContainers()) {
			List<ValidatorResultReason> reasons = new ArrayList<>();
			List<Placement> placements = container.getStack().getPlacements();
			assertThat(new WeightLoadValidator().isValid(placements, reasons)).as("%s", reasons).isTrue();
			assertThat(new MaxPressureLoadValidator().isValid(placements, reasons)).as("%s", reasons).isTrue();
			assertThat(new MaxBoxCountLoadValidator().isValid(placements, reasons)).as("%s", reasons).isTrue();
			assertThat(new IdenticalBoxOnlyLoadValidator().isValid(placements, reasons)).as("%s", reasons).isTrue();
		}
	}
}
