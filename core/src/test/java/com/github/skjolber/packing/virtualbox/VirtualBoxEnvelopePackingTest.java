package com.github.skjolber.packing.virtualbox;

import static org.assertj.core.api.Assertions.assertThat;
import static com.github.skjolber.packing.virtualbox.VirtualBoxLayoutTest.*;
import static com.github.skjolber.packing.virtualbox.VirtualBoxLayoutPreparationTest.placement;
import static com.github.skjolber.packing.virtualbox.VirtualBoxPackagerTest.assertValid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.packer.plain.PlainPackager;

class VirtualBoxEnvelopePackingTest {
	/*
	 * Three layers of eight physical cubes:
	 *
	 *       +---+---+---+---+
	 *       | A | A | A | A |
	 *       +---+---+---+---+
	 *       | A | A | A | A |
	 *       +---+---+---+---+
	 *
	 * Delegate: one 4 x 2 x 3 envelope, one add(index, placement).
	 * Result: 24 original cubes. No physical children enter the calculator.
	 */
	@Test
	void insertsOneEnvelopeAndDoesNotPreparePhysicalLoadState() throws IOException {
		EnvelopeOnlyBuilder builder = new EnvelopeOnlyBuilder();
		try(PlainPackager delegate = builder.build(); VirtualBoxPackager wrapper = new VirtualBoxPackager(delegate)) {
			VirtualBoxPackagerResultBuilder operation = wrapper.newResultBuilder();
			BoxItem original = item(1, 1, 1, 24);
			PackagerResult result = operation
					.withBoxItems(original)
					.withContainerItems(new ContainerItem(container(4, 2, 3), 1))
					.withMaxDelegateBoxes(1)
					.withMaxRefinements(0)
					.build();
			assertValid(result, List.of(original));
			assertThat(builder.calculators).hasSize(1);
			EnvelopeOnlyCalculator calculator = builder.calculators.get(0);
			assertThat(calculator.singleInsertions).isEqualTo(1);
			assertThat(calculator.getPlacements()).hasSize(1);
			Placement envelope = calculator.getPlacements().get(0);
			assertThat(envelope.getStackValue().getVolume()).isEqualTo(24);
			assertThat(envelope.getWeight()).isEqualTo(24);
			assertThat(envelope.getBoxItem()).isNotSameAs(original);
			assertThat(result.get(0).getStack().getPlacements()).hasSize(24);
			assertThat(result.get(0).getLoadWeight()).isEqualTo(24);

		}
	}

	/*
	 * Selected layout is vertical, not its horizontal alternative:
	 *
	 *                         +---+
	 *                         | A |   (3, 1, 2)
	 *                         +---+
	 *                         | A |   (3, 1, 1)
	 *       +-------+         +---+
	 *       |   B   |                 (0, 1, 0)
	 *       +-------+
	 *
	 * Only final expansion translates the relative children. Delegate cloning
	 * and local reindexing must not change the selected layout or original identities.
	 */
	@Test
	void expandsSelectedEnvelopeAtOffsetWithoutWorkerPlacementsOrLoadPreparation() {
		BoxItem a = item(1, 1, 1, 2);
		BoxItem b = item(2, 1, 1, 1);
		VirtualBoxLayout horizontal = new VirtualBoxLayout(new VirtualBoxBounds(2, 1, 1),
				List.of(placement(a, 0, 0, 0), placement(a, 1, 0, 0)));
		VirtualBoxLayout vertical = new VirtualBoxLayout(new VirtualBoxBounds(1, 1, 2),
				List.of(placement(a, 0, 0, 0), placement(a, 0, 0, 1)));
		VirtualBoxPacking packing = new VirtualBoxPacking();
		packing.add(VirtualBox.of(List.of(horizontal, vertical)));
		packing.add(b);
		BoxItem item = packing.getItems().get(0);
		BoxItem clone = new BoxItem(item.getBox().clone(), 1, 91, item.getGlobalIndex());
		Container packed = container(5, 3, 4);
		Placement envelope = new Placement(clone.getBox().getStackValue(1), -1, 3, 1, 1, false);
		packed.getStack().add(envelope);
		packed.getStack().add(new Placement(packing.getItems().get(1).getBox().getStackValue(0), -1, 0, 1, 0, false));
		PackagerResult delegateResult = new PackagerResult(List.of(packed), 0, false);
		for(int attempt = 0; attempt < 2; attempt++) {
			PackagerResult expanded = packing.expand(delegateResult, List.of(a, b), System.nanoTime());
			assertValid(expanded, List.of(a, b));
			List<Placement> children = expanded.get(0).getStack().getPlacements();
			assertThat(children).extracting(Placement::getAbsoluteX).containsExactly(3, 3, 0);
			assertThat(children).extracting(Placement::getAbsoluteY).containsExactly(1, 1, 1);
			assertThat(children).extracting(Placement::getAbsoluteZ).containsExactly(1, 2, 0);
			assertThat(children.get(0).getStackValue()).isSameAs(a.getBox().getStackValue(0));
			assertThat(children.get(2).getStackValue()).isSameAs(b.getBox().getStackValue(0));
			assertThat(expanded.get(0).getLoadWeight()).isEqualTo(packed.getLoadWeight());
		}
		assertThat(packed.getStack().getPlacements()).hasSize(2);
		assertThat(vertical.getPlacements()).extracting(Placement::getAbsoluteZ).containsExactly(0, 1);
	}

