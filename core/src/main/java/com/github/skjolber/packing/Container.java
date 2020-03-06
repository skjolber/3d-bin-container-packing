package com.github.skjolber.packing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.max;

public class Container extends Box {

	protected int stackWeight = 0;
	protected int stackHeight = 0;
	protected ArrayList<Level> levels = new ArrayList<>();

	public Container(Container container) {
		super(container.getName(), container.getWidth(), container.getDepth(), container.getHeight(), container.getWeight());
	}

	/**
	 * Construct new instance.
	 *
	 * @param dimension maximum size the container can contain
	 * @param weight maximum weight the container can hold
	 */
	public Container(Dimension dimension, int weight) {
		super(dimension.getName(), dimension.getWidth(), dimension.getDepth(), dimension.getHeight(), weight);
	}

	/**
	 * Construct new instance.
	 *
	 * @param w width
	 * @param d depth
	 * @param h height
	 * @param weight maximum weight the container can hold
	 */
	public Container(int w, int d, int h, int weight) {
		super(w, d, h, weight);
	}

	/**
	 * Construct new instance.
	 *
	 * @param name container name
	 * @param w width
	 * @param d depth
	 * @param h height
	 * @param weight maximum weight the container can hold
	 */
	public Container(String name, int w, int d, int h, int weight) {
		super(name, w, d, h, weight);
	}

	/**
	 * The 6 different possible rotations. If two of the sides are equal, there are only 3 possible orientations.
	 * If all sides is equal, there is only 1 possible orientation.
	 *
	 * It is sometimes useful to pass this list to the {@link LargestAreaFitFirstPackager}
	 * since it has a better chance to find a packaging than with a single container.
	 * @return list of containers in all 6 rotations.
	 */
	public List<Container> rotations(){
		return rotationsStream().collect(Collectors.toList());
	}

	Stream<Container> rotationsStream() {
		List<Container> result = new ArrayList<>(6);
		Container box = clone();
		boolean square0 = box.isSquare2D();

		result.add(box);

		if(!box.isSquare3D()) {

			box = box.clone().rotate3D();
			boolean square1 = box.isSquare2D();

			result.add(box);

			box = box.clone().rotate3D();
			boolean square2 = box.isSquare2D();

			result.add(box);

			if(!square0 && !square1 && !square2) {
				box = box.clone().rotate2D3D();
				result.add(box);

				box = box.clone().rotate3D();
				result.add(box);

				box = box.clone().rotate3D();
				result.add(box);
			}
		}
		return result.stream();
	}

	public boolean add(Level element) {
		if(!levels.isEmpty()) {
			stackHeight += currentLevelStackHeight();
			stackWeight += currentLevelStackWeight();
		}

		return levels.add(element);
	}
	
	public Level currentLevel() {
		if(!levels.isEmpty()) {
			return levels.get(levels.size() - 1);
		}
		return null;
	}

	public int getStackHeight() {
		return stackHeight + currentLevelStackHeight();
	}

	public int getStackWeight() {
		return stackWeight + currentLevelStackWeight();
	}

	private int currentLevelStackHeight() {
		if(levels.isEmpty()) {
			return 0;
		}
		return levels.get(levels.size() - 1).getHeight();
	}

	private int currentLevelStackWeight() {
		if(levels.isEmpty()) {
			return 0;
		}
		return levels.get(levels.size() - 1).getWeight();
	}

	public void add(Placement placement) {
		levels.get(levels.size() - 1).add(placement);
	}

	public Level addLevel() {
		Level level = new Level();
		add(level);
		return level;
	}

	/**
	 * Get the free level space, i.e. container height with height of 
	 * levels subtracted.
	 * 
	 * @return free height and box dimension
	 */
	
	public Dimension getFreeLevelSpace() {
		if(levels.isEmpty()) {
			return this;
		}
		int remainder = height - getStackHeight();
		if(remainder < 0) {
			throw new IllegalArgumentException("Remaining free space is negative at " + remainder + " for " + this);
		}
		return new Dimension(width, depth, remainder);
	}

