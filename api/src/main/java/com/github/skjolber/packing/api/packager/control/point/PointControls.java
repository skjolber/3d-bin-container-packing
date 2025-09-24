package com.github.skjolber.packing.api.packager.control.point;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestListener;
import com.github.skjolber.packing.api.point.PointSource;

/**
 * 
 * Point Controls (filter) for points which are available for a {@linkplain BoxItem}.
 * 
 * The filter is expected to maintain underlying {@linkplain BoxItemSource} and {@linkplain PointSource} instances.
 * 
 */

public interface PointControls extends ManifestListener {

	PointSource getPoints(BoxItem boxItem);

}
