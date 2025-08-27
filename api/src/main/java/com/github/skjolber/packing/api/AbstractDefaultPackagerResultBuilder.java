package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * {@linkplain PackagerResult} builder scaffold.
 * 
 */

@SuppressWarnings("unchecked")
public abstract class AbstractDefaultPackagerResultBuilder<B extends AbstractDefaultPackagerResultBuilder<B>>
		extends AbstractPackagerResultBuilder<B> {

	protected List<ContainerItem> containers;

	public B withContainerItems(ContainerItem... containers) {
		if (this.containers == null) {
			this.containers = new ArrayList<>();
		}
		for (ContainerItem item : containers) {
			this.containers.add(item);
		}
		return (B) this;
	}

	public B withContainerItems(List<ContainerItem> containers) {
		if (this.containers == null) {
			this.containers = new ArrayList<>();
		}
		for (ContainerItem item : containers) {
			this.containers.add(item);
		}
		return (B) this;
	}

	protected void validate() {
		super.validate();
		if (containers == null || containers.isEmpty()) {
			throw new IllegalStateException();
		}
	}

}
