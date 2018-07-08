package com.github.skjolberg.packing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.github.skjolberg.packing.Packager.Adapter;
import com.github.skjolberg.packing.Packager.PackResult;

public class PackagerTest {

	private PackResult incompleteResult;
	private PackResult completeResult;
	
	public static class MyPackager extends Packager {

		private final Adapter adapter;
		
		public MyPackager(List<Container> containers, Adapter adapter) {
			super(containers);
			
			this.adapter = adapter;
		}

		@Override
		protected Adapter adapter() {
			return adapter;
		}

	}
	
	@Before
	public void init() {
		incompleteResult = mock(PackResult.class);
		when(incompleteResult.isRemainder()).thenReturn(false);
		
		completeResult = mock(PackResult.class);
		when(completeResult.isRemainder()).thenReturn(true);
	}
	
	@Test
	public void testBinarySearchChecksBoxesBelowPositive() {
		
		long deadline = System.currentTimeMillis() + 100000;

		List<Container> containers = new ArrayList<Container>();

		containers.add(new Container("0", 5, 5, 1, 0));
		containers.add(new Container("1", 5, 5, 1, 0));
		containers.add(new Container("2", 5, 5, 1, 0));
		containers.add(new Container("3", 5, 5, 1, 0));
		containers.add(new Container("4", 5, 5, 1, 0));
		containers.add(new Container("5", 5, 5, 1, 0));
		containers.add(new Container("6", 5, 5, 1, 0));

		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("1", 5, 5, 1, 0), 1));

		Adapter mock = mock(Packager.Adapter.class);
		
		// in the middle first
		when(mock.attempt(containers.get(3), deadline))
			.thenReturn(completeResult);

		// then in the middle of 0..2 
		when(mock.attempt(containers.get(1), deadline))
			.thenReturn(incompleteResult);

		// then higher
		when(mock.attempt(containers.get(2), deadline))
			.thenReturn(incompleteResult);

		// then iteration is done for 0...2. Filter out 1 and 2 and try again
		// for 0..0 
		when(mock.attempt(containers.get(0), deadline))
			.thenReturn(incompleteResult);

		when(mock.accepted(any(PackResult.class)))
			.thenReturn(new Container("result", 5, 5, 1, 0));
		
		MyPackager myPackager = new MyPackager(containers, mock);
		
		Container pack = myPackager.pack(products, deadline);
		assertEquals("result", pack.getName());
		assertNotNull(pack);
		verify(mock, times(4)).attempt(any(Container.class), any(Long.class));
	}
	
	@Test
	public void testBinarySearchChecksBoxesBelowPositiveBetterMatch() {
		
		List<Container> containers = new ArrayList<Container>();

		containers.add(new Container("0", 5, 5, 1, 0));
		containers.add(new Container("1", 5, 5, 1, 0));
		containers.add(new Container("2", 5, 5, 1, 0));
		containers.add(new Container("3", 5, 5, 1, 0));
		containers.add(new Container("4", 5, 5, 1, 0));
		containers.add(new Container("5", 5, 5, 1, 0));
		containers.add(new Container("6", 5, 5, 1, 0));

		long deadline = System.currentTimeMillis() + 100000;
				
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("1", 5, 5, 1, 0), 1));

		Adapter mock = mock(Packager.Adapter.class);
		when(mock.accepted(completeResult))
			.thenReturn(new Container("result", 5, 5, 1, 0))
			.thenReturn(new Container("better", 5, 5, 1, 0));
		
		// in the middle first
		when(mock.attempt(containers.get(3), deadline))
			.thenReturn(completeResult);

		// then in the middle of 0..2 
		when(mock.attempt(containers.get(1), deadline))
			.thenReturn(incompleteResult);

		// then higher
		when(mock.attempt(containers.get(2), deadline))
			.thenReturn(incompleteResult);

		// then iteration is done for 0...2. Filter out 1 and 2 and try again
		// for 0..0 
		when(mock.attempt(containers.get(0), deadline))
			.thenReturn(completeResult);

		MyPackager myPackager = new MyPackager(containers, mock);
		
		Container pack = myPackager.pack(products, deadline);
		assertNotNull(pack);
		assertEquals("better", pack.getName());
		
		verify(mock, times(4)).attempt(any(Container.class), any(Long.class));
	}
	
	@Test
	public void testBinarySearchChecksBoxesTheFirstBox() {
		
		List<Container> containers = new ArrayList<Container>();

		containers.add(new Container("0", 5, 5, 1, 0));
		containers.add(new Container("1", 5, 5, 1, 0));
		containers.add(new Container("2", 5, 5, 1, 0));
		containers.add(new Container("3", 5, 5, 1, 0));
		containers.add(new Container("4", 5, 5, 1, 0));
		containers.add(new Container("5", 5, 5, 1, 0));
		containers.add(new Container("6", 5, 5, 1, 0));

		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("1", 5, 5, 1, 0), 1));

		Adapter mock = mock(Packager.Adapter.class);
		
		long deadline = System.currentTimeMillis() + 100000;

		PackResult ok = mock(PackResult.class);
		when(ok.isRemainder()).thenReturn(true);
		
		// in the middle first
		when(mock.attempt(containers.get(3), deadline)).thenReturn(ok);

		PackResult better = mock(PackResult.class);
		when(better.isRemainder()).thenReturn(true);

		// then in the middle of 0..2 
		when(mock.attempt(containers.get(1), deadline)).thenReturn(better);

		PackResult best = mock(PackResult.class);
		when(best.isRemainder()).thenReturn(true);

		when(mock.accepted(any(PackResult.class)))
			.thenReturn(new Container("ok", 5, 5, 1, 0))
			.thenReturn(new Container("better", 5, 5, 1, 0))
			.thenReturn(new Container("best", 5, 5, 1, 0));

		// then lower
		when(mock.attempt(containers.get(0), deadline)).thenReturn(best);

		MyPackager myPackager = new MyPackager(containers, mock);
		
		Container pack = myPackager.pack(products, deadline);
		assertNotNull(pack);
		assertEquals("best", pack.getName());
		
		verify(mock, times(3)).attempt(any(Container.class), any(Long.class));
		
		verify(mock, times(1)).attempt(containers.get(3), deadline);
		verify(mock, times(1)).attempt(containers.get(1), deadline);
		verify(mock, times(1)).attempt(containers.get(0), deadline);

	}
	
	@Test
	public void testBinarySearchChecksBoxesTheLastBox() {
		
		long deadline = System.currentTimeMillis() + 100000;

		List<Container> containers = new ArrayList<Container>();

		containers.add(new Container("0", 5, 5, 1, 0));
		containers.add(new Container("1", 5, 5, 1, 0));
		containers.add(new Container("2", 5, 5, 1, 0));
		containers.add(new Container("3", 5, 5, 1, 0));
		containers.add(new Container("4", 5, 5, 1, 0));
		containers.add(new Container("5", 5, 5, 1, 0));
		containers.add(new Container("6", 5, 5, 1, 0));
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("1", 5, 5, 1, 0), 1));

		Adapter mock = mock(Packager.Adapter.class);
		
		// in the middle first
		when(mock.attempt(containers.get(3), deadline))
			.thenReturn(incompleteResult).thenThrow(RuntimeException.class);

		// then in the middle of 4..6 
		when(mock.attempt(containers.get(5), deadline))
		.thenReturn(incompleteResult).thenThrow(RuntimeException.class);

		// then higher 
		when(mock.attempt(containers.get(6), deadline))
		.thenReturn(completeResult).thenThrow(RuntimeException.class);

		// then no more results
		when(mock.attempt(containers.get(4), deadline))
		.thenReturn(incompleteResult).thenThrow(RuntimeException.class);

		// then no more results
		when(mock.attempt(containers.get(2), deadline))
		.thenReturn(incompleteResult).thenThrow(RuntimeException.class);

		// then no more results
		when(mock.attempt(containers.get(1), deadline))
		.thenReturn(incompleteResult).thenThrow(RuntimeException.class);

		// then no more results
		when(mock.attempt(containers.get(0), deadline))
		.thenReturn(incompleteResult).thenThrow(RuntimeException.class);

		when(mock.accepted(any(PackResult.class)))
			.thenReturn(new Container("result", 5, 5, 1, 0));

		
		MyPackager myPackager = new MyPackager(containers, mock);
		
		Container pack = myPackager.pack(products, deadline);
		assertNotNull(pack);
		assertEquals("result", pack.getName());

		for(Container container : containers) {
			verify(mock, times(1)).attempt(container, deadline);
		}
	}		
}
