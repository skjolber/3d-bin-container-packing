package com.github.skjolber.packing.points2d;

import static com.github.skjolber.packing.points2d.assertj.SimplePoint2DAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.Point;
import com.github.skjolber.packing.ep.points2d.DefaultXYSupportPoint2D;
import com.github.skjolber.packing.ep.points2d.ExtremePoints2D;
import com.github.skjolber.packing.ep.points2d.Point2D;
import com.github.skjolber.packing.points.DefaultExtremePoints2D;

public class ExtremePoints2DTest {

	private StackPlacement createStackPlacement(int x, int y, int endX, int endY) {
		return createStackPlacement(x, y, 0, endX, endY, 0);
	}
	
	private StackPlacement createStackPlacement(int x, int y, int z, int endX, int endY, int endZ) {
		BoxStackValue stackValue = new BoxStackValue(endX + 1 - x, endY + 1 - y, 1, null, -1);
		
		Box box = Box.newBuilder().withSize(endX + 1 - x, endY + 1 - y, 1).withWeight(0).build();
		
		return new StackPlacement(null, new BoxItem(box, 1), stackValue, x, y, z);
	}
	
	@Test
	public void testSinglePoint() {
		ExtremePoints2D ep = new ExtremePoints2D();
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 10, 10));
		assertThat(ep.getValues()).hasSize(2);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(11);
		assertThat(ep.get(0)).isMax(99, 99);
		assertThat(ep.get(0)).isMaxXSupport(10);
		assertThat(ep.get(0)).isMaxYSupport(99);

		assertThat(ep.get(1).getMinX()).isEqualTo(11);
		assertThat(ep.get(1).getMinY()).isEqualTo(0);
		assertThat(ep.get(1)).isMax(99, 99);
		assertThat(ep.get(1)).isMaxXSupport(99);
		assertThat(ep.get(1)).isMaxYSupport(10);
	}

	@Test
	public void testSinglePointCornerCase() {
		ExtremePoints2D ep = new ExtremePoints2D();
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 0, 0));
		assertThat(ep.getValues()).hasSize(2);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(1);
		assertThat(ep.get(0)).isMax(99, 99);
		assertThat(ep.get(0)).isMaxXSupport(0);
		assertThat(ep.get(0)).isMaxYSupport(99);

		assertThat(ep.get(1).getMinX()).isEqualTo(1);
		assertThat(ep.get(1).getMinY()).isEqualTo(0);
		assertThat(ep.get(1)).isMax(99, 99);
		assertThat(ep.get(1)).isMaxXSupport(99);
		assertThat(ep.get(1)).isMaxYSupport(0);
	}

	@Test
	public void testSinglePointCoveringAllX() {
		ExtremePoints2D ep = new ExtremePoints2D();
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 99, 10));
		assertThat(ep.getValues()).hasSize(1);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(11);
	}

	@Test
	public void testSinglePointCoveringAllY() {
		ExtremePoints2D ep = new ExtremePoints2D();
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 10, 99));
		assertThat(ep.getValues()).hasSize(1);

		assertThat(ep.get(0).getMinX()).isEqualTo(11);
		assertThat(ep.get(0).getMinY()).isEqualTo(0);
	}

	@Test
	public void testSinglePointCoveringWholeContainer() {
		ExtremePoints2D ep = new ExtremePoints2D();
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 99, 99));
		assertThat(ep.getValues()).hasSize(0);
	}

	@Test
	public void testStackInXDirection() {
		ExtremePoints2D ep = new ExtremePoints2D();
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 9, 49));

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(50);

		assertThat(ep.get(1).getMinX()).isEqualTo(10);
		assertThat(ep.get(1).getMinY()).isEqualTo(0);

		ep.add(1, createStackPlacement(10, 0, 19, 24));

		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(50);
		assertThat(ep.get(0)).isMax(99, 99);
		assertThat(ep.get(0)).isMaxXSupport(9);
		assertThat(ep.get(0)).isMaxYSupport(99);

		assertThat(ep.get(1).getMinX()).isEqualTo(10);
		assertThat(ep.get(1).getMinY()).isEqualTo(25);
		assertThat(ep.get(1)).isMax(99, 99);
		assertThat(ep.get(1)).isMaxXSupport(19);
		assertThat(ep.get(1)).isMaxYSupport(49);

		assertThat(ep.get(2).getMinX()).isEqualTo(20);
		assertThat(ep.get(2).getMinY()).isEqualTo(0);
		assertThat(ep.get(2)).isMax(99, 99);
		assertThat(ep.get(2)).isMaxXSupport(99);
		assertThat(ep.get(2)).isMaxYSupport(24);

	}

	@Test
	public void testStackInYDirection() {

		//      |
		//      |
		//   74 |--|
		//      |  |
		//      |  |
		//   49 |-----|
		//      |     |
		//      |     |
		//      |     |   
		//      ------------
		//         4  9   

		ExtremePoints2D ep = new ExtremePoints2D();
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 9, 49));

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(50);

		assertThat(ep.get(1).getMinX()).isEqualTo(10);
		assertThat(ep.get(1).getMinY()).isEqualTo(0);

		ep.add(0, createStackPlacement(0, 50, 4, 74));

		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(75);
		assertThat(ep.get(0)).isMax(99, 99);

		assertThat(ep.get(1).getMinX()).isEqualTo(5);
		assertThat(ep.get(1).getMinY()).isEqualTo(50);
		assertThat(ep.get(1)).isMax(99, 99);

		assertThat(ep.get(2).getMinX()).isEqualTo(10);
		assertThat(ep.get(2).getMinY()).isEqualTo(0);
		assertThat(ep.get(2)).isMax(99, 99);

	}

	@Test
	public void testStackEqualItemsInXDirection() throws InterruptedException {

		//
		//      |   
		//      |   
		//    9 |---|----|
		//      |   |    |
		//      |   |    |
		//      -------------
		//          9   19

		DefaultExtremePoints2D ep = new DefaultExtremePoints2D();
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 9, 9));

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(10);

		assertThat(ep.get(1).getMinX()).isEqualTo(10);
		assertThat(ep.get(1).getMinY()).isEqualTo(0);
		assertThat(ep.getValues()).hasSize(2);

		ep.add(1, createStackPlacement(10, 0, 19, 9));

		assertThat(ep.getValues()).hasSize(2);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(10);
		assertThat(ep.get(0)).isMax(99, 99);
		assertThat(ep.get(0)).isMaxXSupport(9); // XXX support is not extended to 19
		assertThat(ep.get(0)).isMaxYSupport(99);

		assertThat(ep.get(1).getMinX()).isEqualTo(20);
		assertThat(ep.get(1).getMinY()).isEqualTo(0);
		assertThat(ep.get(1)).isMax(99, 99);
		assertThat(ep.get(1)).isMaxXSupport(99);
		assertThat(ep.get(1)).isMaxYSupport(9);

	}

	@Test
	public void testStackEqualItemsInYDirection() throws InterruptedException {

		//
		//      |   
		//      |   
		//   19 |---|
		//      |   |
		//      |   |
		//    9 |---|
		//      |   |
		//      |   |
		//      ----------
		//          9 

		ExtremePoints2D ep = new ExtremePoints2D();
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 9, 9));

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(10);

		assertThat(ep.get(1).getMinX()).isEqualTo(10);
		assertThat(ep.get(1).getMinY()).isEqualTo(0);

		ep.add(0, createStackPlacement(0, 10, 9, 19));

		assertThat(ep.getValues()).hasSize(2);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(20);
		assertThat(ep.get(0)).isMax(99, 99);
		assertThat(ep.get(0)).isMaxXSupport(9);
		assertThat(ep.get(0)).isMaxYSupport(99);

		assertThat(ep.get(1).getMinX()).isEqualTo(10);
		assertThat(ep.get(1).getMinY()).isEqualTo(0);
		assertThat(ep.get(1)).isMax(99, 99);
		assertThat(ep.get(1)).isMaxXSupport(99);
		assertThat(ep.get(1)).isMaxYSupport(9); // XXX support is not extended to 19
	}

	@Test
	public void testStackHigherItemsInXDirection() throws InterruptedException {

		//
		//      |
		//      |
		//   19 |   |---|
		//      |   |   |
		//    9 |---|   |
		//      |   |   |
		//      |   |   |
		//      -------------
		//          9   19

		DefaultExtremePoints2D ep = new DefaultExtremePoints2D();
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 9, 9));

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(10);

		assertThat(ep.get(1).getMinX()).isEqualTo(10);
		assertThat(ep.get(1).getMinY()).isEqualTo(0);

		ep.add(1, createStackPlacement(10, 0, 19, 19));

		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(10);
		assertThat(ep.get(0).getMaxY()).isEqualTo(99);
		assertThat(ep.get(0).getMaxX()).isEqualTo(9);
		assertThat(ep.get(0)).isMaxXSupport(9);
		assertThat(ep.get(0)).isMaxYSupport(99);

		assertThat(ep.get(1).getMinX()).isEqualTo(0);
		assertThat(ep.get(1).getMinY()).isEqualTo(20);
		assertThat(ep.get(1)).isMaxYSupport(99);
		assertThat(ep.get(1)).isXSupport(10);
		assertThat(ep.get(1)).isNoXSupport(0);

		assertThat(ep.get(2).getMinX()).isEqualTo(20);
		assertThat(ep.get(2).getMinY()).isEqualTo(0);
		assertThat(ep.get(2)).isMaxXSupport(99);
		assertThat(ep.get(2)).isMaxYSupport(19);
	}

	@Test
	public void testStackWiderItemsInYDirection() throws InterruptedException {

		//
		//      |
		//   19 |-------|
		//      |       |
		//      |       |
		//    9 |-------|
		//      |   |    
		//      |   |    
		//      |   |    
		//      -------------
		//          9   19

		ExtremePoints2D ep = new ExtremePoints2D();
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 9, 9));

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(10);

		assertThat(ep.get(1).getMinX()).isEqualTo(10);
		assertThat(ep.get(1).getMinY()).isEqualTo(0);

		assertThat(ep.getValues()).hasSize(2);

		ep.add(0, createStackPlacement(0, 10, 19, 19));

		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(20);
		assertThat(ep.get(0)).isMaxXSupport(19);
		assertThat(ep.get(0)).isMaxYSupport(99);

		assertThat(ep.get(1).getMinX()).isEqualTo(10);
		assertThat(ep.get(1).getMinY()).isEqualTo(0);
		assertThat(ep.get(1).getMaxX()).isEqualTo(99);
		assertThat(ep.get(1).getMaxY()).isEqualTo(9);
		assertThat(ep.get(1)).isMaxXSupport(99);
		assertThat(ep.get(1)).isMaxYSupport(9);

		assertThat(ep.get(2).getMinX()).isEqualTo(20);
		assertThat(ep.get(2).getMinY()).isEqualTo(0);
		assertThat(ep.get(2)).isNoYSupport(0);
		assertThat(ep.get(2)).isYSupport(10);
		assertThat(ep.get(2)).isMaxXSupport(99);
		assertThat(ep.get(2)).isMax(99, 99);
	}

	@Test
	public void testSwallowedInXDirection1() throws InterruptedException {
		DefaultExtremePoints2D ep = new DefaultExtremePoints2D();
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 9, 9));
		ep.add(1, createStackPlacement(10, 0, 19, 19));

		//    |
		//    |
		//    |    
		// 20 x          |-------|
		//    |          |       |
		//    |          |       |
		// 10 x----------|       | 
		//    |          |       |
		//    |          |       |
		//    |----------|-------x---------
		//              10       20 
		assertThat(ep.getValues()).hasSize(3);

		ep.add(0, createStackPlacement(0, 10, 4, 24));

		//    |
		// 25 x---|
		//    |   |
		// 20 |   x      |-------|
		//    |   |      |       |
		//    |   |      |       |
		// 10 |---x------|       | 
		//    |          |       |
		//    |          |       |
		//    |----------|-------x---------
		//        5     10       20 

		assertThat(ep.get(0)).isMin(0, 25);
		assertThat(ep.get(0).getMaxX()).isEqualTo(99);
		assertThat(ep.get(0).getMaxY()).isEqualTo(99);
		assertThat(ep.get(0)).isMaxXSupport(4);
		assertThat(ep.get(0)).isMaxYSupport(99);

		assertThat(ep.get(1)).isMin(5, 10);
		assertThat(ep.get(1).getMaxX()).isEqualTo(9);
		assertThat(ep.get(1).getMaxY()).isEqualTo(99);
		assertThat(ep.get(1)).isMaxXSupport(9);
		assertThat(ep.get(1)).isMaxYSupport(24);

		assertThat(ep.get(2)).isMin(5, 20);
		assertThat(ep.get(2).getMaxX()).isEqualTo(99);
		assertThat(ep.get(2).getMaxY()).isEqualTo(99);

		assertThat(ep.get(3)).isMin(20, 0);
		assertThat(ep.get(3).getMaxX()).isEqualTo(99);
		assertThat(ep.get(3).getMaxY()).isEqualTo(99);

		assertThat(ep.getValues()).hasSize(4);
	}

	@Test
	public void testSwallowedInXDirection2() throws InterruptedException {
		DefaultExtremePoints2D ep = new DefaultExtremePoints2D();
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 9, 9));
		ep.add(1, createStackPlacement(10, 0, 19, 19));

		//    |
		//    |
		//    |    
		// 20 x          |-------|
		//    |          |       |
		//    |          |       |
		// 10 x----------|       | 
		//    |          |       |
		//    |          |       |
		//    |----------|-------x---------
		//              10       20
		//
		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.get(0)).isMin(0, 10);
		assertThat(ep.get(1)).isMin(0, 20);
		assertThat(ep.get(2)).isMin(20, 0);

		ep.add(2, createStackPlacement(20, 0, 24, 24));

		//    |
		//    |
		// 25 x                  |----|
		//    |                  |    |
		// 20 x          |-------|    |
		//    |          |       |    |
		//    |          |       |    |
		// 10 x----------|       |    |
		//    |          |       |    |
		//    |          |       |    |
		//    |----------|-------|----x----
		//              10       20   25
		//

		assertThat(ep.getValues()).hasSize(4);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(10);
		assertThat(ep.get(0)).isMax(9, 99);

		assertThat(ep.get(1).getMinX()).isEqualTo(0);
		assertThat(ep.get(1).getMinY()).isEqualTo(20);
		assertThat(ep.get(1)).isMax(19, 99);
		assertThat(ep.get(1)).isNoXSupport(0);
		assertThat(ep.get(1)).isXSupport(19);
		assertThat(ep.get(1)).isMaxYSupport(99);

		assertThat(ep.get(2).getMinX()).isEqualTo(0);
		assertThat(ep.get(2).getMinY()).isEqualTo(25);
		assertThat(ep.get(2)).isMax(99, 99);
		assertThat(ep.get(2)).isNoXSupport(0);
		assertThat(ep.get(2)).isXSupport(24);
		assertThat(ep.get(2)).isMaxYSupport(99);

		assertThat(ep.get(3).getMinX()).isEqualTo(25);
		assertThat(ep.get(3).getMinY()).isEqualTo(0);
		assertThat(ep.get(3)).isMax(99, 99);

		ep.add(0, createStackPlacement(0, 10, 4, 29));

		//    |
		//    |
		//    x----|
		//    |    |
		//    |    x             |----|
		//    |    |             |    |
		// 20 |    x     |-------|    |
		//    |    |     |       |    |
		//    |    |     |       |    |
		// 10 |----x-----|       |    |
		//    |          |       |    |
		//    |          |       |    |
		//    |----------|-------|----x----
		//              10       20
		//

		assertThat(ep.getValues()).hasSize(5);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(30);
		assertThat(ep.get(0)).isMax(99, 99);

		assertThat(ep.get(1).getMinX()).isEqualTo(5);
		assertThat(ep.get(1).getMinY()).isEqualTo(10);
		assertThat(ep.get(1)).isXSupport(9);
		assertThat(ep.get(1)).isMaxYSupport(29);

		assertThat(ep.get(2).getMinX()).isEqualTo(5);
		assertThat(ep.get(2).getMinY()).isEqualTo(20);
		assertThat(ep.get(2)).isNoXSupport(0);
		assertThat(ep.get(2)).isXSupport(19);
		assertThat(ep.get(2)).isMaxYSupport(29);

		assertThat(ep.get(3).getMinX()).isEqualTo(5);
		assertThat(ep.get(3).getMinY()).isEqualTo(25);
		assertThat(ep.get(3)).isMax(99, 99);
		assertThat(ep.get(3)).isNoXSupport(0);
		assertThat(ep.get(3)).isXSupport(24);
		assertThat(ep.get(3)).isMaxYSupport(29);

		assertThat(ep.get(4)).isMin(25, 0);
		assertThat(ep.get(4)).isMax(99, 99);
	}

	@Test
	public void testSwallowedInYDirection1() throws InterruptedException {
		ExtremePoints2D ep = new ExtremePoints2D();
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 9, 9));
		ep.add(0, createStackPlacement(0, 10, 19, 19));

		//    |
		// 20 x------------------|
		//    |                  |
		//    |                  |
		//    |                  |
		// 10 |------------------| 
		//    |          |       
		//    |          |       
		//    |          |
		//    |          |           
		//    |----------x-------x---------
		//              10       20 

		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.get(0)).isMin(0, 20);
		assertThat(ep.get(0)).isMaxXSupport(19);
		assertThat(ep.get(0)).isMaxYSupport(99);

		assertThat(ep.get(1)).isMin(10, 0);
		assertThat(ep.get(1)).isMaxXSupport(99);
		assertThat(ep.get(1)).isMaxYSupport(9);

		assertThat(ep.get(2)).isMin(20, 0);
		assertThat(ep.get(2)).isMaxXSupport(99);
		assertThat(ep.get(2)).isNoYSupport(0);
		assertThat(ep.get(2)).isYSupport(19);

		ep.add(1, createStackPlacement(10, 0, 24, 4));

		//    |
		// 20 x------------------|
		//    |                  |
		//    |                  |
		//    |                  |
		// 10 |------------------| 
		//    |          |       
		//    |          |       
		//  5 |          x-------x---|
		//    |          |           |
		//    |----------|-----------x-----
		//             10       20   25

		assertThat(ep.getValues()).hasSize(4);

		assertThat(ep.get(0)).isMin(0, 20);
		assertThat(ep.get(0)).isMax(99, 99);
		assertThat(ep.get(0)).isMaxXSupport(19);
		assertThat(ep.get(0)).isMaxYSupport(99);

		assertThat(ep.get(1)).isMin(10, 5);
		assertThat(ep.get(1)).isMax(99, 9);
		assertThat(ep.get(1)).isXSupport(24);
		assertThat(ep.get(1)).isMaxYSupport(9);

		assertThat(ep.get(2)).isMin(20, 5);
		assertThat(ep.get(2)).isMax(99, 99);
		assertThat(ep.get(2)).isXSupport(24);
		assertThat(ep.get(2)).isNoYSupport(0);
		assertThat(ep.get(2)).isYSupport(19);

		assertThat(ep.get(3)).isMin(25, 0);
		assertThat(ep.get(3)).isMax(99, 99);
		assertThat(ep.get(3)).isMaxXSupport(99);
		assertThat(ep.get(3)).isMaxYSupport(4);

		assertThat(ep.getValues()).hasSize(4);
	}

	@Test
	public void testSwallowedInYDirection2() throws InterruptedException {
		ExtremePoints2D ep = new ExtremePoints2D();
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 9, 9));
		ep.add(0, createStackPlacement(0, 10, 19, 19));

		//    |
		// 20 x------------------|
		//    |                  |
		//    |                  |
		//    |                  |
		// 10 |------------------| 
		//    |          |       
		//    |          |       
		//    |          |
		//    |          |           
		//    |----------x-------x---------
		//              10       20 

		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(20);
		assertThat(ep.get(1).getMinX()).isEqualTo(10);
		assertThat(ep.get(1).getMinY()).isEqualTo(0);
		assertThat(ep.get(2).getMinX()).isEqualTo(20);
		assertThat(ep.get(2).getMinY()).isEqualTo(0);

		ep.add(0, createStackPlacement(0, 20, 24, 24));

		//    |
		//    x-----------------------|
		//    |                       |
		// 20 |------------------|----|
		//    |                  |
		//    |                  |
		//    |                  |
		// 10 |------------------| 
		//    |          |       
		//    |          |       
		//    |          |
		//    |          |           
		//    |----------x-------x----x-----
		//              10       20   25

		assertThat(ep.getValues()).hasSize(4);
		assertThat(ep.get(0)).isMin(0, 25);
		assertThat(ep.get(0)).isMax(99, 99);
		assertThat(ep.get(0)).isSupport(24, 99);

		assertThat(ep.get(1).getMinX()).isEqualTo(10);
		assertThat(ep.get(1).getMinY()).isEqualTo(0);
		assertThat(ep.get(1)).isSupport(99, 9);

		assertThat(ep.get(2).getMinX()).isEqualTo(20);
		assertThat(ep.get(2).getMinY()).isEqualTo(0);
		assertThat(ep.get(2)).isMaxXSupport(99);
		assertThat(ep.get(2)).isYSupport(19);
		assertThat(ep.get(2)).isNoYSupport(0);

		assertThat(ep.get(3).getMinX()).isEqualTo(25);
		assertThat(ep.get(3).getMinY()).isEqualTo(0);
		assertThat(ep.get(3)).isMax(99, 99);
		assertThat(ep.get(3)).isMaxXSupport(99);
		assertThat(ep.get(3)).isNoYSupport(0);
		assertThat(ep.get(3)).isYSupport(24);

		ep.add(1, createStackPlacement(10, 0, 29, 4));

		//    |
		//    x-----------------------|
		//    |                       |
		// 20 |------------------|----|
		//    |                  |
		//    |                  |
		//    |                  |
		// 10 |------------------| 
		//    |          |       
		//    |          |       
		//  5 |          x-------x----x---|
		//    |          |                | 
		//    |----------|----------------x----
		//             10       20   25   30

		assertThat(ep.getValues()).hasSize(5);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(25);
		assertThat(ep.get(1).getMinX()).isEqualTo(10);
		assertThat(ep.get(1).getMinY()).isEqualTo(5);
		assertThat(ep.get(2).getMinX()).isEqualTo(20);
		assertThat(ep.get(2).getMinY()).isEqualTo(5);
		assertThat(ep.get(3).getMinX()).isEqualTo(25);
		assertThat(ep.get(3).getMinY()).isEqualTo(5);
		assertThat(ep.get(4).getMinX()).isEqualTo(30);
		assertThat(ep.get(4).getMinY()).isEqualTo(0);
	}

	@Test
	public void testStacking() {
		ExtremePoints2D ep = new ExtremePoints2D();
		ep.clearToSize(3, 2, 0);
		ep.add(0, createStackPlacement(0, 0, 0, 1));

		assertThat(ep.getValues()).hasSize(1);

		assertThat(ep.get(0).getMinX()).isEqualTo(1);
		assertThat(ep.get(0).getMinY()).isEqualTo(0);

		ep.add(0, createStackPlacement(1, 0, 1, 1));

		assertThat(ep.getValues()).hasSize(1);

		assertThat(ep.get(0).getMinX()).isEqualTo(2);
		assertThat(ep.get(0).getMinY()).isEqualTo(0);

		ep.add(0, createStackPlacement(2, 0, 2, 1));

		assertThat(ep.getValues()).hasSize(0);
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	public void testFloatingX(boolean b) {
		ExtremePoints2D ep = new ExtremePoints2D(b);
		ep.clearToSize(1000, 1000, 0);
		ep.add(0, createStackPlacement(0, 0, 49, 99));
		assertThat(ep.get(0)).isInstanceOf(DefaultXYSupportPoint2D.class);

		ep.add(1, createStackPlacement(50, 0, 99, 49));
		assertThat(ep.get(0)).isInstanceOf(DefaultXYSupportPoint2D.class);
		assertThat(ep.get(1)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(100);

		assertThat(ep.get(1).getMinX()).isEqualTo(50);
		assertThat(ep.get(1).getMinY()).isEqualTo(50);

		assertThat(ep.get(2).getMinX()).isEqualTo(100);
		assertThat(ep.get(2).getMinY()).isEqualTo(0);

		for (Point point : ep.getValues()) {
			assertThat(point.getMaxX()).isEqualTo(999);
			assertThat(point.getMaxY()).isEqualTo(999);
		}

		ep.add(1, createStackPlacement(50, 50, 149, 149));

		//    |           
		//    |           
		// 150x          |--------------------|
		//    |          |                    |
		//    |          |                    | 
		//    |          |                    |
		//    |          |                    | 
		//    |          |                    | 
		// 100x----------|                    |
		//    |          |                    |       
		//    |          |                    |
		//    |          |                    |
		//    |          |                    |
		// 50 |          |--------------------|
		//    |          |         |
		//    |          |         |
		//    |          |         |
		//    |          |         |
		//    |          |         |       
		//    |----------|---------x----------x----------
		//               50       100        150
		assertThat(ep.getValues()).hasSize(4);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(100);
		assertThat(ep.get(0)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(1).getMinX()).isEqualTo(0);
		assertThat(ep.get(1).getMinY()).isEqualTo(150);
		assertThat(ep.get(1)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(2).getMinX()).isEqualTo(100);
		assertThat(ep.get(2).getMinY()).isEqualTo(0);
		assertThat(ep.get(2)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(3).getMinX()).isEqualTo(150);
		assertThat(ep.get(3).getMinY()).isEqualTo(0);
		assertThat(ep.get(3)).isInstanceOf(DefaultXYSupportPoint2D.class);

		ep.add(3, createStackPlacement(150, 0, 169, 19));

		//    |           
		//    |           
		// 150x          |--------------------|
		//    |          |                    |
		//    |          |                    | 
		//    |          |                    |
		//    |          |                    | 
		//    |          |                    | 
		// 100x----------|                    |
		//    |          |                    |       
		//    |          |                    |
		//    |          |                    |
		//    |          |                    |
		// 50 |          |--------------------|
		//    |          |         |
		//    |          |         |
		//    |          |         |
		// 20 |          |         x          x-----|      
		//    |          |         |          |     |
		//    |----------|---------x----------|-----x----------------------
		//               50       100        150   170

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(100);
		assertThat(ep.get(0)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(1).getMinX()).isEqualTo(0);
		assertThat(ep.get(1).getMinY()).isEqualTo(150);
		assertThat(ep.get(1)).isInstanceOf(DefaultXYSupportPoint2D.class);
		assertThat(ep.get(1)).isMax(999, 999);

		assertThat(ep.get(2).getMinX()).isEqualTo(100);
		assertThat(ep.get(2).getMinY()).isEqualTo(0);
		assertThat(ep.get(2)).isInstanceOf(DefaultXYSupportPoint2D.class);
		assertThat(ep.get(2)).isMax(149, 49);

		assertThat(ep.get(3).getMinX()).isEqualTo(100);
		assertThat(ep.get(3).getMinY()).isEqualTo(20);
		assertThat(ep.get(3)).isInstanceOf(DefaultXYSupportPoint2D.class);
		assertThat(ep.get(3)).isMax(999, 49);

		assertThat(ep.get(4).getMinX()).isEqualTo(150);
		assertThat(ep.get(4).getMinY()).isEqualTo(20);
		assertThat(ep.get(4)).isInstanceOf(DefaultXYSupportPoint2D.class);
		assertThat(ep.get(4)).isMax(999, 999);
		assertThat(ep.get(4)).isXSupport(169);
		assertThat(ep.get(4)).isNoYSupport(0);
		assertThat(ep.get(4)).isYSupport(149);

		assertThat(ep.get(5).getMinX()).isEqualTo(170);
		assertThat(ep.get(5).getMinY()).isEqualTo(0);
		assertThat(ep.get(5)).isInstanceOf(DefaultXYSupportPoint2D.class);
		assertThat(ep.get(5)).isMax(999, 999);

		assertThat(ep.getValues()).hasSize(6);

		ep.add(5, createStackPlacement(170, 0, 189, 29));

		//    |           
		//    |           
		// 150x          |--------------------|
		//    |          |                    |
		//    |          |                    | 
		//    |          |                    |
		//    |          |                    | 
		//    |          |                    | 
		// 100x----------|                    |
		//    |          |                    |       
		//    |          |                    |
		//    |          |                    |
		//    |          |                    |
		// 50 |          |--------------------|
		//    |          |         |
		// 30 |          |         x          x     |-----| 
		//    |          |         |                |     |
		// 20 |          |         x          x-----|     |  
		//    |          |         |          |     |     |
		//    |----------|---------x----------|-----|-----x----------------
		//               50       100        150   170

		assertThat(ep.getValues()).hasSize(8);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(100);
		assertThat(ep.get(0)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(1).getMinX()).isEqualTo(0);
		assertThat(ep.get(1).getMinY()).isEqualTo(150);
		assertThat(ep.get(1)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(2).getMinX()).isEqualTo(100);
		assertThat(ep.get(2).getMinY()).isEqualTo(0);
		assertThat(ep.get(2)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(3).getMinX()).isEqualTo(100);
		assertThat(ep.get(3).getMinY()).isEqualTo(20);
		assertThat(ep.get(3)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(4).getMinX()).isEqualTo(100);
		assertThat(ep.get(4).getMinY()).isEqualTo(30);
		assertThat(ep.get(4)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(5).getMinX()).isEqualTo(150);
		assertThat(ep.get(5).getMinY()).isEqualTo(20);
		assertThat(ep.get(5)).isInstanceOf(DefaultXYSupportPoint2D.class);
		assertThat(ep.get(5)).isMax(169, 999);

		assertThat(ep.get(6).getMinX()).isEqualTo(150);
		assertThat(ep.get(6).getMinY()).isEqualTo(30);
		assertThat(ep.get(6)).isInstanceOf(DefaultXYSupportPoint2D.class);
		assertThat(ep.get(6)).isMax(999, 999);

		assertThat(ep.get(7).getMinX()).isEqualTo(190);
		assertThat(ep.get(7).getMinY()).isEqualTo(0);
		assertThat(ep.get(7)).isInstanceOf(DefaultXYSupportPoint2D.class);
		assertThat(ep.get(7)).isMax(999, 999);

		ep.add(6, createStackPlacement(150, 30, 189, 69));

		//    |           
		//    |           
		// 150x          |--------------------|
		//    |          |                    |
		//    |          |                    | 
		//    |          |                    |
		//    |          |                    | 
		//    |          |                    | 
		// 100x----------|                    |
		//    |          |                    |       
		//    |          |                    |
		//    |          |                    x-----------|
		//    |          |                    |           |
		//    |          |                    |           | 
		// 50 |          |--------------------|           |
		//    |          |         |          |           |
		//    |          |         |          |-----------|
		//    |          |         |                |     |
		// 20 |          |         x          |-----|     |  
		//    |          |         |          |     |     |
		//    |----------|---------x----------|-----|-----x----------------
		//               50       100        150   170

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(100);

		assertThat(ep.get(1).getMinX()).isEqualTo(0);
		assertThat(ep.get(1).getMinY()).isEqualTo(150);

		assertThat(ep.get(2).getMinX()).isEqualTo(100);
		assertThat(ep.get(2).getMinY()).isEqualTo(0);

		assertThat(ep.get(3).getMinX()).isEqualTo(100);
		assertThat(ep.get(3).getMinY()).isEqualTo(20);

		assertThat(ep.get(4).getMinX()).isEqualTo(150);
		assertThat(ep.get(4).getMinY()).isEqualTo(70);

		assertThat(ep.get(5).getMinX()).isEqualTo(190);
		assertThat(ep.get(5).getMinY()).isEqualTo(0);

		assertThat(ep.getValues()).hasSize(6);

		ep.add(3, createStackPlacement(100, 20, 139, 24));

		//    |           
		//    |           
		// 150x          |--------------------|
		//    |          |                    |
		//    |          |                    | 
		//    |          |                    |
		//    |          |                    | 
		//    |          |                    | 
		// 100x----------|                    |
		//    |          |                    |       
		//    |          |                    |
		//    |          |                    x-----------|
		//    |          |                    |           |
		//    |          |                    |           | 
		// 50 |          |--------------------|           |
		//    |          |         |          |           |
		//    |          |         |          |-----------|
		//    |          |         x-------|        |     |
		// 20 |          |         |-------x  |-----|     |  
		//    |          |         |          |     |     |
		//    |----------|---------x-------x--|-----|-----x----------------
		//               50       100        150   170

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(100);
		assertThat(ep.get(0)).isSupport(49, 999);

		assertThat(ep.get(1).getMinX()).isEqualTo(0);
		assertThat(ep.get(1).getMinY()).isEqualTo(150);

		assertThat(ep.get(2).getMinX()).isEqualTo(100);
		assertThat(ep.get(2).getMinY()).isEqualTo(0);
		assertThat(ep.get(2)).isSupport(999, 19);

		// split point
		assertThat(ep.get(3).getMinX()).isEqualTo(100);
		assertThat(ep.get(3).getMinY()).isEqualTo(25);
		assertThat(ep.get(3)).isMax(169, 29);
		assertThat(ep.get(3)).isSupport(139, 49);

		assertThat(ep.get(4).getMinX()).isEqualTo(100);
		assertThat(ep.get(4).getMinY()).isEqualTo(25);
		assertThat(ep.get(4)).isMax(149, 49);
		assertThat(ep.get(4)).isSupport(139, 49);

		assertThat(ep.get(5).getMinX()).isEqualTo(140);
		assertThat(ep.get(5).getMinY()).isEqualTo(0);
		assertThat(ep.get(5)).isMax(149, 49);

		assertThat(ep.get(6).getMinX()).isEqualTo(140);
		assertThat(ep.get(6).getMinY()).isEqualTo(20);
		assertThat(ep.get(6)).isMax(169, 29);

		assertThat(ep.get(7).getMinX()).isEqualTo(150);
		assertThat(ep.get(7).getMinY()).isEqualTo(70);
		assertThat(ep.get(7)).isMax(999, 999);

		assertThat(ep.get(8).getMinX()).isEqualTo(190);
		assertThat(ep.get(8).getMinY()).isEqualTo(0);
		assertThat(ep.get(8)).isMax(999, 999);

		assertThat(ep.getValues()).hasSize(9);

	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	public void testFloatingY(boolean b) {
		ExtremePoints2D ep = new ExtremePoints2D(b);
		ep.clearToSize(1000, 1000, 100);
		ep.add(0, createStackPlacement(0, 0, 49, 99));
		assertThat(ep.get(0)).isInstanceOf(DefaultXYSupportPoint2D.class);

		ep.add(1, createStackPlacement(50, 0, 99, 49));
		assertThat(ep.get(0)).isInstanceOf(DefaultXYSupportPoint2D.class);
		assertThat(ep.get(1)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(100);

		assertThat(ep.get(1).getMinX()).isEqualTo(50);
		assertThat(ep.get(1).getMinY()).isEqualTo(50);

		assertThat(ep.get(2).getMinX()).isEqualTo(100);
		assertThat(ep.get(2).getMinY()).isEqualTo(0);

		for (Point point : ep.getValues()) {
			assertThat(point.getMaxX()).isEqualTo(999);
			assertThat(point.getMaxY()).isEqualTo(999);
		}

		ep.add(1, createStackPlacement(50, 50, 149, 149));

		//    |           
		//    |           
		// 150x          |--------------------|
		//    |          |                    |
		//    |          |                    | 
		//    |          |                    |
		//    |          |                    | 
		//    |          |                    | 
		// 100x----------|                    |
		//    |          |                    |       
		//    |          |                    |
		//    |          |                    |
		//    |          |                    |
		// 50 |          |--------------------|
		//    |          |         |
		//    |          |         |
		//    |          |         |
		//    |          |         |
		//    |          |         |       
		//    |----------|---------x----------x----------
		//               50       100        150
		assertThat(ep.getValues()).hasSize(4);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(100);
		assertThat(ep.get(0)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(1).getMinX()).isEqualTo(0);
		assertThat(ep.get(1).getMinY()).isEqualTo(150);
		assertThat(ep.get(1)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(2).getMinX()).isEqualTo(100);
		assertThat(ep.get(2).getMinY()).isEqualTo(0);
		assertThat(ep.get(2)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(3).getMinX()).isEqualTo(150);
		assertThat(ep.get(3).getMinY()).isEqualTo(0);
		assertThat(ep.get(3)).isInstanceOf(DefaultXYSupportPoint2D.class);

		ep.add(1, createStackPlacement(0, 150, 19, 169));

		//    |           
		//    |           
		// 170x---|       
		//    |   |       
		// 150|---x      |--------------------|
		//    |          |                    |
		//    |          |                    | 
		//    |          |                    |
		//    |          |                    | 
		//    |          |                    | 
		// 100x---x------|                    |
		//    |          |                    |       
		//    |          |                    |
		//    |          |                    |
		//    |          |                    |
		// 50 |          |--------------------|
		//    |          |         |
		//    |          |         |
		//    |          |         |
		//    |          |         |
		//    |          |         |       
		//    |----------|---------x----------x----------
		//               50       100        150

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(100);
		assertThat(ep.get(0)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(1).getMinX()).isEqualTo(0);
		assertThat(ep.get(1).getMinY()).isEqualTo(170);
		assertThat(ep.get(1)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(2).getMinX()).isEqualTo(20);
		assertThat(ep.get(2).getMinY()).isEqualTo(100);
		assertThat(ep.get(2)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(3).getMinX()).isEqualTo(20);
		assertThat(ep.get(3).getMinY()).isEqualTo(150);
		assertThat(ep.get(3)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(4).getMinX()).isEqualTo(100);
		assertThat(ep.get(4).getMinY()).isEqualTo(0);
		assertThat(ep.get(4)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(5).getMinX()).isEqualTo(150);
		assertThat(ep.get(5).getMinY()).isEqualTo(0);
		assertThat(ep.get(5)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.getValues()).hasSize(6);

		ep.add(1, createStackPlacement(0, 170, 29, 189));

		//    |           
		//    |           
		// 190x-------|           
		//    |       |    
		// 170|---|---|       
		//    |   |       
		// 150|---x   x  |--------------------|
		//    |          |                    |
		//    |          |                    | 
		//    |          |                    |
		//    |          |                    | 
		//    |          |                    | 
		// 100x---x---x--|                    |
		//    |          |                    |       
		//    |          |                    |
		//    |          |                    |
		//    |          |                    |
		// 50 |          |--------------------|
		//    |          |         |
		//    |          |         |
		//    |          |         |
		//    |          |         |
		//    |          |         |       
		//    |----------|---------x----------x----------
		//               50       100        150

		assertThat(ep.getValues()).hasSize(8);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(100);
		assertThat(ep.get(0)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(1).getMinX()).isEqualTo(0);
		assertThat(ep.get(1).getMinY()).isEqualTo(190);
		assertThat(ep.get(1)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(2).getMinX()).isEqualTo(20);
		assertThat(ep.get(2).getMinY()).isEqualTo(100);
		assertThat(ep.get(2)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(3).getMinX()).isEqualTo(20);
		assertThat(ep.get(3).getMinY()).isEqualTo(150);
		assertThat(ep.get(3)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(4).getMinX()).isEqualTo(30);
		assertThat(ep.get(4).getMinY()).isEqualTo(100);
		assertThat(ep.get(4)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(5).getMinX()).isEqualTo(30);
		assertThat(ep.get(5).getMinY()).isEqualTo(150);
		assertThat(ep.get(5)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(6).getMinX()).isEqualTo(100);
		assertThat(ep.get(6).getMinY()).isEqualTo(0);
		assertThat(ep.get(6)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(7).getMinX()).isEqualTo(150);
		assertThat(ep.get(7).getMinY()).isEqualTo(0);
		assertThat(ep.get(7)).isInstanceOf(DefaultXYSupportPoint2D.class);

		ep.add(5, createStackPlacement(30, 150, 69, 189));

		//    |           
		//    |           
		// 190x---------|--------|           
		//    |         |        |
		// 170|---|-----|        |
		//    |   |     |        |
		// 150|---|     |--|-----x--------------|
		//    |            |                    |
		//    |            |                    | 
		//    |            |                    |
		//    |            |                    | 
		//    |            |                    | 
		// 100x---x--------|                    |
		//    |            |                    |       
		//    |            |                    |
		//    |            |                    |
		//    |            |                    |
		// 50 |            |--------------------|
		//    |            |         |
		//    |            |         |
		//    |            |         |
		//    |            |         |
		//    |            |         |       
		//    |------------|---------x----------x----------
		//               50       100        150

		assertThat(ep.getValues()).hasSize(6);

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(100);
		assertThat(ep.get(0)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(1).getMinX()).isEqualTo(0);
		assertThat(ep.get(1).getMinY()).isEqualTo(190);
		assertThat(ep.get(1)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(2).getMinX()).isEqualTo(20);
		assertThat(ep.get(2).getMinY()).isEqualTo(100);

		assertThat(ep.get(3).getMinX()).isEqualTo(70);
		assertThat(ep.get(3).getMinY()).isEqualTo(150);
		assertThat(ep.get(3)).isMaxYSupport(189);

		assertThat(ep.get(4).getMinX()).isEqualTo(100);
		assertThat(ep.get(4).getMinY()).isEqualTo(0);
		assertThat(ep.get(4)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.get(5).getMinX()).isEqualTo(150);
		assertThat(ep.get(5).getMinY()).isEqualTo(0);
		assertThat(ep.get(5)).isInstanceOf(DefaultXYSupportPoint2D.class);

		ep.add(2, createStackPlacement(20, 100, 24, 139));

		//    |           
		//    |           
		// 190x---------|--------|           
		//    |         |        |
		// 170|---|-----|        |
		//    |   |     |        |
		// 150|---|     |--|-----x--------------|
		//    |            |                    |
		// 140x   x--|     |                    | 
		//    |   |  |     |                    |
		//    |   |  |     |                    | 
		//    |   |  |     |                    | 
		// 100x---|--x-----|                    |
		//    |            |                    |       
		//    |            |                    |
		//    |            |                    |
		//    |            |                    |
		// 50 |            |--------------------|
		//    |            |         |
		//    |            |         |
		//    |            |         |
		//    |            |         |
		//    |            |         |       
		//    |------------|---------x----------x----------
		//               50       100        150

		assertThat(ep.get(0).getMinX()).isEqualTo(0);
		assertThat(ep.get(0).getMinY()).isEqualTo(100);
		assertThat(ep.get(0)).isMax(19, 149);

		assertThat(ep.get(1).getMinX()).isEqualTo(0);
		assertThat(ep.get(1).getMinY()).isEqualTo(140);

		assertThat(ep.get(2).getMinX()).isEqualTo(0);
		assertThat(ep.get(2).getMinY()).isEqualTo(190);

		// split point
		assertThat(ep.get(3).getMinX()).isEqualTo(20);
		assertThat(ep.get(3).getMinY()).isEqualTo(140);

		assertThat(ep.get(4).getMinX()).isEqualTo(25);
		assertThat(ep.get(4).getMinY()).isEqualTo(100);

		assertThat(ep.get(5).getMinX()).isEqualTo(25);
		assertThat(ep.get(5).getMinY()).isEqualTo(100);

		assertThat(ep.get(6).getMinX()).isEqualTo(70);
		assertThat(ep.get(6).getMinY()).isEqualTo(150);
		assertThat(ep.get(6)).isMaxYSupport(189);

		assertThat(ep.get(7).getMinX()).isEqualTo(100);
		assertThat(ep.get(7).getMinY()).isEqualTo(0);
		assertThat(ep.get(7)).isSupport(999, 49);

		assertThat(ep.get(8).getMinX()).isEqualTo(150);
		assertThat(ep.get(8).getMinY()).isEqualTo(0);
		assertThat(ep.get(8)).isInstanceOf(DefaultXYSupportPoint2D.class);
		assertThat(ep.get(8)).isMaxXSupport(999);

		assertThat(ep.getValues()).hasSize(9);

	}

}
