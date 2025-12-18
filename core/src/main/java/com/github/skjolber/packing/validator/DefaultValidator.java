package com.github.skjolber.packing.validator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import com.github.skjolber.packing.api.validator.ValidatorResult;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.manifest.ManifestValidator;
import com.github.skjolber.packing.api.validator.placement.PlacementValidator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.validator.reasons.ValidatorInterruptedException;

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
				List<ValidatorResultReason> reasons = new ArrayList<>();
				boolean valid;
				if(items != null && !items.isEmpty()) {
					valid = validateBoxItems(items, order, containersById, packagerResult, interrupt, reasons);
				} else {
					valid = validateBoxItemGroups(itemGroups, order, containersById, packagerResult, interrupt, reasons);
				}
				
				long duration = System.currentTimeMillis() - start;
				
				return new ValidatorResult(duration, valid, false, reasons);
			} catch (ValidatorInterruptedException e) {
				long duration = System.currentTimeMillis() - start;
				return new ValidatorResult(duration, false, true, Collections.emptyList());
			} finally {
				interrupt.close();
			}
		}
	}
	
	@Override
	public DefaultValidatorResultBuilder newResultBuilder() {
		return new DefaultValidatorResultBuilder();
	}

	private boolean validateBoxItems(List<BoxItem> items, Order order, Map<String, ValidatorContainerItem> containers, PackagerResult result, PackagerInterruptSupplier interrupt, List<ValidatorResultReason> reasons) throws ValidatorInterruptedException {
		// validate first that the box items and containers are used in correct numbers
		if(!validateContainerItemCounts(containers, result, reasons)) {
			return false;
		}
		if(!validateBoxItemCounts(items, result, reasons)) {
			return false;
		}
		if(!validateLoad(containers, result, reasons)) {
			return false;
		}

		return validate(containers, result, interrupt, reasons);
	}

	public boolean validateBoxItemGroups(List<BoxItemGroup> itemGroups, Order order, Map<String, ValidatorContainerItem> containers, PackagerResult result, PackagerInterruptSupplier interrupt, List<ValidatorResultReason> reasons) throws ValidatorInterruptedException {
		// validate first that the box items, groups and containers are used in correct numbers
		if(!validateContainerItemCounts(containers, result, reasons)) {
			return false;
		}
		if(!validateBoxItemGroupsCounts(itemGroups, result, reasons)) {
			return false;
		}
		if(!validateLoad(containers, result, reasons)) {
			return false;
		}

		return validate(containers, result, interrupt, reasons);
	}
	
	protected boolean validate(Map<String, ValidatorContainerItem> containers, PackagerResult result, PackagerInterruptSupplier interrupt, List<ValidatorResultReason> reasons) throws ValidatorInterruptedException {
		for (Container container : result.getContainers()) {
			ValidatorContainerItem referenceItem = containers.get(container.getId());
			
			if(referenceItem.hasManifestValidatorBuilderFactory()) {
				List<Box> boxes = new ArrayList<>();
				for (Placement placement : container.getStack()) {
					boxes.add(placement.getBox());
				}
				ManifestValidator manifestValidator = referenceItem.createManifestValidator(referenceItem.getContainer());
				if(!manifestValidator.isValid(boxes, reasons)){
					return false;
				}
			}
			if(referenceItem.hasPlacementValidatorBuilderFactory()) {
				PlacementValidator placementValidator = referenceItem.createPlacementValidator(referenceItem.getContainer());
				if(!placementValidator.isValid(container.getStack().getPlacements(), reasons)) {
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
