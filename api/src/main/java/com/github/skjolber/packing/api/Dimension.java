package com.github.skjolber.packing.api;

import java.util.Set;

public enum Dimension {
	
	X, Y, Z;
	
	public static Set<Dimension> MOVE_XY = Set.of(X, Y); // i.e. horizontally
	public static Set<Dimension> MOVE_ALL = Set.of(X, Y, Z);
	public static Set<Dimension> MOVE_X = Set.of(X);
	public static Set<Dimension> MOVE_Y = Set.of(Y);
	public static Set<Dimension> MOVE_Z = Set.of(Z);
	
}