package com.github.skjolber.packing.api.ep;

import java.util.List;

public class DefaultFilteredPoints implements FilteredPoints {

	private List<Point> values;
	
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

}
