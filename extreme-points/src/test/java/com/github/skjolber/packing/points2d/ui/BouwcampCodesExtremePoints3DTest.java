package com.github.skjolber.packing.points2d.ui;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.points.BouwkampConverter;
import com.github.skjolber.packing.points.DefaultExtremePoints3D;
import com.github.skjolber.packing.test.BouwkampCode;
import com.github.skjolber.packing.test.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.BouwkampCodes;

public class BouwcampCodesExtremePoints3DTest {

	@Test
	public void testBouwcampCodes() throws Exception {
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		BouwkampConverter converter = new BouwkampConverter(true);
		
		List<BouwkampCodes> codesForCount = directory.getAll();
		for(BouwkampCodes c : codesForCount) {
			for(BouwkampCode bkpLine : c.getCodes()) {
				DefaultExtremePoints3D xyPoints = converter.convert3DXYPlane(bkpLine, 1);
				assertEquals(c.getSource() + " " + bkpLine.getName(), 0, xyPoints.getValues().size());
				
				DefaultExtremePoints3D xzPoints = converter.convert3DXZPlane(bkpLine, 1);
				assertEquals(c.getSource() + " " + bkpLine.getName(), 0, xzPoints.getValues().size());
				
				DefaultExtremePoints3D yzPoints = converter.convert3DYZPlane(bkpLine, 1);
				assertEquals(c.getSource() + " " + bkpLine.getName(), 0, yzPoints.getValues().size());
			}
		}
	}

}
