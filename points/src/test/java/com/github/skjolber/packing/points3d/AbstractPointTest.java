package com.github.skjolber.packing.points3d;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;

public abstract class AbstractPointTest {

	// y
	// |
	// | 
	// |
	// *----------| 
	// |          |
	// |   rear   | 
	// |          |
	// *-------------------|
	// |                   | 
	// |       center      | 
	// |                   | 
	// *----------|        |--------| 
	// |          |        |        |
	// |   top    |        |  right | 
	// |          |        |        |
	// *----------*--------*--------*--- x
	//

	protected Placement centerPlacement = createStackPlacement(0, 0, 0, 9, 9, 9);
	protected Placement rightPlacement = createStackPlacement(10, 0, 0, 14, 4, 4); // x
	protected Placement rearPlacement = createStackPlacement(0, 10, 0, 4, 14, 4); // y
	protected Placement topPlacement = createStackPlacement(0, 0, 10, 4, 4, 14); // z
	protected Placement innerPlacement = createStackPlacement(0, 0, 0, 4, 4, 14); // z
	
	protected Placement createStackPlacement(int x, int y, int z, int endX, int endY, int endZ) {
		BoxStackValue stackValue = new BoxStackValue(endX + 1 - x, endY + 1 - y, endZ + 1 - z, null, -1);
		
		Box box = Box.newBuilder().withSize(endX + 1 - x, endY + 1 - y, endZ + 1 - z).withWeight(1).build();
		stackValue.setBox(box);
		
		new BoxItem(box, 1);
		
		return new Placement(stackValue, new DefaultPoint3D(x, y, z, 0, 0, 0));
	}
}
