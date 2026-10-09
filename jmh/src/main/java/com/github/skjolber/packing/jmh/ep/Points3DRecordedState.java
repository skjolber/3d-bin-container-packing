package com.github.skjolber.packing.jmh.ep;

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.BoxItemGroupComparator;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.jmh.TychoProducts;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * Point calculator operations recorded from a real plain packing (Tycho product sets), for replay
 * without packager overhead. Unlike the Bouwkamp replay (about 3 points per add), the 93-box set
 * keeps about 150 free points, exercising the large-n behavior of the calculator.
 */
@State(Scope.Benchmark)
public class Points3DRecordedState {

	static final int CLEAR = 0;
	static final int LIMIT = 1;
	static final int ADD = 2;

	/** A recorded calculator call. */
	static class Operation {
		final int type;
		final long a;
		final long b;
		final long c;
		final Placement placement;

		Operation(int type, long a, long b, long c, Placement placement) {
			this.type = type;
			this.a = a;
			this.b = b;
			this.c = c;
			this.placement = placement;
		}
	}

	@Param(value = { "93" })
	private String boxes = "93";

	private final List<Operation> operations = new ArrayList<>();
	private DefaultPointCalculator3D calculator;

	@Setup(Level.Trial)
	public void init() {
		List<ContainerItem> containers = TychoProducts.getContainers();
		List<BoxItem> products = TychoProducts.getProducts(boxes);

		RecordingBuilder builder = new RecordingBuilder();
		try (PlainPackager packager = builder.build()) {
			packager.newResultBuilder().withContainerItems(containers).withMaxContainerCount(1).withBoxItems(products).build();
		}
		int adds = 0;
		for(Operation operation : operations) {
			if(operation.type == ADD) {
				adds++;
			}
		}
		calculator = new DefaultPointCalculator3D(false, adds + 1);
	}

	public List<Operation> getOperations() {
		return operations;
	}

	public DefaultPointCalculator3D getCalculator() {
		return calculator;
	}

	private class RecordingCalculator extends DefaultPointCalculator3D {

		RecordingCalculator(BoxItemSource source) {
			super(false, source);
		}

		@Override
		public void clearToSize(int dx, int dy, int dz) {
			operations.add(new Operation(CLEAR, dx, dy, dz, null));
			super.clearToSize(dx, dy, dz);
		}

		@Override
		public void setMinimumAreaAndVolumeLimit(long area, long volume) {
			operations.add(new Operation(LIMIT, area, volume, 0, null));
			super.setMinimumAreaAndVolumeLimit(area, volume);
		}

		@Override
		public boolean add(int index, Placement placement) {
			// copy: the packager may reuse placement objects
			Placement copy = new Placement(placement.getStackValue(), -1, placement.getAbsoluteX(), placement.getAbsoluteY(), placement.getAbsoluteZ(), false);
			operations.add(new Operation(ADD, index, 0, 0, copy));
			return super.add(index, placement);
		}
	}

	private class RecordingBuilder extends PlainPackager.Builder {
		@Override
		protected PlainPackager createPackager(IntermediatePackagerResultComparator packagerResultComparator, BoxItemGroupComparator boxItemGroupComparator,
				PlacementControlsBuilderFactory placementControlsBuilderFactory) {
			return new PlainPackager(packagerResultComparator, boxItemGroupComparator, placementControlsBuilderFactory) {
				@Override
				protected PointCalculator createPointCalculator(BoxItemSource source) {
					return new RecordingCalculator(source);
				}
			};
		}
	}
}
