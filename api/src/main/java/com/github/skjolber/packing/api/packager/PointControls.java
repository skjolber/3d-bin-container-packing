package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.ep.FilteredPoints;

/**
 * 
 * Controls (filter) for points which are available for load into some particular container.
 * 
 * The filter is expected to maintain underlying {@linkplain FilteredBoxItems} and {@linkplain FilteredPoints} instances.
 * 
 */

public interface PointControls extends ManifestListener {

	FilteredPoints getFilteredPoints(BoxItem boxItem);

}
