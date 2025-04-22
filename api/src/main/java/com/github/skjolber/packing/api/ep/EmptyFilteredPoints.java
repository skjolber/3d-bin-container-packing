package com.github.skjolber.packing.api.ep;

import java.util.Collections;
import java.util.Iterator;

public class EmptyFilteredPoints implements FilteredPoints {

	private static final EmptyFilteredPoints INSTANCE = new EmptyFilteredPoints();
	
	public static EmptyFilteredPoints getInstance() {
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
