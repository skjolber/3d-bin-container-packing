package com.github.skjolber.packing.points2d.ui;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.points.DefaultExtremePoints2D;
import com.github.skjolber.packing.points2d.DefaultPlacement2D;
import com.github.skjolber.packing.points2d.Point2D;

public class DrawPoints2DBugTest {

	private static Placement2D createPlacement(Point2D extremePoint, int dx, int dy) {
		return new DefaultPlacement2D(extremePoint.getMinX(), extremePoint.getMinY(), extremePoint.getMinX() + dx - 1, extremePoint.getMinY() + dy - 1);
	}

	public static void main(String[] args) {
		DefaultExtremePoints2D extremePoints = new DefaultExtremePoints2D(106, 99);

		add(extremePoints, 0, 0, 23, 23);
		add(extremePoints, 23, 0, 24, 24);
		add(extremePoints, 0, 23, 7, 7);
		add(extremePoints, 0, 30, 41, 41);
		add(extremePoints, 0, 71, 20, 20);
		add(extremePoints, 47, 0, 18, 18);
		add(extremePoints, 65, 0, 17, 17);
		add(extremePoints, 82, 0, 5, 5);
		add(extremePoints, 20, 71, 28, 28);
		add(extremePoints, 48, 18, 58, 58);
		add(extremePoints, 82, 5, 13, 13);
//		add(extremePoints, 47, 18, 48, 48);
		
		
		/*
		
Got 479001600
0x0x0 23x23x1
23x0x0 24x24x1
0x23x0 7x7x1

0x30x0 41x41x1

0x71x0 20x20x1
47x0x0 18x18x1
65x0x0 17x17x1
82x0x0 5x5x1
20x71x0 28x28x1
48x18x0 58x58x1
82x5x0 13x13x1
47x18x0 48x48x1
Packaged 106x99A order 12 in 25290
Got 6227020800
		
		
		*/

		for (Point2D p : extremePoints.getValues()) {
			System.out.println(" " + p.getMinX() + "x" + p.getMinY() + " " + p.getMaxX() + "x" + p.getMaxY());
		}
		DrawPoints2D.show(extremePoints);
	}
	
	public static void add(DefaultExtremePoints2D extremePoints, int x, int y, int dx, int dy) {
		int index = extremePoints.findPoint(x, y);
		Point2D point = extremePoints.getValue(index);
		extremePoints.add(index, createPlacement(point, dx, dy));
	}
}