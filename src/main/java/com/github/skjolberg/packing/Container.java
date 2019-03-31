package com.github.skjolberg.packing;

import com.github.skjolberg.packing.model.PackModel;
import com.github.skjolberg.packing.model.PlacementModel;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.max;

public class Container extends Box {

	private int stackWeight = 0;
	private int stackHeight = 0;
	private ArrayList<Level> levels = new ArrayList<>();

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

	public Dimension getFreeSpace() {
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

	/**
	 * Returns a representation of this {@link Container} in JSON format
	 * @return a String in JSON format
	 */
	public String toJson() {
		List<PlacementModel> placementModelList = getLevels()
			.stream()
			.map(Collection::stream)
			.flatMap(placementStream ->
				placementStream.map(placement -> new BoxToSpace(placement.getBox(), placement.getSpace()))
			)
			.map(boxToSpace -> new PlacementModel(
				boxToSpace.getBox().getName(),
				boxToSpace.getSpace().getX(),
				boxToSpace.getSpace().getY(),
				boxToSpace.getSpace().getZ(),
				boxToSpace.getBox().getWidth(),
				boxToSpace.getBox().getDepth(),
				boxToSpace.getBox().getHeight()
			))
			.collect(Collectors.toList());

		PackModel packModel = new PackModel(
			placementModelList,
			getWeight(),
			getWidth(),
			getDepth(),
			getHeight(),
			getVolume(),
			getName()
		);

		return new Gson().toJson(packModel);
	}

	/**
	 * Save a representation of this {@link Container} in JSON format to a file
	 * @param pathToFile the path to the file where to save the data
	 * @throws IOException if an I/O error occurs writing to or creating the file
	 */
	public void toFile(String pathToFile) throws IOException {
		Path filePath = Paths.get(pathToFile);
		Files.write(filePath, toJson().getBytes());
	}

	private class BoxToSpace {
		private final Box box;
		private final Space space;

		protected BoxToSpace(Box box, Space space) {
			this.box = box;
			this.space = space;
		}

		public Box getBox() {
			return box;
		}

		public Space getSpace() {
			return space;
		}
	}
}
