package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.PackResultComparator;
import com.github.skjolber.packing.api.Packager;

/**
 * {@linkplain Packager} builder scaffold.
 * 
 */

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractPackagerBuilder<P extends Packager, B extends AbstractPackagerBuilder<P, B>> {

	protected int checkpointsPerDeadlineCheck = 1;
	protected PackResultComparator packResultComparator;

	public B withPackResultComparator(PackResultComparator packResultComparator) {
		this.packResultComparator = packResultComparator;

		return (B)this;
	}

	public B withCheckpointsPerDeadlineCheck(int n) {
		this.checkpointsPerDeadlineCheck = n;
		return (B)this;
	}

	public abstract P build();

}