	public int getFreeWeight() {
		int remainder = weight - getStackWeight();
		if(remainder < 0) {
			throw new IllegalArgumentException("Remaining weight is negative at " + remainder);
		}
		return remainder;
	}

	public List<Level> getLevels() {
		return levels;
	}

	public Placement get(int level, int placement) {
		return levels.get(level).get(placement);
	}

	// keep method for tests
	public void validateCurrentLevel() {
		levels.get(levels.size() - 1).validate();
	}

	public void clear() {
		levels.clear();
		stackHeight = 0;
		stackWeight = 0;
	}

	int getBoxCount() {
		int count = 0;
		for(Level level : levels) {
			count += level.size();
		}
		return count;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((levels == null) ? 0 : levels.hashCode());
		result = prime * result + stackHeight;
		result = prime * result + stackWeight;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Container other = (Container) obj;
		if (levels == null) {
			if (other.levels != null)
				return false;
		} else if (!levels.equals(other.levels))
			return false;
		if (stackHeight != other.stackHeight)
			return false;
		if (stackWeight != other.stackWeight)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Container [stackWeight=" + stackWeight + ", stackHeight=" + stackHeight + ", levels=" + levels
				+ ", weight=" + weight + ", width=" + width + ", depth=" + depth + ", height=" + height + ", volume="
				+ volume + ", name=" + name + "]";
	}

	public Dimension getUsedSpace() {
		Dimension maxBox = Dimension.EMPTY;
		for (Level level : levels) {
			maxBox = getUsedSpace(level, maxBox);
		}
		return maxBox;
	}

	private Dimension getUsedSpace(Level level, Dimension maxBox) {
		for (Placement placement : level) {
			maxBox = boundingBox(maxBox, getOutmostCorner(placement));
		}
		return maxBox;
	}

	private Dimension getOutmostCorner(Placement placement) {
		final Box box = placement.getBox();
		final Space space = placement.getSpace();
		return new Dimension(
				space.getX() + box.getWidth(),
				space.getY() + box.getDepth(),
				space.getZ() + box.getHeight());
	}

	private Dimension boundingBox(final Dimension b1, final Dimension b2) {
		return new Dimension(
				max(b1.getWidth(), b2.getWidth()),
				max(b1.getDepth(), b2.getDepth()),
				max(b1.getHeight(), b2.getHeight()));

	}
	
	public Container clone() {
		// shallow clone
		return new Container(this);
	}

	public Container rotate3D() {
		return (Container)super.rotate3D();
	}

	public Container rotate2D() {
		return (Container)super.rotate2D();
	}

	public Container rotate2D3D() {
		return (Container)super.rotate2D3D();
	}

	public boolean isFreeSpaceInLevel(int i) {
		// check if all volume is used
		Level level = levels.get(i);
		
		long volume = (this.volume / getHeight()) * level.getHeight();
		
		for(Placement p : level) {
			volume -= p.getBox().getVolume();
		}
		
		return volume > 0;
	}
	
	public void removeLevel(int index) {
		Level level = levels.remove(index);
		if(index != levels.size()) {
			stackHeight -= level.getHeight();
			stackWeight -= level.getWeight();
		}
	}
	
	/**
	 * Clear levels up to and including a number of boxes 
	 * 
	 * @param limit number of boxes to keep
	 * @return number of boxes kept
	 */

	public int clearLevelsForBoxes(int limit) {
		int count = 0;
		int i = 0;
		while(limit > count && i < levels.size()) {
			count += levels.get(i).size();
			
			i++;
		}
		
		i--;
		if(count == limit) {
			// see if we can keep the last level
			// if so there must be no free space in it
			Level level = levels.get(i);
			
			long v = (volume / height) * level.getHeight();
			for(Placement p : level) {
				v -= p.getBox().getVolume();
			}
			
			if(v == 0) {
				// keep last level
				i++;
			} else {
				// discard also the last level
				count -= levels.get(i).size();
			}
		} else {
			// discard also the last level
			count -= levels.get(i).size();
		}
		
		while(i < levels.size()) {
			removeLevel(i);
		}
		
		return count;
	}
}
