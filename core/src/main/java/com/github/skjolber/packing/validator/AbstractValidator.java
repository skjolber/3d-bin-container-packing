package com.github.skjolber.packing.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.validator.Validator;
import com.github.skjolber.packing.api.validator.ValidatorResultBuilder;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.reasons.BoxItemCountTooHighReason;
import com.github.skjolber.packing.validator.reasons.BoxItemCountTooLowReason;
import com.github.skjolber.packing.validator.reasons.BoxesIntersectReason;
import com.github.skjolber.packing.validator.reasons.BoxesOutsideContainerReason;
import com.github.skjolber.packing.validator.reasons.ContainerCountTooHighReason;
import com.github.skjolber.packing.validator.reasons.TooFewBoxItemIdsReason;
import com.github.skjolber.packing.validator.reasons.TooHighVolumeReason;
import com.github.skjolber.packing.validator.reasons.TooHighWeightReason;
import com.github.skjolber.packing.validator.reasons.TooManyBoxItemIdsReason;
import com.github.skjolber.packing.validator.reasons.TooManyContainerIdsReason;

/**
 * 
 */

public abstract class AbstractValidator<B extends ValidatorResultBuilder> implements Validator<B> {


	protected boolean validateBoxItemCounts(List<BoxItem> items, PackagerResult result, List<ValidatorResultReason> reasons) {
		
		Map<String, Integer> resultCount = new HashMap<>();
		for (Container container : result.getContainers()) {
			Stack stack = container.getStack();
			for (Placement placement : stack.getPlacements()) {
				Box box = placement.getBox();
				
				String id = box.getId();

				Integer integer = resultCount.get(id);
				if(integer == null) {
					resultCount.put(id, 1);
				} else {
					resultCount.put(id, integer + 1);
				}
			}
		}
		
		for (BoxItem boxItem : items) {
			String id = boxItem.getBox().getId();
			
			Integer count = resultCount.get(id);
			
			if(count == null) {
				reasons.add(new TooFewBoxItemIdsReason(id + ": Not found"));
				return false;
			}

			if(count < boxItem.getCount()) {
				reasons.add(new BoxItemCountTooLowReason(id + ": Expected " + boxItem.getCount() + ", found " + count));
				return false;
			} else if(count > boxItem.getCount()) {
				reasons.add(new BoxItemCountTooHighReason(id + ": Expected " + boxItem.getCount() + ", found " + count));
				return false;
			}
		}
		
		return true;
	}

	protected boolean validateContainerItemCounts(int maxContainerCount, Map<String, ValidatorContainerItem> containersById, PackagerResult result, List<ValidatorResultReason> reasons) {
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
				reasons.add(new TooManyContainerIdsReason("Unknown container " + entry.getKey()));
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
	
	protected boolean validateLoad(Map<String, ValidatorContainerItem> referenceContainersById, PackagerResult result, List<ValidatorResultReason> reasons) {
		for (Container container : result.getContainers()) {
			ValidatorContainerItem referenceContainerItem = referenceContainersById.get(container.getId());
			if(referenceContainerItem == null) {
				reasons.add(new TooManyContainerIdsReason("Unknown container " + container.getId()));
				return false;
			}
			
			Container referenceContainer = referenceContainerItem.getContainer();
			
			Stack stack = container.getStack();
			if(stack.getVolume() > referenceContainer.getMaxLoadVolume()) {
				reasons.add(new TooHighVolumeReason("Expected maximum " + referenceContainer.getMaxLoadVolume() + ", found " + stack.getVolume()));
				return false;
			}
			
			if(stack.getWeight() > referenceContainer.getMaxLoadWeight()) {
				reasons.add(new TooHighWeightReason("Expected maximum " + referenceContainer.getMaxLoadWeight() + ", found " + stack.getWeight()));
				return false;
			}
			
			List<Placement> placements = stack.getPlacements();
			for (Placement placement : placements) {
				if(!isInside(referenceContainer, placement)) {
					reasons.add(new BoxesOutsideContainerReason("Box " + placement.getBox().getId() + " not placed within load limits"));
					return false;
				}
			}
			
			// check if boxes intersect
			for(int i = 0; i < placements.size(); i++) {
				Placement placement1 = placements.get(i);
				for(int k = 0; k < placements.size(); k++) {
					if(i == k) {
						continue;
					}
					Placement placement2 = placements.get(k);
					
					if(placement1.intersects(placement2)) {
						reasons.add(new BoxesIntersectReason(placement1.getBox().getId() + " intersects " + placement2.getBox().getId()));
						return false;
					}
				}
			}
		}
		
		return true;
	}

	private boolean isInside(Container container, Placement placement) {
		if(placement.getAbsoluteEndX() >= container.getLoadDx()) {
			return false;
		}
		if(placement.getAbsoluteEndY() >= container.getLoadDy()) {
			return false;
		}
		if(placement.getAbsoluteEndZ() >= container.getLoadDz()) {
			return false;
		}

		if(placement.getAbsoluteX() < 0) {
			return false;
		}
		if(placement.getAbsoluteY() < 0) {
			return false;
		}
		if(placement.getAbsoluteZ() < 0) {
			return false;
		}

		return true;
	}

	// check that each group is just within one container
	protected boolean validateBoxItemGroupsCounts(List<BoxItemGroup> groups, PackagerResult result, List<ValidatorResultReason> reasons) {
		
		Map<String, BoxItemGroup> boxToGroup = new HashMap<>();
		
		for (BoxItemGroup boxItemGroup : groups) {
			
			for (BoxItem item: boxItemGroup.getItems()) {
				boxToGroup.put(item.getBox().getId(), boxItemGroup);
			}
		}
		
		for (Container container : result.getContainers()) {
			Stack stack = container.getStack();

			Map<String, BoxItemGroup> groupsInContainer = new HashMap<>(); // box id to group

			Map<String, Integer> resultCount = new HashMap<>();
			for (Placement placement : stack.getPlacements()) {
				String id = placement.getBox().getId();
				
				Integer integer = resultCount.get(id);
				if(integer == null) {
					resultCount.put(id, 1);
				} else {
					resultCount.put(id, integer + 1);
				}
				
				BoxItemGroup boxItemGroup = boxToGroup.get(id);
				if(boxItemGroup == null) {
					reasons.add(new TooManyBoxItemIdsReason(id + " not found"));
					return false;
				}

				groupsInContainer.put(id, boxToGroup.get(id));
			}

			for (Entry<String, BoxItemGroup> entry : groupsInContainer.entrySet()) {
				BoxItemGroup boxItemGroup = entry.getValue();
				
				for (BoxItem boxItem : boxItemGroup.getItems()) {
					Integer count = resultCount.remove(boxItem.getBox().getId());

					if(count == null) {
						reasons.add(new TooFewBoxItemIdsReason(boxItem.getBox().getId() + " not found"));
						return false;
					}

					if(count < boxItem.getCount()) {
						reasons.add(new BoxItemCountTooLowReason("Box id " + boxItem.getBox().getId() + " expected count " + boxItem.getCount() + ", found " + count));
						return false;
					} else if(count > boxItem.getCount()) {
						reasons.add(new BoxItemCountTooHighReason("Box id " + boxItem.getBox().getId() + " expected count " + boxItem.getCount() + ", found " + count));
						return false;
					}
				}
			}
			
			// is there any boxes which did not belong to a group?
			if(!resultCount.isEmpty()) {
				reasons.add(new TooManyBoxItemIdsReason("Unexpectedly found " + resultCount.keySet()));
				return false;
			}
		}
				
		return true;
	}

}
