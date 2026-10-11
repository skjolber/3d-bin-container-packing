package com.github.skjolber.packing.validator;

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
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.reasons.BoxItemCountTooHighReason;
import com.github.skjolber.packing.validator.reasons.BoxItemCountTooLowReason;
import com.github.skjolber.packing.validator.reasons.TooFewBoxItemIdsReason;
import com.github.skjolber.packing.validator.reasons.TooManyBoxItemIdsReason;

public class BoxCountValidator {

	// check that each group is just within one container
	public boolean validateBoxItemGroupsCounts(List<BoxItemGroup> groups, PackagerResult result, List<ValidatorResultReason> reasons) {
		
		Map<String, BoxItemGroup> boxToGroup = new HashMap<>();
		Set<String> expectedGroups = new HashSet<>();
		
		for (BoxItemGroup boxItemGroup : groups) {
			expectedGroups.add(boxItemGroup.getId());
			
			for (BoxItem item: boxItemGroup.getItems()) {
				boxToGroup.put(item.getBox().getId(), boxItemGroup);
			}
		}
		
		Set<String> consumedGroups = new HashSet<>();
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

				groupsInContainer.put(boxItemGroup.getId(), boxItemGroup);
			}

			for (Entry<String, BoxItemGroup> entry : groupsInContainer.entrySet()) {
				BoxItemGroup boxItemGroup = entry.getValue();
				if(!consumedGroups.add(entry.getKey())) {
					reasons.add(new TooManyBoxItemIdsReason("Group " + entry.getKey() + " found in multiple containers"));
					return false;
				}
				
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
		if(!consumedGroups.equals(expectedGroups)) {
			Set<String> missing = new HashSet<>(expectedGroups);
			missing.removeAll(consumedGroups);
			reasons.add(new TooFewBoxItemIdsReason("Missing groups " + missing));
			return false;
		}
				
		return true;
	}

	public boolean validate(List<BoxItem> items, PackagerResult result, List<ValidatorResultReason> reasons) {
		Map<String, Integer> expectedCount = new HashMap<>();
		for (BoxItem boxItem : items) {
			expectedCount.merge(boxItem.getBox().getId(), boxItem.getCount(), Integer::sum);
		}
		
		Map<String, Integer> resultCount = new HashMap<>();
		for (Container container : result.getContainers()) {
			Stack stack = container.getStack();
			for (Placement placement : stack.getPlacements()) {
				Box box = placement.getBox();
				
				String id = box.getId();
				if(!expectedCount.containsKey(id)) {
					reasons.add(new TooManyBoxItemIdsReason(id + ": Not expected"));
					return false;
				}

				Integer integer = resultCount.get(id);
				if(integer == null) {
					resultCount.put(id, 1);
				} else {
					resultCount.put(id, integer + 1);
				}
			}
		}
		
		for (Entry<String, Integer> entry : expectedCount.entrySet()) {
			String id = entry.getKey();
			
			Integer count = resultCount.get(id);
			
			if(count == null) {
				reasons.add(new TooFewBoxItemIdsReason(id + ": Not found"));
				return false;
			}

			if(count < entry.getValue()) {
				reasons.add(new BoxItemCountTooLowReason(id + ": Expected " + entry.getValue() + ", found " + count));
				return false;
			} else if(count > entry.getValue()) {
				reasons.add(new BoxItemCountTooHighReason(id + ": Expected " + entry.getValue() + ", found " + count));
				return false;
			}
		}
		
		return true;
	}

}
