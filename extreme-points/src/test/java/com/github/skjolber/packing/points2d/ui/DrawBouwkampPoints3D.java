package com.github.skjolber.packing.points2d.ui;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.ep.points3d.DefaultPlacement3D;
import com.github.skjolber.packing.points.BouwkampConverter;
import com.github.skjolber.packing.points.DefaultExtremePoints3D;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;

public class DrawBouwkampPoints3D {

	public static void main(String[] args) {
		
		BouwkampConverter converter = new BouwkampConverter(false);
		
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();
		
		List<BouwkampCodes> target = directory.getAll();
		
		for(BouwkampCodes codes : target) {
			for(BouwkampCode c : codes.getCodes()) {
				
				DefaultExtremePoints3D plane = converter.convert3DXYPlane(c, 1);
				
				List<Point3D<?>> list = new ArrayList<>();
				
				List<Point3D<DefaultPlacement3D>> values = plane.getValuesAsList();

				List<DefaultPlacement3D> placements = plane.getPlacements();

				Placement3D last = placements.get(placements.size() - 1);
				boolean rotate = last.getAbsoluteX() == 0;

				for (Point3D<DefaultPlacement3D> point3d : values) {
					if(rotate) {
						list.add(point3d.rotate());
					} else {
						list.add(point3d);
					}
				}
				
				List<Placement3D> results = new ArrayList<>();
				
				for(Placement3D p : placements) {
					if(rotate) {
						results.add(rotate(p));
					} else {
						results.add(p);
					}
				}
				
				if(rotate) {
					DrawPoints2D.show3D(list, results, plane.getDepth(), plane.getHeight());
				} else {
					DrawPoints2D.show3D(list, results, plane.getWidth(), plane.getDepth());
				}
			}
		}
	}

	private static Placement3D rotate(Placement3D p) {
		return new DefaultPlacement3D(p.getAbsoluteY(), p.getAbsoluteZ(), p.getAbsoluteX(), p.getAbsoluteEndY(), p.getAbsoluteEndZ(), p.getAbsoluteEndX());
	}
	
	
	
	
	
}