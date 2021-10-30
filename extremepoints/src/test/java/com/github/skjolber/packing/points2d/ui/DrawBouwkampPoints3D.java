package com.github.skjolber.packing.points2d.ui;

import java.util.List;

import com.github.skjolber.packing.points.BouwkampConverter;
import com.github.skjolber.packing.test.BouwkampCode;
import com.github.skjolber.packing.test.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.BouwkampCodes;

public class DrawBouwkampPoints3D {

	public static void main(String[] args) {
		
		BouwkampConverter converter = new BouwkampConverter();
		
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();
		
		//List<BouwkampCodes> simpleImperfectSquaredSquares = directory.getSimpleImperfectSquaredSquares( p -> p.contains("o13siss.bkp"));
		//List<BouwkampCodes> target = directory.getSimpleImperfectSquaredSquares( p -> p.contains("o13siss.bkp"));
		List<BouwkampCodes> target = directory.getSimplePerfectSquaredRectangles( p -> p.contains("o13spsr.bkp"));
		
		for(BouwkampCodes codes : target) {
			for(BouwkampCode c : codes.getCodes()) {
				if(true || c.getName().equals("521x416A")) {
					DrawPoints2D.show(converter.convert3D(c, 2));
					return;
				}
			}
		}
	}
}