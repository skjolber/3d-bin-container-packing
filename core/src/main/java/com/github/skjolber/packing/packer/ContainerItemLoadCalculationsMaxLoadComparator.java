package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.ContainerItem;

public class ContainerItemLoadCalculationsMaxLoadComparator implements Comparator<ContainerItemLoadCalculation> {

	// compare the cost efficiency of the maximum load of a single container
	
	@Override
	public int compare(ContainerItemLoadCalculation o1, ContainerItemLoadCalculation o2) {

		// calculate how much weight the max volume of the container corresponds to
		ContainerItem container1 = o1.getContainer();
		
		
		
		long maxContainerWeight1 = Math.min(container1.getContainer().getMaxLoadWeight(), o1.getLoadableWeight());

		ContainerItem container2 = o2.getContainer();
		long maxContainerWeight2 = Math.min(container2.getContainer().getMaxLoadWeight(), o2.getLoadableWeight());

		
		o1.getCostCalculator().calculateCost(maxContainerVolume1);
		
		
		
		return 0;
	}
	
}
