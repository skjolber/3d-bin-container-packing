package com.github.skjolber.packing.points;

import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points2d.DefaultPoint2D;
import com.github.skjolber.packing.ep.points2d.Point2D;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeLine;

@SuppressWarnings({ "rawtypes" })
public class BouwkampConverter {

	private boolean throwException;

	public BouwkampConverter(boolean throwException) {
		this.throwException = throwException;
	}
	
	private Placement createStackPlacement(int x, int y, int endX, int endY) {
		return createStackPlacement(x, y, 0, endX, endY, 0);
	}
	
	private Placement createStackPlacement(int x, int y, int z, int endX, int endY, int endZ) {
		BoxStackValue stackValue = new BoxStackValue(endX + 1 - x, endY + 1 - y, endZ + 1 - z, null, -1);
		
		Box box = Box.newBuilder().withSize(endX + 1 - x, endY + 1 - y, endZ + 1 - z).withWeight(0).build();
		stackValue.setBox(box);
		
		return new Placement(stackValue, new DefaultPoint2D(x, y, z, 0, 0, 0));
	}

	public ValidatingPointCalculator2D convert2D(BouwkampCode bkpLine, int factor) {

		ValidatingPointCalculator2D points = new ValidatingPointCalculator2D();
		points.setSize(bkpLine.getWidth() * factor, bkpLine.getDepth() * factor, factor);
		points.clear();
		
		List<BouwkampCodeLine> lines = bkpLine.getLines();
		
		if(points.isEmpty()) {
			throw new RuntimeException();
		}

		int count = 0;

		lines: for (BouwkampCodeLine line : lines) {
			List<Integer> squares = line.getSquares();
			int minY = points.getMinY();

			Point2D value = points.get(minY);
			
			if(value == null) throw new RuntimeException("No point at " + minY + ", got " + points.getAll());
			
			int offset = value.getMinX();

			int nextY = minY;

			for (int i = 0; i < squares.size(); i++) {
				Integer square = squares.get(i);

				int factoredSquare = factor * square;

				points.add(nextY, createStackPlacement(offset, value.getMinY(), offset + factoredSquare - 1, value.getMinY() + factoredSquare - 1));

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

		if(points.size() > 0) {
			if(throwException) {
				throw new IllegalStateException("Still have " + points.size() + ": " + points.getAll());
			} else {
				System.out.println("Still have " + points.size() + ": " + points.getAll());
			}
		}

		return points;
	}

	public ValidatingPointCalculator3D convert3DXYPlane(BouwkampCode bkpLine, int factor) {

		ValidatingPointCalculator3D points = new ValidatingPointCalculator3D();
		points.clearToSize(bkpLine.getWidth() * factor, bkpLine.getDepth() * factor, factor);

		List<BouwkampCodeLine> lines = bkpLine.getLines();

		int count = 0;

		lines: for (BouwkampCodeLine line : lines) {
			List<Integer> squares = line.getSquares();
			int minY = points.getMinY();

			Point value = points.get(minY);

			int offset = value.getMinX();

			int nextY = minY;

			for (int i = 0; i < squares.size(); i++) {
				Integer square = squares.get(i);
				int factoredSquare = factor * square;

				points.add(nextY, createStackPlacement(offset, value.getMinY(), 0, offset + factoredSquare - 1, value.getMinY() + factoredSquare - 1, factor - 1));

				offset += factoredSquare;

				nextY = points.get(offset, value.getMinY(), 0);

				count++;

				if(nextY == -1 && i + 1 < squares.size()) {

					if(throwException) {
						throw new IllegalStateException("No next y at " + offset + "x" + value.getMinY() + "x0 with " + (squares.size() - 1 - i) + " remaining for " + bkpLine);
					} else {
						System.out.println("No next y at " + offset + "x" + value.getMinY() + "x0 with " + (squares.size() - 1 - i) + " remaining for " + bkpLine);
					}

					break lines;
				}

			}
		}

		if(points.size() > 0) {
			if(throwException) {
				throw new IllegalStateException("Still have " + points.size() + ": " + points.getAll());
			} else {
				System.out.println("Still have " + points.size() + ": " + points.getAll());
			}
		}

		return points;
	}

	public ValidatingPointCalculator3D convert3DXZPlane(BouwkampCode bkpLine, int factor) {

		ValidatingPointCalculator3D points = new ValidatingPointCalculator3D();
		points.clearToSize(bkpLine.getWidth() * factor, factor, bkpLine.getDepth() * factor);

		List<BouwkampCodeLine> lines = bkpLine.getLines();

		int count = 0;

		lines: for (BouwkampCodeLine line : lines) {
			List<Integer> squares = line.getSquares();
			int minZ = points.getMinZ();

			Point value = points.get(minZ);

			int offset = value.getMinX();

			int nextZ = minZ;

			for (int i = 0; i < squares.size(); i++) {
				Integer square = squares.get(i);
				int factoredSquare = factor * square;

				points.add(nextZ, createStackPlacement(offset, 0, value.getMinZ(), offset + factoredSquare - 1, factor - 1, value.getMinZ() + factoredSquare - 1));

				offset += factoredSquare;

				nextZ = points.get(offset, 0, value.getMinZ());

				count++;

				if(nextZ == -1 && i + 1 < squares.size()) {

					if(throwException) {
						throw new IllegalStateException("No next z at " + offset + "x" + 0 + "x" + value.getMinZ() + " with " + (squares.size() - 1 - i) + " remaining");
					} else {
						System.out.println("No next z at " + 0 + "x" + offset + "x" + value.getMinZ() + " with " + (squares.size() - 1 - i) + " remaining");
					}

					break lines;
				}
			}
		}

		if(points.size() > 0) {
			if(throwException) {
				throw new IllegalStateException("Still have " + points.size() + ": " + points.getAll());
			} else {
				System.out.println("Still have " + points.size() + ": " + points.getAll());
			}
		}

		return points;
	}

	public ValidatingPointCalculator3D convert3DYZPlane(BouwkampCode bkpLine, int factor) {

		ValidatingPointCalculator3D points = new ValidatingPointCalculator3D();
		points.clearToSize(factor, bkpLine.getWidth() * factor, bkpLine.getDepth() * factor);

		List<BouwkampCodeLine> lines = bkpLine.getLines();

		int count = 0;

		lines: for (BouwkampCodeLine line : lines) {
			List<Integer> squares = line.getSquares();
			int minZ = points.getMinZ();

			Point value = points.get(minZ);

			int offset = value.getMinY();

			int nextZ = minZ;

			for (int i = 0; i < squares.size(); i++) {
				Integer square = squares.get(i);
				int factoredSquare = factor * square;

				points.add(nextZ, createStackPlacement(0, offset, value.getMinZ(), factor - 1, offset + factoredSquare - 1, value.getMinZ() + factoredSquare - 1));

				offset += factoredSquare;

				nextZ = points.get(0, offset, value.getMinZ());

				count++;

				if(nextZ == -1 && i + 1 < squares.size()) {

					if(throwException) {
						throw new IllegalStateException("No next z at " + 0 + "x" + offset + "x" + value.getMinZ() + " with " + (squares.size() - 1 - i) + " remaining");
					} else {
						System.out.println("No next z at " + 0 + "x" + offset + "x" + value.getMinZ() + " with " + (squares.size() - 1 - i) + " remaining");
					}

					break lines;
				}
			}
		}

		if(points.size() > 0) {
			if(throwException) {
				throw new IllegalStateException("Still have " + points.size() + ": " + points.getAll());
			} else {
				System.out.println("Still have " + points.size() + ": " + points.getAll());
			}
		}

		return points;
	}

}