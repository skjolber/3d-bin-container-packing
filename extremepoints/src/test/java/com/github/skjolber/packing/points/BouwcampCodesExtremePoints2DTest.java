package com.github.skjolber.packing.points;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.test.BouwkampCode;
import com.github.skjolber.packing.test.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.BouwkampCodes;

public class BouwcampCodesExtremePoints2DTest {

	@Test
	public void testBouwcampCodes() {
		// these does not really result in successful stacking, but still should run as expected
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		BouwkampConverter converter = new BouwkampConverter();
		
		List<BouwkampCodes> codesForCount = directory.codesForCount(13);
		for(BouwkampCodes c : codesForCount) {

			for(BouwkampCode bkpLine : c.getCodes()) {
				DefaultExtremePoints2D points = converter.convert(bkpLine, 2);
				assertEquals(0, points.getValues().size());
			}
		}
	}

}
