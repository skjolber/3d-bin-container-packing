package com.github.skjolber.packing.points3d;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.points.DefaultPlacement3D;

public class ExtremePoints3DTest {

	@Test
	public void testSinglePoint() {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 9));
		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinZ()).isEqualTo(10);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinZ()).isEqualTo(0);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(2).getMinZ()).isEqualTo(0);
	}
	
	@Test
	public void testSinglePointCoveringAllX() {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 99, 10, 0));
		assertThat(ep.getValues()).hasSize(2);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(11);
	}
	
	@Test
	public void testSinglePointCoveringAllY() {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 10, 99, 0));
		assertThat(ep.getValues()).hasSize(2);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(11);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(0);
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
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 49, 0));

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(50);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		
		ep.add(1, new DefaultPlacement3D(10, 0, 0, 19, 24, 0));
		
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
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 49, 0));

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(50);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		
		ep.add(0, new DefaultPlacement3D(0, 50, 0, 4, 74, 0));
		
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
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 0));

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(10);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		
		ep.add(1, new DefaultPlacement3D(10, 0, 0, 19, 9, 0));

		assertThat(ep.getValues()).hasSize(2);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(10);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(20);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
	}
	
	@Test
	public void testStackEqualItemsInYDirection() throws InterruptedException {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 0));

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(10);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		
		ep.add(0, new DefaultPlacement3D(0, 10, 0, 9, 19, 0));
		
		assertThat(ep.getValues()).hasSize(2);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(20);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
	}

	@Test
	public void testStackHigherItemsInXDirection() throws InterruptedException {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 0));

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(10);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		
		ep.add(1, new DefaultPlacement3D(10, 0, 0, 19, 19, 0));

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
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 0));

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(10);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		
		ep.add(0, new DefaultPlacement3D(0, 10, 0, 19, 19, 0));
		
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
