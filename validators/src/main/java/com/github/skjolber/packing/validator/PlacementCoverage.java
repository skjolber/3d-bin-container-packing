package com.github.skjolber.packing.validator;

/**
 * Which placement checks a validation performed, for the containers of one packager result. Not thread safe: belongs to one validation.
 * 
 * @see com.github.skjolber.packing.api.validator.ValidatorResult#isLoadValidated()
 */

class PlacementCoverage {

	private final boolean defaultPlacementValidation;

	private int loadValidatedContainers;
	private int stabilityValidatedContainers;
	private boolean loadConstrained;

	/**
	 * @param defaultPlacementValidation whether to validate the loads of the container items which have no placement validator builder factory
	 */

	PlacementCoverage(boolean defaultPlacementValidation) {
		this.defaultPlacementValidation = defaultPlacementValidation;
	}

	boolean isDefaultPlacementValidation() {
		return defaultPlacementValidation;
	}

	/**
	 * Record the placement validation of a container.
	 * 
	 * @param loadValidated whether the loads were validated
	 * @param stabilityValidated whether a stability rule was validated
	 * @param loadConstrained whether any box declares a load constraint (only if the loads were validated)
	 */

	void add(boolean loadValidated, boolean stabilityValidated, boolean loadConstrained) {
		if(loadValidated) {
			loadValidatedContainers++;
			this.loadConstrained |= loadConstrained;
		}
		if(stabilityValidated) {
			stabilityValidatedContainers++;
		}
	}

	/**
	 * @param containerCount the number of containers of the packager result
	 * @return true if the loads of every container were validated
	 */

	boolean isLoadValidated(int containerCount) {
		return containerCount > 0 && loadValidatedContainers == containerCount;
	}

	/**
	 * @param containerCount the number of containers of the packager result
	 * @return true if a stability rule was validated for every container
	 */

	boolean isStabilityValidated(int containerCount) {
		return containerCount > 0 && stabilityValidatedContainers == containerCount;
	}

	boolean isLoadConstrained() {
		return loadConstrained;
	}
}
