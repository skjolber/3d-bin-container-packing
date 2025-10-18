package com.github.skjolber.packing.points3d;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.points.ValidatingPointCalculator3D;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeConverter;

public class BouwcampCodesPointCalculator3DTest {

	@Test
	public void testBouwcampCodes() throws Exception {
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		BouwkampCodeConverter converter = new BouwkampCodeConverter(true);

		List<BouwkampCodes> codesForCount = directory.getAll();
		for (BouwkampCodes c : codesForCount) {
			for (BouwkampCode bkpLine : c.getCodes()) {
				for (int i = 1; i < 3; i++) {
					ValidatingPointCalculator3D xyPoints = new ValidatingPointCalculator3D(false, bkpLine.getOrder());
					converter.convert3DXYPlane(bkpLine, i, xyPoints);
					assertEquals(c.getSource() + " " + bkpLine.getName(), 0, xyPoints.size());

					ValidatingPointCalculator3D xzPoints = new ValidatingPointCalculator3D(false, bkpLine.getOrder());
					converter.convert3DXZPlane(bkpLine, i, xzPoints);
					assertEquals(c.getSource() + " " + bkpLine.getName(), 0, xzPoints.size());

					ValidatingPointCalculator3D yzPoints = new ValidatingPointCalculator3D(false, bkpLine.getOrder());
					converter.convert3DYZPlane(bkpLine, i, yzPoints );
					assertEquals(c.getSource() + " " + bkpLine.getName(), 0, yzPoints.size());
				}
			}
		}
	}

}
