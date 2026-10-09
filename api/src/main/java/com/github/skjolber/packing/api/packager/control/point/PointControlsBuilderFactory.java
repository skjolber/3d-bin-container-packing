package com.github.skjolber.packing.api.packager.control.point;

/**
 * Creates the {@link PointControlsBuilder} which filters the points available for each box item in a container. Configure it
 * per container with {@code ContainerItemBuilder.withPointControlsBuilderFactory(..)}.
 * <p>
 * <b>Result reuse:</b> packagers may reuse an intermediate result for another container (see
 * {@code PackagerSession.peek(..)}) when the containers' factories carry the same {@linkplain #getId() id}, or are the same instance.
 * The default id is {@code null}, which means no reuse across instances (a lambda has no id, see {@link #of(String, PointControlsBuilderFactory)}).
 * <p>
 * <b>Thread-safety:</b> packagers run concurrently, for example with a parallel container packing strategy or a parallel
 * brute-force packager, so implementations must be safe for concurrent use; stateless implementations are.
 */
@FunctionalInterface
public interface PointControlsBuilderFactory {

	/**
	 * Wrap a factory, typically a lambda, so that it carries an id and thereby participates in result reuse.
	 *
	 * @param id      the id, see {@link #getId()}
	 * @param factory the factory to delegate to
	 * @return a factory which delegates to the argument factory, and has the argument id
	 */
	static PointControlsBuilderFactory of(String id, PointControlsBuilderFactory factory) {
		if(id == null) {
			throw new IllegalArgumentException("Expected id");
		}
		if(factory == null) {
			throw new IllegalArgumentException("Expected factory");
		}
		return new PointControlsBuilderFactory() {
			@Override
			public PointControlsBuilder createPointControlsBuilder() {
				return factory.createPointControlsBuilder();
			}

			@Override
			public String getId() {
				return id;
			}
		};
	}

	PointControlsBuilder createPointControlsBuilder();

	/**
	 * Identifies the controls this factory builds. Packagers reuse a result packed for one container for another
	 * container when both factories carry the same id, or are the same instance. The id must cover everything
	 * which changes the controls' behaviour, for example a weight limit. Default null: no reuse across instances.
	 *
	 * @return the id, or null
	 */
	default String getId() {
		return null;
	}
}
