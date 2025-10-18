package com.github.skjolber.packing.points3d;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.MarkResetPointCalculator3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

public class MarkResetPointCalculator3DTest {

	@Test
	public void testMarkReset() {
		MarkResetPointCalculator3D ep = new MarkResetPointCalculator3D(false, 16);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		assertThat(ep.getAll()).hasSize(3);

		SimplePoint3D point1 = ep.get(0);
		ep.add(0, createStackPlacement(point1.getMinX(), point1.getMinY(), point1.getMinZ(), point1.getMinX() + 9, point1.getMinY() + 9, point1.getMinZ() + 9));

		ep.mark();
		
		SimplePoint3D point2 = ep.get(0);
		ep.add(0, createStackPlacement(point2.getMinX(), point2.getMinY(), point2.getMinZ(), point2.getMinX() + 9, point2.getMinY() + 9, point2.getMinZ() + 9));
		
		assertThat(ep.getPlacements()).hasSize(3);
		
		ep.reset();
		
		assertThat(ep.getPlacements()).hasSize(2);
	}
	
	private Placement createStackPlacement(int x, int y, int z, int endX, int endY, int endZ) {
		BoxStackValue stackValue = new BoxStackValue(endX + 1 - x, endY + 1 - y, endZ + 1 - z, null, -1);
		
		Box box = Box.newBuilder().withSize(endX + 1 - x, endY + 1 - y, endZ + 1 - z).withWeight(1).build();
		stackValue.setBox(box);
		
		new BoxItem(box, 1);
		
		return new Placement(stackValue, new DefaultPoint3D(x, y, z, 0, 0, 0));
	}
	
}
