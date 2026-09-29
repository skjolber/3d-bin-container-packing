package com.github.skjolber.packing.virtualbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.github.skjolber.packing.virtualbox.VirtualBoxLayoutTest.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.virtualbox.bounds.VirtualBoxBounds;

class VirtualBoxLayoutPreparationTest {
	/*
	 *       +---+-------+
	 *       | A |  B3   |   Only these two top faces are exposed.
	 *       |   +-------+
	 *       | A |  B2   |   Internal contacts have area 2.
	 *       |   +-------+
	 *       | A |  B1   |
	 *       |   +-------+
	 *       | A |  B0   |   Only these two bottom faces are exposed.
	 *       +---+-------+
	 *
	 * A supplies minimum area (1); B supplies minimum volume (2).
	 */
	@Test
	void preparesMinimaAndLazilyCachesPhysicalContacts() {
		BoxItem a = item(1, 1, 4, 1);
		BoxItem b = item(2, 1, 1, 4);
		var layout = new VirtualBoxLayout(new VirtualBoxBounds(3, 1, 4), List.of(
				placement(a, 0, 0, 0), placement(b, 1, 0, 0), placement(b, 1, 0, 1), placement(b, 1, 0, 2), placement(b, 1, 0, 3)));
		VirtualBox.of(List.of(layout));
		assertThat(layout.getMinimumArea()).isEqualTo(1);
		assertThat(layout.getMinimumVolume()).isEqualTo(2);
		assertThat(layout.loadSupport).isNull();
		long[] order = layout.placementOrder;
		layout.prepare();
		assertThat(layout.placementOrder).isSameAs(order);
		var support = layout.getLoadSupport();
		assertThat(layout.getLoadSupport()).isSameAs(support);
		assertThat(support.getSupporters()).containsExactly(1, 2, 3);
		assertThat(support.getSupportees()).containsExactly(2, 3, 4);
		assertThat(support.getContactAreas()).containsExactly(2, 2, 2);
		assertThat(support.getTopFaces()).containsExactly(0, 4);
		assertThat(support.getBottomFaces()).containsExactly(0, 1);
	}

	/* Each grid is filled; X, Y and Z sweeps must find exactly the vertical contacts. */
	@Test
	void contactsAreIndependentOfSweepAxisAndInputOrder() {
		for(int[] dimensions : new int[][] {{6, 1, 2}, {1, 6, 2}, {1, 1, 12}}) {
			var layout = new GridVirtualBoxLayoutGenerator().generate(item(1, 1, 1, 12),
					List.of(container(dimensions[0], dimensions[1], dimensions[2])), 1, () -> false).get(0);
			var support = layout.getLoadSupport();
			int footprint = dimensions[0] * dimensions[1];
			assertThat(support.getContactAreas()).hasSize(12 - footprint).containsOnly(1);
			assertThat(support.getTopFaces()).hasSize(footprint);
			assertThat(support.getBottomFaces()).hasSize(footprint);
		}
		BoxItem item = item(1, 1, 1, 2);
		var reversed = new VirtualBoxLayout(new VirtualBoxBounds(1, 1, 2), List.of(placement(item, 0, 0, 1), placement(item, 0, 0, 0)));
		assertThat(reversed.getLoadSupport().getSupporters()).containsExactly(1);
		assertThat(reversed.getLoadSupport().getSupportees()).containsExactly(0);
	}

