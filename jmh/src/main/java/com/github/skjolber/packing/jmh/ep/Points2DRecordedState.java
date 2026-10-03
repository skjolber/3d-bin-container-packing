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
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.ep.points2d.DefaultPointCalculator2D;
import com.github.skjolber.packing.jmh.TychoProducts;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;

/**
 * 2D point calculator operations recorded from a real fast largest-area-fit-first packing
 * (Tycho product sets), for replay without packager overhead. The packager resets the points
 * for every level, so resets are recorded too.
 */
@State(Scope.Benchmark)
public class Points2DRecordedState {

	static final int CLEAR_TO_SIZE = 0;
	static final int LIMIT = 1;
	static final int ADD = 2;
	static final int SET_POINTS = 3;
	static final int CLEAR = 4;

	/** A recorded calculator call. */
	static class Operation {
		final int type;
		final long a;
		final long b;
		final long c;
		final Placement placement;
		final List<Point> points;

		Operation(int type, long a, long b, long c, Placement placement, List<Point> points) {
			this.type = type;
			this.a = a;
			this.b = b;
			this.c = c;
			this.placement = placement;
			this.points = points;
		}
	}

	@Param(value = { "93" })
	private String boxes = "93";

	private final List<Operation> operations = new ArrayList<>();
	private DefaultPointCalculator2D calculator;

	@Setup(Level.Trial)
	public void init() {
		List<ContainerItem> containers = TychoProducts.getContainers();
		List<BoxItem> products = TychoProducts.getProducts(boxes);

		try (FastLargestAreaFitFirstPackager packager = new RecordingBuilder().build()) {
			packager.newResultBuilder().withContainerItems(containers).withMaxContainerCount(1).withBoxItems(products).build();
		}
		int adds = 0;
		for(Operation operation : operations) {
			if(operation.type == ADD) {
				adds++;
			}
		}
		calculator = new DefaultPointCalculator2D(false, adds + 1);
	}

	public List<Operation> getOperations() {
		return operations;
	}

	public DefaultPointCalculator2D getCalculator() {
		return calculator;
	}

	private class RecordingCalculator extends DefaultPointCalculator2D {

		RecordingCalculator(BoxItemSource source) {
			super(false, source);
		}

		@Override
		public void clearToSize(int dx, int dy, int dz) {
			operations.add(new Operation(CLEAR_TO_SIZE, dx, dy, dz, null, null));
			super.clearToSize(dx, dy, dz);
		}

		@Override
		public void setPoints(List<Point> points) {
			operations.add(new Operation(SET_POINTS, 0, 0, 0, null, new ArrayList<>(points)));
			super.setPoints(points);
		}

		@Override
		public void clear() {
			operations.add(new Operation(CLEAR, 0, 0, 0, null, null));
			super.clear();
		}

		@Override
		public void setMinimumAreaAndVolumeLimit(long area, long volume) {
			operations.add(new Operation(LIMIT, area, volume, 0, null, null));
			super.setMinimumAreaAndVolumeLimit(area, volume);
		}

		@Override
		public boolean add(int index, Placement placement) {
			// copy: the packager may reuse placement objects
			Placement copy = new Placement(placement.getStackValue(), -1, placement.getAbsoluteX(), placement.getAbsoluteY(), placement.getAbsoluteZ(), false);
			operations.add(new Operation(ADD, index, 0, 0, copy, null));
			return super.add(index, placement);
		}
	}

	private class RecordingBuilder extends FastLargestAreaFitFirstPackager.Builder {
		@Override
		public FastLargestAreaFitFirstPackager build() {
			try (FastLargestAreaFitFirstPackager defaults = super.build()) {
				return new FastLargestAreaFitFirstPackager(intermediatePackagerResultComparator, boxItemGroupComparator, firstBoxItemGroupComparator, placementControlsBuilderFactory,
						firstPlacementControlsBuilderFactory) {
					@Override
					protected PointCalculator createPointCalculator(BoxItemSource source) {
						return new RecordingCalculator(source);
					}
				};
			}
		}
	}
}
