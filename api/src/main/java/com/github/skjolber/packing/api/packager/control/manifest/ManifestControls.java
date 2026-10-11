package com.github.skjolber.packing.api.packager.control.manifest;

import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.point.PointSource;

/**
 * Controls (filter) for items which are available for load into some particular container.
 * <p>
 * Manifest controls filter by changing the shared {@linkplain BoxItemSource} (and {@linkplain PointSource}) which the
 * {@link ManifestControlsBuilder} was given: box items which are removed from the source are not attempted in this container.
 * For example, controls which keep two kinds of goods apart remove the box items of the other kind from the source once a
 * box of the first kind is {@linkplain #accepted(com.github.skjolber.packing.api.BoxItem) accepted}. The packager reads the
 * source again before every placement, so there is nothing to return from the callbacks.
 * <p>
 * The callbacks are those of {@link ManifestListener}; see there for when they are called, and in which order.
 * <p>
 * Manifest controls are created per packing attempt in a container, by the {@link ManifestControlsBuilderFactory} of the
 * container item.
 */

public interface ManifestControls extends ManifestListener {

}