	@Test
	void rejectsInvalidGeometryAtVirtualBoxBoundary() {
		BoxItem cube = item(1, 1, 1, 1);
		BoxItem domino = item(2, 1, 1, 2);
		for(VirtualBoxLayout invalid : List.of(
				new VirtualBoxLayout(new VirtualBoxBounds(2, 1, 1), List.of(placement(cube, 0, 0, 0))),
				new VirtualBoxLayout(new VirtualBoxBounds(4, 1, 1), List.of(placement(domino, 0, 0, 0), placement(domino, 1, 0, 0))),
				new VirtualBoxLayout(new VirtualBoxBounds(1, 1, 1), List.of(placement(cube, -1, 0, 0))),
				new VirtualBoxLayout(new VirtualBoxBounds(1, 1, 1), List.of(placement(cube, 1, 0, 0))),
				new VirtualBoxLayout(new VirtualBoxBounds(1, 1, 1), List.of()))) {
			assertThatThrownBy(() -> VirtualBox.of(List.of(invalid))).isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Test
	void resolvesClonedAndReindexedOrientationsAndKeepsWorkersIndependent() {
		BoxItem original = item(1, 1, 1, 2);
		var horizontal = new VirtualBoxLayout(new VirtualBoxBounds(2, 1, 1), List.of(placement(original, 0, 0, 0), placement(original, 1, 0, 0)));
		var vertical = new VirtualBoxLayout(new VirtualBoxBounds(1, 1, 2), List.of(placement(original, 0, 0, 0), placement(original, 0, 0, 1)));
		VirtualBoxPacking packing = new VirtualBoxPacking();
		packing.add(item(2, 2, 2, 1));
		packing.add(VirtualBox.of(List.of(horizontal, vertical)));
		BoxItem delegate = packing.getItems().get(1);
		BoxItem cloned = new BoxItem(delegate.getBox().clone(), 1, 73, delegate.getGlobalIndex());
		assertThat(packing.getLayout(cloned.getBox().getStackValue(0))).isSameAs(horizontal);
		assertThat(packing.getLayout(cloned.getBox().getStackValue(1))).isSameAs(vertical);
		assertThat(packing.getLayout(packing.getItems().get(0).getBox().getStackValue(0))).isNull();
		List<Placement> first = vertical.createPlacements(true);
		List<Placement> second = vertical.createPlacements(true);
		vertical.translate(3, 4, 5, first);
		assertThat(first.get(1).getAbsoluteZ()).isEqualTo(6);
		assertThat(second.get(1).getAbsoluteZ()).isEqualTo(1);
		assertThat(vertical.getPlacements().get(1).getAbsoluteZ()).isEqualTo(1);
		assertThat(first.get(0).getSupporters()).isNotSameAs(second.get(0).getSupporters());
		assertThat(first.get(0).getBoxItem()).isSameAs(original);
		vertical.translate(7, 8, 9, first);
		assertThat(first.get(1).getAbsoluteZ()).isEqualTo(10);
		assertThat(second.get(1).getAbsoluteZ()).isEqualTo(1);
	}

	protected static Placement placement(BoxItem item, int x, int y, int z) {
		return new Placement(item.getBox().getStackValue(0), -1, x, y, z, false);
	}

	/*
	 *       +---------------+
	 *       |       T       |   Keep both supporter links and their contact areas.
	 *       +-------+-------+
	 *       |   A   |   B   |
	 *       +-------+-------+
	 */
	@Test
	void preservesMultipleSupportersInsteadOfMergingUniformSurface() {
		BoxItem lower = item(2, 1, 1, 2);
		BoxItem upper = item(4, 1, 1, 1);
		var layout = new VirtualBoxLayout(new VirtualBoxBounds(4, 1, 2), List.of(
				placement(upper, 0, 0, 1), placement(lower, 0, 0, 0), placement(lower, 2, 0, 0)));
		var support = layout.getLoadSupport();
		assertThat(support.getSupporters()).containsExactly(1, 2);
		assertThat(support.getSupportees()).containsExactly(0, 0);
		assertThat(support.getContactAreas()).containsExactly(2, 2);
		assertThat(support.getTopFaces()).containsExactly(0);
		assertThat(support.getBottomFaces()).containsExactly(1, 2);
	}

	/* A large one-row grid should use an X sweep; no horizontal neighbour is an internal load contact. */
	@Test
	void preparesLargeGridWithoutBuildingLoadMetadataForOrdinaryPacking() {
		var layout = new GridVirtualBoxLayoutGenerator().generate(item(1, 1, 1, 10_000), List.of(container(10_000, 1, 1)), 1, () -> false).get(0);
		VirtualBox.of(List.of(layout));
		assertThat(layout.sweepAxis).isZero();
		assertThat(layout.getMinimumArea()).isEqualTo(1);
		assertThat(layout.getMinimumVolume()).isEqualTo(1);
		assertThat(layout.loadSupport).isNull();
		var support = layout.getLoadSupport();
		assertThat(support.getContactAreas()).isEmpty();
		assertThat(support.getTopFaces()).hasSize(10_000);
		assertThat(support.getBottomFaces()).hasSize(10_000);
	}
}
