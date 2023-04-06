package com.github.skjolber.packing.points3d;

import static com.github.skjolber.packing.points3d.assertj.SimplePoint3DAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.ep.points3d.DefaultPlacement3D;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;

public class ExtremePoints3DTest {

	@Test
	public void testSinglePoint() {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 9));
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

	@Test
	public void testSinglePointCornercase() {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 0, 0, 0));
		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.getValue(0)).isMin(0, 0, 1);
		assertThat(ep.getValue(1)).isMin(0, 1, 0);
		assertThat(ep.getValue(2)).isMin(1, 0, 0);
	}

	@Test
	public void testSinglePointCoveringAllX() {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 99, 9, 9));

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

	@Test
	public void testSinglePointCoveringAllY() {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 99, 9));

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

	@Test
	public void testSinglePointCoveringAllZ() {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 99));
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

	@Test
	public void testSinglePointCoveringWholeContainer() {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 99, 99, 99));
		assertThat(ep.getValues()).hasSize(0);
	}

	@Test
	public void testStackInXDirection() {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 9));

		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.getValue(0)).isMin(0, 0, 10);
		assertThat(ep.getValue(1)).isMin(0, 10, 0);
		assertThat(ep.getValue(2)).isMin(10, 0, 0);

		ep.add(2, new DefaultPlacement3D(10, 0, 0, 19, 4, 4));

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

	@Test
	public void testStackInXDirectionWithIntermediate() {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 0));

		assertThat(ep.getValues()).hasSize(3);

		ep.add(2, new DefaultPlacement3D(10, 0, 0, 19, 3, 0));

		assertThat(ep.getValues()).hasSize(4);

		ep.add(3, new DefaultPlacement3D(20, 0, 0, 29, 6, 0));
	}

	@Test
	public void testStackInZDirection() {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 9));

		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.getValue(0)).isMin(0, 0, 10);
		assertThat(ep.getValue(1)).isMin(0, 10, 0);
		assertThat(ep.getValue(2)).isMin(10, 0, 0);

		ep.add(0, new DefaultPlacement3D(0, 0, 10, 4, 4, 19));

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

	@Test
	public void testStackInYDirection() {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 9));

		assertThat(ep.getValues()).hasSize(3);

		ep.add(1, new DefaultPlacement3D(0, 10, 0, 4, 19, 4));

		assertThat(ep.getValues()).hasSize(5);

		assertThat(ep.getValue(0)).isMin(0, 0, 10);
		assertThat(ep.getValue(1)).isMin(0, 10, 5);
		assertThat(ep.getValue(2)).isMin(0, 20, 0);
		assertThat(ep.getValue(3)).isMin(5, 10, 0);
		assertThat(ep.getValue(4)).isMin(10, 0, 0);
	}

	@Test
	public void testStackEqualItemsInXDirection() throws InterruptedException {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 9));

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

		ep.add(2, new DefaultPlacement3D(10, 0, 0, 19, 9, 9));

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

	@Test
	public void testStackEqualItemsInYDirection() throws InterruptedException {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 9));

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

		ep.add(1, new DefaultPlacement3D(0, 10, 0, 9, 19, 9));

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

	@Test
	public void testStackEqualItemsInZDirection() throws InterruptedException {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 9));

		ep.add(0, new DefaultPlacement3D(0, 0, 10, 9, 9, 19));

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

	@Test
	public void testStackHigherItemsInXDirection() throws InterruptedException {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 9));

		assertThat(ep.getValues()).hasSize(3);

		ep.add(2, new DefaultPlacement3D(10, 0, 0, 19, 19, 19));

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

	@Test
	public void testStackHigherItemsInYDirection() throws InterruptedException {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 9));

		assertThat(ep.getValues()).hasSize(3);

		ep.add(1, new DefaultPlacement3D(0, 10, 0, 19, 19, 19));

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

	@Test
	public void testStackHigherItemsInZDirection() throws InterruptedException {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 9));

		assertThat(ep.getValues()).hasSize(3);

		ep.add(0, new DefaultPlacement3D(0, 0, 10, 19, 19, 19));
	}

}
