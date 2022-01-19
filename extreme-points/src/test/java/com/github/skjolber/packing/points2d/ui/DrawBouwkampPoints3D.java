package com.github.skjolber.packing.points2d.ui;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.ep.points3d.DefaultPlacement3D;
import com.github.skjolber.packing.points.BouwkampConverter;
import com.github.skjolber.packing.points.DefaultExtremePoints3D;
import com.github.skjolber.packing.test.BouwkampCode;
import com.github.skjolber.packing.test.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.BouwkampCodes;

public class DrawBouwkampPoints3D {

	public static void main(String[] args) {
		
		BouwkampConverter converter = new BouwkampConverter(false);
		
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();
		
		List<BouwkampCodes> target = directory.getAll();
		
		for(BouwkampCodes codes : target) {
			for(BouwkampCode c : codes.getCodes()) {
				
				if(codes.getSource().contains("o15siss.bkp") && c.getName().equals("39B")) {
				//if(codes.getSource().equals("/simpleImperfectSquaredRectangles/o9sisr.bkp") && c.getName().equals("15x11A")) {
					DefaultExtremePoints3D convert3dyzPlane = converter.convert3DYZPlane(c, 12);
					
					List<Point3D<?>> list = new ArrayList<>();
					
					List<Point3D<Placement3D>> values = convert3dyzPlane.getValuesAsList();
					for (Point3D<Placement3D> point3d : values) {
						list.add(point3d.rotate());
					}
					
					List<Placement3D> results = new ArrayList<>();
					
					List<Placement3D> placements = convert3dyzPlane.getPlacements();
					for(Placement3D p : placements) {
						results.add(rotate(p));
					}
					
					DrawPoints2D.show3D(list, results, convert3dyzPlane.getDepth(), convert3dyzPlane.getHeight());
					return;
				}
			}
		}
	}

	private static Placement3D rotate(Placement3D p) {
		return new DefaultPlacement3D(p.getAbsoluteY(), p.getAbsoluteZ(), p.getAbsoluteX(), p.getAbsoluteEndY(), p.getAbsoluteEndZ(), p.getAbsoluteEndX());
	}
	
	
	
	
	
}