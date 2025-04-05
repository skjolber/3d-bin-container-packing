package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.packager.PackResultComparator;

/**
 * {@linkplain Packager} builder scaffold.
 * 
 */

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractPackagerBuilder<P extends Packager, B extends AbstractPackagerBuilder<P, B>> {

	protected PackResultComparator packResultComparator;

	public B withPackResultComparator(PackResultComparator packResultComparator) {
		this.packResultComparator = packResultComparator;

		return (B)this;
	}

	public abstract P build();

}
