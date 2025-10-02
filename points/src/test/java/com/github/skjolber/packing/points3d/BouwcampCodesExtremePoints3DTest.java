package com.github.skjolber.packing.points3d;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.points.BouwkampConverter;
import com.github.skjolber.packing.points.DefaultExtremePoints3D;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;

public class BouwcampCodesExtremePoints3DTest {

	@Test
	public void testBouwcampCodes() throws Exception {
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		BouwkampConverter converter = new BouwkampConverter(true);

		List<BouwkampCodes> codesForCount = directory.getAll();
		for (BouwkampCodes c : codesForCount) {
			for (BouwkampCode bkpLine : c.getCodes()) {
				for (int i = 1; i < 3; i++) {
					DefaultExtremePoints3D xyPoints = converter.convert3DXYPlane(bkpLine, i);
					assertEquals(c.getSource() + " " + bkpLine.getName(), 0, xyPoints.size());

					DefaultExtremePoints3D xzPoints = converter.convert3DXZPlane(bkpLine, i);
					assertEquals(c.getSource() + " " + bkpLine.getName(), 0, xzPoints.size());

					DefaultExtremePoints3D yzPoints = converter.convert3DYZPlane(bkpLine, i);
					assertEquals(c.getSource() + " " + bkpLine.getName(), 0, yzPoints.size());
				}
			}
		}
	}

}
