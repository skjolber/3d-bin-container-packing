package com.github.skjolber.packing.test.bouwkamp;

import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;

@SuppressWarnings({ "rawtypes" })
public class BouwkampCodeConverter {

	private static class BouwkampPoint extends Point {

		public BouwkampPoint(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
		}

		@Override
		public Point clone(int maxX, int maxY, int maxZ) {
			return new BouwkampPoint(minX,  minY,  minZ, maxX, maxY, maxZ);
		}
	}
	
	private boolean throwException;

	public BouwkampCodeConverter(boolean throwException) {
		this.throwException = throwException;
	}
	
	private Placement createStackPlacement(int x, int y, int endX, int endY) {
		return createStackPlacement(x, y, 0, endX, endY, 0);
	}
	
	private Placement createStackPlacement(int x, int y, int z, int endX, int endY, int endZ) {
		BoxStackValue stackValue = new BoxStackValue(endX + 1 - x, endY + 1 - y, endZ + 1 - z, null, -1);
		
		Box box = Box.newBuilder().withSize(endX + 1 - x, endY + 1 - y, endZ + 1 - z).withWeight(0).build();
		stackValue.setBox(box);
		
		return new Placement(stackValue, new BouwkampPoint(x, y, z, 0, 0, 0));
	}

	public void convert2D(BouwkampCode bkpLine, int factor, PointCalculator points) {

		points.clearToSize(bkpLine.getWidth() * factor, bkpLine.getDepth() * factor, factor);
		points.clear();
		
		List<BouwkampCodeLine> lines = bkpLine.getLines();
		
		if(points.isEmpty()) {
			throw new RuntimeException();
		}

		int count = 0;

		lines: for (BouwkampCodeLine line : lines) {
			List<Integer> squares = line.getSquares();
			int minY = getMinY(points.getAll());

			Point value = points.get(minY);
			
			if(value == null) throw new RuntimeException("No point at " + minY + ", got " + points.getAll());
			
			int offset = value.getMinX();

			int nextY = minY;

			for (int i = 0; i < squares.size(); i++) {
				Integer square = squares.get(i);

				int factoredSquare = factor * square;

				points.add(nextY, createStackPlacement(offset, value.getMinY(), offset + factoredSquare - 1, value.getMinY() + factoredSquare - 1));

				offset += factoredSquare;

				nextY = findPoint(points.getAll(), offset, value.getMinY(), 0);

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
	}

	public void convert3DXYPlane(BouwkampCode bkpLine, int factor, PointCalculator points) {
		points.clearToSize(bkpLine.getWidth() * factor, bkpLine.getDepth() * factor, factor);

		List<BouwkampCodeLine> lines = bkpLine.getLines();

		int count = 0;

		lines: for (BouwkampCodeLine line : lines) {
			List<Integer> squares = line.getSquares();
			int minY = getMinY(points.getAll());

			Point value = points.get(minY);

			int offset = value.getMinX();

			int nextY = minY;

			for (int i = 0; i < squares.size(); i++) {
				Integer square = squares.get(i);
				int factoredSquare = factor * square;

				points.add(nextY, createStackPlacement(offset, value.getMinY(), 0, offset + factoredSquare - 1, value.getMinY() + factoredSquare - 1, factor - 1));

				offset += factoredSquare;

				nextY = findPoint(points.getAll(), offset, value.getMinY(), 0);

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
	}

	public void convert3DXZPlane(BouwkampCode bkpLine, int factor, PointCalculator points) {

		points.clearToSize(bkpLine.getWidth() * factor, factor, bkpLine.getDepth() * factor);

		List<BouwkampCodeLine> lines = bkpLine.getLines();

		int count = 0;

		lines: for (BouwkampCodeLine line : lines) {
			List<Integer> squares = line.getSquares();
			int minZ = getMinZ(points.getAll());

			Point value = points.get(minZ);

			int offset = value.getMinX();

			int nextZ = minZ;

			for (int i = 0; i < squares.size(); i++) {
				Integer square = squares.get(i);
				int factoredSquare = factor * square;

				points.add(nextZ, createStackPlacement(offset, 0, value.getMinZ(), offset + factoredSquare - 1, factor - 1, value.getMinZ() + factoredSquare - 1));

				offset += factoredSquare;

				nextZ =findPoint(points.getAll(), offset, 0, value.getMinZ());

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
	}

	public void convert3DYZPlane(BouwkampCode bkpLine, int factor, PointCalculator points) {

		points.clearToSize(factor, bkpLine.getWidth() * factor, bkpLine.getDepth() * factor);

		List<BouwkampCodeLine> lines = bkpLine.getLines();

		int count = 0;

		lines: for (BouwkampCodeLine line : lines) {
			List<Integer> squares = line.getSquares();
			int minZ = getMinZ(points.getAll());

			Point value = points.get(minZ);

			int offset = value.getMinY();

			int nextZ = minZ;

			for (int i = 0; i < squares.size(); i++) {
				Integer square = squares.get(i);
				int factoredSquare = factor * square;

				points.add(nextZ, createStackPlacement(0, offset, value.getMinZ(), factor - 1, offset + factoredSquare - 1, value.getMinZ() + factoredSquare - 1));

				offset += factoredSquare;

				nextZ = findPoint(points.getAll(), 0, offset, value.getMinZ());

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
	}
	

	public int getMinY(List<Point> values) {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			Point point = values.get(i);

			if(point.getMinY() < values.get(min).getMinY()) {
				min = i;
			}
		}
		return min;
	}

	public int getMinX(List<Point> values) {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			Point point = values.get(i);

			if(point.getMinX() < values.get(min).getMinX()) {
				min = i;
			}
		}
		return min;
	}

	public int getMinZ(List<Point> values) {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			Point point2d = values.get(i);

			if(point2d.getMinZ() < values.get(min).getMinZ()) {
				min = i;
			}
		}
		return min;
	}
	
	public int findPoint(List<Point> values, int x, int y, int z) {
		for (int i = 0; i < values.size(); i++) {
			Point point = values.get(i);
			if(point.getMinX() == x && point.getMinY() == y && point.getMinZ() == z) {
				return i;
			}
		}
		return -1;
	}



}