package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.Packager;

/**
 * {@linkplain Packager} builder scaffold.
 * 
 */

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractPackagerBuilder<P extends Packager, B extends AbstractPackagerBuilder<P, B>> {

	public abstract P build();

}
