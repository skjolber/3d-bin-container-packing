package com.github.skjolber.packing.api.packager.control.point;

public class DefaultPointControlsBuilderFactory implements PointControlsBuilderFactory {

	@Override
	public PointControlsBuilder createPointControlsBuilder() {
		return new DefaultPointControlsBuilder();
	}

	/**
	 * This factory is stateless, so all instances are equal: packagers can reuse the result of a container for another with an equal
	 * factory (see {@link PointControlsBuilderFactory}).
	 */
	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		return obj != null && getClass() == obj.getClass();
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

}
