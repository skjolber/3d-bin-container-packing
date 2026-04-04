package com.github.skjolber.packing.packer;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ContainerItemLoadCalculations {
	
	private static final Comparator<ContainerItemLoadCalculation> COST_COMPARATOR = new Comparator<ContainerItemLoadCalculation>() {
		@Override
		public int compare(ContainerItemLoadCalculation o1, ContainerItemLoadCalculation o2) {
			return Long.compare(o1.getCostCalculator().calculateCost(o1.getContainer()), o2.getCostCalculator().calculateCost(o2.getContainer()));
		}
	};
	
	
	private static final ContainerItemLoadCalculations EMPTY = new ContainerItemLoadCalculations(Collections.emptyList(), false);
	
	public static ContainerItemLoadCalculations empty() {
		return EMPTY;
	}

	protected final List<ContainerItemLoadCalculation> containerItemLoadCalculations;
	
	protected final boolean cost;
	
	public ContainerItemLoadCalculations(List<ContainerItemLoadCalculation> containerItemLoadCalculations, boolean cost) {
		super();
		this.containerItemLoadCalculations = containerItemLoadCalculations;
		this.cost = cost;
		
		if(cost) {
			for (ContainerItemLoadCalculation containerItemLoadCalculation : containerItemLoadCalculations) {
				if(containerItemLoadCalculation.getCostCalculator() == null) {
					throw new IllegalArgumentException("Cost calculator is required for cost calculations");
				}
			}
		}
	}
	
	public boolean isCost() {
		return cost;
	}
	
	public int size() {
		return containerItemLoadCalculations.size();
	}
	
	public ContainerItemLoadCalculation get(int index) {
		return containerItemLoadCalculations.get(index);
	}
	
	public List<ContainerItemLoadCalculation> getContainerItemLoadCalculations() {
		return containerItemLoadCalculations;
	}
	
	public boolean isEmpty() {
		return containerItemLoadCalculations.isEmpty();
	}

	public void remove(int i) {
		containerItemLoadCalculations.remove(i);
	}
}