	/*
	 *       +---+---+---+---+
	 *       | A | A | A | A |   Four ordinary insertions when controls need children.
	 *       +---+---+---+---+
	 *
	 * Explicit opt-out skips preparation altogether, even if preprocessing limits
	 * would otherwise exclude this many delegate boxes.
	 */
	@Test
	void canBypassEnvelopesForPhysicalChildControls() throws IOException {
		EnvelopeOnlyBuilder builder = new EnvelopeOnlyBuilder();
		try(PlainPackager delegate = builder.build(); VirtualBoxPackager wrapper = new VirtualBoxPackager(delegate)) {
			VirtualBoxPackagerResultBuilder operation = new VirtualBoxPackagerResultBuilder(delegate, wrapper.scheduler) {
				@Override
				protected VirtualBoxPlan prepare(PackagerInterruptSupplier stop) {
					throw new AssertionError("Aggregation opt-out must bypass preprocessing");
				}
			};
			BoxItem original = item(1, 1, 1, 4);
			PackagerResult result = operation
					.withAggregation(false)
					.withBoxItems(original)
					.withContainerItems(new ContainerItem(container(4, 1, 1), 1))
					.withMaxDelegateBoxes(1)
					.build();
			// Direct delegation retains the delegate's usual inventory-cloning
			// semantics; there is deliberately no wrapper expansion/remapping.
			assertThat(result.isSuccess()).isTrue();
			assertThat(result.get(0).getStack().getPlacements()).hasSize(4)
					.extracting(Placement::getAbsoluteX).containsExactlyInAnyOrder(0, 1, 2, 3);
			assertThat(result.get(0).getLoadWeight()).isEqualTo(4);
			assertThat(original.getCount()).isEqualTo(4);
			assertThat(builder.calculators).hasSize(1);
			assertThat(builder.calculators.get(0).singleInsertions).isEqualTo(4);
			assertThat(builder.calculators.get(0).getPlacements()).hasSize(4);
			for(Placement physical : builder.calculators.get(0).getPlacements()) {
				assertThat(physical.getStackValue().getVolume()).isEqualTo(1);
				assertThat(physical.getWeight()).isEqualTo(1);
			}
		}
	}

	protected static class EnvelopeOnlyCalculator extends DefaultPointCalculator3D {
		protected int singleInsertions;

		protected EnvelopeOnlyCalculator(BoxItemSource source) {
			super(false, source);
		}

		@Override
		public boolean add(int index, Placement placement) {
			singleInsertions++;
			return super.add(index, placement);
		}

		@Override
		protected boolean addBatch(int index, List<Placement> batch, long area, long volume, boolean applyRemainingLimits) {
			throw new AssertionError("Envelope packing must use single-placement insertion");
		}
	}

	protected static class EnvelopeOnlyBuilder extends PlainPackager.Builder {
		protected List<EnvelopeOnlyCalculator> calculators = new ArrayList<>();

		@Override
		public PlainPackager build() {
			// Use the real builder defaults without duplicating placement policy.
			try(PlainPackager defaults = super.build()) {
				return new PlainPackager(packagerResultComparator, boxItemGroupComparator, placementControlsBuilderFactory) {
					@Override
					protected PointCalculator createPointCalculator(BoxItemSource source) {
						EnvelopeOnlyCalculator calculator = new EnvelopeOnlyCalculator(source);
						calculators.add(calculator);
						return calculator;
					}
				};
			}
		}
	}
}
