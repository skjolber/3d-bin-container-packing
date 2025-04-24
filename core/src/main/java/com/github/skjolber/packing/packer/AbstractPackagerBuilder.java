package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.packager.IntermediatePlacementResultBuilderSupplier;
import com.github.skjolber.packing.api.packager.PackResultComparator;

/**
 * {@linkplain Packager} builder scaffold.
 * 
 */

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractPackagerBuilder<P extends Packager, B extends AbstractPackagerBuilder<P, B>> {

	public abstract P build();

}
