package com.github.skjolber.packing.points;

import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.test.BouwkampCode;
import com.github.skjolber.packing.test.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.BouwkampCodeLine;
import com.github.skjolber.packing.test.BouwkampCodes;

public class ExtermePoints2DTest {

	@Test
	public void testBouwcampCodes() {
		// these does not really result in successful stacking, but still should run as expected
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		List<BouwkampCodes> codesForCount = directory.codesForCount(13);
		for(BouwkampCodes c : codesForCount) {

			System.out.println("Parse " + c.getSource());
			
			if(!c.getSource().contains("o13siss.bkp")) {
				continue;
			}
			
			for(BouwkampCode bkpLine : c.getCodes()) {
				
				DefaultExtremePoints2D points = new DefaultExtremePoints2D(bkpLine.getWidth(), bkpLine.getDepth()); 
				
				List<BouwkampCodeLine> lines = bkpLine.getLines();
				
				for(BouwkampCodeLine line : lines) {
					
					List<Integer> squares = line.getSquares();
				
					int minY = points.getMinY();
					
					Point2D value = points.getValue(minY);
					
					int offset = value.getMinX();
					
					int nextY = minY;
					
					for(int square : squares) {
						System.out.println("Add " + offset + "x" + value.getMinY() + " " + square + "x" + square);
						
						points.add(nextY, new DefaultPlacement2D(offset, value.getMinY(), offset + square, value.getMinY() + square));

						offset += square;

						nextY = points.get(offset, value.getMinY());
						
					}
				}
				
				System.out.println("**************************************\n\n");
				
			}
		}
	}

}
