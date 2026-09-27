package com.github.skjolber.packing.virtualbox;

import static org.assertj.core.api.Assertions.*;
import static com.github.skjolber.packing.virtualbox.VirtualBoxLayoutTest.*;
import static com.github.skjolber.packing.virtualbox.VirtualBoxPackagerTest.assertValid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.github.skjolber.packing.api.*;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.packer.PackagerInterruptedException;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.validator.load.*;
import com.github.skjolber.packing.virtualbox.VirtualBoxPackagerTest.RecordingPackager;

class VirtualBoxLoadTest {
	/*
	 * Three identical cubes:
	 *
	 *             +-------+    top carries 0
	 *             |   A   |
	 *             +-------+    middle carries 1
	 *             |   A   |
	 *             +-------+    bottom carries 2
	 *             |   A   |
	 *             +-------+
	 *
	 * Grid checks and expanded physical support graph agree on weight, pressure and depth.
	 */
	@Test
	void expandsAValidTowerWithOriginalLoadGraph() throws IOException {
		BoxItem a = new BoxItem(Box.newBuilder().withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(2)
				.withMaxLoadPressure(2).withMaxLoadIdenticalBoxCount(2).build(), 3);
		try(PlainPackager delegate = PlainPackager.newBuilder().build(); VirtualBoxPackager wrapper = new VirtualBoxPackager(delegate)) {
			PackagerResult result = wrapper.newResultBuilder().withBoxItems(a).withContainerItems(new ContainerItem(container(1, 1, 3), 1)).build();
			assertValid(result, List.of(a));
			assertLoads(result);
			for(Placement placement : result.get(0).getStack().getPlacements()) {
				assertThat(placement.getLoadWeight()).isEqualTo(2 - placement.getAbsoluteZ());
			}
		}
	}

	/*
	 * The same three-cube column is forbidden by any one of:
	 * max weight 1, max pressure 1, or max depth 1.
	 */
	@Test
	void gridRejectsEachInternalLoadViolation() {
		List<Box> boxes = List.of(
				Box.newBuilder().withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(1).build(),
				Box.newBuilder().withSize(1, 1, 1).withWeight(1).withMaxLoadPressure(1).build(),
				Box.newBuilder().withSize(1, 1, 1).withWeight(1).withMaxLoadBoxCount(1).build());
		for(Box box : boxes) {
			assertThat(new GridVirtualBoxLayoutGenerator().generate(new BoxItem(box, 3), List.of(container(1, 1, 3)), 8, () -> false)).isEmpty();
		}
	}

	/*
	 * A is an internally valid two-box floor; B is put on top by the first delegate attempt.
	 *
	 *             +---------------+
	 *             |       B       |
	 *             +-------+-------+
	 *             |   A   |   A   |    A permits no load
	 *             +-------+-------+
	 *
	 * Reject the expanded result, then retry ungrouped. B can support the loose A boxes.
	 */
	@Test
	void externalLoadViolationTriggersUngroupedFallback() throws IOException {
		BoxItem a = new BoxItem(Box.newBuilder().withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(0).build(), 2);
		BoxItem b = item(2, 1, 1, 1);
		try(PlainPackager delegate = PlainPackager.newBuilder().build(); RecordingPackager recording = new RecordingPackager(delegate);
				VirtualBoxPackager wrapper = new VirtualBoxPackager(recording)) {
			recording.stackFirst = true;
			PackagerResult result = wrapper.newResultBuilder().withBoxItems(a, b).withContainerItems(new ContainerItem(container(2, 2, 2), 1))
					.withBruteForce(false).withMaxRefinements(0).build();
			assertThat(recording.counts).containsExactly(2, 3);
			assertValid(result, List.of(a, b));
			assertLoads(result);
		}
	}

	/*
	 *      +-------+-------+
	 *      |   T   |   T   |    T cannot support another box
	 *      +-------+-------+
	 *      |       B       |    B carries weight 2
	 *      +---------------+
	 *
	 * Mixed brute-force assembly accepts B's limit 2, but rejects limit 1.
	 */
	@Test
	void mixedLayoutSearchUsesLoadAwareBoundingBoxes() {
		for(int limit : new int[] {1, 2}) {
			BoxItem base = new BoxItem(Box.newBuilder().withSize(2, 1, 1).withWeight(1).withMaxLoadWeight(limit).build());
			BoxItem tops = new BoxItem(Box.newBuilder().withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(0).build(), 2);
			var layouts = new BruteForceVirtualBoxLayoutGenerator(4, 8).generate(List.of(base, tops), List.of(container(2, 1, 2)), () -> false);
			if(limit == 1) {
				assertThat(layouts).isEmpty();
			} else {
				assertThat(layouts).isNotEmpty();
				assertFilled(layouts);
			}
		}
	}

	/*
	 * Distinct box items of identical dimensions remain distinct types:
	 *
	 *             +-------+
	 *             |   B   |     not identical to A
	 *             +-------+
	 *             |   A   |     only identical items allowed
	 *             +-------+
	 */
	@Test
	void expandedValidationRejectsDifferentIdentities() throws PackagerInterruptedException {
		BoxItem a = new BoxItem(Box.newBuilder().withSize(1, 1, 1).withWeight(1).withMaxLoadIdenticalBoxCount(1).build());
		BoxItem b = item(1, 1, 1, 1);
		Container container = container(1, 1, 2);
		container.getStack().add(new Placement(a.getBox().getStackValue(0), -1, 0, 0, 0, false));
		container.getStack().add(new Placement(b.getBox().getStackValue(0), -1, 0, 0, 1, false));
		assertThat(new VirtualBoxLoadValidator(() -> false).validate(container)).isNull();
	}

	/*
	 * Reusing a validator for a smaller container must not retain old supporters:
	 *
	 *       [ A ]               [ A ]
	 *       [ A ]     --->      floor only
	 *       [ A ]
	 */
	@Test
	void reusedValidationBuffersHandleDifferentAssemblySizes() throws PackagerInterruptedException {
		BoxItem a = item(1, 1, 1, 3);
		VirtualBoxLoadValidator validator = new VirtualBoxLoadValidator(() -> false);
		Container tower = container(1, 1, 3);
		for(int z = 0; z < 3; z++) {
			tower.getStack().add(new Placement(a.getBox().getStackValue(0), -1, 0, 0, z, false));
		}
		assertThat(validator.validate(tower).getStack().getPlacements().get(0).getLoadWeight()).isEqualTo(2);
		Container single = container(1, 1, 1);
		single.getStack().add(new Placement(a.getBox().getStackValue(0), -1, 0, 0, 0, false));
		Placement result = validator.validate(single).getStack().getPlacements().get(0);
		assertThat(result.getLoadWeight()).isZero();
		assertThat(result.getSupportees()).isEmpty();
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
