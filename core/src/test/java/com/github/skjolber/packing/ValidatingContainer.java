package com.github.skjolber.packing;

/**
 * Container wrapper used for testing. Validates that boxes do not intercept.
 *
 */

public class ValidatingContainer extends Container {

	public ValidatingContainer(Container container) {
		super(container);
	}

	public ValidatingContainer(Dimension dimension, int weight) {
		super(dimension, weight);
	}

	public ValidatingContainer(int w, int d, int h, int weight) {
		super(w, d, h, weight);
	}

	public ValidatingContainer(String name, int w, int d, int h, int weight) {
		super(name, w, d, h, weight);
	}

	@Override
	public void add(Placement placement) {
		super.add(placement);
		
		levels.get(levels.size() - 1).validate();
	}
	
	public Container clone() {
		// shallow clone
		return new ValidatingContainer(this);
	}
	
}
