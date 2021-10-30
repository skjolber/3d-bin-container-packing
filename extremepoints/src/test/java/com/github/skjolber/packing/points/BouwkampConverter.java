package com.github.skjolber.packing.points;

import java.util.List;

import com.github.skjolber.packing.points2d.Point2D;
import com.github.skjolber.packing.points3d.Point3D;
import com.github.skjolber.packing.test.BouwkampCode;
import com.github.skjolber.packing.test.BouwkampCodeLine;

public class BouwkampConverter {

	public DefaultExtremePoints2D convert2D(BouwkampCode bkpLine, int factor) {
		
		DefaultExtremePoints2D points = new DefaultExtremePoints2D(bkpLine.getWidth() * factor, bkpLine.getDepth() * factor); 
		
		System.out.println("Size is " + points.getWidth() + "x" + points.getDepth());
		
		List<BouwkampCodeLine> lines = bkpLine.getLines();

		for(BouwkampCodeLine line : lines) {
			List<Integer> squares = line.getSquares();
			int minY = points.getMinY();
			
			Point2D value = points.getValue(minY);
			
			int offset = value.getMinX();
			
			int nextY = minY;
			
			for(int square : squares) {
				int factoredSquare = factor * square;
				
				points.add(nextY, new DefaultPlacement2D(offset, value.getMinY(), offset + factoredSquare - 1, value.getMinY() + factoredSquare - 1));
	
				offset += factoredSquare;
	
				nextY = points.get(offset, value.getMinY());
			}
		}
		
		if(!points.getValues().isEmpty()) {
			throw new IllegalStateException();			
		}

		return points;
	}

	public DefaultExtremePoints3D convert3D(BouwkampCode bkpLine, int factor) {
		
		DefaultExtremePoints3D points = new DefaultExtremePoints3D(bkpLine.getWidth() * factor, bkpLine.getDepth() * factor, factor); 
		
		List<BouwkampCodeLine> lines = bkpLine.getLines();

		for(BouwkampCodeLine line : lines) {
			List<Integer> squares = line.getSquares();
			int minY = points.getMinY();
			
			Point3D value = points.getValue(minY);
			
			int offset = value.getMinX();
			
			int nextY = minY;
			
			for(int square : squares) {
				int factoredSquare = factor * square;
				
				points.add(nextY, new DefaultPlacement3D(offset, value.getMinY(), 0, offset + factoredSquare - 1, value.getMinY() + factoredSquare - 1, factor - 1));
	
				offset += factoredSquare;
	
				nextY = points.get(offset, value.getMinY(), 0);
				
				if(nextY == -1) {
					break;
				}
			}
		}
		
		if(!points.getValues().isEmpty()) {
		//	throw new IllegalStateException();
			System.out.println("UAPAD");
		}

		return points;
	}
}