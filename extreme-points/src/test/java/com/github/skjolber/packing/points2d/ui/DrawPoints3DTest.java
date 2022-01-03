package com.github.skjolber.packing.points2d.ui;

import java.util.List;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.Point3D;
import com.github.skjolber.packing.points.DefaultExtremePoints3D;
import com.github.skjolber.packing.points3d.DefaultPlacement3D;

public class DrawPoints3DTest {

	private static Placement3D createPlacement(Point3D extremePoint, int dx, int dy, int dz) {
		if(extremePoint.getMinZ() != 0) {
			throw new IllegalArgumentException(extremePoint.getMinX() + "x" + extremePoint.getMinY() + "x" + extremePoint.getMinZ());
		}
		return new DefaultPlacement3D(extremePoint.getMinX(), extremePoint.getMinY(), extremePoint.getMinZ(), extremePoint.getMinX() + dx - 1, extremePoint.getMinY() + dy - 1, extremePoint.getMinZ() + dz - 1);
	}

	public static void main(String[] args) {
		DefaultExtremePoints3D extremePoints = new DefaultExtremePoints3D(1000, 1000, 1000);

		Point3D extremePoint = extremePoints.getValues().get(0);
		extremePoints.add(0, createPlacement(extremePoint, 50, 100, 1));

		extremePoint = extremePoints.getValues().get(extremePoints.getValues().size() - 1);
		extremePoints.add(extremePoints.getValues().size() - 1, createPlacement(extremePoint, 50, 50, 1));
		
		extremePoint = extremePoints.getValues().get(extremePoints.getValues().size() - 2);
		extremePoints.add(extremePoints.getValues().size() - 2, createPlacement(extremePoint, 100, 100, 1));

		extremePoint = extremePoints.getValues().get(extremePoints.getValues().size() - 1);
		extremePoints.add(extremePoints.getValues().size() - 1, createPlacement(extremePoint, 20, 20, 1));
		
		extremePoint = extremePoints.getValues().get(extremePoints.getValues().size() - 1);
		extremePoints.add(extremePoints.getValues().size() - 1, createPlacement(extremePoint, 20, 35, 1));

		extremePoint = extremePoints.getValues().get(2);
		extremePoints.add(2, createPlacement(extremePoint, 20, 20, 1));

		extremePoint = extremePoints.getValues().get(1);
		extremePoints.add(1, createPlacement(extremePoint, 30, 20, 1));

		extremePoint = extremePoints.getValues().get(2);
		extremePoints.add(2, createPlacement(extremePoint, 30, 20, 1));

		int offset = 2;
		extremePoint = extremePoints.getValues().get(extremePoints.getValues().size() - offset);
		//extremePoints.add(extremePoints.getValues().size() - offset, createPlacement(extremePoint, 50, 50, 1));
		
		//extremePoint = extremePoints.getValues().get(6);
//		extremePoints.add(6, createPlacement(extremePoint, 50, 50, 1));

		System.out.println("xyz:");
		
		for (Point3D p : extremePoints.getValues()) {
			System.out.println(" " + p.getMinX() + "x" + p.getMinY() + "x" + p.getMinZ() + " " + p.getMaxX() + "x" + p.getMaxY() + "x" + p.getMaxZ());
		}
		
		List<Point3D<Placement3D>> values = extremePoints.getValues();
		for (int i = 0; i < values.size(); i++) {
			Point3D point3d = values.get(i);
			
			if(point3d.getMinZ() != 0) {
				values.remove(i);
				i--;
			}
		}
		
		System.out.println("xy:");
		for (Point3D p : extremePoints.getValues()) {
			System.out.println(" " + p.getMinX() + "x" + p.getMinY() + "x" + p.getMinZ() + " " + p.getMaxX() + "x" + p.getMaxY() + "x" + p.getMaxZ());
		}
		DrawPoints2D.show(extremePoints);
	}
}