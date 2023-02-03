package com.github.skjolber.packing.points2d.ui;

import java.util.List;

import com.github.skjolber.packing.points.BouwkampConverter;
import com.github.skjolber.packing.points.DefaultExtremePoints2D;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;

public class DrawBouwkampPoints2D {

	public static void main(String[] args) {

		BouwkampConverter converter = new BouwkampConverter(false);

		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		List<BouwkampCodes> target = directory.getSimplePerfectSquaredRectangles();
		for (BouwkampCodes codes : target) {
			System.out.println(codes.getSource());
			for (BouwkampCode c : codes.getCodes()) {
				if(codes.getSource().contains("o12spsr.bkp") && c.getName().equals("106x99A")) {
					DefaultExtremePoints2D convert2d = converter.convert2D(c, 10);

					DrawPoints2D.show(convert2d);

					return;
				}
			}
		}
	}
}
