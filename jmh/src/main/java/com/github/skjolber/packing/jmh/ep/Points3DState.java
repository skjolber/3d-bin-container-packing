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
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points2d.DefaultPointCalculator2D;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeLine;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeConverter;

@State(Scope.Benchmark)
public class Points3DState {

	private List<Points3DEntries> entries = new ArrayList<>();

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
		BouwkampCodeConverter converter = new BouwkampCodeConverter(true);

		Points3DEntries extremePointsEntries = new Points3DEntries();

		DefaultPointCalculator3D points = new DefaultPointCalculator3D() {
			@Override
			public boolean add(int index, Placement placement) {
				extremePointsEntries.add(new Point3DEntry(index, placement));

				return super.add(index, placement);
			}
		};

		converter.convert2D(bkpLine, 1, points);

		DefaultPointCalculator3D defaultPointCalculator3D = new DefaultPointCalculator3D();
		defaultPointCalculator3D.clearToSize(points.getWidth(), points.getDepth(), 1);
		extremePointsEntries.setExtremePoints3D(defaultPointCalculator3D);
		
		entries.add(extremePointsEntries);
	}

	public List<Points3DEntries> getEntries() {
		return entries;
	}

	@TearDown(Level.Trial)
	public void shutdown() throws InterruptedException {
		// NOOP
	}
}
