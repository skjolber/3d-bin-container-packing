package com.github.skjolber.packing.virtualbox;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.packer.PackagerInterruptedException;
import com.github.skjolber.packing.virtualbox.bounds.VirtualBoxBoundsLoadSupport;

/**
 * Validates loads on physical boxes, never on aggregate box approximations.
 * Does not imply stability or full support. One instance is reusable within an operation.
 */
public class VirtualBoxLoadValidator {
	
	protected final VirtualBoxBoundsLoadSupport support;

	public VirtualBoxLoadValidator(PackagerInterruptSupplier interrupt) {
		support = new VirtualBoxBoundsLoadSupport(interrupt);
	}

	public boolean isValid(VirtualBoxLayout layout) throws PackagerInterruptedException {
		return support.isValidLayout(layout.getPlacements().toArray(Placement[]::new));
	}

	/** Return a container with a rebuilt physical support graph, or null if any load constraint fails. */
	public Container validate(Container container) throws PackagerInterruptedException {
		Placement[] placements = container.getStack().getPlacements().toArray(Placement[]::new);
		if(!support.isValidLayout(placements)) {
			return null;
		}
		Container result = container.clone(placements.length);
		for(Placement placement : support.createSnapshot(placements, null).getPlacements()) {
			result.getStack().add(placement);
		}
		return result;
	}
}
