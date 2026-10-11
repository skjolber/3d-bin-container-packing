package com.github.skjolber.packing.validator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.api.interrupt.DefaultPackagerInterrupt;
import com.github.skjolber.packing.api.validator.ValidatorResult;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.manifest.ManifestValidator;
import com.github.skjolber.packing.api.validator.placement.PlacementValidator;
import com.github.skjolber.packing.validator.load.DefaultLoadValidatorBuilder;
import com.github.skjolber.packing.validator.reasons.ValidatorInterruptedException;

public class DefaultValidator extends AbstractValidator<DefaultValidator.DefaultValidatorResultBuilder> {

	private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

	public DefaultValidator() {
		scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1, runnable -> {
			Thread thread = new Thread(runnable, "packing-validator-deadline");
			thread.setDaemon(true);
			return thread;
		});
		scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
	}

	public class DefaultValidatorResultBuilder extends AbstractValidatorResultBuilder<DefaultValidatorResultBuilder> {
		
		public ValidatorResult build() {
			validate();
			
			if( (items == null || items.isEmpty()) && (itemGroups == null || itemGroups.isEmpty())) {
				throw new IllegalStateException();
			}
			long start = System.currentTimeMillis();

			PackagerInterruptSupplierBuilder interruptBuilder = PackagerInterruptSupplierBuilder.newBuilder();
			interruptBuilder.withScheduledExecutorService(scheduledThreadPoolExecutor);
			if(deadline != -1L) {
				interruptBuilder.withDeadline(deadline);
			}
			if(interrupt != null) {
				interruptBuilder.withInterrupt(new DefaultPackagerInterrupt(interrupt));
			}

			PackagerInterruptSupplier interrupt = interruptBuilder.build();
			PlacementCoverage coverage = new PlacementCoverage(defaultPlacementValidation);
			try {
				checkInterrupted(interrupt);
				List<ValidatorResultReason> reasons = new ArrayList<>();
				boolean valid;
				if(items != null && !items.isEmpty()) {
					valid = validateBoxItems(items, order, maxContainerCount, containers, packagerResult, interrupt, coverage, reasons);
				} else {
					valid = validateBoxItemGroups(itemGroups, order, maxContainerCount, containers, packagerResult, interrupt, coverage, reasons);
				}
				
				long duration = System.currentTimeMillis() - start;
				
				int containerCount = packagerResult.getContainers().size();
				return new ValidatorResult(duration, valid, false, reasons, coverage.isLoadValidated(containerCount), coverage.isStabilityValidated(containerCount), coverage.isLoadConstrained());
			} catch (ValidatorInterruptedException e) {
				long duration = System.currentTimeMillis() - start;
				return new ValidatorResult(duration, false, true, Collections.emptyList());
			} finally {
				interrupt.close();
			}
		}
	}
	
	private BoxItemOrderValidator orderValidator = new BoxItemOrderValidator();
	private BoxCountValidator boxCountValidator = new BoxCountValidator();
	private ContainerCountValidator containerCountValidator = new ContainerCountValidator();
	private StackValidator stackValidator = new StackValidator();
	private ObstacleIntersectionValidator obstacleValidator = new ObstacleIntersectionValidator();
	private InsertionOrderValidator insertionOrderValidator = new InsertionOrderValidator();
	private ExtractionOrderValidator extractionOrderValidator = new ExtractionOrderValidator();
	private GroupInsertionValidator groupInsertionValidator = new GroupInsertionValidator();
	private ContainerPriorityValidator containerPriorityValidator = new ContainerPriorityValidator();
	
	@Override
	public DefaultValidatorResultBuilder newResultBuilder() {
		return new DefaultValidatorResultBuilder();
	}

	private boolean validateBoxItems(List<BoxItem> items, Order order, int maxContainerCount, List<ValidatorContainerItem> containers, PackagerResult result,
			PackagerInterruptSupplier interrupt, PlacementCoverage coverage, List<ValidatorResultReason> reasons) throws ValidatorInterruptedException {
		
		checkInterrupted(interrupt);
		if(!orderValidator.validate(items, order, result.getContainers())) {
			return false;
		}
		
		// validate first that the box items and containers are used in correct numbers
		checkInterrupted(interrupt);
		if(!containerCountValidator.validate(maxContainerCount, containers, result, reasons)) {
			return false;
		}
		checkInterrupted(interrupt);
		if(!boxCountValidator.validate(items, result, reasons)) {
			return false;
		}
		checkInterrupted(interrupt);
		if(!stackValidator.validate(containers, result, interrupt, reasons)) {
			return false;
		}
		// boxes are in containers in order of their container priority
		if(!containerPriorityValidator.validate(result.getContainers(), reasons)) {
			return false;
		}

		return validate(containers, result, interrupt, coverage, reasons);
	}

	private boolean validateBoxItemGroups(List<BoxItemGroup> itemGroups, Order order, int maxContainerCount, List<ValidatorContainerItem> containers, PackagerResult result,
			PackagerInterruptSupplier interrupt, PlacementCoverage coverage, List<ValidatorResultReason> reasons) throws ValidatorInterruptedException {

		checkInterrupted(interrupt);
		if(!orderValidator.validate(itemGroups, order, result)) {
			return false;
		}
		
		// validate first that the box items, groups and containers are used in correct numbers
		checkInterrupted(interrupt);
		if(!containerCountValidator.validate(maxContainerCount, containers, result, reasons)) {
			return false;
		}
		checkInterrupted(interrupt);
		if(!boxCountValidator.validateBoxItemGroupsCounts(itemGroups, result, reasons)) {
			return false;
		}
		checkInterrupted(interrupt);
		if(!stackValidator.validate(containers, result, interrupt, reasons)) {
			return false;
		}
		// boxes are in containers in order of their container priority
		if(!containerPriorityValidator.validate(result.getContainers(), reasons)) {
			return false;
		}

		return validate(containers, result, interrupt, coverage, reasons);
	}

	private boolean validate(List<ValidatorContainerItem> containers, PackagerResult result, PackagerInterruptSupplier interrupt, PlacementCoverage coverage,
			List<ValidatorResultReason> reasons) throws ValidatorInterruptedException {
		
		Map<String, ValidatorContainerItem> containersById = new HashMap<>();
		for (ValidatorContainerItem validatorContainerItem : containers) {
			containersById.put(validatorContainerItem.getContainer().getId(), validatorContainerItem);
		}		
		
		for (Container container : result.getContainers()) {
			checkInterrupted(interrupt);
			ValidatorContainerItem referenceItem = containersById.get(container.getId());

			// the boxes leave the obstacles of the container empty
			if(!obstacleValidator.validate(container, reasons)) {
				return false;
			}
			// the placements must be in a possible insertion order
			if(!insertionOrderValidator.validate(container, reasons)) {
				return false;
			}
			// boxes can be extracted in their extraction order
			if(!extractionOrderValidator.validate(container, reasons)) {
				return false;
			}
			// the boxes of each group are inserted together
			if(!groupInsertionValidator.validate(container, reasons)) {
				return false;
			}
			
			if(referenceItem.hasManifestValidatorBuilderFactory()) {
				List<Box> boxes = new ArrayList<>();
				for (Placement placement : container.getStack()) {
					checkInterrupted(interrupt);
					boxes.add(placement.getBox());
				}
				ManifestValidator manifestValidator = referenceItem.createManifestValidator(referenceItem.getContainer());
				if(!manifestValidator.isValid(boxes, reasons)){
					return false;
				}
			}
			if(!validatePlacements(referenceItem, container, coverage, reasons)) {
				return false;
			}
		}
		
		return true;
	}

	/**
	 * Validate the placements of a container. With a placement validator builder factory, that decides what is validated. Without, the load
	 * constraints which the boxes declare are validated (unless opted out of), and there is no stability rule. As the load validators
	 * build a support graph, nothing is created if no box declares a load constraint.
	 */

	private boolean validatePlacements(ValidatorContainerItem referenceItem, Container container, PlacementCoverage coverage, List<ValidatorResultReason> reasons) {
		List<Placement> placements = container.getStack().getPlacements();
		if(referenceItem.hasPlacementValidatorBuilderFactory()) {
			PlacementValidator placementValidator = referenceItem.createPlacementValidator(referenceItem.getContainer(), placements);
			boolean valid = placementValidator.isValid(placements, reasons);
			boolean loadValidated = placementValidator.isLoadValidated();
			coverage.add(loadValidated, placementValidator.isStabilityValidated(), loadValidated && DefaultLoadValidatorBuilder.hasLoadConstraints(placements));
			return valid;
		}
		if(!coverage.isDefaultPlacementValidation()) {
			return true;
		}
		if(!DefaultLoadValidatorBuilder.hasLoadConstraints(placements)) {
			// the loads are validated, but there is nothing to check
			coverage.add(true, false, false);
			return true;
		}
		PlacementValidator placementValidator = referenceItem.createPlacementValidator(referenceItem.getContainer(), placements);
		boolean valid = placementValidator.isValid(placements, reasons);
		coverage.add(placementValidator.isLoadValidated(), placementValidator.isStabilityValidated(), true);
		return valid;
	}

	protected void checkInterrupted(PackagerInterruptSupplier interrupt) throws ValidatorInterruptedException {
		if(interrupt.getAsBoolean()) {
			throw new ValidatorInterruptedException();
		}
	}

	@Override
	public void close() throws IOException {
		scheduledThreadPoolExecutor.shutdownNow();
	}

}
