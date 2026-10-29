package com.github.skjolber.packing.jmh;

import java.lang.reflect.Method;

/**
 * Options of the tree search of the brute force packagers, set on a builder by reflection so that the benchmarks compile against versions
 * without them: {@code -Dtree.anyBest=true} lets the tree return any arrangement of the best value.
 */
public final class TreeOptions {

	private TreeOptions() {
	}

	public static <T> T apply(T builder) {
		if(Boolean.getBoolean("tree.anyBest")) {
			try {
				Method method = builder.getClass().getMethod("withTreeSearchAnyBest", boolean.class);
				method.invoke(builder, true);
			} catch (NoSuchMethodException e) {
				// a version without the option: ignore
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		}
		return builder;
	}
}
