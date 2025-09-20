package com.github.skjolber.packing.jmh.ep;

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.ep.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeLine;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;

@State(Scope.Benchmark)
public class ExtremePoints3DState {

	private List<ExtremePoints3DEntries> entries = new ArrayList<>();

	private Placement createStackPlacement(int x, int y, int z, int endX, int endY, int endZ) {
		BoxStackValue stackValue = new BoxStackValue(endX + 1 - x, endY + 1 - y, endZ + 1 - z, null, -1);
		
		Box box = Box.newBuilder().withSize(endX + 1 - x, endY + 1 - y, endZ + 1 - z).withWeight(0).build();
		stackValue.setBox(box);
		return new Placement(stackValue, new DefaultPoint3D(x, y, z, 0, 0, 0));
	}
	
	@Setup(Level.Trial)
	public void init() {
		// these does not really result in successful stacking, but still should run as expected
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		List<BouwkampCodes> codesForCount = directory.getAll();
		for (BouwkampCodes c : codesForCount) {
			for (BouwkampCode bkpLine : c.getCodes()) {
				//add(bkpLine);
				convert3DXYPlane(bkpLine);
			}
		}
	}

	public void convert3DXYPlane(BouwkampCode bkpLine) {
		ExtremePoints3D points = new ExtremePoints3D();
		points.clearToSize(bkpLine.getWidth(), bkpLine.getDepth(), 1);

		ExtremePoints3DEntries extremePointsEntries = new ExtremePoints3DEntries(points);

		List<BouwkampCodeLine> lines = bkpLine.getLines();

		int count = 0;

		lines: for (BouwkampCodeLine line : lines) {
			List<Integer> squares = line.getSquares();
			int minY = points.getMinY();

			Point value = points.get(minY);

			int offset = value.getMinX();

			int nextY = minY;

			for (int i = 0; i < squares.size(); i++) {
				Integer square = squares.get(i);
				int factoredSquare = square;

				Placement stackPlacement = createStackPlacement(offset, value.getMinY(), 0, offset + factoredSquare - 1, value.getMinY() + factoredSquare - 1, 1);
				
				extremePointsEntries.add(new ExtremePoint3DEntry(nextY, stackPlacement));
				
				points.add(nextY, stackPlacement);

				offset += factoredSquare;

				nextY = points.get(offset, value.getMinY(), 0);

				count++;

				if(nextY == -1 && i + 1 < squares.size()) {
					throw new IllegalStateException("No next y at " + offset + "x" + value.getMinY() + "x0 with " + (squares.size() - 1 - i) + " remaining for " + bkpLine + " and " + points.size() + " points");
				}

			}
		}

		if(points.size() > 0) {
			throw new IllegalStateException("Still have " + points.size() + ": " + points.getAll());
		}

		points.clear();

		entries.add(extremePointsEntries);
	}

	public List<ExtremePoints3DEntries> getEntries() {
		return entries;
	}

	@TearDown(Level.Trial)
	public void shutdown() throws InterruptedException {
		// NOOP
	}
}
