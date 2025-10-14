package com.github.skjolber.packing.jmh.ep;

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points2d.DefaultPoint2D;
import com.github.skjolber.packing.ep.points2d.DefaultPointCalculator2D;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeConverter;

@State(Scope.Benchmark)
public class Points2DState {

	private List<Points2DEntries> entries = new ArrayList<>();

	@Setup(Level.Trial)
	public void init() {
		// these does not really result in successful stacking, but still should run as expected
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		List<BouwkampCodes> codesForCount = directory.getAll();
		for (BouwkampCodes c : codesForCount) {
			for (BouwkampCode bkpLine : c.getCodes()) {
				add(bkpLine);
			}
		}
	}

	private void add(BouwkampCode bkpLine) {
		BouwkampCodeConverter converter = new BouwkampCodeConverter(true);

		Points2DEntries extremePointsEntries = new Points2DEntries();

		DefaultPointCalculator2D points = new DefaultPointCalculator2D() {
			@Override
			public boolean add(int index, Placement placement) {
				extremePointsEntries.add(new Point2DEntry(index, placement));

				return super.add(index, placement);
			}
		};
		converter.convert2D(bkpLine, 1, points);

		DefaultPointCalculator2D defaultPointCalculator2D = new DefaultPointCalculator2D();
		defaultPointCalculator2D.clearToSize(points.getWidth(), points.getDepth(), 1);
		extremePointsEntries.setExtremePoints2D(defaultPointCalculator2D);

		entries.add(extremePointsEntries);
	}

	public List<Points2DEntries> getEntries() {
		return entries;
	}

	@TearDown(Level.Trial)
	public void shutdown() throws InterruptedException {
		// NOOP
	}
}
