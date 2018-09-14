package com.github.skjolberg.packing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

	@BeforeEach
	public void init() {
		incompleteResult = mock(PackResult.class);
		when(incompleteResult.isEmpty()).thenReturn(false);

		completeResult = mock(PackResult.class);
		when(completeResult.isEmpty()).thenReturn(false);
	}

	@Test
	public void testBinarySearchChecksBoxesBelowPositive() {

		long deadline = System.currentTimeMillis() + 100000;

		List<Container> containers = new ArrayList<>();

		containers.add(new Container("0", 5, 5, 1, 0));
		containers.add(new Container("1", 5, 5, 1, 0));
		containers.add(new Container("2", 5, 5, 1, 0));
		containers.add(new Container("3", 5, 5, 1, 0));
		containers.add(new Container("4", 5, 5, 1, 0));
		containers.add(new Container("5", 5, 5, 1, 0));
		containers.add(new Container("6", 5, 5, 1, 0));


		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("1", 5, 5, 1, 0), 1));

		Adapter mock = mock(Packager.Adapter.class);

		// in the middle first
		when(mock.attempt(3, deadline))
			.thenReturn(completeResult);

		// then in the middle of 0..2
		when(mock.attempt(1, deadline))
			.thenReturn(incompleteResult);

		// then higher
		when(mock.attempt(2, deadline))
			.thenReturn(incompleteResult);

		// then iteration is done for 0...2. Filter out 1 and 2 and try again
		// for 0..0
		when(mock.attempt(0, deadline))
			.thenReturn(incompleteResult);

		when(mock.accepted(any(PackResult.class)))
			.thenReturn(new Container("result", 5, 5, 1, 0));

		when(mock.hasMore(any(PackResult.class))).thenReturn(false, true, true, true);

		MyPackager myPackager = new MyPackager(containers, mock);

		Container pack = myPackager.pack(products, deadline);
		assertEquals("result", pack.getName());
		assertNotNull(pack);
		verify(mock, times(4)).attempt(any(int.class), any(Long.class));
	}

	@Test
	public void testBinarySearchChecksBoxesBelowPositiveBetterMatch() {

		List<Container> containers = new ArrayList<>();

		containers.add(new Container("0", 5, 5, 1, 0));
		containers.add(new Container("1", 5, 5, 1, 0));
		containers.add(new Container("2", 5, 5, 1, 0));
		containers.add(new Container("3", 5, 5, 1, 0));
		containers.add(new Container("4", 5, 5, 1, 0));
		containers.add(new Container("5", 5, 5, 1, 0));
		containers.add(new Container("6", 5, 5, 1, 0));

		long deadline = System.currentTimeMillis() + 100000;

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("1", 5, 5, 1, 0), 1));

		Adapter mock = mock(Packager.Adapter.class);
		when(mock.accepted(completeResult))
			.thenReturn(new Container("final", 5, 5, 1, 0));

		// in the middle first
		when(mock.attempt(3, deadline))
			.thenReturn(completeResult);

		// then in the middle of 0..2
		when(mock.attempt(1, deadline))
			.thenReturn(incompleteResult);

		// then higher
		when(mock.attempt(2, deadline))
			.thenReturn(incompleteResult);

		// then iteration is done for 0...2. Filter out 1 and 2 and try again
		// for 0..0
		when(mock.attempt(0, deadline))
			.thenReturn(completeResult);

		when(mock.hasMore(any(PackResult.class))).thenReturn(false, true, true, false);

		MyPackager myPackager = new MyPackager(containers, mock);

		Container pack = myPackager.pack(products, deadline);
		assertNotNull(pack);
		assertEquals("final", pack.getName());

		verify(mock, times(1)).attempt(3, deadline);
		verify(mock, times(1)).attempt(2, deadline);
		verify(mock, times(1)).attempt(1, deadline);
		verify(mock, times(1)).attempt(0, deadline);
	}

	@Test
	public void testBinarySearchChecksBoxesTheFirstBox() {

		List<Container> containers = new ArrayList<>();

		containers.add(new Container("0", 5, 5, 1, 0));
		containers.add(new Container("1", 5, 5, 1, 0));
		containers.add(new Container("2", 5, 5, 1, 0));
		containers.add(new Container("3", 5, 5, 1, 0));
		containers.add(new Container("4", 5, 5, 1, 0));
		containers.add(new Container("5", 5, 5, 1, 0));
		containers.add(new Container("6", 5, 5, 1, 0));

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("1", 5, 5, 1, 0), 1));

		Adapter mock = mock(Packager.Adapter.class);
		when(mock.hasMore(any(PackResult.class))).thenReturn(false);

		long deadline = System.currentTimeMillis() + 100000;

		PackResult ok = mock(PackResult.class);

		// in the middle first
		when(mock.attempt(3, deadline)).thenReturn(ok);

		PackResult better = mock(PackResult.class);

		// then in the middle of 0..2
		when(mock.attempt(1, deadline)).thenReturn(better);

		PackResult best = mock(PackResult.class);

		// then lower
		when(mock.attempt(0, deadline)).thenReturn(best);

		when(mock.accepted(any(PackResult.class)))
		.thenReturn(new Container("final", 5, 5, 1, 0));

		when(mock.hasMore(any(PackResult.class))).thenReturn(false, false, false);

		MyPackager myPackager = new MyPackager(containers, mock);

		Container pack = myPackager.pack(products, deadline);
		assertNotNull(pack);
		assertEquals("final", pack.getName());

		verify(mock, times(3)).attempt(any(Integer.class), any(Long.class));

		verify(mock, times(1)).attempt(3, deadline);
		verify(mock, times(1)).attempt(1, deadline);
		verify(mock, times(1)).attempt(0, deadline);

	}

	@Test
	public void testBinarySearchChecksBoxesTheLastBox() {

		long deadline = System.currentTimeMillis() + 100000;

		List<Container> containers = new ArrayList<>();

		containers.add(new Container("0", 5, 5, 1, 0));
		containers.add(new Container("1", 5, 5, 1, 0));
		containers.add(new Container("2", 5, 5, 1, 0));
		containers.add(new Container("3", 5, 5, 1, 0));
		containers.add(new Container("4", 5, 5, 1, 0));
		containers.add(new Container("5", 5, 5, 1, 0));
		containers.add(new Container("6", 5, 5, 1, 0));

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("1", 5, 5, 1, 0), 1));

		Adapter mock = mock(Packager.Adapter.class);

		// in the middle first
		when(mock.attempt(3, deadline))
			.thenReturn(incompleteResult).thenThrow(RuntimeException.class);

		// then in the middle of 4..6
		when(mock.attempt(5, deadline))
		.thenReturn(incompleteResult).thenThrow(RuntimeException.class);

		// then higher
		when(mock.attempt(6, deadline))
		.thenReturn(completeResult).thenThrow(RuntimeException.class);

		// then no more results
		when(mock.attempt(4, deadline))
		.thenReturn(incompleteResult).thenThrow(RuntimeException.class);

		// then no more results
		when(mock.attempt(2, deadline))
		.thenReturn(incompleteResult).thenThrow(RuntimeException.class);

		// then no more results
		when(mock.attempt(1, deadline))
		.thenReturn(incompleteResult).thenThrow(RuntimeException.class);

		// then no more results
		when(mock.attempt(0, deadline))
		.thenReturn(incompleteResult).thenThrow(RuntimeException.class);

		when(mock.accepted(any(PackResult.class)))
			.thenReturn(new Container("result", 5, 5, 1, 0));

		when(mock.hasMore(any(PackResult.class))).thenReturn(true, true, false, true, true, true, true);

		MyPackager myPackager = new MyPackager(containers, mock);

		Container pack = myPackager.pack(products, deadline);
		assertNotNull(pack);
		assertEquals("result", pack.getName());

		for(int i = 0; i < containers.size(); i++) {
			verify(mock, times(1)).attempt(i, deadline);
		}
	}


	@Test
	public void testLimitIsRespected() {

		long deadline = System.currentTimeMillis() + 100000;

		List<Container> containers = new ArrayList<>();

		containers.add(new Container("0", 5, 5, 1, 0));

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("1", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("2", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("3", 5, 5, 1, 0), 1));

		Adapter mock = mock(Packager.Adapter.class);

		when(mock.attempt(0, deadline))
			.thenReturn(completeResult);

		when(mock.hasMore(any(PackResult.class))).thenReturn(true, true, false);

		MyPackager myPackager = new MyPackager(containers, mock);

		List<Container> pack = myPackager.packList(products, 2, deadline);
		assertNull(pack);
	}

}
