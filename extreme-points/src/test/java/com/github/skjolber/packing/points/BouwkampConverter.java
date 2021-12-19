package com.github.skjolber.packing.points;

import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.points2d.DefaultPlacement2D;
import com.github.skjolber.packing.points2d.Point2D;
import com.github.skjolber.packing.points3d.DefaultPlacement3D;
import com.github.skjolber.packing.points3d.Point3D;
import com.github.skjolber.packing.test.BouwkampCode;
import com.github.skjolber.packing.test.BouwkampCodeLine;

public class BouwkampConverter {

	private boolean throwException;
	
	public BouwkampConverter(boolean throwException) {
		this.throwException = throwException;
	}

	public DefaultExtremePoints2D convert2D(BouwkampCode bkpLine, int factor) {
		
		DefaultExtremePoints2D points = new DefaultExtremePoints2D(bkpLine.getWidth() * factor, bkpLine.getDepth() * factor); 
		
		List<BouwkampCodeLine> lines = bkpLine.getLines();

		int count = 0;
		
		lines:
		for(BouwkampCodeLine line : lines) {
			List<Integer> squares = line.getSquares();
			int minY = points.getMinY();
			
			Point2D value = points.getValue(minY);
			
			int offset = value.getMinX();
			
			int nextY = minY;
			
			for (int i = 0; i < squares.size(); i++) {
				Integer square = squares.get(i);
				
				int factoredSquare = factor * square;
				
				points.add(nextY, new DefaultPlacement2D(offset, value.getMinY(), offset + factoredSquare - 1, value.getMinY() + factoredSquare - 1, Collections.emptyList()));
	
				offset += factoredSquare;
	
				nextY = points.findPoint(offset, value.getMinY());
				
				count++;
				
				if(nextY == -1 && i + 1 < squares.size()) {
					
					if(throwException) {
						throw new IllegalStateException("No next y at " + offset + "x" + value.getMinY() + " with " + (squares.size() - 1 - i) + " remaining");
					}
					
					break lines;
				}
				
				if(count == -1) {
					break lines;
				}
			}
		}
		
		if(!points.getValues().isEmpty()) {
			if(throwException) {
				throw new IllegalStateException("Still have " + points.getValues().size() + ": " + points.getValues());
			}
		}
		
		return points;
	}

	public DefaultExtremePoints3D convert3DXYPlane(BouwkampCode bkpLine, int factor) {
		
		DefaultExtremePoints3D points = new DefaultExtremePoints3D(bkpLine.getWidth() * factor, bkpLine.getDepth() * factor, factor); 
		
		List<BouwkampCodeLine> lines = bkpLine.getLines();

		int count = 0;
		
		lines:
		for(BouwkampCodeLine line : lines) {
			List<Integer> squares = line.getSquares();
			int minY = points.getMinY();
			
			Point3D value = points.getValue(minY);
			
			int offset = value.getMinX();
			
			int nextY = minY;
			
			for (int i = 0; i < squares.size(); i++) {
				Integer square = squares.get(i);
				int factoredSquare = factor * square;
				
				points.add(nextY, new DefaultPlacement3D(offset, value.getMinY(), 0, offset + factoredSquare - 1, value.getMinY() + factoredSquare - 1, factor - 1, Collections.emptyList()));
	
				offset += factoredSquare;
	
				nextY = points.get(offset, value.getMinY(), 0);
				
				count++;
				
				if(nextY == -1 && i + 1 < squares.size()) {
					
					if(throwException) {
						throw new IllegalStateException("No next y at " + offset + "x" + value.getMinY() +  "x0 with " + (squares.size() - 1 - i) + " remaining");
					}
					
					break lines;
				}

			}
		}
		
		if(!points.getValues().isEmpty()) {
			if(throwException) {
				throw new IllegalStateException("Still have " + points.getValues().size() + ": " + points.getValues());
			}
		}

		return points;
	}
	
	public DefaultExtremePoints3D convert3DXZPlane(BouwkampCode bkpLine, int factor) {
		
		DefaultExtremePoints3D points = new DefaultExtremePoints3D(bkpLine.getWidth() * factor, factor, bkpLine.getDepth() * factor); 
		
		List<BouwkampCodeLine> lines = bkpLine.getLines();

		int count = 0;
		
		lines:
		for(BouwkampCodeLine line : lines) {
			List<Integer> squares = line.getSquares();
			int minZ = points.getMinZ();
			
			Point3D value = points.getValue(minZ);
			
			int offset = value.getMinX();
			
			int nextZ = minZ;
			
			for (int i = 0; i < squares.size(); i++) {
				Integer square = squares.get(i);
				int factoredSquare = factor * square;
				
				points.add(nextZ, new DefaultPlacement3D(offset, 0, value.getMinZ(), offset + factoredSquare - 1, factor - 1, value.getMinZ() + factoredSquare - 1, Collections.emptyList()));
	
				offset += factoredSquare;
	
				nextZ = points.get(offset, 0, value.getMinZ());
				
				count++;
				
				if(nextZ == -1 && i + 1 < squares.size()) {
					
					if(throwException) {
						throw new IllegalStateException("No next z at " + offset + "x" + 0 + "x" + value.getMinZ() +  " with " + (squares.size() - 1 - i) + " remaining");
					}
					
					break lines;
				}
			}
		}
		
		if(!points.getValues().isEmpty()) {
			if(throwException) {
				throw new IllegalStateException("Still have " + points.getValues().size() + ": " + points.getValues());
			}
		}

		return points;
	}
	
	public DefaultExtremePoints3D convert3DYZPlane(BouwkampCode bkpLine, int factor) {
		
		DefaultExtremePoints3D points = new DefaultExtremePoints3D(factor, bkpLine.getWidth() * factor, bkpLine.getDepth() * factor); 
		
		List<BouwkampCodeLine> lines = bkpLine.getLines();

		int count = 0;
		
		lines:
		for(BouwkampCodeLine line : lines) {
			List<Integer> squares = line.getSquares();
			int minZ = points.getMinZ();
			
			Point3D value = points.getValue(minZ);
			
			int offset = value.getMinY();
			
			int nextZ = minZ;
			
			for (int i = 0; i < squares.size(); i++) {
				Integer square = squares.get(i);
				int factoredSquare = factor * square;
				
				points.add(nextZ, new DefaultPlacement3D(0, offset, value.getMinZ(), factor - 1, offset + factoredSquare - 1, value.getMinZ() + factoredSquare - 1, Collections.emptyList()));
	
				offset += factoredSquare;
	
				nextZ = points.get(0, offset, value.getMinZ());
				
				count++;
				
				if(nextZ == -1 && i + 1 < squares.size()) {
					
					if(throwException) {
						throw new IllegalStateException("No next z at " + 0 + "x" + offset + "x" + value.getMinZ() +  " with " + (squares.size() - 1 - i) + " remaining");
					}
					
					break lines;
				}

			}
		}
		
		if(!points.getValues().isEmpty()) {
			if(throwException) {
				throw new IllegalStateException("Still have " + points.getValues().size() + ": " + points.getValues());
			}
		}

		return points;
	}
	

	

}