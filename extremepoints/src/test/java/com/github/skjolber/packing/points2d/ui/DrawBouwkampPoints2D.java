package com.github.skjolber.packing.points2d.ui;

import java.util.List;

import com.github.skjolber.packing.points.BouwkampConverter;
import com.github.skjolber.packing.test.BouwkampCode;
import com.github.skjolber.packing.test.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.BouwkampCodes;

public class DrawBouwkampPoints2D {

	public static void main(String[] args) {
		
		BouwkampConverter converter = new BouwkampConverter();
		
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();
		
		List<BouwkampCodes> simpleImperfectSquaredSquares = directory.getSimpleImperfectSquaredSquares( p -> p.contains("o13siss.bkp"));
		
		for(BouwkampCodes codes : simpleImperfectSquaredSquares) {
			for(BouwkampCode c : codes.getCodes()) {
				DrawPoints2D.show(converter.convert(c, 16));
			}
		}
	}
}