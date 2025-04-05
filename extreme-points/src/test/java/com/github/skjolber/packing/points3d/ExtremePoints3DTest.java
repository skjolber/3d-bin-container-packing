package com.github.skjolber.packing.points3d;

import static com.github.skjolber.packing.points3d.assertj.SimplePoint3DAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;

public class ExtremePoints3DTest {
	
	private StackPlacement createStackPlacement(int x, int y, int z, int endX, int endY, int endZ) {
		BoxStackValue stackValue = new BoxStackValue(endX + 1 - x, endY + 1 - y, endZ + 1 - z, null, null, -1);
		
		Box box = Box.newBuilder().withSize(endX + 1 - x, endY + 1 - y, endZ + 1 - z).withWeight(0).build();
		
		return new StackPlacement(box, stackValue, x, y, z);
	}
	
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testSinglePoint(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(100, 100, 100, clone);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));
		assertThat(ep.getValues()).hasSize(3);

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

		assertThat(ep.getValue(0)).isMin(0, 0, 10);
		assertThat(ep.getValue(0)).isMax(99, 99, 99);
		assertThat(ep.getValue(0)).isXYSupportAt(9, 9);
		assertThat(ep.getValue(0)).isYZSupportAt(99, 99);
		assertThat(ep.getValue(0)).isXZSupportAt(99, 99);

		assertThat(ep.getValue(1)).isMin(0, 10, 0);
		assertThat(ep.getValue(1)).isMax(99, 99, 99);
		assertThat(ep.getValue(1)).isXYSupportAt(99, 99);
		assertThat(ep.getValue(1)).isYZSupportAt(99, 99);
		assertThat(ep.getValue(1)).isXZSupportAt(9, 9);

		assertThat(ep.getValue(2)).isMin(10, 0, 0);
		assertThat(ep.getValue(2)).isMax(99, 99, 99);
		assertThat(ep.getValue(2)).isXYSupportAt(99, 99);
		assertThat(ep.getValue(2)).isYZSupportAt(9, 9);
		assertThat(ep.getValue(2)).isXZSupportAt(99, 99);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testSinglePointCornercase(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(100, 100, 100, clone);
		ep.add(0, createStackPlacement(0, 0, 0, 0, 0, 0));
		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.getValue(0)).isMin(0, 0, 1);
		assertThat(ep.getValue(1)).isMin(0, 1, 0);
		assertThat(ep.getValue(2)).isMin(1, 0, 0);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testSinglePointCoveringAllX(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(100, 100, 100, clone);
		ep.add(0, createStackPlacement(0, 0, 0, 99, 9, 9));

		assertThat(ep.getValues()).hasSize(2);

		assertThat(ep.getValue(0)).isMin(0, 0, 10);
		assertThat(ep.getValue(0)).isXYSupportAt(99, 9);
		assertThat(ep.getValue(0)).isYZSupportAt(99, 99);
		assertThat(ep.getValue(0)).isXZSupportAt(99, 99);

		assertThat(ep.getValue(1)).isMin(0, 10, 0);
		assertThat(ep.getValue(1)).isXYSupportAt(99, 99);
		assertThat(ep.getValue(1)).isYZSupportAt(99, 99);
		assertThat(ep.getValue(1)).isXZSupportAt(99, 9);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testSinglePointCoveringAllY(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(100, 100, 100, clone);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 99, 9));

		assertThat(ep.getValues()).hasSize(2);

		assertThat(ep.getValue(0)).isMin(0, 0, 10);
		assertThat(ep.getValue(0)).isXYSupportAt(9, 99);
		assertThat(ep.getValue(0)).isYZSupportAt(99, 99);
		assertThat(ep.getValue(0)).isXZSupportAt(99, 99);

		assertThat(ep.getValue(1)).isMin(10, 0, 0);
		assertThat(ep.getValue(1)).isXYSupportAt(99, 99);
		assertThat(ep.getValue(1)).isYZSupportAt(99, 9);
		assertThat(ep.getValue(1)).isXZSupportAt(99, 99);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testSinglePointCoveringAllZ(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(100, 100, 100, clone);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 99));
		assertThat(ep.getValues()).hasSize(2);

		assertThat(ep.getValue(0)).isMin(0, 10, 0);
		assertThat(ep.getValue(0)).isXYSupportAt(99, 99);
		assertThat(ep.getValue(0)).isYZSupportAt(99, 99);
		assertThat(ep.getValue(0)).isXZSupportAt(9, 99);

		assertThat(ep.getValue(1)).isMin(10, 0, 0);
		assertThat(ep.getValue(1)).isXYSupportAt(99, 99);
		assertThat(ep.getValue(1)).isYZSupportAt(9, 99);
		assertThat(ep.getValue(1)).isXZSupportAt(99, 99);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testSinglePointCoveringWholeContainer(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(100, 100, 100, clone);
		ep.add(0, createStackPlacement(0, 0, 0, 99, 99, 99));
		assertThat(ep.getValues()).hasSize(0);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackInXDirection(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(100, 100, 100, clone);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.getValue(0)).isMin(0, 0, 10);
		assertThat(ep.getValue(1)).isMin(0, 10, 0);
		assertThat(ep.getValue(2)).isMin(10, 0, 0);

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

		assertThat(ep.getValues()).hasSize(5);

		assertThat(ep.getValue(0)).isMin(0, 0, 10);
		assertThat(ep.getValue(0)).isXYSupportAt(9, 9);
		assertThat(ep.getValue(0)).isYZSupportAt(99, 99);
		assertThat(ep.getValue(0)).isXZSupportAt(99, 99);

		assertThat(ep.getValue(1)).isMin(0, 10, 0);
		assertThat(ep.getValue(1)).isXYSupportAt(99, 99);
		assertThat(ep.getValue(1)).isYZSupportAt(99, 99);
		assertThat(ep.getValue(1)).isXZSupportAt(9, 9);

		assertThat(ep.getValue(2)).isMin(10, 0, 5);
		assertThat(ep.getValue(2)).isXYSupportAt(19, 4);
		assertThat(ep.getValue(2)).isYZSupportAt(9, 9);
		assertThat(ep.getValue(2)).isXZSupportAt(99, 99);

		assertThat(ep.getValue(3)).isMin(10, 5, 0);
		assertThat(ep.getValue(3)).isXYSupportAt(99, 99);
		assertThat(ep.getValue(3)).isYZSupportAt(9, 9);
		assertThat(ep.getValue(3)).isXZSupportAt(19, 4);

		assertThat(ep.getValue(4)).isMin(20, 0, 0);
		assertThat(ep.getValue(4)).isXYSupportAt(99, 99);
		assertThat(ep.getValue(4)).isYZSupportAt(4, 4);
		assertThat(ep.getValue(4)).isXZSupportAt(99, 99);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackInXDirectionWithIntermediate(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(100, 100, 100, clone);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 0));

		assertThat(ep.getValues()).hasSize(3);

		ep.add(2, createStackPlacement(10, 0, 0, 19, 3, 0));

		assertThat(ep.getValues()).hasSize(4);

		ep.add(3, createStackPlacement(20, 0, 0, 29, 6, 0));
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackInZDirection(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(100, 100, 100, clone);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.getValue(0)).isMin(0, 0, 10);
		assertThat(ep.getValue(1)).isMin(0, 10, 0);
		assertThat(ep.getValue(2)).isMin(10, 0, 0);

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

		assertThat(ep.getValues()).hasSize(5);

		assertThat(ep.getValue(0)).isMin(0, 0, 20);
		assertThat(ep.getValue(0)).isXYSupportAt(4, 4);
		assertThat(ep.getValue(0)).isYZSupportAt(99, 99);
		assertThat(ep.getValue(0)).isXZSupportAt(99, 99);

		assertThat(ep.getValue(1)).isMin(0, 5, 10);
		assertThat(ep.getValue(1)).isXYSupportAt(9, 9);
		assertThat(ep.getValue(1)).isYZSupportAt(99, 99);
		assertThat(ep.getValue(1)).isXZSupportAt(4, 19);

		assertThat(ep.getValue(2)).isMin(0, 10, 0);
		assertThat(ep.getValue(2)).isXYSupportAt(99, 99);
		assertThat(ep.getValue(2)).isYZSupportAt(99, 99);
		assertThat(ep.getValue(2)).isXZSupportAt(9, 9);

		assertThat(ep.getValue(3)).isMin(5, 0, 10);
		assertThat(ep.getValue(3)).isXYSupportAt(9, 9);
		assertThat(ep.getValue(3)).isYZSupportAt(4, 19);
		assertThat(ep.getValue(3)).isXZSupportAt(99, 99);

		assertThat(ep.getValue(4)).isMin(10, 0, 0);
		assertThat(ep.getValue(4)).isXYSupportAt(99, 99);
		assertThat(ep.getValue(4)).isYZSupportAt(9, 9);
		assertThat(ep.getValue(4)).isXZSupportAt(99, 99);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackInYDirection(boolean clone) {
		ExtremePoints3D ep = new ExtremePoints3D(100, 100, 100, clone);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		assertThat(ep.getValues()).hasSize(3);

		ep.add(1, createStackPlacement(0, 10, 0, 4, 19, 4));

		assertThat(ep.getValues()).hasSize(5);

		assertThat(ep.getValue(0)).isMin(0, 0, 10);
		assertThat(ep.getValue(1)).isMin(0, 10, 5);
		assertThat(ep.getValue(2)).isMin(0, 20, 0);
		assertThat(ep.getValue(3)).isMin(5, 10, 0);
		assertThat(ep.getValue(4)).isMin(10, 0, 0);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackEqualItemsInXDirection(boolean clone) throws InterruptedException {
		ExtremePoints3D ep = new ExtremePoints3D(100, 100, 100, clone);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		assertThat(ep.getValue(0)).isMin(0, 0, 10);
		assertThat(ep.getValue(0)).isXYSupportAt(9, 9);
		assertThat(ep.getValue(0)).isYZSupportAt(99, 99);
		assertThat(ep.getValue(0)).isXZSupportAt(99, 99);

		assertThat(ep.getValue(1)).isMin(0, 10, 0);
		assertThat(ep.getValue(2)).isMin(10, 0, 0);

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

		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.getValue(0)).isMin(0, 0, 10);
		assertThat(ep.getValue(0)).isXYSupportAt(9, 9);
		assertThat(ep.getValue(0)).isYZSupportAt(99, 99);
		assertThat(ep.getValue(0)).isXZSupportAt(99, 99);

		assertThat(ep.getValue(1)).isMin(0, 10, 0);
		assertThat(ep.getValue(1)).isXYSupportAt(99, 99);
		assertThat(ep.getValue(1)).isYZSupportAt(99, 99);
		assertThat(ep.getValue(1)).isXZSupportAt(9, 9);

		assertThat(ep.getValue(2)).isMin(20, 0, 0);
		assertThat(ep.getValue(2)).isXYSupportAt(99, 99);
		assertThat(ep.getValue(2)).isYZSupportAt(9, 9);
		assertThat(ep.getValue(2)).isXZSupportAt(99, 99);

	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackEqualItemsInYDirection(boolean clone) throws InterruptedException {
		ExtremePoints3D ep = new ExtremePoints3D(100, 100, 100, clone);
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

		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.getValue(0)).isMin(0, 0, 10);
		assertThat(ep.getValue(0)).isXYSupportAt(9, 9);
		assertThat(ep.getValue(0)).isYZSupportAt(99, 99);
		assertThat(ep.getValue(0)).isXZSupportAt(99, 99);

		assertThat(ep.getValue(1)).isMin(0, 20, 0);
		assertThat(ep.getValue(1)).isXYSupportAt(99, 99);
		assertThat(ep.getValue(1)).isYZSupportAt(99, 99);
		assertThat(ep.getValue(1)).isXZSupportAt(9, 9);

		assertThat(ep.getValue(2)).isMin(10, 0, 0);
		assertThat(ep.getValue(2)).isXYSupportAt(99, 99);
		assertThat(ep.getValue(2)).isYZSupportAt(9, 9);
		assertThat(ep.getValue(2)).isXZSupportAt(99, 99);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackEqualItemsInZDirection(boolean clone) throws InterruptedException {
		ExtremePoints3D ep = new ExtremePoints3D(100, 100, 100, clone);
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

		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.getValue(0)).isMin(0, 0, 20);
		assertThat(ep.getValue(0)).isXYSupportAt(9, 9);
		assertThat(ep.getValue(0)).isYZSupportAt(99, 99);
		assertThat(ep.getValue(0)).isXZSupportAt(99, 99);

		assertThat(ep.getValue(1)).isMin(0, 10, 0);
		assertThat(ep.getValue(1)).isXYSupportAt(99, 99);
		assertThat(ep.getValue(1)).isYZSupportAt(99, 99);
		assertThat(ep.getValue(1)).isXZSupportAt(9, 9);

		assertThat(ep.getValue(2)).isMin(10, 0, 0);
		assertThat(ep.getValue(2)).isXYSupportAt(99, 99);
		assertThat(ep.getValue(2)).isYZSupportAt(9, 9);
		assertThat(ep.getValue(2)).isXZSupportAt(99, 99);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackHigherItemsInXDirection(boolean clone) throws InterruptedException {
		ExtremePoints3D ep = new ExtremePoints3D(100, 100, 100, clone);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		assertThat(ep.getValues()).hasSize(3);

		ep.add(2, createStackPlacement(10, 0, 0, 19, 19, 19));

		assertThat(ep.getValues()).hasSize(5);

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

		assertThat(ep.getValue(0)).isMin(0, 0, 10);
		assertThat(ep.getValue(1)).isMin(0, 0, 20);
		assertThat(ep.getValue(2)).isMin(0, 10, 0);
		assertThat(ep.getValue(3)).isMin(0, 20, 0);

		assertThat(ep.getValue(4)).isMin(20, 0, 0);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackHigherItemsInYDirection(boolean clone) throws InterruptedException {
		ExtremePoints3D ep = new ExtremePoints3D(100, 100, 100, clone);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		assertThat(ep.getValues()).hasSize(3);

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

		assertThat(ep.getValues()).hasSize(5);

	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackHigherItemsInZDirection(boolean clone) throws InterruptedException {
		ExtremePoints3D ep = new ExtremePoints3D(100, 100, 100, clone);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		assertThat(ep.getValues()).hasSize(3);

		ep.add(0, createStackPlacement(0, 0, 10, 19, 19, 19));
	}

}
