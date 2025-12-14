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
import com.github.skjolber.packing.api.Validator;
import com.github.skjolber.packing.api.ValidatorResultBuilder;

/**
 * 
 */

public abstract class AbstractValidator<B extends ValidatorResultBuilder> implements Validator<B> {


	protected boolean validateBoxItemCounts(List<BoxItem> items, PackagerResult result) {
		
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
				return false;
			}

			if(count != boxItem.getCount()) {
				return false; 
			}
		}
		
		return true;
	}

	protected boolean validateContainerItemCounts(Map<String, ValidatorContainerItem> containersById, PackagerResult result) {
		Map<String, List<Container>> resultContainersById = new HashMap<>();
		
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
				return false;
			}
			
			List<Container> list = entry.getValue();
			if(list.size() > referenceItem.getCount()) {
				return false; 
			}
		}
		
		return true;
	}
	
	protected boolean validateLoad(Map<String, ValidatorContainerItem> referenceContainersById, PackagerResult result) {
		for (Container container : result.getContainers()) {
			ValidatorContainerItem referenceContainerItem = referenceContainersById.get(container.getId());
			if(referenceContainerItem == null) {
				return false;
			}
			
			Container referenceContainer = referenceContainerItem.getContainer();
			
			Stack stack = container.getStack();
			if(stack.getVolume() > referenceContainer.getMaxLoadVolume()) {
				return false;
			}
			
			if(stack.getWeight() > referenceContainer.getMaxLoadWeight()) {
				return false;
			}
			
			List<Placement> placements = stack.getPlacements();
			for (Placement placement : placements) {
				if(!isInside(referenceContainer, placement)) {
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

		return false;
	}

	// check that each group is just within one container
	protected boolean validateBoxItemGroupsCounts(List<BoxItemGroup> groups, PackagerResult result) {
		
		Map<String, BoxItemGroup> boxToGroup = new HashMap<>();
		
		for (BoxItemGroup boxItemGroup : groups) {
			Set<String> set = new HashSet<>();
			
			for (BoxItem item: boxItemGroup.getItems()) {
				boxToGroup.put(item.getBox().getId(), boxItemGroup);

				set.add(item.getBox().getId());
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
					return false;
				}

				groupsInContainer.put(id, boxToGroup.get(id));
			}

			for (Entry<String, BoxItemGroup> entry : groupsInContainer.entrySet()) {
				BoxItemGroup boxItemGroup = entry.getValue();
				
				for (BoxItem boxItem : boxItemGroup.getItems()) {
					Integer count = resultCount.remove(boxItem.getBox().getId());

					if(count == null) {
						return false;
					}
					
					if(count.intValue() != boxItem.getCount()) {
						return false;
					}
				}
			}
			
			// is there any boxes which did not belong to a group?
			if(!resultCount.isEmpty()) {
				return false;
			}
		}
				
		return false;
	}

}
