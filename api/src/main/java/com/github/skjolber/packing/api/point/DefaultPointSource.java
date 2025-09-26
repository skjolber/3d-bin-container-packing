package com.github.skjolber.packing.api.point;

import java.util.Iterator;
import java.util.List;

public class DefaultPointSource implements PointSource {

	protected List<Point> values;
	
	public DefaultPointSource(List<Point> values) {
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
