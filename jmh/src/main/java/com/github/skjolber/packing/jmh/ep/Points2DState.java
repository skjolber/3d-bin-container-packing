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
import com.github.skjolber.packing.ep.points2d.Point2D;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeLine;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;

@State(Scope.Benchmark)
public class Points2DState {

	private List<Points2DEntries> entries = new ArrayList<>();

	private Placement createStackPlacement(int x, int y, int dx, int dy) {
		BoxStackValue stackValue = new BoxStackValue(dx, dy, 0, null, -1);
		
		return new Placement(stackValue, new DefaultPoint2D(x, y, 0, 0, 0, 0));
	}
	
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
		DefaultPointCalculator2D points = new DefaultPointCalculator2D();
		points.clearToSize(bkpLine.getWidth(), bkpLine.getDepth(), 1);

		Points2DEntries extremePointsEntries = new Points2DEntries(points);

		// run through the stacking, recording point index + placements

		List<BouwkampCodeLine> lines = bkpLine.getLines();

		int count = 0;

		lines: for (BouwkampCodeLine line : lines) {
			List<Integer> squares = line.getSquares();
			int minY = points.getMinY();

			Point2D value = points.get(minY);

			int offset = value.getMinX();

			int nextY = minY;

			for (int i = 0; i < squares.size(); i++) {
				Integer square = squares.get(i);

				int factoredSquare = square;

				Placement placement = createStackPlacement(offset, value.getMinY(), offset + factoredSquare - 1, value.getMinY() + factoredSquare - 1);
				extremePointsEntries.add(new Point2DEntry(nextY, placement));
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

		if(points.size() > 0) {
			throw new IllegalStateException("Still have " + points.size() + ": " + points.getAll());
		}

		points.redo();

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
