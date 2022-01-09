package com.github.skjolber.packing.points2d;

import static com.github.skjolber.packing.test.assertj.Point2DAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.ep.Point2D;
import com.github.skjolber.packing.ep.points2d.DefaultPlacement2D;
import com.github.skjolber.packing.ep.points2d.DefaultPoint2D;
import com.github.skjolber.packing.ep.points2d.DefaultXSupportPoint2D;
import com.github.skjolber.packing.ep.points2d.DefaultXYSupportPoint2D;
import com.github.skjolber.packing.ep.points2d.DefaultYSupportPoint2D;
import com.github.skjolber.packing.ep.points2d.ExtremePoints2D;
import com.github.skjolber.packing.points.DefaultExtremePoints2D;

public class ExtremePoints2DTest {

	@Test
	public void testSinglePoint() {
		ExtremePoints2D<DefaultPlacement2D> ep = new ExtremePoints2D<>(100, 100);
		ep.add(0, new DefaultPlacement2D(0, 0, 10, 10));
		assertThat(ep.getValues()).hasSize(2);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(11);
		assertThat(ep.getValue(0)).isMax(99, 99);
		assertThat(ep.getValue(0)).isXSupport(10);
		assertThat(ep.getValue(0)).isYSupport(99);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(11);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(1)).isMax(99, 99);
		assertThat(ep.getValue(1)).isXSupport(99);
		assertThat(ep.getValue(1)).isYSupport(10);
	}
	
	@Test
	public void testSinglePointCornerCase() {
		ExtremePoints2D<DefaultPlacement2D> ep = new ExtremePoints2D<>(100, 100);
		ep.add(0, new DefaultPlacement2D(0, 0, 0, 0));
		assertThat(ep.getValues()).hasSize(2);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(1);
		assertThat(ep.getValue(0)).isMax(99, 99);
		assertThat(ep.getValue(0)).isXSupport(0);
		assertThat(ep.getValue(0)).isYSupport(99);
		
		assertThat(ep.getValue(1).getMinX()).isEqualTo(1);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(1)).isMax(99, 99);
		assertThat(ep.getValue(1)).isXSupport(99);
		assertThat(ep.getValue(1)).isYSupport(0);
	}
	
	@Test
	public void testSinglePointCoveringAllX() {
		ExtremePoints2D<DefaultPlacement2D> ep = new ExtremePoints2D<>(100, 100);
		ep.add(0, new DefaultPlacement2D(0, 0, 99, 10));
		assertThat(ep.getValues()).hasSize(1);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(11);
	}
	
	@Test
	public void testSinglePointCoveringAllY() {
		ExtremePoints2D<DefaultPlacement2D> ep = new ExtremePoints2D<>(100, 100);
		ep.add(0, new DefaultPlacement2D(0, 0, 10, 99));
		assertThat(ep.getValues()).hasSize(1);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(11);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(0);
	}
	
	@Test
	public void testSinglePointCoveringWholeContainer() {
		ExtremePoints2D<DefaultPlacement2D> ep = new ExtremePoints2D<>(100, 100);
		ep.add(0, new DefaultPlacement2D(0, 0, 99, 99));
		assertThat(ep.getValues()).hasSize(0);
	}
	
	@Test
	public void testStackInXDirection() {
		ExtremePoints2D<DefaultPlacement2D> ep = new ExtremePoints2D<>(100, 100);
		ep.add(0, new DefaultPlacement2D(0, 0, 9, 49));

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(50);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		
		ep.add(1, new DefaultPlacement2D(10, 0, 19, 24));
		
		assertThat(ep.getValues()).hasSize(3);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(50);
		assertThat(ep.getValue(0)).isMax(99, 99);
		assertThat(ep.getValue(0)).isXSupport(9);
		assertThat(ep.getValue(0)).isYSupport(99);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(25);
		assertThat(ep.getValue(1)).isMax(99, 99);
		assertThat(ep.getValue(1)).isXSupport(19);
		assertThat(ep.getValue(1)).isYSupport(49);

		assertThat(ep.getValue(2).getMinX()).isEqualTo(20);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);		
		assertThat(ep.getValue(2)).isMax(99, 99);
		assertThat(ep.getValue(2)).isXSupport(99);
		assertThat(ep.getValue(2)).isYSupport(24);

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

		ExtremePoints2D<DefaultPlacement2D> ep = new ExtremePoints2D<>(100, 100);
		ep.add(0, new DefaultPlacement2D(0, 0, 9, 49));

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(50);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		
		ep.add(0, new DefaultPlacement2D(0, 50, 4, 74));
		
		assertThat(ep.getValues()).hasSize(3);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(75);
		assertThat(ep.getValue(0)).isMax(99, 99);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(5);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(50);
		assertThat(ep.getValue(1)).isMax(99, 99);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(2)).isMax(99, 99);
		

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
		
		DefaultExtremePoints2D ep = new DefaultExtremePoints2D(100, 100);
		ep.add(0, new DefaultPlacement2D(0, 0, 9, 9));

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(10);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		
		ep.add(1, new DefaultPlacement2D(10, 0, 19, 9));

		assertThat(ep.getValues()).hasSize(2);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(10);
		assertThat(ep.getValue(0)).isMax(99, 99);
		assertThat(ep.getValue(0)).isXSupport(9); // XXX support is not extended to 19
		assertThat(ep.getValue(0)).isYSupport(99);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(20);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(1)).isMax(99, 99);
		assertThat(ep.getValue(1)).isXSupport(99);
		assertThat(ep.getValue(1)).isYSupport(9);

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

		ExtremePoints2D<DefaultPlacement2D> ep = new ExtremePoints2D<>(100, 100);
		ep.add(0, new DefaultPlacement2D(0, 0, 9, 9));

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(10);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		
		ep.add(0, new DefaultPlacement2D(0, 10, 9, 19));
		
		assertThat(ep.getValues()).hasSize(2);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(20);
		assertThat(ep.getValue(0)).isMax(99, 99);
		assertThat(ep.getValue(0)).isXSupport(9);
		assertThat(ep.getValue(0)).isYSupport(99);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(1)).isMax(99, 99);
		assertThat(ep.getValue(1)).isXSupport(99);
		assertThat(ep.getValue(1)).isYSupport(9); // XXX support is not extended to 19
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
		
		
		DefaultExtremePoints2D ep = new DefaultExtremePoints2D(100, 100);
		ep.add(0, new DefaultPlacement2D(0, 0, 9, 9));

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(10);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		
		ep.add(1, new DefaultPlacement2D(10, 0, 19, 19));

		assertThat(ep.getValues()).hasSize(3);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(10);
		assertThat(ep.getValue(0).getMaxY()).isEqualTo(99);
		assertThat(ep.getValue(0).getMaxX()).isEqualTo(9);
		assertThat(ep.getValue(0)).isXSupport(9);
		assertThat(ep.getValue(0)).isYSupport(99);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(20);
		assertThat(ep.getValue(1)).isYSupport(99);
		assertThat(ep.getValue(1)).isNoXSupport();
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(20);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(2)).isXSupport(99);
		assertThat(ep.getValue(2)).isYSupport(19);
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
		
		ExtremePoints2D<DefaultPlacement2D> ep = new ExtremePoints2D<>(100, 100);
		ep.add(0, new DefaultPlacement2D(0, 0, 9, 9));

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(10);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		
		ep.add(0, new DefaultPlacement2D(0, 10, 19, 19));
		
		assertThat(ep.getValues()).hasSize(3);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(20);
		assertThat(ep.getValue(0)).isXSupport(19);
		assertThat(ep.getValue(0)).isYSupport(99);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(1).getMaxX()).isEqualTo(99);
		assertThat(ep.getValue(1).getMaxY()).isEqualTo(9);
		assertThat(ep.getValue(1)).isXSupport(99);
		assertThat(ep.getValue(1)).isYSupport(9);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(20);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(2)).isNoYSupport();
		assertThat(ep.getValue(2)).isXSupport(99);
		assertThat(ep.getValue(2)).isMax(99, 99);
	}		
	
	@Test
	public void testSwallowedInXDirection1() throws InterruptedException {
		DefaultExtremePoints2D ep = new DefaultExtremePoints2D(100, 100);
		ep.add(0, new DefaultPlacement2D(0, 0, 9, 9));
		ep.add(1, new DefaultPlacement2D(10, 0, 19, 19));

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
		

		ep.add(0, new DefaultPlacement2D(0, 10, 4, 24));

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

		assertThat(ep.getValue(0)).isMin(0, 25);
		assertThat(ep.getValue(0).getMaxX()).isEqualTo(99);
		assertThat(ep.getValue(0).getMaxY()).isEqualTo(99);
		assertThat(ep.getValue(0)).isXSupport(4);
		assertThat(ep.getValue(0)).isYSupport(99);

		assertThat(ep.getValue(1)).isMin(5, 10);
		assertThat(ep.getValue(1).getMaxX()).isEqualTo(9);
		assertThat(ep.getValue(1).getMaxY()).isEqualTo(99);
		assertThat(ep.getValue(1)).isXSupport(9);
		assertThat(ep.getValue(1)).isYSupport(24);

		assertThat(ep.getValue(2)).isMin(5, 20);
		assertThat(ep.getValue(2).getMaxX()).isEqualTo(99);
		assertThat(ep.getValue(2).getMaxY()).isEqualTo(99);
		
		assertThat(ep.getValue(3)).isMin(20, 0);
		assertThat(ep.getValue(3).getMaxX()).isEqualTo(99);
		assertThat(ep.getValue(3).getMaxY()).isEqualTo(99);


		assertThat(ep.getValues()).hasSize(4);
	}
	
	@Test
	public void testSwallowedInXDirection2() throws InterruptedException {
		DefaultExtremePoints2D ep = new DefaultExtremePoints2D(100, 100);
		ep.add(0, new DefaultPlacement2D(0, 0, 9, 9));
		ep.add(1, new DefaultPlacement2D(10, 0, 19, 19));

		
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
		List<Point2D<Placement2D>> values = ep.getValues();
		assertThat(values).hasSize(3);
		
		assertThat(values.get(0).getMinX()).isEqualTo(0);
		assertThat(values.get(0).getMinY()).isEqualTo(10);
		assertThat(values.get(1).getMinX()).isEqualTo(0);
		assertThat(values.get(1).getMinY()).isEqualTo(20);
		assertThat(values.get(2).getMinX()).isEqualTo(20);
		assertThat(values.get(2).getMinY()).isEqualTo(0);

		ep.add(2, new DefaultPlacement2D(20, 0, 24, 24));

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
		
		assertThat(values).hasSize(4);
		
		assertThat(values.get(0).getMinX()).isEqualTo(0);
		assertThat(values.get(0).getMinY()).isEqualTo(10);
		assertThat(ep.getValue(0)).isMax(9, 99);
		
		assertThat(values.get(1).getMinX()).isEqualTo(0);
		assertThat(values.get(1).getMinY()).isEqualTo(20);
		assertThat(ep.getValue(1)).isMax(19, 99);
		assertThat(ep.getValue(1)).isNoXSupport();
		assertThat(ep.getValue(1)).isYSupport(99);

		assertThat(values.get(2).getMinX()).isEqualTo(0);
		assertThat(values.get(2).getMinY()).isEqualTo(25);
		assertThat(ep.getValue(2)).isMax(99, 99);
		assertThat(ep.getValue(2)).isNoXSupport();
		assertThat(ep.getValue(2)).isYSupport(99);

		assertThat(values.get(3).getMinX()).isEqualTo(25);
		assertThat(values.get(3).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(3)).isMax(99, 99);

		ep.add(0, new DefaultPlacement2D(0, 10, 4, 29));

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
		
		assertThat(values).hasSize(5);

		assertThat(values.get(0).getMinX()).isEqualTo(0);
		assertThat(values.get(0).getMinY()).isEqualTo(30);
		assertThat(values.get(0)).isMax(99, 99);
		
		assertThat(values.get(1).getMinX()).isEqualTo(5);
		assertThat(values.get(1).getMinY()).isEqualTo(10);
		assertThat(ep.getValue(1)).isXSupportAt(9);
		assertThat(ep.getValue(1)).isYSupport(29);

		assertThat(values.get(2).getMinX()).isEqualTo(5);
		assertThat(values.get(2).getMinY()).isEqualTo(20);
		assertThat(ep.getValue(2)).isNoXSupport();
		assertThat(ep.getValue(2)).isYSupport(29);
		
		assertThat(values.get(3).getMinX()).isEqualTo(5);
		assertThat(values.get(3).getMinY()).isEqualTo(25);
		assertThat(values.get(3)).isMax(99, 99);
		assertThat(ep.getValue(3)).isNoXSupport();
		assertThat(ep.getValue(3)).isYSupport(29);
		
		assertThat(values.get(4)).isMin(25, 0);
		assertThat(values.get(4)).isMax(99, 99);

	}
	
	@Test
	public void testSwallowedInYDirection1() throws InterruptedException {
		ExtremePoints2D<DefaultPlacement2D> ep = new ExtremePoints2D<>(100, 100);
		ep.add(0, new DefaultPlacement2D(0, 0, 9, 9));
		ep.add(0, new DefaultPlacement2D(0, 10, 19, 19));

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
		
		List<Point2D<DefaultPlacement2D>> values = ep.getValues();
		assertThat(values).hasSize(3);

		assertThat(values.get(0).getMinX()).isEqualTo(0);
		assertThat(values.get(0).getMinY()).isEqualTo(20);
		assertThat(ep.getValue(0)).isXSupport(19);
		assertThat(ep.getValue(0)).isYSupport(99);

		assertThat(values.get(1).getMinX()).isEqualTo(10);
		assertThat(values.get(1).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(1)).isXSupport(99);
		assertThat(ep.getValue(1)).isYSupport(9);
		
		assertThat(values.get(2).getMinX()).isEqualTo(20);
		assertThat(values.get(2).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(2)).isXSupport(99);
		assertThat(ep.getValue(2)).isNoYSupport();

		ep.add(1, new DefaultPlacement2D(10, 0, 24, 4));
		
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

		assertThat(values.get(0).getMinX()).isEqualTo(0);
		assertThat(values.get(0).getMinY()).isEqualTo(20);
		assertThat(ep.getValue(0)).isMax(99, 99);
		assertThat(ep.getValue(0)).isXSupport(19);
		assertThat(ep.getValue(0)).isYSupport(99);

		assertThat(values.get(1).getMinX()).isEqualTo(10);
		assertThat(values.get(1).getMinY()).isEqualTo(5);
		assertThat(ep.getValue(1)).isMax(99, 9);
		assertThat(ep.getValue(1)).isXSupportAt(24);
		assertThat(ep.getValue(1)).isYSupport(9);

		assertThat(values.get(2).getMinX()).isEqualTo(20);
		assertThat(values.get(2).getMinY()).isEqualTo(5);
		assertThat(ep.getValue(2)).isMax(99, 99);
		assertThat(ep.getValue(2)).isXSupportAt(24);
		assertThat(ep.getValue(2)).isNoYSupport();
		
		assertThat(values.get(3).getMinX()).isEqualTo(25);
		assertThat(values.get(3).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(3)).isMax(99, 99);
		assertThat(ep.getValue(3)).isXSupport(99);
		assertThat(ep.getValue(3)).isYSupport(4);

		assertThat(ep.getValues()).hasSize(4);
	}
	
	@Test
	public void testSwallowedInYDirection2() throws InterruptedException {
		ExtremePoints2D<DefaultPlacement2D> ep = new ExtremePoints2D<>(100, 100);
		ep.add(0, new DefaultPlacement2D(0, 0, 9, 9));
		ep.add(0, new DefaultPlacement2D(0, 10, 19, 19));

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

		List<Point2D<DefaultPlacement2D>> values = ep.getValues();
		assertThat(values).hasSize(3);

		assertThat(values.get(0).getMinX()).isEqualTo(0);
		assertThat(values.get(0).getMinY()).isEqualTo(20);
		assertThat(values.get(1).getMinX()).isEqualTo(10);
		assertThat(values.get(1).getMinY()).isEqualTo(0);
		assertThat(values.get(2).getMinX()).isEqualTo(20);
		assertThat(values.get(2).getMinY()).isEqualTo(0);


		ep.add(0, new DefaultPlacement2D(0, 20, 24, 24));

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
		assertThat(values.get(0)).isMin(0, 25);
		assertThat(values.get(0)).isMax(99, 99);
		assertThat(values.get(0)).isSupport(24, 99);
		
		assertThat(values.get(1).getMinX()).isEqualTo(10);
		assertThat(values.get(1).getMinY()).isEqualTo(0);
		assertThat(values.get(1)).isSupport(99, 9);
		
		assertThat(values.get(2).getMinX()).isEqualTo(20);
		assertThat(values.get(2).getMinY()).isEqualTo(0);
		assertThat(values.get(2)).isXSupport(99);
		assertThat(values.get(2)).isNoYSupport();
		
		assertThat(values.get(3).getMinX()).isEqualTo(25);
		assertThat(values.get(3).getMinY()).isEqualTo(0);
		assertThat(values.get(3)).isMax(99, 99);
		assertThat(values.get(3)).isXSupport(99);
		assertThat(values.get(3)).isNoYSupport();
		
		ep.add(1, new DefaultPlacement2D(10, 0, 29, 4));
		
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
		
		assertThat(values.get(0).getMinX()).isEqualTo(0);
		assertThat(values.get(0).getMinY()).isEqualTo(25);
		assertThat(values.get(1).getMinX()).isEqualTo(10);
		assertThat(values.get(1).getMinY()).isEqualTo(5);
		assertThat(values.get(2).getMinX()).isEqualTo(20);
		assertThat(values.get(2).getMinY()).isEqualTo(5);
		assertThat(values.get(3).getMinX()).isEqualTo(25);
		assertThat(values.get(3).getMinY()).isEqualTo(5);		
		assertThat(values.get(4).getMinX()).isEqualTo(30);
		assertThat(values.get(4).getMinY()).isEqualTo(0);		
	}
	
	@Test
	public void testStacking() {
		ExtremePoints2D<DefaultPlacement2D> ep = new ExtremePoints2D<>(3, 2);
		ep.add(0, new DefaultPlacement2D(0, 0, 0, 1));
		
		assertThat(ep.getValues()).hasSize(1);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(1);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(0);

		ep.add(0, new DefaultPlacement2D(1, 0, 1, 1));
		
		assertThat(ep.getValues()).hasSize(1);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(2);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(0);

		ep.add(0, new DefaultPlacement2D(2, 0, 2, 1));
		
		assertThat(ep.getValues()).hasSize(0);
	}
	
	@Test
	public void testFloatingX() {
		ExtremePoints2D<DefaultPlacement2D> ep = new ExtremePoints2D<>(1000, 1000);
		ep.add(0, new DefaultPlacement2D(0, 0, 49, 99));
		assertThat(ep.getValue(0)).isInstanceOf(DefaultXYSupportPoint2D.class);
				
		ep.add(1, new DefaultPlacement2D(50, 0, 99, 49));
		assertThat(ep.getValue(0)).isInstanceOf(DefaultXYSupportPoint2D.class);
		assertThat(ep.getValue(1)).isInstanceOf(DefaultXYSupportPoint2D.class);
		
		List<Point2D<DefaultPlacement2D>> values = ep.getValues();
		assertThat(values).hasSize(3);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(100);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(50);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(50);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);

		for(Point2D point : values) {
			assertThat(point.getMaxX()).isEqualTo(999);
			assertThat(point.getMaxY()).isEqualTo(999);
		}
		
		ep.add(1, new DefaultPlacement2D(50, 50, 149, 149));

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
		assertThat(values).hasSize(4);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(100);
		assertThat(ep.getValue(0)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(150);
		assertThat(ep.getValue(1)).isInstanceOf(DefaultYSupportPoint2D.class);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(2)).isInstanceOf(DefaultXYSupportPoint2D.class);
		
		assertThat(ep.getValue(3).getMinX()).isEqualTo(150);
		assertThat(ep.getValue(3).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(3)).isInstanceOf(DefaultXSupportPoint2D.class);
		
		ep.add(3, new DefaultPlacement2D(150, 0, 169, 19));

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
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(100);
		assertThat(ep.getValue(0)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(150);
		assertThat(ep.getValue(1)).isInstanceOf(DefaultYSupportPoint2D.class);
		assertThat(ep.getValue(1)).isMax(999, 999);

		assertThat(ep.getValue(2).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(2)).isInstanceOf(DefaultXYSupportPoint2D.class);
		assertThat(ep.getValue(2)).isMax(149, 49);

		assertThat(ep.getValue(3).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(3).getMinY()).isEqualTo(20);
		assertThat(ep.getValue(3)).isInstanceOf(DefaultYSupportPoint2D.class);
		assertThat(ep.getValue(3)).isMax(999, 49);

		assertThat(ep.getValue(4).getMinX()).isEqualTo(150);
		assertThat(ep.getValue(4).getMinY()).isEqualTo(20);
		assertThat(ep.getValue(4)).isInstanceOf(DefaultXSupportPoint2D.class);
		assertThat(ep.getValue(4)).isMax(999, 999);
		assertThat(ep.getValue(4)).isXSupportAt(169);
		assertThat(ep.getValue(4)).isNoYSupport();

		assertThat(ep.getValue(5).getMinX()).isEqualTo(170);
		assertThat(ep.getValue(5).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(5)).isInstanceOf(DefaultXYSupportPoint2D.class);
		assertThat(ep.getValue(5)).isMax(999, 999);

		assertThat(values).hasSize(6);

		ep.add(5, new DefaultPlacement2D(170, 0, 189, 29));

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
		
		assertThat(values).hasSize(8);

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(100);
		assertThat(ep.getValue(0)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(150);
		assertThat(ep.getValue(1)).isInstanceOf(DefaultYSupportPoint2D.class);

		assertThat(ep.getValue(2).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(2)).isInstanceOf(DefaultXYSupportPoint2D.class);
		
		assertThat(ep.getValue(3).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(3).getMinY()).isEqualTo(20);
		assertThat(ep.getValue(3)).isInstanceOf(DefaultYSupportPoint2D.class);

		assertThat(ep.getValue(4).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(4).getMinY()).isEqualTo(30);
		assertThat(ep.getValue(4)).isInstanceOf(DefaultYSupportPoint2D.class);

		assertThat(ep.getValue(5).getMinX()).isEqualTo(150);
		assertThat(ep.getValue(5).getMinY()).isEqualTo(20);
		assertThat(ep.getValue(5)).isInstanceOf(DefaultXSupportPoint2D.class);
		assertThat(ep.getValue(5)).isMax(169, 999);

		assertThat(ep.getValue(6).getMinX()).isEqualTo(150);
		assertThat(ep.getValue(6).getMinY()).isEqualTo(30);
		assertThat(ep.getValue(6)).isInstanceOf(DefaultPoint2D.class);
		assertThat(ep.getValue(6)).isMax(999, 999);

		assertThat(ep.getValue(7).getMinX()).isEqualTo(190);
		assertThat(ep.getValue(7).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(7)).isInstanceOf(DefaultXYSupportPoint2D.class);
		assertThat(ep.getValue(7)).isMax(999, 999);

		ep.add(6, new DefaultPlacement2D(150, 30, 189, 69));

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

		assertThat(values).hasSize(6);

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(100);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(150);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
		
		assertThat(ep.getValue(3).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(3).getMinY()).isEqualTo(20);

		assertThat(ep.getValue(4).getMinX()).isEqualTo(150);
		assertThat(ep.getValue(4).getMinY()).isEqualTo(70);

		assertThat(ep.getValue(5).getMinX()).isEqualTo(190);
		assertThat(ep.getValue(5).getMinY()).isEqualTo(0);
		
		ep.add(3, new DefaultPlacement2D(100, 20, 139, 24));

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
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(100);
		assertThat(ep.getValue(0)).isSupport(49, 999);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(150);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(2)).isSupport(999, 19);
		
		// split point
		assertThat(ep.getValue(3).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(3).getMinY()).isEqualTo(25);
		assertThat(ep.getValue(3)).isMax(169, 29);
		assertThat(ep.getValue(3)).isSupport(139, 49);

		assertThat(ep.getValue(4).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(4).getMinY()).isEqualTo(25);
		assertThat(ep.getValue(4)).isMax(149, 49);
		assertThat(ep.getValue(4)).isSupport(139, 49);

		assertThat(ep.getValue(5).getMinX()).isEqualTo(140);
		assertThat(ep.getValue(5).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(5)).isMax(149, 49);
		
		assertThat(ep.getValue(6).getMinX()).isEqualTo(140);
		assertThat(ep.getValue(6).getMinY()).isEqualTo(20);
		assertThat(ep.getValue(6)).isMax(169, 29);

		assertThat(ep.getValue(7).getMinX()).isEqualTo(150);
		assertThat(ep.getValue(7).getMinY()).isEqualTo(70);
		assertThat(ep.getValue(7)).isMax(999, 999);

		assertThat(ep.getValue(8).getMinX()).isEqualTo(190);
		assertThat(ep.getValue(8).getMinY()).isEqualTo(0);	
		assertThat(ep.getValue(8)).isMax(999, 999);

		assertThat(values).hasSize(9);

	}

	@Test
	public void testFloatingY() {
		ExtremePoints2D<DefaultPlacement2D> ep = new ExtremePoints2D<>(1000, 1000);
		ep.add(0, new DefaultPlacement2D(0, 0, 49, 99));
		assertThat(ep.getValue(0)).isInstanceOf(DefaultXYSupportPoint2D.class);
				
		ep.add(1, new DefaultPlacement2D(50, 0, 99, 49));
		assertThat(ep.getValue(0)).isInstanceOf(DefaultXYSupportPoint2D.class);
		assertThat(ep.getValue(1)).isInstanceOf(DefaultXYSupportPoint2D.class);
		
		List<Point2D<DefaultPlacement2D>> values = ep.getValues();
		assertThat(values).hasSize(3);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(100);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(50);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(50);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);

		for(Point2D point : values) {
			assertThat(point.getMaxX()).isEqualTo(999);
			assertThat(point.getMaxY()).isEqualTo(999);
		}
		
		ep.add(1, new DefaultPlacement2D(50, 50, 149, 149));

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
		assertThat(values).hasSize(4);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(100);
		assertThat(ep.getValue(0)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(150);
		assertThat(ep.getValue(1)).isInstanceOf(DefaultYSupportPoint2D.class);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(2)).isInstanceOf(DefaultXYSupportPoint2D.class);
		
		assertThat(ep.getValue(3).getMinX()).isEqualTo(150);
		assertThat(ep.getValue(3).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(3)).isInstanceOf(DefaultXSupportPoint2D.class);
		
		ep.add(1, new DefaultPlacement2D(0, 150, 19, 169));

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
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(100);
		assertThat(ep.getValue(0)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(170);
		assertThat(ep.getValue(1)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.getValue(2).getMinX()).isEqualTo(20);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(100);
		assertThat(ep.getValue(2)).isInstanceOf(DefaultXSupportPoint2D.class);

		assertThat(ep.getValue(3).getMinX()).isEqualTo(20);
		assertThat(ep.getValue(3).getMinY()).isEqualTo(150);
		assertThat(ep.getValue(3)).isInstanceOf(DefaultYSupportPoint2D.class);

		assertThat(ep.getValue(4).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(4).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(4)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.getValue(5).getMinX()).isEqualTo(150);
		assertThat(ep.getValue(5).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(5)).isInstanceOf(DefaultXSupportPoint2D.class);

		assertThat(values).hasSize(6);

		ep.add(1, new DefaultPlacement2D(0, 170, 29, 189));

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
		
		assertThat(values).hasSize(8);

		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(100);
		assertThat(ep.getValue(0)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(190);
		assertThat(ep.getValue(1)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.getValue(2).getMinX()).isEqualTo(20);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(100);
		assertThat(ep.getValue(2)).isInstanceOf(DefaultXSupportPoint2D.class);

		assertThat(ep.getValue(3).getMinX()).isEqualTo(20);
		assertThat(ep.getValue(3).getMinY()).isEqualTo(150);
		assertThat(ep.getValue(3)).isInstanceOf(DefaultYSupportPoint2D.class);

		assertThat(ep.getValue(4).getMinX()).isEqualTo(30);
		assertThat(ep.getValue(4).getMinY()).isEqualTo(100);
		assertThat(ep.getValue(4)).isInstanceOf(DefaultXSupportPoint2D.class);
		
		assertThat(ep.getValue(5).getMinX()).isEqualTo(30);
		assertThat(ep.getValue(5).getMinY()).isEqualTo(150);
		assertThat(ep.getValue(5)).isInstanceOf(DefaultPoint2D.class);

		assertThat(ep.getValue(6).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(6).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(6)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.getValue(7).getMinX()).isEqualTo(150);
		assertThat(ep.getValue(7).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(7)).isInstanceOf(DefaultXSupportPoint2D.class);
		
		ep.add(5, new DefaultPlacement2D(30, 150, 69, 189));

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
		
		
		assertThat(values).hasSize(6);

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(100);
		assertThat(ep.getValue(0)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(190);
		assertThat(ep.getValue(1)).isInstanceOf(DefaultXYSupportPoint2D.class);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(20);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(100);

		assertThat(ep.getValue(3).getMinX()).isEqualTo(70);
		assertThat(ep.getValue(3).getMinY()).isEqualTo(150);
		assertThat(ep.getValue(3)).isYSupport(189);

		assertThat(ep.getValue(4).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(4).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(4)).isInstanceOf(DefaultXYSupportPoint2D.class);

		assertThat(ep.getValue(5).getMinX()).isEqualTo(150);
		assertThat(ep.getValue(5).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(5)).isInstanceOf(DefaultXSupportPoint2D.class);
		
		ep.add(2, new DefaultPlacement2D(20, 100, 24, 139));

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

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(100);
		assertThat(ep.getValue(0)).isMax(19, 149);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(140);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(190);
		
		// split point
		assertThat(ep.getValue(3).getMinX()).isEqualTo(20);
		assertThat(ep.getValue(3).getMinY()).isEqualTo(140);

		assertThat(ep.getValue(4).getMinX()).isEqualTo(25);
		assertThat(ep.getValue(4).getMinY()).isEqualTo(100);

		assertThat(ep.getValue(5).getMinX()).isEqualTo(25);
		assertThat(ep.getValue(5).getMinY()).isEqualTo(100);
		
		assertThat(ep.getValue(6).getMinX()).isEqualTo(70);
		assertThat(ep.getValue(6).getMinY()).isEqualTo(150);
		assertThat(ep.getValue(6)).isYSupport(189);
		
		assertThat(ep.getValue(7).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(7).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(7)).isSupport(999, 49);

		assertThat(ep.getValue(8).getMinX()).isEqualTo(150);
		assertThat(ep.getValue(8).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(8)).isInstanceOf(DefaultXSupportPoint2D.class);
		assertThat(ep.getValue(8)).isXSupport(999);
		
		assertThat(values).hasSize(9);

	}

}
