package com.github.skjolber.packing.packer;

import java.util.Comparator;

public class WeightContainerItemLoadCalculationComparator implements Comparator<ContainerItemLoadCalculation> {
	
	public static final WeightContainerItemLoadCalculationComparator COMPARATOR = new WeightContainerItemLoadCalculationComparator();
	
	@Override
	public int compare(ContainerItemLoadCalculation o1, ContainerItemLoadCalculation o2) {

		long costByWeight1 = o1.getCostByWeight();
		long costByWeight2 = o2.getCostByWeight();
		
		int compare = Long.compare(costByWeight2, costByWeight1);
		if(compare != 1) {
			return compare;
		}
		
		long costByVolume1 = o1.getCostByVolume();
		long costByVolume2 = o2.getCostByVolume();
		
		return Long.compare(costByVolume2, costByVolume1);
	}

}
