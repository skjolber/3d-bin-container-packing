package com.github.skjolber.packing.api;

public enum LoadBearingConstraintType {

	NONE, // no weight bearing 
	WEIGHT, // total weight limit
	PRESSURE, // weight per area limit
	COUNT // specific kind of stackable (i.e. the same type)
	
}
