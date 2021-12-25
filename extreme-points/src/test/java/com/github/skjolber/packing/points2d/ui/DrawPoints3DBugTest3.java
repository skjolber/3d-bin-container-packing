package com.github.skjolber.packing.points2d.ui;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.Point2D;
import com.github.skjolber.packing.api.Point3D;
import com.github.skjolber.packing.points.DefaultExtremePoints2D;
import com.github.skjolber.packing.points.DefaultExtremePoints3D;
import com.github.skjolber.packing.points2d.DefaultPlacement2D;
import com.github.skjolber.packing.points3d.DefaultPlacement3D;

public class DrawPoints3DBugTest3 {

	private static Placement3D createPlacement(Point3D extremePoint, int xx, int yy, int zz) {
		if(extremePoint.getMinZ() != 0) {
			throw new IllegalArgumentException(extremePoint.getMinX() + "x" + extremePoint.getMinY() + "x" + extremePoint.getMinZ());
		}
		return new DefaultPlacement3D(extremePoint.getMinX(), extremePoint.getMinY(), extremePoint.getMinZ(), xx, yy, zz);
	}

	public static void main(String[] args) {
		DefaultExtremePoints3D extremePoints = new DefaultExtremePoints3D(1000, 1000, 1);

		add(extremePoints, 0, 0, 5, 5);
		add(extremePoints, 0, 6, 4, 10);
		add(extremePoints, 5, 6, 9, 10);
		add(extremePoints, 6, 0, 6, 0);
		add(extremePoints, 10, 0, 10, 0);
		add(extremePoints, 11, 0, 14, 3);
		add(extremePoints, 10, 4, 13, 7);
		

		add(extremePoints, 7, 0, 8, 1);

		/*
 6[0x0x0 5x5x0]
 5[0x6x0 4x10x0]
 5[5x6x0 9x10x0]
 1[6x0x0 6x0x0]
 1[10x0x0 10x0x0]
 4[11x0x0 14x3x0]
 4[10x4x0 13x7x0]
		
		
		*/

		System.out.println("Draw");
		for (Point3D p : extremePoints.getValues()) {
			System.out.println(" " + p.getMinX() + "x" + p.getMinY() + "x" + p.getMinZ() + " " + p.getMaxX() + "x" + p.getMaxY() + "x" + p.getMaxZ());
		}	
		DrawPoints2D.show(extremePoints);
	}
	
	public static void add(DefaultExtremePoints3D extremePoints, int x, int y, int xx, int yy) {
		
		int index = extremePoints.findPoint(x, y, 0);
		Point3D point = extremePoints.getValue(index);
		extremePoints.add(index, createPlacement(point, xx, yy, 0));
	}
}