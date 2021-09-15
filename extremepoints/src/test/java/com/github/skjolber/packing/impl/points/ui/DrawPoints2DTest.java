package com.github.skjolber.packing.impl.points.ui;

import com.github.skjolber.packing.points.DefaultExtremePoints2D;
import com.github.skjolber.packing.points.DefaultPlacement2D;
import com.github.skjolber.packing.points.Placement2D;
import com.github.skjolber.packing.points.Point2D;

public class DrawPoints2DTest {

	private static Placement2D createPlacement(Point2D extremePoint, int dx, int dy) {
		return new DefaultPlacement2D(extremePoint.getMinX(), extremePoint.getMinY(), extremePoint.getMinX() + dx, extremePoint.getMinY() + dy);
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

		extremePoint = extremePoints.getValues().get(extremePoints.getValues().size() - 4);
		extremePoints.add(extremePoints.getValues().size() - 4, createPlacement(extremePoint, 20, 30));

		extremePoint = extremePoints.getValues().get(1);
		extremePoints.add(1, createPlacement(extremePoint, 20, 20));

		extremePoint = extremePoints.getValues().get(1);
		extremePoints.add(1, createPlacement(extremePoint, 30, 10));

		extremePoint = extremePoints.getValues().get(0);
		extremePoints.add(0, createPlacement(extremePoint, 30, 20));

		DrawPoints2D.show(extremePoints);
	}
}