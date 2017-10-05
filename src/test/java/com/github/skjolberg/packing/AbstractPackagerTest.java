package com.github.skjolberg.packing;

public abstract class AbstractPackagerTest {


	public static void validate(Container pack) {
		for(Level level : pack.getLevels()) {
			level.validate();
		}
	}
	
	public static void print(Container fits) {
		System.out.println();
		System.out.println(Visualizer.visualize(fits, 100, 2));
		System.out.println();
	}
}
