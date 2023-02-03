package com.github.skjolber.packing.jmh.ep;

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.ep.Point2D;
import com.github.skjolber.packing.ep.points2d.DefaultPlacement2D;
import com.github.skjolber.packing.ep.points2d.ExtremePoints2D;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeLine;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;

@State(Scope.Benchmark)
public class ExtremePoints2DState {

	private List<ExtremePoints2DEntries> entries = new ArrayList<>();

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
		ExtremePoints2D<DefaultPlacement2D> points = new ExtremePoints2D<>(bkpLine.getWidth(), bkpLine.getDepth());

		ExtremePoints2DEntries extremePointsEntries = new ExtremePoints2DEntries(points);

		// run through the stacking, recording point index + placements

		List<BouwkampCodeLine> lines = bkpLine.getLines();

		int count = 0;

		lines: for (BouwkampCodeLine line : lines) {
			List<Integer> squares = line.getSquares();
			int minY = points.getMinY();

			Point2D value = points.getValue(minY);

			int offset = value.getMinX();

			int nextY = minY;

			for (int i = 0; i < squares.size(); i++) {
				Integer square = squares.get(i);

				int factoredSquare = square;

				DefaultPlacement2D placement = new DefaultPlacement2D(offset, value.getMinY(), offset + factoredSquare - 1, value.getMinY() + factoredSquare - 1);
				extremePointsEntries.add(new ExtremePoint2DEntry(nextY, placement));
				points.add(nextY, placement);

				offset += factoredSquare;

				nextY = points.findPoint(offset, value.getMinY());

				count++;

				if(nextY == -1 && i + 1 < squares.size()) {
					throw new IllegalStateException("No next y at " + offset + "x" + value.getMinY() + " with " + (squares.size() - 1 - i) + " remaining");
				}

				if(count == -1) {
					break lines;
				}
			}
		}

		if(points.getValueCount() > 0) {
			throw new IllegalStateException("Still have " + points.getValueCount() + ": " + points.getValues());
		}

		points.redo();

		entries.add(extremePointsEntries);
	}

	public List<ExtremePoints2DEntries> getEntries() {
		return entries;
	}

	@TearDown(Level.Trial)
	public void shutdown() throws InterruptedException {
		// NOOP
	}
}
