package com.github.skjolber.packing.api.point;

import java.util.Collections;
import java.util.Iterator;

public class EmptyPointSource implements PointSource {

	private static final EmptyPointSource INSTANCE = new EmptyPointSource();
	
	public static EmptyPointSource getInstance() {
		return INSTANCE;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public Point get(int index) {
		throw new ArrayIndexOutOfBoundsException();
	}

	@Override
	public void remove(int index) {
		throw new ArrayIndexOutOfBoundsException();
	}

	@Override
	public Iterator<Point> iterator() {
		return Collections.emptyIterator(); 
	} 
}
