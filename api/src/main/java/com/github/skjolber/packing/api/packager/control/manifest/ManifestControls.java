package com.github.skjolber.packing.api.packager.control.manifest;

import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.point.PointSource;

/**
 * 
 * Controls (filter) for items which are available for load into some particular container.
 * 
 * The filter is expected to maintain underlying {@linkplain BoxItemSource} and {@linkplain PointSource} instances.
 * 
 */

public interface ManifestControls extends ManifestListener {

}
