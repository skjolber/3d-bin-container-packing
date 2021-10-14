package com.github.skjolber.packing.points2d;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.points.DefaultExtremePoints2D;
import com.github.skjolber.packing.points.DefaultPlacement2D;

public class ExtremePoints2DTest {

	@Test
	public void testSinglePoint() {
		ExtremePoints2D<DefaultPlacement2D> ep = new ExtremePoints2D<>(100, 100);
		ep.add(0, new DefaultPlacement2D(0, 0, 10, 10));
		assertThat(ep.getValues()).hasSize(2);
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
	
	
}
