package com.github.skjolber.packing.jmh;

import java.lang.reflect.Method;

/**
 * Options of the tree search of the brute force packagers, set on a builder by reflection so that the benchmarks compile against versions
 * without them: {@code -Dtree.anyBest=true} lets the tree return any arrangement of the best value, {@code -Dtree.anyBest=false} the loop's
 * arrangement exactly; without the property, the version's default.
 */
public final class TreeOptions {

	private TreeOptions() {
	}

	/** {@code -Dtree.threads=n} searches on n threads where the builder supports it (the fast brute force packager) */
	public static <T> T threads(T builder) {
		int threads = Integer.getInteger("tree.threads", 1);
		if(threads > 1) {
			try {
				Method method = builder.getClass().getMethod("withThreads", int.class);
				method.invoke(builder, threads);
			} catch (NoSuchMethodException e) {
				// a version without the option: ignore
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		}
		return apply(builder);
	}

	public static <T> T apply(T builder) {
		String anyBest = System.getProperty("tree.anyBest");
		if(anyBest != null) {
			try {
				Method method = builder.getClass().getMethod("withTreeSearchAnyBest", boolean.class);
				method.invoke(builder, Boolean.parseBoolean(anyBest));
			} catch (NoSuchMethodException e) {
				// a version without the option: ignore
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		}
		return builder;
	}
}
