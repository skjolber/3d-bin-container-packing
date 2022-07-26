package com.github.skjolber.packing.points2d.ui;

import com.github.skjolber.packing.api.ep.Point2D;
import com.github.skjolber.packing.ep.points2d.DefaultPlacement2D;
import com.github.skjolber.packing.points.DefaultExtremePoints2D;

public class DrawPoints2DTest {

	private static DefaultPlacement2D createPlacement(Point2D extremePoint, int dx, int dy) {
		return new DefaultPlacement2D(extremePoint.getMinX(), extremePoint.getMinY(), extremePoint.getMinX() + dx - 1, extremePoint.getMinY() + dy - 1);
	}

	public static void main(String[] args) {
		DefaultExtremePoints2D extremePoints = new DefaultExtremePoints2D(1000, 1000);

		Point2D extremePoint = extremePoints.getValues().get(0);
		extremePoints.add(0, createPlacement(extremePoint, 50, 100));

		extremePoint = extremePoints.getValues().get(extremePoints.getValues().size() - 1);
		extremePoints.add(extremePoints.getValues().size() - 1, createPlacement(extremePoint, 50, 50));
		
		extremePoint = extremePoints.getValues().get(extremePoints.getValues().size() - 2);
		extremePoints.add(extremePoints.getValues().size() - 2, createPlacement(extremePoint, 100, 100));
		
		extremePoint = extremePoints.getValues().get(extremePoints.getValues().size() - 1);
		extremePoints.add(extremePoints.getValues().size() - 1, createPlacement(extremePoint, 20, 20));
		
		extremePoint = extremePoints.getValues().get(extremePoints.getValues().size() - 1);
		extremePoints.add(extremePoints.getValues().size() - 1, createPlacement(extremePoint, 20, 35));
		
		extremePoint = extremePoints.getValues().get(1);
		extremePoints.add(1, createPlacement(extremePoint, 20, 20));

		extremePoint = extremePoints.getValues().get(1);
		extremePoints.add(1, createPlacement(extremePoint, 30, 20));
		
		extremePoint = extremePoints.getValues().get(0);
		extremePoints.add(0, createPlacement(extremePoint, 25, 20));
/*
		int offset = 6;
		extremePoint = extremePoints.getValues().get(extremePoints.getValues().size() - offset);
		extremePoints.add(extremePoints.getValues().size() - offset, createPlacement(extremePoint, 20, 30));
		/*
		extremePoint = extremePoints.getValues().get(6);
		extremePoints.add(6, createPlacement(extremePoint, 50, 50));

		offset = 2;
		extremePoint = extremePoints.getValues().get(extremePoints.getValues().size() - offset);
		extremePoints.add(extremePoints.getValues().size() - offset, createPlacement(extremePoint, 50, 50));
		*/
		for (Point2D p : extremePoints.getValues()) {
			System.out.println(" " + p.getMinX() + "x" + p.getMinY() + " " + p.getMaxX() + "x" + p.getMaxY());
		}
		DrawPoints2D.show(extremePoints);
	}
}