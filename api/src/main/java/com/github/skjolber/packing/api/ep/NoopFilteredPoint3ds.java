package com.github.skjolber.packing.api.ep;

public class NoopFilteredPoint3ds implements FilteredPoint3Ds {

	private ExtremePoints extremePoints;
	
	@Override
	public int size() {
		return extremePoints.getValueCount();
	}

	@Override
	public Point3D get(int index) {
		return extremePoints.getValue(index);
	}

	@Override
	public void remove(int index) {
		// do nothing
	}

}
