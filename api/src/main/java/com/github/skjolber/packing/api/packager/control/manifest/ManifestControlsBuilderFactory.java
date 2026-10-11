package com.github.skjolber.packing.api.packager.control.manifest;

/**
 * Creates the {@link ManifestControlsBuilder} which filters the box items for a container. Configure it per container
 * with {@code ContainerItemBuilder.withManifestControlsBuilderFactory(..)}.
 * <p>
 * <b>Result reuse:</b> packagers may reuse an intermediate result for another container (see
 * {@code PackagerSession.peek(..)}) when the containers' factories carry the same {@linkplain #getId() id}, or are the same instance.
 * The default id is {@code null}, which means no reuse across instances (a lambda has no id, see {@link #of(String, ManifestControlsBuilderFactory)}).
 * <p>
 * <b>Thread-safety:</b> packagers run concurrently, for example with a parallel container packing strategy or a parallel
 * brute-force packager, so implementations must be safe for concurrent use; stateless implementations are.
 */
@FunctionalInterface
public interface ManifestControlsBuilderFactory {

	/**
	 * Wrap a factory, typically a lambda, so that it carries an id and thereby participates in result reuse.
	 *
	 * @param id      the id, see {@link #getId()}
	 * @param factory the factory to delegate to
	 * @return a factory which delegates to the argument factory, and has the argument id
	 */
	static ManifestControlsBuilderFactory of(String id, ManifestControlsBuilderFactory factory) {
		if(id == null) {
			throw new IllegalArgumentException("Expected id");
		}
		if(factory == null) {
			throw new IllegalArgumentException("Expected factory");
		}
		return new ManifestControlsBuilderFactory() {
			@Override
			public ManifestControlsBuilder createManifestControlsBuilder() {
				return factory.createManifestControlsBuilder();
			}

			@Override
			public String getId() {
				return id;
			}
		};
	}

	ManifestControlsBuilder createManifestControlsBuilder();

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
