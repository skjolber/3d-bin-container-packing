package com.github.skjolber.packing.points2d;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.points.DefaultExtremePoints2D;
import com.github.skjolber.packing.points3d.Point3D;

public class ExtremePoints2DTest {

	@Test
	public void testSinglePoint() {
		ExtremePoints2D<DefaultPlacement2D> ep = new ExtremePoints2D<>(100, 100);
		ep.add(0, new DefaultPlacement2D(0, 0, 10, 10));
		assertThat(ep.getValues()).hasSize(2);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(11);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(11);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
	}
	
	@Test
	public void testSinglePointCornerCase() {
		ExtremePoints2D<DefaultPlacement2D> ep = new ExtremePoints2D<>(100, 100);
		ep.add(0, new DefaultPlacement2D(0, 0, 0, 0));
		assertThat(ep.getValues()).hasSize(2);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(1);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(1);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
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

		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(25);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(20);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
	}
	
	@Test
	public void testStackInYDirection() {
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

		assertThat(ep.getValue(1).getMinX()).isEqualTo(5);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(50);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
	}
	
	@Test
	public void testStackEqualItemsInXDirection() throws InterruptedException {
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

		assertThat(ep.getValue(1).getMinX()).isEqualTo(20);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
	}

	@Test
	public void testStackEqualItemsInYDirection() throws InterruptedException {
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

		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
	}

	@Test
	public void testStackHigherItemsInXDirection() throws InterruptedException {
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

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(20);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(20);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
	}

	@Test
	public void testStackHigherItemsInYDirection() throws InterruptedException {
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

		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(1).getMaxX()).isEqualTo(99);
		assertThat(ep.getValue(1).getMaxY()).isEqualTo(9);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(20);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
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
		

		ep.add(0, new DefaultPlacement2D(0, 10, 5, 25));

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
		List<Point2D> values = ep.getValues();
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
		assertThat(values.get(1).getMinX()).isEqualTo(0);
		assertThat(values.get(1).getMinY()).isEqualTo(20);
		assertThat(values.get(2).getMinX()).isEqualTo(0);
		assertThat(values.get(2).getMinY()).isEqualTo(25);
		assertThat(values.get(3).getMinX()).isEqualTo(25);
		assertThat(values.get(3).getMinY()).isEqualTo(0);
		
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
		assertThat(values.get(1).getMinX()).isEqualTo(5);
		assertThat(values.get(1).getMinY()).isEqualTo(10);
		assertThat(values.get(2).getMinX()).isEqualTo(5);
		assertThat(values.get(2).getMinY()).isEqualTo(20);
		
		assertThat(values.get(3).getMinX()).isEqualTo(5);
		assertThat(values.get(3).getMinY()).isEqualTo(25);
		
		assertThat(values.get(4).getMinX()).isEqualTo(25);
		assertThat(values.get(4).getMinY()).isEqualTo(0);
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
		
		List<Point2D> values = ep.getValues();
		assertThat(values).hasSize(3);

		assertThat(values.get(0).getMinX()).isEqualTo(0);
		assertThat(values.get(0).getMinY()).isEqualTo(20);
		assertThat(values.get(1).getMinX()).isEqualTo(10);
		assertThat(values.get(1).getMinY()).isEqualTo(0);
		assertThat(values.get(2).getMinX()).isEqualTo(20);
		assertThat(values.get(2).getMinY()).isEqualTo(0);

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

		assertThat(values.get(0).getMinX()).isEqualTo(0);
		assertThat(values.get(0).getMinY()).isEqualTo(20);
		assertThat(values.get(1).getMinX()).isEqualTo(10);
		assertThat(values.get(1).getMinY()).isEqualTo(5);
		assertThat(values.get(2).getMinX()).isEqualTo(20);
		assertThat(values.get(2).getMinY()).isEqualTo(5);
		assertThat(values.get(3).getMinX()).isEqualTo(25);
		assertThat(values.get(3).getMinY()).isEqualTo(0);

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

		List<Point2D> values = ep.getValues();
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
		assertThat(values.get(0).getMinX()).isEqualTo(0);
		assertThat(values.get(0).getMinY()).isEqualTo(25);
		assertThat(values.get(1).getMinX()).isEqualTo(10);
		assertThat(values.get(1).getMinY()).isEqualTo(0);
		assertThat(values.get(2).getMinX()).isEqualTo(20);
		assertThat(values.get(2).getMinY()).isEqualTo(0);
		assertThat(values.get(3).getMinX()).isEqualTo(25);
		assertThat(values.get(3).getMinY()).isEqualTo(0);
		
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
		ep.add(1, new DefaultPlacement2D(50, 0, 99, 49));
		
		List<Point2D> values = ep.getValues();
		assertThat(values).hasSize(3);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(100);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(50);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(50);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);

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

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(150);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
		
		assertThat(ep.getValue(3).getMinX()).isEqualTo(150);
		assertThat(ep.getValue(3).getMinY()).isEqualTo(0);
		
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
		assertThat(ep.getValue(4).getMinY()).isEqualTo(20);

		assertThat(ep.getValue(5).getMinX()).isEqualTo(170);
		assertThat(ep.getValue(5).getMinY()).isEqualTo(0);

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

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(150);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
		
		assertThat(ep.getValue(3).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(3).getMinY()).isEqualTo(20);

		assertThat(ep.getValue(4).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(4).getMinY()).isEqualTo(30);

		assertThat(ep.getValue(5).getMinX()).isEqualTo(150);
		assertThat(ep.getValue(5).getMinY()).isEqualTo(20);

		assertThat(ep.getValue(6).getMinX()).isEqualTo(150);
		assertThat(ep.getValue(6).getMinY()).isEqualTo(30);

		assertThat(ep.getValue(7).getMinX()).isEqualTo(190);
		assertThat(ep.getValue(7).getMinY()).isEqualTo(0);
		
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

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(150);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
		
		// split point
		assertThat(ep.getValue(3).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(3).getMinY()).isEqualTo(25);

		assertThat(ep.getValue(4).getMinX()).isEqualTo(100);
		assertThat(ep.getValue(4).getMinY()).isEqualTo(25);

		// split point
		assertThat(ep.getValue(5).getMinX()).isEqualTo(140);
		assertThat(ep.getValue(5).getMinY()).isEqualTo(0);
		
		assertThat(ep.getValue(6).getMinX()).isEqualTo(140);
		assertThat(ep.getValue(6).getMinY()).isEqualTo(20);
		
		
		assertThat(ep.getValue(values.size() - 2).getMinX()).isEqualTo(150);
		assertThat(ep.getValue(values.size() - 2).getMinY()).isEqualTo(70);

		assertThat(ep.getValue(values.size() - 1).getMinX()).isEqualTo(190);
		assertThat(ep.getValue(values.size() - 1).getMinY()).isEqualTo(0);	
		
	}
	
}
