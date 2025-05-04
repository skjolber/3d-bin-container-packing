package com.github.skjolber.packing.api.ep;

import java.util.Iterator;
import java.util.List;

public class DefaultFilteredPoints implements FilteredPoints {

	protected List<Point> values;
	
	public DefaultFilteredPoints(List<Point> values) {
		this.values = values;
	}

	public void setValues(List<Point> values) {
		this.values = values;
	}
	
	@Override
	public int size() {
		return values.size();
	}

	@Override
	public Point get(int index) {
		return values.get(index);
	}

	@Override
	public void remove(int index) {
		values.remove(index);
	}

	@Override
	public Iterator<Point> iterator() {
		return values.listIterator();
	}

}
