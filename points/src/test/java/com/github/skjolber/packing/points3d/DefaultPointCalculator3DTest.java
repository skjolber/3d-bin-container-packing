package com.github.skjolber.packing.points3d;

import static com.github.skjolber.packing.points3d.assertj.SimplePoint3DAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

public class DefaultPointCalculator3DTest {
	
	private Placement createStackPlacement(int x, int y, int z, int endX, int endY, int endZ) {
		BoxStackValue stackValue = new BoxStackValue(endX + 1 - x, endY + 1 - y, endZ + 1 - z, null, -1);
		
		Box box = Box.newBuilder().withSize(endX + 1 - x, endY + 1 - y, endZ + 1 - z).withWeight(1).build();
		stackValue.setBox(box);
		
		new BoxItem(box, 1);
		
		return new Placement(stackValue, new DefaultPoint3D(x, y, z, endX, endY, endZ));
	}
	
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testSinglePoint(boolean clone) {
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
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
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
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
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
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
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
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
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
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
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 99, 99, 99));
		assertThat(ep.getAll()).hasSize(0);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackInXDirection(boolean clone) {
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
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
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
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
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
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
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
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
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
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

		assertThat(ep.calculateUsedWeight()).isEqualTo(ep.getPlacements().size());
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testStackEqualItemsInYDirection(boolean clone) throws InterruptedException {
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
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
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
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
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
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
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
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
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		assertThat(ep.getAll()).hasSize(3);

		ep.add(0, createStackPlacement(0, 0, 10, 19, 19, 19));
	}
	
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testInitialPointOneLevelUp(boolean clone) {
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
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

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testObstacleInXYPlaneMiddle(boolean clone) {
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
		ep.clearToSize(100, 100, 100);
		ep.addObstacle(createStackPlacement(50, 50, 0, 54, 54, 4));
		assertThat(ep.getAll()).hasSize(5); // i.e. not below
		
		// y
		// |
		// |---------------------|
		// |                     |
		// |       |-----|       | 54
		// |       |     |       |
		// |       |-----|       | 50
		// |                     |
		// |---------------------|--- x
		//        50    54
		// Two extra parts
		//
		// y
		// |
		// |--------
		// |       |
		// |       |
		// |   0   |
		// |       |
		// |       |
		// |-------|--- x
		//
		// y
		// |
		// |---------------------|
		// |         1           |
		// |---------------------|--- x
		//
		// and xx, yy, zz components
		
		SimplePoint3D point0 = ep.get(0);
		assertThat(point0).isMin(0, 0, 0);
		assertThat(point0).isMax(49, 99, 99);
		assertThat(point0).isXYSupportAt(99, 99);
		assertThat(point0).isYZSupportAt(99, 99);
		assertThat(point0).isXZSupportAt(99, 99);
		
		SimplePoint3D point1 = ep.get(1);
		assertThat(point1).isMin(0, 0, 0);
		assertThat(point1).isMax(99, 49, 99);
		assertThat(point1).isXYSupportAt(99, 99);
		assertThat(point1).isYZSupportAt(99, 99);
		assertThat(point1).isXZSupportAt(99, 99);

		// copy "regular" tests for a placement size 55x55x4
		// TOP
		assertThat(ep.get(2)).isMin(0, 0, 5);
		assertThat(ep.get(2)).isMax(99, 99, 99);
		
		// xy support is lost
		assertThat(ep.get(2)).isNoXYSupportAt(10, 10);
		assertThat(ep.get(2)).isNoXYSupportAt(51, 51);
		assertThat(ep.get(2)).isYZSupportAt(99, 99);
		assertThat(ep.get(2)).isXZSupportAt(99, 99);

		// y
		// |        
		// |---------------------|
		// |          3          |
		// *-------|═════|-------| <- double lined piece of support does not count
		// |       |     |
		// |       |-----| 
		// |              
		// |------------------------ x
		// 
		
		assertThat(ep.get(3)).isMin(0, 55, 0);
		assertThat(ep.get(3)).isMax(99, 99, 99);
		assertThat(ep.get(3)).isXYSupportAt(99, 99);
		assertThat(ep.get(3)).isYZSupportAt(99, 99);
		
		// xz support is lost
		assertThat(ep.get(3)).isNoXZSupportAt(9, 9);
		assertThat(ep.get(3)).isNoXZSupportAt(52, 3);

		// y
		// |
		// |             |-------|
		// |             |       |
		// |       |-----║       | 
		// |       |     ║   4   | double lined piece of support does not count
		// |       |-----║       | 
		// |             |       |
		// |-------------*-------|--- x
		// 
		
		assertThat(ep.get(4)).isMin(55, 0, 0);
		assertThat(ep.get(4)).isMax(99, 99, 99);
		assertThat(ep.get(4)).isXYSupportAt(99, 99);
		assertThat(ep.get(4)).isXZSupportAt(99, 99);

		// yz support is lost
		assertThat(ep.get(4)).isNoYZSupportAt(9, 9);
		assertThat(ep.get(4)).isNoYZSupportAt(52, 3);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testObstacleInXYPlaneX(boolean clone) {
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
		ep.clearToSize(100, 100, 100);
		ep.addObstacle(createStackPlacement(50, 0, 0, 54, 54, 4));
		assertThat(ep.getAll()).hasSize(4); // i.e. not below
		
		// y
		// |
		// |---------------------|
		// |                     |
		// |                     |
		// |                     |
		// |       |-----|       | 54
		// |       |     |       |
		// |-------|-----|-------|--- x
		//         50   54
		// y
		// |
		// |--------
		// |       |
		// |       |
		// |   0   |
		// |       |
		// |       |
		// |-------|--- x
		//
		
		SimplePoint3D point0 = ep.get(0);
		assertThat(point0).isMin(0, 0, 0);
		assertThat(point0).isMax(49, 99, 99);
		assertThat(point0).isXYSupportAt(99, 99);
		assertThat(point0).isYZSupportAt(99, 99);
		assertThat(point0).isXZSupportAt(99, 99);

		// copy "regular" tests for a placement size 55x55x4
		assertThat(ep.get(1)).isMin(0, 0, 5);
		assertThat(ep.get(1)).isMax(99, 99, 99);
		
		// xy support is lost
		// TOP
		assertThat(ep.get(1)).isNoXYSupportAt(10, 10);
		assertThat(ep.get(1)).isNoXYSupportAt(51, 0);
		assertThat(ep.get(1)).isYZSupportAt(99, 99);
		assertThat(ep.get(1)).isXZSupportAt(99, 99);

		// y
		// |        
		// |---------------------|
		// |                     |
		// |          2          |
		// |                     |
		// *-------|═════|-------| 54
		// |       |     |       |
		// |-------|-----|---------- x
		//         50   54
		//
		//  Double lined piece of support does not count
		
		assertThat(ep.get(2)).isMin(0, 55, 0);
		assertThat(ep.get(2)).isMax(99, 99, 99);
		assertThat(ep.get(2)).isXYSupportAt(99, 99);
		assertThat(ep.get(2)).isYZSupportAt(99, 99);
		
		// xz support is lost
		assertThat(ep.get(2)).isNoXZSupportAt(9, 9);
		assertThat(ep.get(2)).isNoXZSupportAt(52, 3);

		// y
		// |
		// |             |-------|
		// |             |       |
		// |             |       |
		// |       |-----║       | 
		// |       |     ║   3   | 
		// |-------|-----║-------|--- x 
		// 
		
		assertThat(ep.get(3)).isMin(55, 0, 0);
		assertThat(ep.get(3)).isMax(99, 99, 99);
		assertThat(ep.get(3)).isXYSupportAt(99, 99);
		assertThat(ep.get(3)).isXZSupportAt(99, 99);

		// yz support is kept
		assertThat(ep.get(3)).isNoYZSupportAt(9, 9);
		assertThat(ep.get(3)).isYZSupportAt(1, 1);
	}
	
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testObstacleInXYPlaneY(boolean clone) {
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
		ep.clearToSize(100, 100, 100);
		ep.addObstacle(createStackPlacement(0, 50, 0, 54, 54, 4));
		assertThat(ep.getAll()).hasSize(4); // i.e. not below
		
		// y
		// |
		// |-------------------|
		// |                   |
		// |-------|           | 54
		// |       |           |
		// |       |           |
		// |-------|           | 50
		// |                   |
		// |-------------------|--- x
		//        54
		// y
		// |
		// |
		// |
		// |
		// |---------------------|
		// |         0           |
		// |---------------------|--- x
		//
		
		SimplePoint3D point0 = ep.get(0);
		assertThat(point0).isMin(0, 0, 0);
		assertThat(point0).isMax(99, 49, 99);
		assertThat(point0).isXYSupportAt(99, 99);
		assertThat(point0).isYZSupportAt(99, 99);
		assertThat(point0).isXZSupportAt(99, 99);

		// xy support is lost
		// TOP
		assertThat(ep.get(1)).isNoXYSupportAt(10, 10);
		assertThat(ep.get(1)).isNoXYSupportAt(51, 0);
		assertThat(ep.get(1)).isYZSupportAt(99, 99);
		assertThat(ep.get(1)).isXZSupportAt(99, 99);
	
		// y
		// |
		// |-------------------| 99
		// |                   |
		// *════════-----------| 55
		// |       |           |
		// |       |           |
		// |-------|           | 50
		// |
		// |
		// |---------------------- x
		//        54
		
		assertThat(ep.get(2)).isMin(0, 55, 0);
		assertThat(ep.get(2)).isMax(99, 99, 99);
		assertThat(ep.get(2)).isXYSupportAt(99, 99);
		assertThat(ep.get(2)).isYZSupportAt(99, 99);
		
		// xz support is kept
		assertThat(ep.get(2)).isXZSupportAt(1, 1);
		assertThat(ep.get(2)).isNoXZSupportAt(55, 1);
		
		// y
		// |
		// |-------------------|
		// |       |           |
		// |-------║           | 54
		// |       ║           |
		// |       ║           |
		// |-------║           | 50
		// |       |           |
		// |-------------------|--- x
		//        50 
		
		assertThat(ep.get(3)).isMin(55, 0, 0);
		assertThat(ep.get(3)).isMax(99, 99, 99);
		assertThat(ep.get(3)).isXYSupportAt(99, 99);
		assertThat(ep.get(3)).isXZSupportAt(99, 99);

		// yz support is kept
		assertThat(ep.get(3)).isNoYZSupportAt(9, 9);
		assertThat(ep.get(3)).isNoYZSupportAt(1, 1);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testTwoObstacleInXYPlaneX(boolean clone) {
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
		ep.clearToSize(100, 100, 100);
		ep.addObstacle(createStackPlacement(50, 0, 0, 54, 24, 4));
		ep.addObstacle(createStackPlacement(50, 75, 0, 54, 99, 4));
		
		assertThat(ep.getAll()).hasSize(4); // i.e. not below
		
		List<Point> all = ep.getAll();
		for (int i = 0; i < all.size(); i++) {
			Point point = all.get(i);
			System.out.println(point);
		}
		
		// y
		// |
		// |-------|-----|-------|
		// |       |     |       |
		// |       |-----|       | 75
		// |                     |
		// |                     |
		// |                     |
		// |                     |
		// |                     |
		// |       |-----|       | 24
		// |       |     |       |
		// |-------|-----|-------|--- x
		//         50   54
		//
		// y
		// |
		// |-------|
		// |       |
		// |       |
		// |       |
		// |       |
		// |       |
		// |       |
		// |       |
		// |       |
		// |       |
		// |-------|--------------- x
		//         50  
		//
		// |
		// |             |-------|
		// |             |       |
		// |             |       |
		// |             |       |
		// |             |       |
		// |             |       |
		// |             |       |
		// |             |       |
		// |             |       |
		// |             |       |
		// |-------------|-------|--- x
		//         50   54
		//
		// y
		// |
		// |
		// |
		// |---------------------| 74
		// |                     |
		// |                     |
		// |                     |
		// |                     |
		// |---------------------| 25
		// |
		// |------------------------ x
		//
		//
		// and TOP
		SimplePoint3D point0 = ep.get(0);
		assertThat(point0).isMin(0, 0, 0);
		assertThat(point0).isMax(49, 99, 99);
		assertThat(point0).isXYSupportAt(99, 99);
		assertThat(point0).isYZSupportAt(99, 99);
		assertThat(point0).isXZSupportAt(99, 99);
		
		// TOP
		assertThat(ep.get(1)).isMin(0, 0, 5);
		assertThat(ep.get(1)).isMax(99, 99, 99);

		// xy support is lost
		assertThat(ep.get(1)).isNoXYSupportAt(10, 10);
		assertThat(ep.get(1)).isNoXYSupportAt(51, 51);
		assertThat(ep.get(1)).isYZSupportAt(99, 99);
		assertThat(ep.get(1)).isXZSupportAt(99, 99);
		
		assertThat(ep.get(2)).isMin(0, 25, 0);
		assertThat(ep.get(2)).isMax(99, 74, 99);
		
		assertThat(ep.get(3)).isMin(55, 0, 0);
		assertThat(ep.get(3)).isMax(99, 99, 99);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testObstacleInXYPlaneTop(boolean clone) {
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
		ep.clearToSize(100, 100, 100);
		ep.addObstacle(createStackPlacement(50, 50, 0, 54, 99, 4));
		assertThat(ep.getAll()).hasSize(4); // i.e. not below

		// y
		// |
		// |-------|-----|-------|
		// |       |     |       |
		// |       |-----|       | 50
		// |                     |
		// |                     |
		// |                     |
		// |---------------------|--- x
		//        50    54
		// 
		//
		// y
		// |
		// |--------
		// |       |
		// |       |
		// |   0   |
		// |       |
		// |       |
		// |-------|--- x
		//
		// y
		// |
		// |
		// |
		// |---------------------|
		// |         1           |
		// |---------------------|--- x
		//
		// TOP
		// and xx, zz components
		
		SimplePoint3D point0 = ep.get(0);
		assertThat(point0).isMin(0, 0, 0);
		assertThat(point0).isMax(49, 99, 99);
		assertThat(point0).isXYSupportAt(99, 99);
		assertThat(point0).isYZSupportAt(99, 99);
		assertThat(point0).isXZSupportAt(99, 99);
		
		SimplePoint3D point1 = ep.get(1);
		assertThat(point1).isMin(0, 0, 0);
		assertThat(point1).isMax(99, 49, 99);
		assertThat(point1).isXYSupportAt(99, 99);
		assertThat(point1).isYZSupportAt(99, 99);
		assertThat(point1).isXZSupportAt(99, 99);
		
		// TOP
		assertThat(ep.get(2)).isMin(0, 0, 5);
		assertThat(ep.get(2)).isMax(99, 99, 99);
		
		// yy
		assertThat(ep.get(3)).isMin(55, 0, 0);
		assertThat(ep.get(3)).isMax(99, 99, 99);
	}
	
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testObstacleInXYPlaneRight(boolean clone) {
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(clone, 16);
		ep.clearToSize(100, 100, 100);
		ep.addObstacle(createStackPlacement(50, 50, 0, 99, 54, 4));
		assertThat(ep.getAll()).hasSize(4); // i.e. not below
		
		// y
		// |
		// |---------------------|
		// |                     |
		// |             |-------| 54
		// |             |       |
		// |             |-------| 50
		// |                     |
		// |---------------------|--- x
		//            50
		// 
		//
		// y
		// |
		// |-------------|
		// |             |
		// |             |
		// |      0      |
		// |             |
		// |             |
		// |-------------|
		//      
		// y
		// |
		// |
		// |
		// |---------------------|
		// |         1           |
		// |---------------------|--- x
		//
		// TOP

		// y
		// |
		// |---------------------|
		// |         2           |
		// |---------------------|
		// |
		// |
		// |------------------------ x
		//
		
		SimplePoint3D point0 = ep.get(0);
		assertThat(point0).isMin(0, 0, 0);
		assertThat(point0).isMax(49, 99, 99);
		assertThat(point0).isXYSupportAt(99, 99);
		assertThat(point0).isYZSupportAt(99, 99);
		assertThat(point0).isXZSupportAt(99, 99);
		
		SimplePoint3D point1 = ep.get(1);
		assertThat(point1).isMin(0, 0, 0);
		assertThat(point1).isMax(99, 49, 99);
		assertThat(point1).isXYSupportAt(99, 99);
		assertThat(point1).isYZSupportAt(99, 99);
		assertThat(point1).isXZSupportAt(99, 99);
		
		// TOP
		assertThat(ep.get(2)).isMin(0, 0, 5);
		assertThat(ep.get(2)).isMax(99, 99, 99);
		
		// yy
		assertThat(ep.get(3)).isMin(0, 55, 0);
		assertThat(ep.get(3)).isMax(99, 99, 99);
	}

}
