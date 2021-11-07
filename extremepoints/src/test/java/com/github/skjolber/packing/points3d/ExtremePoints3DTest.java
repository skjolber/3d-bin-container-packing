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
	public void testSinglePointCornercase() {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 0, 0, 0));
		assertThat(ep.getValues()).hasSize(3);

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinZ()).isEqualTo(1);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(1);
		assertThat(ep.getValue(1).getMinZ()).isEqualTo(0);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(1);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(2).getMinZ()).isEqualTo(0);
	}
	
	@Test
	public void testSinglePointCoveringAllX() {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 99, 9, 9));
		
		assertThat(ep.getValues()).hasSize(2);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinZ()).isEqualTo(10);
		
		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinZ()).isEqualTo(0);
	}
	
	@Test
	public void testSinglePointCoveringAllY() {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 99, 9));
		
		assertThat(ep.getValues()).hasSize(2);

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinZ()).isEqualTo(10);
		
		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinZ()).isEqualTo(0);
	}
	
	@Test
	public void testSinglePointCoveringAllZ() {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 99));
		assertThat(ep.getValues()).hasSize(2);

		assertThat(ep.getValues()).hasSize(2);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(10);
		assertThat(ep.getValue(0).getMinZ()).isEqualTo(0);
		
		assertThat(ep.getValue(1).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinZ()).isEqualTo(0);
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

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinZ()).isEqualTo(10);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinZ()).isEqualTo(0);

		assertThat(ep.getValue(2).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(2).getMinZ()).isEqualTo(0);
		
		ep.add(2, new DefaultPlacement3D(10, 0, 0, 19, 4, 4));
		
		assertThat(ep.getValues()).hasSize(5);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinZ()).isEqualTo(10);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinZ()).isEqualTo(0);

		assertThat(ep.getValue(2).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(2).getMinZ()).isEqualTo(5);
		
		assertThat(ep.getValue(3).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(3).getMinY()).isEqualTo(5);
		assertThat(ep.getValue(3).getMinZ()).isEqualTo(0);
		
		assertThat(ep.getValue(4).getMinX()).isEqualTo(20);
		assertThat(ep.getValue(4).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(4).getMinZ()).isEqualTo(0);
	}
	
	@Test
	public void testStackInZDirection() {
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
		
		ep.add(0, new DefaultPlacement3D(0, 0, 10, 4, 4, 19));
		
		assertThat(ep.getValues()).hasSize(5);

		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinZ()).isEqualTo(20);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(5);
		assertThat(ep.getValue(1).getMinZ()).isEqualTo(10);

		assertThat(ep.getValue(2).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(10);
		assertThat(ep.getValue(2).getMinZ()).isEqualTo(0);
		
		assertThat(ep.getValue(3).getMinX()).isEqualTo(5);
		assertThat(ep.getValue(3).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(3).getMinZ()).isEqualTo(10);
		
		assertThat(ep.getValue(4).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(4).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(4).getMinZ()).isEqualTo(0);
	}
	
	@Test
	public void testStackInYDirection() {
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
		
		ep.add(1, new DefaultPlacement3D(0, 10, 0, 4, 19, 4));
		
		assertThat(ep.getValues()).hasSize(5);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinZ()).isEqualTo(10);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinZ()).isEqualTo(5);

		assertThat(ep.getValue(2).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(20);
		assertThat(ep.getValue(2).getMinZ()).isEqualTo(0);

		assertThat(ep.getValue(3).getMinX()).isEqualTo(5);
		assertThat(ep.getValue(3).getMinY()).isEqualTo(10);
		assertThat(ep.getValue(3).getMinZ()).isEqualTo(0);

		assertThat(ep.getValue(4).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(4).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(4).getMinZ()).isEqualTo(0);
	}
	
	@Test
	public void testStackEqualItemsInXDirection() throws InterruptedException {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 9));
		
		ep.add(2, new DefaultPlacement3D(10, 0, 0, 19, 9, 9));

		assertThat(ep.getValues()).hasSize(3);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinZ()).isEqualTo(10);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinZ()).isEqualTo(0);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(20);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(2).getMinZ()).isEqualTo(0);

	}
	
	@Test
	public void testStackEqualItemsInYDirection() throws InterruptedException {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 9));
		
		ep.add(1, new DefaultPlacement3D(0, 10, 0, 9, 19, 9));

		assertThat(ep.getValues()).hasSize(3);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinZ()).isEqualTo(10);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(20);
		assertThat(ep.getValue(1).getMinZ()).isEqualTo(0);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(2).getMinZ()).isEqualTo(0);
	}

	@Test
	public void testStackEqualItemsInZDirection() throws InterruptedException {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 9));
		
		ep.add(0, new DefaultPlacement3D(0, 0, 10, 9, 9, 19));

		assertThat(ep.getValues()).hasSize(3);
		
		assertThat(ep.getValue(0).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(0).getMinZ()).isEqualTo(20);

		assertThat(ep.getValue(1).getMinX()).isEqualTo(0);
		assertThat(ep.getValue(1).getMinY()).isEqualTo(10);
		assertThat(ep.getValue(1).getMinZ()).isEqualTo(0);
		
		assertThat(ep.getValue(2).getMinX()).isEqualTo(10);
		assertThat(ep.getValue(2).getMinY()).isEqualTo(0);
		assertThat(ep.getValue(2).getMinZ()).isEqualTo(0);
	}
	
	@Test
	public void testStackHigherItemsInXDirection() throws InterruptedException {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 9));

		assertThat(ep.getValues()).hasSize(3);

		ep.add(2, new DefaultPlacement3D(10, 0, 0, 19, 19, 19));

	}
	
	@Test
	public void testStackHigherItemsInYDirection() throws InterruptedException {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 9));

		assertThat(ep.getValues()).hasSize(3);

		ep.add(1, new DefaultPlacement3D(0, 10, 0, 19, 19, 19));
	}	
	
	
	@Test
	public void testStackHigherItemsInZDirection() throws InterruptedException {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(100, 100, 100);
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 9, 9, 9));

		assertThat(ep.getValues()).hasSize(3);

		ep.add(0, new DefaultPlacement3D(0, 0, 10, 19, 19, 19));
	}	
	
}
