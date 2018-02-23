package com.github.skjolberg.packing;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import com.github.skjolberg.packing.Packager.Adapter;

public class PackagerTest {

	public static class MyPackager extends Packager {

		private final Adapter adapter;
		
		public MyPackager(List<? extends Dimension> containers, Adapter adapter) {
			super(containers);
			
			this.adapter = adapter;
		}

		@Override
		protected Adapter adapter(List<Box> boxes) {
			return adapter;
		}
	}
	
	@Test
	public void testBinarySearchChecksBoxesBelowPositive() {
		
		long deadline = System.currentTimeMillis() + 100000;

		List<Box> containers = new ArrayList<Box>();

		containers.add(new Box("0", 5, 5, 1));
		containers.add(new Box("1", 5, 5, 1));
		containers.add(new Box("2", 5, 5, 1));
		containers.add(new Box("3", 5, 5, 1));
		containers.add(new Box("4", 5, 5, 1));
		containers.add(new Box("5", 5, 5, 1));
		containers.add(new Box("6", 5, 5, 1));

		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("1", 5, 5, 1));

		Adapter mock = mock(Packager.Adapter.class);
		
		// in the middle first
		when(mock.pack(products, containers.get(3), deadline))
			.thenReturn(new Container("result", 5, 5, 1));

		// then in the middle of 0..2 
		when(mock.pack(products, containers.get(1), deadline))
			.thenReturn(null);

		// then higher
		when(mock.pack(products, containers.get(2), deadline))
			.thenReturn(null);

		// then iteration is done for 0...2. Filter out 1 and 2 and try again
		// for 0..0 
		when(mock.pack(products, containers.get(0), deadline))
			.thenReturn(null);

		MyPackager myPackager = new MyPackager(containers, mock);
		
		Container pack = myPackager.pack(products, deadline);
		assertEquals("result", pack.getName());
		assertNotNull(pack);
		verify(mock, times(4)).pack(any(List.class), any(Dimension.class), any(Long.class));
	}
	
	@Test
	public void testBinarySearchChecksBoxesBelowPositiveBetterMatch() {
		
		List<Box> containers = new ArrayList<Box>();

		containers.add(new Box("0", 5, 5, 1));
		containers.add(new Box("1", 5, 5, 1));
		containers.add(new Box("2", 5, 5, 1));
		containers.add(new Box("3", 5, 5, 1));
		containers.add(new Box("4", 5, 5, 1));
		containers.add(new Box("5", 5, 5, 1));
		containers.add(new Box("6", 5, 5, 1));

		long deadline = System.currentTimeMillis() + 100000;
				
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("1", 5, 5, 1));

		Adapter mock = mock(Packager.Adapter.class);
		
		// in the middle first
		when(mock.pack(products, containers.get(3), deadline))
			.thenReturn(new Container("result", 5, 5, 1));

		// then in the middle of 0..2 
		when(mock.pack(products, containers.get(1), deadline))
			.thenReturn(null);

		// then higher
		when(mock.pack(products, containers.get(2), deadline))
			.thenReturn(null);

		// then iteration is done for 0...2. Filter out 1 and 2 and try again
		// for 0..0 
		when(mock.pack(products, containers.get(0), deadline))
			.thenReturn(new Container("better", 5, 5, 1));

		MyPackager myPackager = new MyPackager(containers, mock);
		
		Container pack = myPackager.pack(products, deadline);
		assertNotNull(pack);
		assertEquals("better", pack.getName());
		
		verify(mock, times(4)).pack(any(List.class), any(Dimension.class), any(Long.class));
	}
	
	@Test
	public void testBinarySearchChecksBoxesTheFirstBox() {
		
		List<Box> containers = new ArrayList<Box>();

		containers.add(new Box("0", 5, 5, 1));
		containers.add(new Box("1", 5, 5, 1));
		containers.add(new Box("2", 5, 5, 1));
		containers.add(new Box("3", 5, 5, 1));
		containers.add(new Box("4", 5, 5, 1));
		containers.add(new Box("5", 5, 5, 1));
		containers.add(new Box("6", 5, 5, 1));

		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("1", 5, 5, 1));

		Adapter mock = mock(Packager.Adapter.class);
		
		long deadline = System.currentTimeMillis() + 100000;

		// in the middle first
		when(mock.pack(products, containers.get(3), deadline))
			.thenReturn(new Container("ok", 5, 5, 1));

		// then in the middle of 0..2 
		when(mock.pack(products, containers.get(1), deadline))
		.thenReturn(new Container("better", 5, 5, 1));

		// then lower
		when(mock.pack(products, containers.get(0), deadline))
		.thenReturn(new Container("best", 5, 5, 1));

		MyPackager myPackager = new MyPackager(containers, mock);
		
		Container pack = myPackager.pack(products, deadline);
		assertNotNull(pack);
		assertEquals("best", pack.getName());
		
		verify(mock, times(3)).pack(any(List.class), any(Dimension.class), any(Long.class));
		
		verify(mock, times(1)).pack(products, containers.get(3), deadline);
		verify(mock, times(1)).pack(products, containers.get(1), deadline);
		verify(mock, times(1)).pack(products, containers.get(0), deadline);

	}	
	
	@Test
	public void testBinarySearchChecksBoxesTheLastBox() {
		
		long deadline = System.currentTimeMillis() + 100000;

		List<Box> containers = new ArrayList<Box>();

		containers.add(new Box("0", 5, 5, 1));
		containers.add(new Box("1", 5, 5, 1));
		containers.add(new Box("2", 5, 5, 1));
		containers.add(new Box("3", 5, 5, 1));
		containers.add(new Box("4", 5, 5, 1));
		containers.add(new Box("5", 5, 5, 1));
		containers.add(new Box("6", 5, 5, 1));

		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("1", 5, 5, 1));

		Adapter mock = mock(Packager.Adapter.class);
		
		// in the middle first
		when(mock.pack(products, containers.get(3), deadline))
			.thenReturn(null).thenThrow(RuntimeException.class);

		// then in the middle of 4..6 
		when(mock.pack(products, containers.get(5), deadline))
		.thenReturn(null).thenThrow(RuntimeException.class);

		// then higher 
		when(mock.pack(products, containers.get(6), deadline))
		.thenReturn(new Container("last", 5, 5, 1)).thenThrow(RuntimeException.class);

		// then no more results
		when(mock.pack(products, containers.get(4), deadline))
		.thenReturn(null).thenThrow(RuntimeException.class);

		// then no more results
		when(mock.pack(products, containers.get(2), deadline))
		.thenReturn(null).thenThrow(RuntimeException.class);

		// then no more results
		when(mock.pack(products, containers.get(1), deadline))
		.thenReturn(null).thenThrow(RuntimeException.class);

		// then no more results
		when(mock.pack(products, containers.get(0), deadline))
		.thenReturn(null).thenThrow(RuntimeException.class);

		MyPackager myPackager = new MyPackager(containers, mock);
		
		Container pack = myPackager.pack(products, deadline);
		assertNotNull(pack);
		assertEquals("last", pack.getName());

		for(Box container : containers) {
			verify(mock, times(1)).pack(products, container, deadline);
		}
	}		
}
