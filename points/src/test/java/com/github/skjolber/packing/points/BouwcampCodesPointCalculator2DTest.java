package com.github.skjolber.packing.points;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeConverter;

public class BouwcampCodesPointCalculator2DTest {

	@Test
	public void testBouwcampCodes() throws Exception {
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		BouwkampCodeConverter converter = new BouwkampCodeConverter(true);

		List<BouwkampCodes> codesForCount = directory.getAll();
		for (BouwkampCodes c : codesForCount) {
			for (BouwkampCode bkpLine : c.getCodes()) {
				System.out.println(c.getSource() + " " + bkpLine.getName());
				ValidatingPointCalculator2D calculator = new ValidatingPointCalculator2D(false, bkpLine.getOrder());
				converter.convert2D(bkpLine, 1, calculator);
				assertEquals(c.getSource() + " " + bkpLine.getName(), 0, calculator.size());
			}
		}
	}

}
