package com.github.skjolber.packing.validator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.ValidatorResult;
import com.github.skjolber.packing.api.validator.manifest.ManifestValidator;
import com.github.skjolber.packing.api.validator.placement.PlacementValidator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplierBuilder;

public class DefaultValidator extends AbstractValidator<DefaultValidator.DefaultValidatorResultBuilder> {

	public class DefaultValidatorResultBuilder extends AbstractValidatorResultBuilder<DefaultValidatorResultBuilder> {
		
		public ValidatorResult build() {
			validate();
			
			if( (items == null || items.isEmpty()) && (itemGroups == null || itemGroups.isEmpty())) {
				throw new IllegalStateException();
			}
			long start = System.currentTimeMillis();

			PackagerInterruptSupplierBuilder booleanSupplierBuilder = PackagerInterruptSupplierBuilder.builder();
			if(deadline != -1L) {
				booleanSupplierBuilder.withDeadline(deadline);
			}
			if(interrupt != null) {
				booleanSupplierBuilder.withInterrupt(interrupt);
			}
			
			Map<String, ValidatorContainerItem> containersById = new HashMap<>();
			for (ValidatorContainerItem validatorContainerItem : containers) {
				containersById.put(validatorContainerItem.getContainer().getId(), validatorContainerItem);
			}

			PackagerInterruptSupplier interrupt = booleanSupplierBuilder.build();
			try {
				boolean valid;
				if(items != null && !items.isEmpty()) {
					valid = validateBoxItems(items, order, containersById, packagerResult, interrupt);
				} else {
					valid = validateBoxItemGroups(itemGroups, order, containersById, packagerResult, interrupt);
				}
				
				long duration = System.currentTimeMillis() - start;
				return new ValidatorResult(duration, valid, false);
			} catch (ValidatorInterruptedException e) {
				long duration = System.currentTimeMillis() - start;
				return new ValidatorResult(duration, false, true);
			} finally {
				interrupt.close();
			}
		}
	}
	
	@Override
	public DefaultValidatorResultBuilder newResultBuilder() {
		return new DefaultValidatorResultBuilder();
	}

	private boolean validateBoxItems(List<BoxItem> items, Order order, Map<String, ValidatorContainerItem> containers, PackagerResult result, PackagerInterruptSupplier interrupt) throws ValidatorInterruptedException {
		// validate first that the box items and containers are used in correct numbers
		if(!validateContainerItemCounts(containers, result)) {
			return false;
		}
		if(!validateBoxItemCounts(items, result)) {
			return false;
		}
		if(!validateLoad(containers, result)) {
			return false;
		}

		return validate(containers, result, interrupt);
	}

	public boolean validateBoxItemGroups(List<BoxItemGroup> itemGroups, Order order, Map<String, ValidatorContainerItem> containers, PackagerResult result, PackagerInterruptSupplier interrupt) throws ValidatorInterruptedException {
		// validate first that the box items, groups and containers are used in correct numbers
		if(!validateContainerItemCounts(containers, result)) {
			return false;
		}
		if(!validateBoxItemGroupsCounts(itemGroups, result)) {
			return false;
		}
		if(!validateLoad(containers, result)) {
			return false;
		}

		return validate(containers, result, interrupt);
	}
	
	protected boolean validate(Map<String, ValidatorContainerItem> containers, PackagerResult result, PackagerInterruptSupplier interrupt) throws ValidatorInterruptedException {
		for (Container container : result.getContainers()) {
			ValidatorContainerItem referenceItem = containers.get(container.getId());
			
			if(referenceItem.hasManifestValidatorBuilderFactory()) {
				List<Box> boxes = new ArrayList<>();
				for (Placement placement : container.getStack()) {
					boxes.add(placement.getBox());
				}
				ManifestValidator manifestValidator = referenceItem.createManifestValidator(referenceItem.getContainer());
				if(!manifestValidator.isValid(boxes)){
					return false;
				}
			}
			if(referenceItem.hasPlacementValidatorBuilderFactory()) {
				PlacementValidator placementValidator = referenceItem.createPlacementValidator(referenceItem.getContainer());
				if(!placementValidator.isValid(container.getStack().getPlacements())) {
					return false;
				}
			}
		}
		
		return true;
	}

	@Override
	public void close() throws IOException {
		// do nothing
	}

}
