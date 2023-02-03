package com.github.skjolber.packing.points;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;

public class BouwcampCodesExtremePoints2DTest {

	@Test
	public void testBouwcampCodes() throws Exception {
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		BouwkampConverter converter = new BouwkampConverter(true);

		List<BouwkampCodes> codesForCount = directory.getAll();
		for (BouwkampCodes c : codesForCount) {
			for (BouwkampCode bkpLine : c.getCodes()) {
				System.out.println(c.getSource() + " " + bkpLine.getName());
				DefaultExtremePoints2D points = converter.convert2D(bkpLine, 1);
				assertEquals(c.getSource() + " " + bkpLine.getName(), 0, points.getValueCount());
			}
		}
	}

}
