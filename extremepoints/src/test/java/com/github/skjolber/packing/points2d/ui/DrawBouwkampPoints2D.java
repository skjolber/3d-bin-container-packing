package com.github.skjolber.packing.points2d.ui;

import java.util.List;

import com.github.skjolber.packing.points.BouwkampConverter;
import com.github.skjolber.packing.points.DefaultExtremePoints2D;
import com.github.skjolber.packing.test.BouwkampCode;
import com.github.skjolber.packing.test.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.BouwkampCodes;

public class DrawBouwkampPoints2D {

	public static void main(String[] args) {
		
		BouwkampConverter converter = new BouwkampConverter(false);
		
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();
		
		List<BouwkampCodes> target = directory.getAll();
		for(BouwkampCodes codes : target) {
			System.out.println(codes.getSource());
			for(BouwkampCode c : codes.getCodes()) {
				if(codes.getSource().contains("o15siss.bkp") && c.getName().equals("41A")) {
					DefaultExtremePoints2D convert2d = converter.convert2D(c, 1);
					
					DrawPoints2D.show(convert2d);
					
					return;
				}
			}
		}
	}
}