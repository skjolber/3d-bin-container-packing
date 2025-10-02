package com.github.skjolber.packing.points3d;

import static com.github.skjolber.packing.points3d.assertj.SimplePoint3DAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;

public class ExtremePoints3DTest {
	
	private Placement createStackPlacement(int x, int y, int z, int endX, int endY, int endZ) {
		BoxStackValue stackValue = new BoxStackValue(endX + 1 - x, endY + 1 - y, endZ + 1 - z, null, -1);
		
		Box box = Box.newBuilder().withSize(endX + 1 - x, endY + 1 - y, endZ + 1 - z).withWeight(0).build();
		stackValue.setBox(box);
		
		new BoxItem(box, 1);
		
		return new Placement(stackValue, new DefaultPoint3D(x, y, z, 0, 0, 0));
	}
	
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testSinglePoint(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(clone);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));
		assertThat(ep.getAll()).hasSize(3);

		// y
		// |
		// |
		// |-----|
		// |     |
		// |     |
		// |-----|---- x
		//

		// z
		// |
		// |
		// |-----|
		// |     |
		// |     |
		// |-----|---- x
		//

		assertThat(ep.get(0)).isMin(0, 0, 10);
		assertThat(ep.get(0)).isMax(99, 99, 99);
		assertThat(ep.get(0)).isXYSupportAt(9, 9);
		assertThat(ep.get(0)).isYZSupportAt(99, 99);
		assertThat(ep.get(0)).isXZSupportAt(99, 99);

		assertThat(ep.get(1)).isMin(0, 10, 0);
		assertThat(ep.get(1)).isMax(99, 99, 99);
		assertThat(ep.get(1)).isXYSupportAt(99, 99);
		assertThat(ep.get(1)).isYZSupportAt(99, 99);
		assertThat(ep.get(1)).isXZSupportAt(9, 9);

		assertThat(ep.get(2)).isMin(10, 0, 0);
		assertThat(ep.get(2)).isMax(99, 99, 99);
		assertThat(ep.get(2)).isXYSupportAt(99, 99);
		assertThat(ep.get(2)).isYZSupportAt(9, 9);
		assertThat(ep.get(2)).isXZSupportAt(99, 99);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testSinglePointCornercase(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(clone);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 0, 0, 0));
		assertThat(ep.getAll()).hasSize(3);

		assertThat(ep.get(0)).isMin(0, 0, 1);
		assertThat(ep.get(1)).isMin(0, 1, 0);
		assertThat(ep.get(2)).isMin(1, 0, 0);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testSinglePointCoveringAllX(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(clone);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 99, 9, 9));

		assertThat(ep.getAll()).hasSize(2);

		assertThat(ep.get(0)).isMin(0, 0, 10);
		assertThat(ep.get(0)).isXYSupportAt(99, 9);
		assertThat(ep.get(0)).isYZSupportAt(99, 99);
		assertThat(ep.get(0)).isXZSupportAt(99, 99);

		assertThat(ep.get(1)).isMin(0, 10, 0);
		assertThat(ep.get(1)).isXYSupportAt(99, 99);
		assertThat(ep.get(1)).isYZSupportAt(99, 99);
		assertThat(ep.get(1)).isXZSupportAt(99, 9);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testSinglePointCoveringAllY(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(clone);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 99, 9));

		assertThat(ep.getAll()).hasSize(2);

		assertThat(ep.get(0)).isMin(0, 0, 10);
		assertThat(ep.get(0)).isXYSupportAt(9, 99);
		assertThat(ep.get(0)).isYZSupportAt(99, 99);
		assertThat(ep.get(0)).isXZSupportAt(99, 99);

		assertThat(ep.get(1)).isMin(10, 0, 0);
		assertThat(ep.get(1)).isXYSupportAt(99, 99);
		assertThat(ep.get(1)).isYZSupportAt(99, 9);
		assertThat(ep.get(1)).isXZSupportAt(99, 99);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testSinglePointCoveringAllZ(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(clone);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 99));
		assertThat(ep.getAll()).hasSize(2);

		assertThat(ep.get(0)).isMin(0, 10, 0);
		assertThat(ep.get(0)).isXYSupportAt(99, 99);
		assertThat(ep.get(0)).isYZSupportAt(99, 99);
		assertThat(ep.get(0)).isXZSupportAt(9, 99);

		assertThat(ep.get(1)).isMin(10, 0, 0);
		assertThat(ep.get(1)).isXYSupportAt(99, 99);
		assertThat(ep.get(1)).isYZSupportAt(9, 99);
		assertThat(ep.get(1)).isXZSupportAt(99, 99);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testSinglePointCoveringWholeContainer(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(clone);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 99, 99, 99));
		assertThat(ep.getAll()).hasSize(0);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackInXDirection(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(clone);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		assertThat(ep.getAll()).hasSize(3);

		assertThat(ep.get(0)).isMin(0, 0, 10);
		assertThat(ep.get(1)).isMin(0, 10, 0);
		assertThat(ep.get(2)).isMin(10, 0, 0);

		ep.add(2, createStackPlacement(10, 0, 0, 19, 4, 4));

		//    y
		//    |
		//    |
		// 10 |-----|
		//    |     |     
		//  4 |     |-----|
		//    |     |     | 
		//    |-----|-----|-- x
		//          10    19

		//    z
		//    |
		//    |
		// 10 |-----|
		//    |     |     
		//  4 |     |-----|
		//    |     |     | 
		//    |-----|-----|-- x
		//          10    19
		//

		//    z
		//    |
		//    |
		// 10 |-----|
		//    |     |     
		//  4 | - - |
		//    |     | 
		//    |-----|------- y
		//          10    
		//

		assertThat(ep.getAll()).hasSize(5);

		assertThat(ep.get(0)).isMin(0, 0, 10);
		assertThat(ep.get(0)).isXYSupportAt(9, 9);
		assertThat(ep.get(0)).isYZSupportAt(99, 99);
		assertThat(ep.get(0)).isXZSupportAt(99, 99);

		assertThat(ep.get(1)).isMin(0, 10, 0);
		assertThat(ep.get(1)).isXYSupportAt(99, 99);
		assertThat(ep.get(1)).isYZSupportAt(99, 99);
		assertThat(ep.get(1)).isXZSupportAt(9, 9);

		assertThat(ep.get(2)).isMin(10, 0, 5);
		assertThat(ep.get(2)).isXYSupportAt(19, 4);
		assertThat(ep.get(2)).isYZSupportAt(9, 9);
		assertThat(ep.get(2)).isXZSupportAt(99, 99);

		assertThat(ep.get(3)).isMin(10, 5, 0);
		assertThat(ep.get(3)).isXYSupportAt(99, 99);
		assertThat(ep.get(3)).isYZSupportAt(9, 9);
		assertThat(ep.get(3)).isXZSupportAt(19, 4);

		assertThat(ep.get(4)).isMin(20, 0, 0);
		assertThat(ep.get(4)).isXYSupportAt(99, 99);
		assertThat(ep.get(4)).isYZSupportAt(4, 4);
		assertThat(ep.get(4)).isXZSupportAt(99, 99);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackInXDirectionWithIntermediate(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(clone);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 0));

		assertThat(ep.getAll()).hasSize(3);

		ep.add(2, createStackPlacement(10, 0, 0, 19, 3, 0));

		assertThat(ep.getAll()).hasSize(4);

		ep.add(3, createStackPlacement(20, 0, 0, 29, 6, 0));
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackInZDirection(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(clone);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		assertThat(ep.getAll()).hasSize(3);

		assertThat(ep.get(0)).isMin(0, 0, 10);
		assertThat(ep.get(1)).isMin(0, 10, 0);
		assertThat(ep.get(2)).isMin(10, 0, 0);

		ep.add(0, createStackPlacement(0, 0, 10, 4, 4, 19));

		//    y
		//    |
		//    |
		// 10 |-----|
		//    |     |     
		//  4 |--|  |
		//    |  |  | 
		//    |--|--|------- x
		//          10    

		//    z
		//    |
		// 19 |--|
		//    |  |
		//    |  |
		//    |  |
		//    |  |
		// 10 |-----|
		//    |     |     
		//  4 |     |
		//    |     | 
		//    |-----|------- x
		//          10    
		//

		//    z
		//    |
		//    |
		// 19 |--|
		//    |  |
		//    |  |
		//    |  |
		//    |  |
		// 10 |-----|
		//    |     |     
		//    |     |
		//    |     | 
		//    |-----|------- y
		//       4  10    
		//

		assertThat(ep.getAll()).hasSize(5);

		assertThat(ep.get(0)).isMin(0, 0, 20);
		assertThat(ep.get(0)).isXYSupportAt(4, 4);
		assertThat(ep.get(0)).isYZSupportAt(99, 99);
		assertThat(ep.get(0)).isXZSupportAt(99, 99);

		assertThat(ep.get(1)).isMin(0, 5, 10);
		assertThat(ep.get(1)).isXYSupportAt(9, 9);
		assertThat(ep.get(1)).isYZSupportAt(99, 99);
		assertThat(ep.get(1)).isXZSupportAt(4, 19);

		assertThat(ep.get(2)).isMin(0, 10, 0);
		assertThat(ep.get(2)).isXYSupportAt(99, 99);
		assertThat(ep.get(2)).isYZSupportAt(99, 99);
		assertThat(ep.get(2)).isXZSupportAt(9, 9);

		assertThat(ep.get(3)).isMin(5, 0, 10);
		assertThat(ep.get(3)).isXYSupportAt(9, 9);
		assertThat(ep.get(3)).isYZSupportAt(4, 19);
		assertThat(ep.get(3)).isXZSupportAt(99, 99);

		assertThat(ep.get(4)).isMin(10, 0, 0);
		assertThat(ep.get(4)).isXYSupportAt(99, 99);
		assertThat(ep.get(4)).isYZSupportAt(9, 9);
		assertThat(ep.get(4)).isXZSupportAt(99, 99);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackInYDirection(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(clone);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		assertThat(ep.getAll()).hasSize(3);

		ep.add(1, createStackPlacement(0, 10, 0, 4, 19, 4));

		assertThat(ep.getAll()).hasSize(5);

		assertThat(ep.get(0)).isMin(0, 0, 10);
		assertThat(ep.get(1)).isMin(0, 10, 5);
		assertThat(ep.get(2)).isMin(0, 20, 0);
		assertThat(ep.get(3)).isMin(5, 10, 0);
		assertThat(ep.get(4)).isMin(10, 0, 0);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackEqualItemsInXDirection(boolean clone) throws InterruptedException {
		ExtremePoints3D ep = new ExtremePoints3D(clone);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		assertThat(ep.get(0)).isMin(0, 0, 10);
		assertThat(ep.get(0)).isXYSupportAt(9, 9);
		assertThat(ep.get(0)).isYZSupportAt(99, 99);
		assertThat(ep.get(0)).isXZSupportAt(99, 99);

		assertThat(ep.get(1)).isMin(0, 10, 0);
		assertThat(ep.get(2)).isMin(10, 0, 0);

		// y
		// |
		// |
		// |-----|
		// |     |
		// |     |
		// |-----|---- x
		//

		// z
		// |
		// |
		// |-----|
		// |     |
		// |     |
		// |-----|---- x
		//

		ep.add(2, createStackPlacement(10, 0, 0, 19, 9, 9));

		// y
		// |
		// |
		// |-----|-----|
		// |     |     |
		// |     |     | 
		// |-----|-------- x
		//

		// z
		// |
		// |
		// |-----|-----|
		// |     |     |
		// |     |     | 
		// |-----|-------- x
		//

		assertThat(ep.getAll()).hasSize(3);

		assertThat(ep.get(0)).isMin(0, 0, 10);
		assertThat(ep.get(0)).isXYSupportAt(9, 9);
		assertThat(ep.get(0)).isYZSupportAt(99, 99);
		assertThat(ep.get(0)).isXZSupportAt(99, 99);

		assertThat(ep.get(1)).isMin(0, 10, 0);
		assertThat(ep.get(1)).isXYSupportAt(99, 99);
		assertThat(ep.get(1)).isYZSupportAt(99, 99);
		assertThat(ep.get(1)).isXZSupportAt(9, 9);

		assertThat(ep.get(2)).isMin(20, 0, 0);
		assertThat(ep.get(2)).isXYSupportAt(99, 99);
		assertThat(ep.get(2)).isYZSupportAt(9, 9);
		assertThat(ep.get(2)).isXZSupportAt(99, 99);

	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackEqualItemsInYDirection(boolean clone) throws InterruptedException {
		ExtremePoints3D ep = new ExtremePoints3D(clone);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		// y
		// |
		// |
		// |-----|
		// |     |
		// |     |
		// |-----|---- x
		//

		// z
		// |
		// |
		// |-----|
		// |     |
		// |     |
		// |-----|---- x
		//

		ep.add(1, createStackPlacement(0, 10, 0, 9, 19, 9));

		// y
		// |
		// |
		// *-----|
		// |     |
		// |     |
		// |-----|
		// |     |
		// |     |
		// *-----*---- x
		//

		// z
		// |
		// |            
		// *-----|
		// |     |
		// |     |
		// *-----*---- x
		//

		assertThat(ep.getAll()).hasSize(3);

		assertThat(ep.get(0)).isMin(0, 0, 10);
		assertThat(ep.get(0)).isXYSupportAt(9, 9);
		assertThat(ep.get(0)).isYZSupportAt(99, 99);
		assertThat(ep.get(0)).isXZSupportAt(99, 99);

		assertThat(ep.get(1)).isMin(0, 20, 0);
		assertThat(ep.get(1)).isXYSupportAt(99, 99);
		assertThat(ep.get(1)).isYZSupportAt(99, 99);
		assertThat(ep.get(1)).isXZSupportAt(9, 9);

		assertThat(ep.get(2)).isMin(10, 0, 0);
		assertThat(ep.get(2)).isXYSupportAt(99, 99);
		assertThat(ep.get(2)).isYZSupportAt(9, 9);
		assertThat(ep.get(2)).isXZSupportAt(99, 99);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackEqualItemsInZDirection(boolean clone) throws InterruptedException {
		ExtremePoints3D ep = new ExtremePoints3D(clone);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		ep.add(0, createStackPlacement(0, 0, 10, 9, 9, 19));

		// y
		// |
		// |
		// |
		// |
		// |-----|
		// |     |
		// |     |
		// |---------- x
		//

		// z
		// |
		// |
		// |-----|
		// |     |
		// |     |
		// |-----|
		// |     |
		// |     |
		// |---------- x
		//

		assertThat(ep.getAll()).hasSize(3);

		assertThat(ep.get(0)).isMin(0, 0, 20);
		assertThat(ep.get(0)).isXYSupportAt(9, 9);
		assertThat(ep.get(0)).isYZSupportAt(99, 99);
		assertThat(ep.get(0)).isXZSupportAt(99, 99);

		assertThat(ep.get(1)).isMin(0, 10, 0);
		assertThat(ep.get(1)).isXYSupportAt(99, 99);
		assertThat(ep.get(1)).isYZSupportAt(99, 99);
		assertThat(ep.get(1)).isXZSupportAt(9, 9);

		assertThat(ep.get(2)).isMin(10, 0, 0);
		assertThat(ep.get(2)).isXYSupportAt(99, 99);
		assertThat(ep.get(2)).isYZSupportAt(9, 9);
		assertThat(ep.get(2)).isXZSupportAt(99, 99);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackHigherItemsInXDirection(boolean clone) throws InterruptedException {
		ExtremePoints3D ep = new ExtremePoints3D(clone);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		assertThat(ep.getAll()).hasSize(3);

		ep.add(2, createStackPlacement(10, 0, 0, 19, 19, 19));

		assertThat(ep.getAll()).hasSize(5);

		//    y
		//    |
		//    |     |----|
		//    |     |    |
		//    |     |    |
		//    |     |    |
		// 10 |-----|    |
		//    |     |    | 
		//    |     |    |
		//    |     |    |
		//    |-----|----|-- x
		//          10   19    

		//    z
		//    |
		//    |     |----|
		//    |     |    |
		//    |     |    |
		//    |     |    |
		// 10 |-----|    |
		//    |     |    | 
		//    |     |    |
		//    |     |    |
		//    |-----|----|-- x
		//          10   19    

		//    z
		//    |
		//    |-----------|
		//    |           |
		//    |           |
		//    |           |
		// 10 |-----|     |
		//    |     |     |
		//    |     |     |
		//    |     |     |
		//    |-----|-----|-- y
		//          10   19    

		assertThat(ep.get(0)).isMin(0, 0, 10);
		assertThat(ep.get(1)).isMin(0, 0, 20);
		assertThat(ep.get(2)).isMin(0, 10, 0);
		assertThat(ep.get(3)).isMin(0, 20, 0);

		assertThat(ep.get(4)).isMin(20, 0, 0);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackHigherItemsInYDirection(boolean clone) throws InterruptedException {
		ExtremePoints3D ep = new ExtremePoints3D(clone);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		assertThat(ep.getAll()).hasSize(3);

		ep.add(1, createStackPlacement(0, 10, 0, 19, 19, 19));

		//    y
		//    |
		//    |
		// 19 |-----|
		//    |     |     
		//    |     |
		//    |     | 
		//    |-----|------- x
		//          19    

		//    z
		//    |
		// 19 |-----------|
		//    |           |
		//    |           |
		//    |           |
		// 10 |-----|-----|
		//    |     |     
		//    |     |
		//    |     | 
		//    |-----|------- x
		//          10    
		//

		//    z
		//    |
		//    |
		// 19 |-----------|
		//    |           |
		//    |           |
		//    |           |
		// 10 |-----|-----|
		//    |     |     
		//    |     |
		//    |     | 
		//    |-----|------- y
		//          10    
		//

		assertThat(ep.getAll()).hasSize(5);

	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackHigherItemsInZDirection(boolean clone) throws InterruptedException {
		ExtremePoints3D ep = new ExtremePoints3D(clone);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		assertThat(ep.getAll()).hasSize(3);

		ep.add(0, createStackPlacement(0, 0, 10, 19, 19, 19));
	}
	
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testInitialPointOneLevelUp(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(clone);
		ep.setSize(100, 100, 100);
		
		Point levelPoint = new DefaultPoint3D(0, 0, 20, 99, 99, 99);
		ep.setPoints(Arrays.asList(levelPoint));
		ep.clear();
		
		ep.add(0, createStackPlacement(0, 0, 20, 9, 9, 29));
		assertThat(ep.getAll()).hasSize(3);

		// y
		// |
		// |
		// |-----|
		// |     |
		// |     |
		// |-----|---- x
		//

		// z
		// |
		// |
		// |-----|
		// |     |
		// |     |
		// |-----|
		// |
		// |
		// |
		// |--------- x
		//

		assertThat(ep.get(0)).isMin(0, 0, 10 + 20);
		assertThat(ep.get(0)).isMax(99, 99, 99);
		assertThat(ep.get(0)).isXYSupportAt(9, 9);
		assertThat(ep.get(0)).isYZSupportAt(99, 99);
		assertThat(ep.get(0)).isXZSupportAt(99, 99);

		// no xy support
		assertThat(ep.get(1)).isMin(0, 10, 20);
		assertThat(ep.get(1)).isMax(99, 99, 99);
		assertThat(ep.get(1)).isYZSupportAt(99, 99);
		assertThat(ep.get(1)).isXZSupportAt(9, 29);

		// no xy support
		assertThat(ep.get(2)).isMin(10, 0, 20);
		assertThat(ep.get(2)).isMax(99, 99, 99);
		assertThat(ep.get(2)).isYZSupportAt(9, 29);
		assertThat(ep.get(2)).isXZSupportAt(99, 99);
	}

}
