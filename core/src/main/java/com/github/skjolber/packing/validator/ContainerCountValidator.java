package com.github.skjolber.packing.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.reasons.ContainerCountTooHighReason;
import com.github.skjolber.packing.validator.reasons.UnknownContainerIdReason;

public class ContainerCountValidator {


	public boolean validate(int maxContainerCount, List<ValidatorContainerItem> containers, PackagerResult result, List<ValidatorResultReason> reasons) {
		Map<String, ValidatorContainerItem> containersById = new HashMap<>();
		for (ValidatorContainerItem validatorContainerItem : containers) {
			containersById.put(validatorContainerItem.getContainer().getId(), validatorContainerItem);
		}
		
		Map<String, List<Container>> resultContainersById = new HashMap<>();
		
		if(result.getContainers().size() > maxContainerCount) {
			reasons.add(new ContainerCountTooHighReason("Expected max container count " + maxContainerCount + ", got " + result.getContainers().size()));
			return false;
		}
		
		for (Container container : result.getContainers()) {
			String id = container.getId();
			List<Container> list = resultContainersById.get(id);
			if(list == null) {
				list = new ArrayList<>();
				resultContainersById.put(id, list);
			}
			list.add(container);
		}

		for (Entry<String, List<Container>> entry : resultContainersById.entrySet()) {
			ValidatorContainerItem referenceItem = containersById.get(entry.getKey());
			if(referenceItem == null) {
				reasons.add(new UnknownContainerIdReason("Unknown container " + entry.getKey()));
				return false;
			}
			
			List<Container> list = entry.getValue();
			if(list.size() > referenceItem.getCount()) {
				reasons.add(new ContainerCountTooHighReason("Expected maximum " + referenceItem.getCount() + "'" + referenceItem.getContainer().getId() + "' containers, found " + list.size()));
				return false; 
			}
		}
		
		return true;
	}
}
