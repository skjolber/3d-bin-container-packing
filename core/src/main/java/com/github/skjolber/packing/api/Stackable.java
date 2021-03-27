package com.github.skjolber.packing.api;

public abstract class Stackable {

	protected final String name;

	public Stackable(String name) {
		super();
		this.name = name;
	}

	public abstract long getVolume();
	
	public abstract int getWeight();
	
	public abstract StackValue[] getStackValues();
	
	public String getName() {
		return name;
	}

}
