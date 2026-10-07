package com.github.skjolber.packing.virtualbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.github.skjolber.packing.virtualbox.VirtualBoxLayoutTest.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Placement;

class VirtualBoxLayoutPreparationTest {
	/*
	 *       +---+-------+
	 *       | A |  B3   |
	 *       |   +-------+
	 *       | A |  B2   |
	 *       |   +-------+
	 *       | A |  B1   |
	 *       |   +-------+
	 *       | A |  B0   |
	 *       +---+-------+
	 *
	 * Mixed geometry must still fill the envelope exactly, without overlaps.
	 */
	@Test
	void validatesMixedGeometryOnlyOnce() {
		BoxItem a = item(1, 1, 4, 1);
		BoxItem b = item(2, 1, 1, 4);
		int[] preparations = {0};
		var layout = new VirtualBoxLayout(new VirtualBoxBounds(3, 1, 4), List.of(
				placement(a, 0, 0, 0), placement(b, 1, 0, 0), placement(b, 1, 0, 1), placement(b, 1, 0, 2), placement(b, 1, 0, 3))) {
			@Override
			protected void prepareGeometry() {
				preparations[0]++;
				super.prepareGeometry();
			}
		};
		VirtualBox.of(List.of(layout));
		layout.prepare();
		assertThat(layout.prepared).isTrue();
		assertThat(preparations[0]).isEqualTo(1);
	}

	/* Each grid is filled; general geometry validation works along X, Y and Z. */
	@Test
	void geometryValidationIsIndependentOfSweepAxisAndInputOrder() {
		for(int[] dimensions : new int[][] {{6, 1, 2}, {1, 6, 2}, {1, 1, 12}}) {
			var layout = new GridVirtualBoxLayoutGenerator().generate(item(1, 1, 1, 12),
					List.of(container(dimensions[0], dimensions[1], dimensions[2])), 1, () -> false).get(0);
			VirtualBoxLayout generic = new VirtualBoxLayout(layout.getBoundingBox(), layout.getPlacements());
			generic.prepare();
			assertThat(generic.prepared).isTrue();
		}
		BoxItem item = item(1, 1, 1, 2);
		var reversed = new VirtualBoxLayout(new VirtualBoxBounds(1, 1, 2), List.of(placement(item, 0, 0, 1), placement(item, 0, 0, 0)));
		reversed.prepare();
		assertThat(reversed.prepared).isTrue();
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
	void resolvesOrientationsOfTheDelegateItems() {
		BoxItem original = item(1, 1, 1, 2);
		var horizontal = new VirtualBoxLayout(new VirtualBoxBounds(2, 1, 1), List.of(placement(original, 0, 0, 0), placement(original, 1, 0, 0)));
		var vertical = new VirtualBoxLayout(new VirtualBoxBounds(1, 1, 2), List.of(placement(original, 0, 0, 0), placement(original, 0, 0, 1)));
		VirtualBoxPacking packing = new VirtualBoxPacking();
		packing.add(item(2, 2, 2, 1));
		packing.add(VirtualBox.of(List.of(horizontal, vertical)));
		BoxItem delegate = packing.getItems().get(1);
		assertThat(packing.getLayout(delegate, delegate.getBox().getStackValue(0))).isSameAs(horizontal);
		assertThat(packing.getLayout(delegate, delegate.getBox().getStackValue(1))).isSameAs(vertical);
		// the delegate's results refer to the box items it was given
		BoxItem copied = new BoxItem(delegate.getBox().copy(), 1);
		assertThatThrownBy(() -> packing.getLayout(copied, copied.getBox().getStackValue(0))).isInstanceOf(IllegalStateException.class);
		assertThat(packing.getLayout(packing.getItems().get(0), packing.getItems().get(0).getBox().getStackValue(0))).isNull();
		assertThat(vertical.getPlacements().get(1).getAbsoluteZ()).isEqualTo(1);
	}

	protected static Placement placement(BoxItem item, int x, int y, int z) {
		return new Placement(item, item.getBox().getStackValue(0), -1, x, y, z, false);
	}

	/* A generated grid is already valid; preparation must not redo the general geometry sweep. */
	@Test
	void preparesLargeGridWithoutGeneralGeometryValidation() {
		var layout = new GridVirtualBoxLayoutGenerator().generate(item(1, 1, 1, 10_000), List.of(container(10_000, 1, 1)), 1, () -> false).get(0);
		VirtualBox.of(List.of(layout));
		assertThat(layout).isInstanceOf(GridVirtualBoxLayoutGenerator.GridLayout.class);
		assertThat(layout.prepared).isTrue();
		assertThat(layout.getPlacements()).hasSize(10_000);
	}
}
