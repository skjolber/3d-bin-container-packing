package com.github.skjolber.packing.ep.points3d;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;

class MovedSupportMaskTest {

	@Test
	void matchesMovedPointForEverySupportCombinationAndCoordinateBoundary() {
		int[] coordinates = { -1, 0, 2, 3, 4, 5, 6, 8, 9, 10, 14, 15, 16, 20, 21, 22, 25, 26, 27 };
		for(int combination = 0; combination < 8; combination++) {
			Placement yz = (combination & 1) != 0 ? placement(3, 7, 13, 4, 9, 13) : null;
			Placement xz = (combination & 2) != 0 ? placement(11, 2, 1, 11, 4, 4) : null;
			Placement xy = (combination & 4) != 0 ? placement(2, 4, 6, 4, 6, 2) : null;
			DefaultPoint3D point = new DefaultPoint3D(0, 0, 0, 30, 30, 30, yz, xz, xy);
			for(int coordinate : coordinates) {
				assertEquals(supports(point.moveX(coordinate)), point.getSupportMaskAfterMoveX(coordinate), "moveX, supports=" + combination + ", coordinate=" + coordinate);
				assertEquals(supports(point.moveY(coordinate)), point.getSupportMaskAfterMoveY(coordinate), "moveY, supports=" + combination + ", coordinate=" + coordinate);
				assertEquals(supports(point.moveZ(coordinate)), point.getSupportMaskAfterMoveZ(coordinate), "moveZ, supports=" + combination + ", coordinate=" + coordinate);
			}
		}
	}

	private static int supports(SimplePoint3D point) {
		return (point.isSupportedXYPlane() ? 4 : 0) + (point.isSupportedXZPlane() ? 2 : 0) + (point.isSupportedYZPlane() ? 1 : 0);
	}

	private static Placement placement(int x, int y, int z, int dx, int dy, int dz) {
		Box box = Box.newBuilder()
				.withSize(dx, dy, dz)
				.withWeight(1)
				.build();
		return new Placement(box.getStackValue(0), -1, x, y, z, false);
	}
}
