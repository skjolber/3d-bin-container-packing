package com.github.skjolber.packing.comparator.placement;

import java.util.Objects;
import java.util.Set;

public class DefaultPlacementComparatorAttribute implements PlacementComparatorAttribute {

	protected String id;
	
	protected boolean higherIsBetter;
	
	protected boolean presenceIsBetter;

	public DefaultPlacementComparatorAttribute(String id, boolean higherIsBetter) {
		this(id, higherIsBetter, higherIsBetter);
	}

	public DefaultPlacementComparatorAttribute(String id, boolean higherIsBetter, boolean presenceIsBetter) {
		super();
		this.id = id;
		this.higherIsBetter = higherIsBetter;
		this.presenceIsBetter = presenceIsBetter;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public boolean presenceIsBetter() {
		return presenceIsBetter;
	}

	@Override
	public boolean absenceIsBetter() {
		return !presenceIsBetter;
	}

	@Override
	public boolean higherIsBetter() {
		return higherIsBetter;
	}

	@Override
	public boolean lowerIsBetter() {
		return !higherIsBetter;
	}

	@Override
	public boolean isSkippable(Set<String> available) {
		return !available.contains(id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(higherIsBetter, id, presenceIsBetter);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultPlacementComparatorAttribute other = (DefaultPlacementComparatorAttribute) obj;
		return higherIsBetter == other.higherIsBetter && Objects.equals(id, other.id)
				&& presenceIsBetter == other.presenceIsBetter;
	}

}
