package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackResultComparator;
import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager.Builder;


/**
 * {@linkplain Packager} builder scaffold.
 * 
 */

public abstract class AbstractPackagerBuilder<P extends Packager, B extends AbstractPackagerBuilder<P, B>> {

	protected List<Container> containers;
	protected int checkpointsPerDeadlineCheck = 1;
	protected PackResultComparator packResultComparator;
	
	public B withContainers(Container ...  containers) {
		if(this.containers == null) {
			this.containers = new ArrayList<>();
		}
		for (Container container : containers) {
			this.containers.add(container);
		}
		return (B) this;
	}
	
	public B withContainers(List<Container> containers) {
		this.containers = containers;
		return (B) this;
	}
	
	public B withPackResultComparator(PackResultComparator packResultComparator) {
		this.packResultComparator = packResultComparator;
		
		return (B) this;
	}
	

	public B withCheckpointsPerDeadlineCheck(int n) {
		this.checkpointsPerDeadlineCheck = n;
		return (B) this;
	}
	
	public abstract P build();
		
}
