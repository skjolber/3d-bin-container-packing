package com.github.skjolber.packing.api.packager.control.point;

/**
 * Creates the {@link PointControlsBuilder} which filters the points available for each box item in a container. Configure it
 * per container with {@code ContainerItemBuilder.withPointControlsBuilderFactory(..)}.
 * <p>
 * <b>Result reuse:</b> packagers may reuse an intermediate result for another container (see
 * {@code PackagerSession.peek(..)}) when the containers' factories are {@linkplain Object#equals(Object) equal}.
 * Lambdas and method references of different expressions never compare equal, so reuse silently does not happen for them:
 * implement {@code equals} and {@code hashCode} (or share one factory instance between the containers) to enable it.
 * <p>
 * <b>Thread-safety:</b> packagers run concurrently, for example with a parallel container packing strategy or a parallel
 * brute-force packager, so implementations must be safe for concurrent use; stateless implementations are.
 */
@FunctionalInterface
public interface PointControlsBuilderFactory {
	
	PointControlsBuilder createPointControlsBuilder();
}
