package com.github.skjolber.packing.test.example;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.ContainerInventory;
import com.github.skjolber.packing.api.packager.strategy.ContainerResult;
import com.github.skjolber.packing.api.packager.strategy.ContainerStrategy;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;

/**
 * Example container strategy, which depends on the api module only: fill the largest available container
 * (by load volume) with as many boxes as possible, then the next largest, until all boxes are packed.
 */
public class LargestContainerFirstStrategy implements ContainerStrategy {

	@Override
	public ContainerResult pack(PackagerInterruptSupplier interrupt, PackagerSession session) throws PackagerInterruptedException {
		ContainerInventory inventory = session.getContainerInventory();

		List<Container> containers = new ArrayList<>();
		while(session.countRemainingBoxes() > 0) {
			if(interrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}
			int index = getLargestAvailableContainer(inventory);
			if(index == -1) {
				// no more containers
				return null;
			}
			IntermediatePackagerResult result = session.attempt(index, null, false);
			if(result.isEmpty()) {
				return null;
			}
			containers.add(session.accept(result));
		}
		return new ContainerResult(inventory.getCost(), containers);
	}

	protected int getLargestAvailableContainer(ContainerInventory inventory) {
		if(inventory.getContainerCount() == 0) {
			return -1;
		}
		int index = -1;
		long maxLoadVolume = -1L;
		for(int i = 0; i < inventory.getContainerItemCount(); i++) {
			ContainerItem containerItem = inventory.getContainerItem(i);
			if(containerItem.isAvailable() && containerItem.getContainer().getMaxLoadVolume() > maxLoadVolume) {
				maxLoadVolume = containerItem.getContainer().getMaxLoadVolume();
				index = i;
			}
		}
		return index;
	}
}
