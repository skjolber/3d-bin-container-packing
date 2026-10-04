package com.github.skjolber.packing.test.assertj;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResult;
import com.github.skjolber.packing.api.validator.ValidatorResultBuilder;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.LoadValidator;

public abstract class AbstractPackagerResultAssert<SELF extends AbstractPackagerResultAssert<SELF, ACTUAL>, ACTUAL extends PackagerResult>
		extends AbstractObjectAssert<SELF, ACTUAL> {

	protected AbstractPackagerResultAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF isSuccess() {
		isNotNull();
		if(!actual.isSuccess()) {
			failWithMessage("Expected success, got " + actual.size() + " container(s)");
		}
		return myself;
	}

	public SELF isNotSuccess() {
		isNotNull();
		if(actual.isSuccess()) {
			failWithMessage("Expected no success, got " + actual.size() + " container(s)");
		}
		return myself;
	}

	public SELF hasContainerCount(int count) {
		isNotNull();
		if(actual.size() != count) {
			failWithMessage("Expected " + count + " container(s), got " + actual.size());
		}
		return myself;
	}

	/**
	 * @param containerIndex index of the container in the result
	 * @param count expected number of placed boxes
	 */
	public SELF hasStackSize(int containerIndex, int count) {
		isNotNull();
		if(containerIndex >= actual.size()) {
			failWithMessage("Expected container at index " + containerIndex + ", got " + actual.size() + " container(s)");
		} else {
			int size = actual.get(containerIndex).getStack().size();
			if(size != count) {
				failWithMessage("Expected " + count + " box(es) in container " + containerIndex + ", got " + size);
			}
		}
		return myself;
	}

	/**
	 * At least one container, and every container is stacked within its load dimensions and weight, without intersecting placements.
	 */
	public SELF isStackedWithinConstraints() {
		isNotNull();
		List<Container> containers = actual.getContainers();
		if(containers.isEmpty()) {
			failWithMessage("Expected at least one container");
		}
		for(int i = 0; i < containers.size(); i++) {
			ContainerAssert.assertThat(containers.get(i)).as("container %s", i).isStackedWithinConstraints();
		}
		return myself;
	}

	/**
	 * Validate the result, for example {@code isAcceptedBy(validator.newResultBuilder().withContainerItems(..).withBoxItems(..))}.
	 *
	 * @param builder validator result builder with everything but the packager result
	 */
	public SELF isAcceptedBy(ValidatorResultBuilder builder) {
		isNotNull();
		ValidatorResult result = builder.withPackagerResult(actual).build();
		if(!result.isValid()) {
			failWithMessage("Expected result to be accepted by the validator, got reasons %s", AbstractValidatorResultAssert.describe(result.getReasons()));
		}
		return myself;
	}

	/**
	 * @param builder validator result builder with everything but the packager result
	 * @param reasons expected reason types, in order; empty for any reasons
	 */
	@SafeVarargs
	public final SELF isRejectedBy(ValidatorResultBuilder builder, Class<? extends ValidatorResultReason>... reasons) {
		isNotNull();
		ValidatorResult result = builder.withPackagerResult(actual).build();
		ValidatorResultAssert.assertThat(result).isNotValid();
		if(reasons.length > 0) {
			ValidatorResultAssert.assertThat(result).hasReasons(reasons);
		}
		return myself;
	}

	/**
	 * The placements of every container are accepted by every validator.
	 */
	public SELF isAcceptedBy(LoadValidator... validators) {
		isNotNull();
		List<Container> containers = actual.getContainers();
		for(int i = 0; i < containers.size(); i++) {
			List<Placement> placements = containers.get(i).getStack().getPlacements();
			for(LoadValidator validator : validators) {
				PlacementsAssert.assertThat(placements).as("container %s", i).isAcceptedBy(validator);
			}
		}
		return myself;
	}

	/**
	 * The result places exactly the given box item instances, each {@link BoxItem#getCount()} times, in one of their own stack values.
	 * Packagers place copies of the input, so this applies to results which refer to the input (for example virtual boxes).
	 */
	public SELF placesExactly(List<BoxItem> boxItems) {
		isNotNull();
		Map<BoxItem, Integer> counts = new IdentityHashMap<>();
		for(BoxItem boxItem : boxItems) {
			counts.put(boxItem, 0);
		}
		for(Container container : actual.getContainers()) {
			for(Placement placement : container.getStack().getPlacements()) {
				BoxItem boxItem = placement.getBoxItem();
				Integer count = counts.get(boxItem);
				if(count == null) {
					failWithMessage("Expected only the given box items, got %s", boxItem);
				} else {
					counts.put(boxItem, count + 1);
				}
				boolean ownStackValue = false;
				for(var stackValue : boxItem.getBox().getStackValues()) {
					if(stackValue == placement.getStackValue()) {
						ownStackValue = true;
						break;
					}
				}
				if(!ownStackValue) {
					failWithMessage("Expected %s to use a stack value of its own box", placement);
				}
			}
		}
		for(BoxItem boxItem : boxItems) {
			if(counts.get(boxItem) != boxItem.getCount()) {
				failWithMessage("Expected %s placement(s) of %s, got %s", boxItem.getCount(), boxItem, counts.get(boxItem));
			}
		}
		return myself;
	}
}
