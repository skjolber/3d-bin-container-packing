package com.github.skjolber.packing.test.ascii;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;

/**
 * Draw the boxes of a container as text, for example for documentation or to explain a unit test.
 * <p>
 * Orientation: x always increases from left to right and z always increases from bottom to top. The y dimension is the
 * one going into the container.
 * <ul>
 * <li>{@linkplain #front()}: x to the right, z up. Seen from low y: the box with the smallest y is the nearest.</li>
 * <li>{@linkplain #top()}: x to the right, y up the page. Seen from above: the box with the highest z is the nearest.</li>
 * <li>{@linkplain #side()}: y to the right, z up. Seen from the right, at high x: the box with the largest x is the nearest.</li>
 * <li>{@linkplain #oblique()}: x to the right, z up, y going inwards, drawn up and to the right. The front, top and right sides of the
 * boxes are visible.</li>
 * </ul>
 * Boxes are drawn with their label, which is the id or description of the box, or else A, B, C and so on in the order of the placements.
 * Boxes which are partly hidden behind other boxes are drawn partly. Obstacles are filled with '#'. A unit (as in the dimensions of boxes)
 * is drawn as a number of columns and lines, see {@linkplain Builder#withScale(double, double)}. The edges are drawn in
 * a {@linkplain Style}, by default with thin lines.
 * <p>
 * The views have the coordinates at the axes: 0, the size of the container, and where the boxes start and end.
 * <p>
 * Example of the front view of three boxes (a larger scale than the default is used to make the example small):
 *
 * <pre>
 * z
 * 2 ┌───────┐
 *   │   C   │
 * 1 ├───────┼───────┐
 *   │   A   │   B   │
 * 0 └───────┴───────┘
 *   0       1       2   x
 * </pre>
 *
 * Example of the oblique view of a single box, 3 x 2 x 2 units, at the default scale. The axes are along the edges of the container, with the
 * coordinates of the box:
 *
 * <pre>
 *   z   ┌───────────┐   y
 *      ╱           ╱│
 *   │ ╱           ╱ │ ╱
 *   │╱           ╱  │╱
 * 2 ┌───────────┐   │ 2
 *   │           │  ╱
 *   │     A     │ ╱
 *   │           │╱
 * 0 └───────────┘── x
 *   0           3
 * </pre>
 */
public class ContainerAsciiArt {

	/**
	 * @return a new builder
	 */
	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * Builder for {@linkplain ContainerAsciiArt}. The container (its size and boxes) is required.
	 */
	public static class Builder {

		private List<Placement> placements = new ArrayList<>();
		private List<Placement> obstacles = new ArrayList<>();
		private int dx = -1;
		private int dy = -1;
		private int dz = -1;

		private double scaleX = -1;
		private double scaleZ = -1;
		private int width = -1;

		private Function<Placement, String> labels;
		private boolean axes = true;
		private boolean containerOutline = false;
		private String comment;
		private Style style = Style.LIGHT;
		private boolean shading = false;

		/**
		 * Draw the stack and obstacles of a container, within its load size.
		 *
		 * @param container the container
		 * @return this builder
		 */
		public Builder withContainer(Container container) {
			Objects.requireNonNull(container);
			withSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
			this.placements = new ArrayList<>();
			if (container.getStack() != null) {
				this.placements.addAll(container.getStack().getPlacements());
			}
			this.obstacles = new ArrayList<>();
			if (container.getObstacles() != null) {
				this.obstacles.addAll(container.getObstacles());
			}
			return this;
		}

		/**
		 * @param stack the boxes
		 * @param dx container size along x
		 * @param dy container size along y
		 * @param dz container size along z
		 * @return this builder
		 */
		public Builder withStack(Stack stack, int dx, int dy, int dz) {
			Objects.requireNonNull(stack);
			return withPlacements(stack.getPlacements(), dx, dy, dz);
		}

		/**
		 * @param placements the boxes
		 * @param dx container size along x
		 * @param dy container size along y
		 * @param dz container size along z
		 * @return this builder
		 */
		public Builder withPlacements(List<Placement> placements, int dx, int dy, int dz) {
			Objects.requireNonNull(placements);
			withSize(dx, dy, dz);
			this.placements = new ArrayList<>(placements);
			return this;
		}

		/**
		 * @param obstacles boxes which are already in the container, drawn filled with '#'
		 * @return this builder
		 */
		public Builder withObstacles(List<Placement> obstacles) {
			Objects.requireNonNull(obstacles);
			this.obstacles = new ArrayList<>(obstacles);
			return this;
		}

		private void withSize(int dx, int dy, int dz) {
			if (dx <= 0 || dy <= 0 || dz <= 0) {
				throw new IllegalArgumentException("Expected positive container size, got " + dx + "x" + dy + "x" + dz);
			}
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
		}

		/**
		 * Draw a unit as a number of columns and lines. The default is the smallest scale, but at least 4 x 2, at which the labels fit within
		 * their boxes (with a blank line above and below and a space on each side) for 90 % of the boxes.
		 * <p>
		 * In the oblique view, one unit of y is drawn as {@code cz} steps up and to the right.
		 *
		 * @param cx columns per unit of x (of y in the side view)
		 * @param cz lines per unit of z (of y in the top view)
		 * @return this builder
		 */
		public Builder withScale(double cx, double cz) {
			if (!(cx > 0.0) || !(cz > 0.0)) {
				throw new IllegalArgumentException("Expected positive scale, got " + cx + " x " + cz);
			}
			this.scaleX = cx;
			this.scaleZ = cz;
			this.width = -1;
			return this;
		}

		/**
		 * Draw the container at the given width, with half as many lines per unit as columns per unit.
		 *
		 * @param columns the number of columns for the container, not counting axes
		 * @return this builder
		 */
		public Builder withWidth(int columns) {
			if (columns < 2) {
				throw new IllegalArgumentException("Expected at least two columns, got " + columns);
			}
			this.width = columns;
			this.scaleX = -1;
			this.scaleZ = -1;
			return this;
		}

		/**
		 * @param labels the label of each box, null for the default label of that box
		 * @return this builder
		 */
		public Builder withLabels(Function<Placement, String> labels) {
			this.labels = labels;
			return this;
		}

		/**
		 * @param axes true to draw the axes (the default): the names of the axes in all views, and also the coordinates in the front, top and side views:
		 *        0, the size of the container and where the boxes start and end (as many as there is room for). In the oblique view, the axes are along
		 *        the edges of the container, with the coordinates of the boxes with their front at y = 0 on the x and z axes (the other boxes are
		 *        drawn further up and to the right), and the coordinates of all boxes on the y axis.
		 * @return this builder
		 */
		public Builder withAxes(boolean axes) {
			this.axes = axes;
			return this;
		}

		/**
		 * @param containerOutline true to draw the edges of the container as dots, behind the boxes
		 * @return this builder
		 */
		public Builder withContainerOutline(boolean containerOutline) {
			this.containerOutline = containerOutline;
			return this;
		}

		/**
		 * @param style the characters of the edges, {@linkplain Style#LIGHT} by default
		 * @return this builder
		 */
		public Builder withStyle(Style style) {
			this.style = Objects.requireNonNull(style);
			return this;
		}

		/**
		 * Fill the top faces and the right faces in the oblique view, so that the sides of the boxes are easier to tell apart.
		 * The front faces stay blank.
		 *
		 * @param shading true to fill the top faces with '░' and the right faces with '▒' ('.' and ':' in the {@linkplain Style#ASCII} style), false (the default) for blank faces
		 * @return this builder
		 */
		public Builder withShading(boolean shading) {
			this.shading = shading;
			return this;
		}

		/**
		 * @param comment put in front of every line, for example "// " to paste the figure into source code
		 * @return this builder
		 */
		public Builder withComment(String comment) {
			this.comment = comment;
			return this;
		}

		/**
		 * @return the drawings
		 * @throws IllegalStateException if the container size was not set
		 */
		public ContainerAsciiArt build() {
			if (dx == -1) {
				throw new IllegalStateException("Expected container size: use withContainer, withStack or withPlacements");
			}
			return new ContainerAsciiArt(this);
		}
	}

	private enum View {
		FRONT, TOP, SIDE, OBLIQUE
	}

	private static final class Item {

		private final int x;
		private final int y;
		private final int z;
		private final int dx;
		private final int dy;
		private final int dz;
		private final String label;
		private final boolean obstacle;

		private Item(Placement placement, String label, boolean obstacle) {
			this.x = placement.getAbsoluteX();
			this.y = placement.getAbsoluteY();
			this.z = placement.getAbsoluteZ();
			this.dx = placement.getStackValue().getDx();
			this.dy = placement.getStackValue().getDy();
			this.dz = placement.getStackValue().getDz();
			this.label = label;
			this.obstacle = obstacle;
		}
	}

	/** The number of cells the axes of the oblique view extend past the container */
	private static final int AXIS_EXTENSION = 2;

	private static final int MINIMUM_COLUMNS_PER_UNIT = 4;
	private static final int MINIMUM_LINES_PER_UNIT = 2;
	/** The share of the boxes for which the labels should fit */
	private static final int LABEL_FIT_PERCENT = 90;

	private final List<Item> items;
	/** Size of the container */
	private final int containerX;
	private final int containerY;
	private final int containerZ;
	/** Size of the container, or the boxes if they stick out of it */
	private final int extentX;
	private final int extentY;
	private final int extentZ;

	private final double scaleX;
	private final double scaleZ;
	private final int width;
	private final boolean axes;
	private final boolean containerOutline;
	private final String comment;
	private final Style style;
	private final boolean shading;

	private ContainerAsciiArt(Builder builder) {
		this.scaleX = builder.scaleX;
		this.scaleZ = builder.scaleZ;
		this.width = builder.width;
		this.axes = builder.axes;
		this.containerOutline = builder.containerOutline;
		this.comment = builder.comment;
		this.style = builder.style;
		this.shading = builder.shading;

		this.items = new ArrayList<>(builder.placements.size() + builder.obstacles.size());

		int extentX = builder.dx;
		int extentY = builder.dy;
		int extentZ = builder.dz;
		for (int i = 0; i < builder.placements.size(); i++) {
			Placement placement = builder.placements.get(i);
			Item item = new Item(placement, getLabel(builder.labels, placement, i), false);
			items.add(item);

			extentX = Math.max(extentX, item.x + item.dx);
			extentY = Math.max(extentY, item.y + item.dy);
			extentZ = Math.max(extentZ, item.z + item.dz);
		}
		for (Placement placement : builder.obstacles) {
			Item item = new Item(placement, null, true);
			items.add(item);

			extentX = Math.max(extentX, item.x + item.dx);
			extentY = Math.max(extentY, item.y + item.dy);
			extentZ = Math.max(extentZ, item.z + item.dz);
		}
		this.extentX = extentX;
		this.extentY = extentY;
		this.extentZ = extentZ;
		this.containerX = builder.dx;
		this.containerY = builder.dy;
		this.containerZ = builder.dz;
	}

	private static String getLabel(Function<Placement, String> labels, Placement placement, int index) {
		if (labels != null) {
			String label = labels.apply(placement);
			if (label != null) {
				return label;
			}
		}
		Box box = placement.getStackValue().getBox();
		if (box != null) {
			if (box.getId() != null && !box.getId().isEmpty()) {
				return box.getId();
			}
			if (box.getDescription() != null && !box.getDescription().isEmpty()) {
				return box.getDescription();
			}
		}
		return letters(index);
	}

	/**
	 * @return A, B, C, ..., Z, AA, AB, ...
	 */
	static String letters(int index) {
		StringBuilder builder = new StringBuilder();
		int i = index;
		do {
			builder.insert(0, (char) ('A' + (i % 26)));
			i = i / 26 - 1;
		} while (i >= 0);
		return builder.toString();
	}

	/**
	 * From the front: x to the right, z up. The nearest box is the one with the smallest y.
	 *
	 * @return the drawing
	 */
	public Figure front() {
		return draw2D(View.FRONT);
	}

	/**
	 * From above: x to the right, y up the page. The nearest box is the one with the highest z.
	 *
	 * @return the drawing
	 */
	public Figure top() {
		return draw2D(View.TOP);
	}

	/**
	 * From the right: y to the right, z up. The nearest box is the one with the largest x.
	 *
	 * @return the drawing
	 */
	public Figure side() {
		return draw2D(View.SIDE);
	}

	/**
	 * Oblique projection: x to the right, z up, y going inwards, up and to the right.
	 *
	 * @return the drawing
	 */
	public Figure oblique() {
		// the projection: column = x * cx + y * d, row = z * cz + y * d, so that the points
		// (x, y, z) which are on the same line of sight from the viewer are the points with different y.
		// The viewer is at a lower y (and a higher x and z), so that the nearest point is the one with the lowest y.
		double[] scale = getScale(View.OBLIQUE);
		double cx = scale[0];
		double cz = scale[1];
		double d = cz;

		int gridX = round(extentX * cx);
		int gridY = round(extentY * d);
		int gridZ = round(extentZ * cz);

		// the axes are along the edges of the container, with the coordinates of the boxes with their front at y = 0 on the x and z axes
		// (the other boxes are drawn further up and to the right), and the coordinates of all boxes on the y axis
		int[] xValues = null;
		int[] zValues = null;
		int[] yValues = null;
		int ox = 0;
		int oy = 0;
		int width = gridX + gridY + 1;
		int height = gridZ + gridY + 1;
		if (axes) {
			xValues = selectColumnValues(getFrontKeyValues(true), cx, true);
			zValues = selectRowValues(getFrontKeyValues(false), cz);
			yValues = selectRowValues(getDepthKeyValues(), d);

			// the values of z on the left of the z axis, and the values of x below the x axis
			int digits = 0;
			for (int value : zValues) {
				digits = Math.max(digits, Integer.toString(value).length());
			}
			ox = digits + 1;
			oy = 1;

			width = ox + gridX + gridY + 1;
			height = oy + gridZ + gridY + 1;

			// the names of the axes, after the axes (which extend past the container) and a blank
			width = Math.max(width, ox + gridX + AXIS_EXTENSION + 2 + 1);
			width = Math.max(width, ox + gridX + gridY + AXIS_EXTENSION + 2 + 1);
			height = Math.max(height, oy + gridZ + AXIS_EXTENSION + 2 + 1);
			height = Math.max(height, oy + gridY + AXIS_EXTENSION + 2 + 1);
			for (int value : xValues) {
				width = Math.max(width, ox + round(value * cx) - Integer.toString(value).length() / 2 + Integer.toString(value).length());
			}
			for (int value : yValues) {
				width = Math.max(width, ox + gridX + round(value * d) + 2 + Integer.toString(value).length());
			}
		}

		AsciiCanvas canvas = new AsciiCanvas(width, height);

		int[] bounds = new int[items.size() * 4];
		int[] interiors = new int[items.size() * 3];
		for (int i = 0; i < items.size(); i++) {
			Item item = items.get(i);

			int x0 = round(item.x * cx);
			int x1 = round((item.x + item.dx) * cx);
			int y0 = round(item.y * d);
			int y1 = round((item.y + item.dy) * d);
			int z0 = round(item.z * cz);
			int z1 = round((item.z + item.dz) * cz);

			char topFill = ' ';
			char rightFill = ' ';
			if (item.obstacle) {
				topFill = '#';
				rightFill = '#';
			} else if (shading) {
				topFill = style.topShade;
				rightFill = style.rightShade;
			}
			drawObliqueBox(canvas, i * 3, item.obstacle ? '#' : ' ', topFill, rightFill, ox, oy, x0, x1, y0, y1, z0, z1);

			bounds[i * 4] = ox + x0 + y0;
			bounds[i * 4 + 1] = ox + x1 + y1;
			bounds[i * 4 + 2] = oy + z0 + y0;
			bounds[i * 4 + 3] = oy + z1 + y1;

			// the cells between the edges of the front face, top face and right face
			interiors[i * 3] = interior(x1 - x0) * interior(z1 - z0);
			interiors[i * 3 + 1] = interior(x1 - x0) * interior(y1 - y0);
			interiors[i * 3 + 2] = interior(y1 - y0) * interior(z1 - z0);
		}

		if (containerOutline) {
			drawObliqueOutline(canvas, ox, oy, gridX, gridY, gridZ);
		}
		if (axes) {
			// behind the boxes: the x axis along the front bottom edge of the container, the z axis along the front left edge, and the y axis
			// along the right bottom edge
			for (int column = 0; column <= gridX + AXIS_EXTENSION; column++) {
				canvas.putEdge(ox + column, oy, AsciiCanvas.HORIZONTAL, AsciiCanvas.AXIS);
			}
			for (int row = 0; row <= gridZ + AXIS_EXTENSION; row++) {
				canvas.putEdge(ox, oy + row, AsciiCanvas.VERTICAL, AsciiCanvas.AXIS);
			}
			for (int step = 0; step <= gridY + AXIS_EXTENSION; step++) {
				canvas.putEdge(ox + gridX + step, oy + step, AsciiCanvas.DIAGONAL, AsciiCanvas.AXIS);
			}
			// the names, after a blank
			canvas.putCharacter(ox + gridX + AXIS_EXTENSION + 2, oy, 'x', AsciiCanvas.AXIS, AsciiCanvas.NO_OWNER);
			canvas.putCharacter(ox, oy + gridZ + AXIS_EXTENSION + 2, 'z', AsciiCanvas.AXIS, AsciiCanvas.NO_OWNER);
			canvas.putCharacter(ox + gridX + gridY + AXIS_EXTENSION + 2, oy + gridY + AXIS_EXTENSION + 2, 'y', AsciiCanvas.AXIS, AsciiCanvas.NO_OWNER);

			// the values: of x below the axis, centered at the coordinate; of z to the left of the axis, aligned to the right;
			// of y to the right of the axis
			for (int value : xValues) {
				String text = Integer.toString(value);
				drawText(canvas, ox + round(value * cx) - text.length() / 2, 0, text, AsciiCanvas.AXIS);
			}
			for (int value : zValues) {
				String text = Integer.toString(value);
				drawText(canvas, ox - 1 - text.length(), oy + round(value * cz), text, AsciiCanvas.AXIS);
			}
			for (int value : yValues) {
				drawText(canvas, ox + gridX + round(value * d) + 2, oy + round(value * d), Integer.toString(value), AsciiCanvas.AXIS);
			}
		}
		drawLabels(canvas, bounds, interiors, 3);

		return newFigure(canvas, true);
	}

	/**
	 * Leave out the blank lines above and below the drawing: for example above the boxes when the container is higher than the boxes.
	 * The container is shown by the axes and the outline, if any.
	 */
	private Figure newFigure(AsciiCanvas canvas, boolean oblique) {
		String[] lines = canvas.toLines(style, oblique);
		int first = 0;
		while (first < lines.length && lines[first].isEmpty()) {
			first++;
		}
		int last = lines.length;
		while (last > first && lines[last - 1].isEmpty()) {
			last--;
		}
		String[] trimmed = new String[last - first];
		System.arraycopy(lines, first, trimmed, 0, trimmed.length);
		return new Figure(trimmed, comment);
	}

	/**
	 * Draw the visible faces of a box, as a grid box with corners x0, y0, z0 and x1, y1, z1. The projection of a point on the grid is
	 * column = x + y, row = z + y. So the nearest point on a line of sight is the one with the lowest y, which is also the
	 * depth.
	 */
	private static void drawObliqueBox(AsciiCanvas canvas, int owner, char frontFill, char topFill, char rightFill, int ox, int oy, int x0, int x1, int y0, int y1, int z0, int z1) {
		// front face, at the lowest y
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				int edges = 0;
				if (x == x0 || x == x1) {
					edges |= AsciiCanvas.VERTICAL;
				}
				if (z == z0 || z == z1) {
					edges |= AsciiCanvas.HORIZONTAL;
				}
				put(canvas, ox + x + y0, oy + z + y0, edges, frontFill, y0, owner);
			}
		}
		// top face, at the highest z
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				int edges = 0;
				if (x == x0 || x == x1) {
					edges |= AsciiCanvas.DIAGONAL;
				}
				if (y == y0 || y == y1) {
					edges |= AsciiCanvas.HORIZONTAL;
				}
				put(canvas, ox + x + y, oy + z1 + y, edges, topFill, y, owner + 1);
			}
		}
		// right face, at the highest x
		for (int y = y0; y <= y1; y++) {
			for (int z = z0; z <= z1; z++) {
				int edges = 0;
				if (y == y0 || y == y1) {
					edges |= AsciiCanvas.VERTICAL;
				}
				if (z == z0 || z == z1) {
					edges |= AsciiCanvas.DIAGONAL;
				}
				put(canvas, ox + x1 + y, oy + z + y, edges, rightFill, y, owner + 2);
			}
		}
	}

	private static void put(AsciiCanvas canvas, int column, int row, int edges, char fill, int depth, int owner) {
		if (edges != 0) {
			canvas.putEdge(column, row, edges, depth);
		} else {
			canvas.putCharacter(column, row, fill, depth, owner);
		}
	}

	private static void drawObliqueOutline(AsciiCanvas canvas, int ox, int oy, int gridX, int gridY, int gridZ) {
		int[] xs = { 0, gridX };
		int[] ys = { 0, gridY };
		int[] zs = { 0, gridZ };

		// the edges along x, then along y, then along z
		for (int y : ys) {
			for (int z : zs) {
				for (int x = 0; x <= gridX; x++) {
					drawOutline(canvas, ox + x + y, oy + z + y);
				}
			}
		}
		for (int x : xs) {
			for (int z : zs) {
				for (int y = 0; y <= gridY; y++) {
					drawOutline(canvas, ox + x + y, oy + z + y);
				}
			}
		}
		for (int x : xs) {
			for (int y : ys) {
				for (int z = 0; z <= gridZ; z++) {
					drawOutline(canvas, ox + x + y, oy + z + y);
				}
			}
		}
	}

	private static void drawOutline(AsciiCanvas canvas, int column, int row) {
		canvas.putCharacter(column, row, '.', AsciiCanvas.OUTLINE, AsciiCanvas.NO_OWNER);
	}

	private Figure draw2D(View view) {
		double[] scale = getScale(view);
		double horizontalScale = scale[0];
		double verticalScale = scale[1];

		int horizontalExtent = (int) getHorizontalExtent(view);
		int verticalExtent = (int) getVerticalExtent(view);

		int gridWidth = round(horizontalExtent * horizontalScale);
		int gridHeight = round(verticalExtent * verticalScale);

		// the coordinates on the left and at the bottom
		int[] horizontalValues = null;
		int[] verticalValues = null;
		int marginColumns = 0;
		int marginRows = 0;
		int width = gridWidth + 1;
		int height = gridHeight + 1;
		int nameColumn = 0;
		if (axes) {
			horizontalValues = selectColumnValues(getKeyValues(view, true), horizontalScale, false);
			verticalValues = selectRowValues(getKeyValues(view, false), verticalScale);

			int digits = 0;
			for (int value : verticalValues) {
				digits = Math.max(digits, Integer.toString(value).length());
			}
			marginColumns = digits + 1;
			marginRows = 1;

			int numbersEnd = marginColumns + gridWidth + 1;
			for (int value : horizontalValues) {
				numbersEnd = Math.max(numbersEnd, marginColumns + round(value * horizontalScale) + Integer.toString(value).length());
			}
			// the name of the axis, after the last value
			nameColumn = numbersEnd + 3;

			width = nameColumn + 1;
			// the values below the drawing, the name of the axis above the drawing
			height = marginRows + gridHeight + 1 + 1;
		}

		AsciiCanvas canvas = new AsciiCanvas(width, height);

		int[] bounds = new int[items.size() * 4];
		int[] interiors = new int[items.size()];
		for (int i = 0; i < items.size(); i++) {
			Item item = items.get(i);

			int c0 = marginColumns + round(getHorizontalStart(view, item) * horizontalScale);
			int c1 = marginColumns + round((getHorizontalStart(view, item) + getHorizontalSize(view, item)) * horizontalScale);
			int r0 = marginRows + round(getVerticalStart(view, item) * verticalScale);
			int r1 = marginRows + round((getVerticalStart(view, item) + getVerticalSize(view, item)) * verticalScale);

			int depth = getDepth(view, item);
			char fill = item.obstacle ? '#' : ' ';
			for (int column = c0; column <= c1; column++) {
				for (int row = r0; row <= r1; row++) {
					int edges = 0;
					if (column == c0 || column == c1) {
						edges |= AsciiCanvas.VERTICAL;
					}
					if (row == r0 || row == r1) {
						edges |= AsciiCanvas.HORIZONTAL;
					}
					put(canvas, column, row, edges, fill, depth, i);
				}
			}

			bounds[i * 4] = c0;
			bounds[i * 4 + 1] = c1;
			bounds[i * 4 + 2] = r0;
			bounds[i * 4 + 3] = r1;

			interiors[i] = interior(c1 - c0) * interior(r1 - r0);
		}

		if (containerOutline) {
			for (int column = 0; column <= gridWidth; column++) {
				drawOutline(canvas, marginColumns + column, marginRows);
				drawOutline(canvas, marginColumns + column, marginRows + gridHeight);
			}
			for (int row = 0; row <= gridHeight; row++) {
				drawOutline(canvas, marginColumns, marginRows + row);
				drawOutline(canvas, marginColumns + gridWidth, marginRows + row);
			}
		}
		if (axes) {
			// the names of the axes, and the values at the edge of the drawing
			canvas.putCharacter(0, marginRows + gridHeight + 1, view == View.TOP ? 'y' : 'z', AsciiCanvas.LABEL, AsciiCanvas.NO_OWNER);
			canvas.putCharacter(nameColumn, 0, view == View.SIDE ? 'y' : 'x', AsciiCanvas.LABEL, AsciiCanvas.NO_OWNER);

			int valueWidth = marginColumns - 1;
			for (int value : verticalValues) {
				String text = Integer.toString(value);
				// right aligned
				drawText(canvas, valueWidth - text.length(), marginRows + round(value * verticalScale), text, AsciiCanvas.LABEL);
			}
			for (int value : horizontalValues) {
				drawText(canvas, marginColumns + round(value * horizontalScale), 0, Integer.toString(value), AsciiCanvas.LABEL);
			}
		}
		drawLabels(canvas, bounds, interiors, 1);

		return newFigure(canvas, false);
	}

	private static void drawText(AsciiCanvas canvas, int column, int row, String text, int depth) {
		for (int i = 0; i < text.length(); i++) {
			canvas.putCharacter(column + i, row, text.charAt(i), depth, AsciiCanvas.NO_OWNER);
		}
	}

	/**
	 * The coordinates which are written at an axis, in the order of priority: 0, the size of the container, and then the coordinates where
	 * a box starts or ends, from low to high.
	 *
	 * @param horizontal true for the horizontal axis of the view, false for the vertical axis
	 */
	private int[] getKeyValues(View view, boolean horizontal) {
		int container;
		if (horizontal) {
			container = view == View.SIDE ? containerY : containerX;
		} else {
			container = view == View.TOP ? containerY : containerZ;
		}
		int[] boxes = new int[items.size() * 2];
		for (int i = 0; i < items.size(); i++) {
			Item item = items.get(i);
			int start = horizontal ? getHorizontalStart(view, item) : getVerticalStart(view, item);
			int size = horizontal ? getHorizontalSize(view, item) : getVerticalSize(view, item);
			boxes[i * 2] = start;
			boxes[i * 2 + 1] = start + size;
		}
		return getKeyValues(container, boxes, true);
	}

	/**
	 * @param container the size of the container
	 * @param boxes the coordinates where boxes start or end, in no particular order
	 * @param origin true to include 0 as the first coordinate
	 * @return the coordinates in the order of priority: 0 (if origin), the size of the container, and then the coordinates of the boxes from low to high
	 */
	private static int[] getKeyValues(int container, int[] boxes, boolean origin) {
		Arrays.sort(boxes);

		int[] values = new int[boxes.length + 2];
		int count = 0;
		if (origin) {
			values[count++] = 0;
		}
		values[count++] = container;
		int first = count;
		for (int value : boxes) {
			// the values are not negative, and in order
			if (value != 0 && value != container && (count == first || values[count - 1] != value)) {
				values[count++] = value;
			}
		}
		return Arrays.copyOf(values, count);
	}

	/**
	 * The coordinates which are written at the x axis or the z axis of the oblique view, in the order of priority: 0, the size of the container,
	 * and then the coordinates where a box with its front at y = 0 starts or ends, from low to high. The other boxes are drawn further up
	 * and to the right, so their coordinates would be next to the wrong edge.
	 *
	 * @param horizontal true for the x axis, false for the z axis
	 */
	private int[] getFrontKeyValues(boolean horizontal) {
		int[] boxes = new int[items.size() * 2];
		int count = 0;
		for (Item item : items) {
			if (item.y == 0) {
				int start = horizontal ? item.x : item.z;
				boxes[count++] = start;
				boxes[count++] = start + (horizontal ? item.dx : item.dz);
			}
		}
		return getKeyValues(horizontal ? containerX : containerZ, Arrays.copyOf(boxes, count), true);
	}

	/**
	 * The coordinates which are written at the y axis of the oblique view, in the order of priority: the size of the container, and then the
	 * coordinates where a box starts or ends, from low to high. Not 0.
	 */
	private int[] getDepthKeyValues() {
		int[] boxes = new int[items.size() * 2];
		for (int i = 0; i < items.size(); i++) {
			Item item = items.get(i);
			boxes[i * 2] = item.y;
			boxes[i * 2 + 1] = item.y + item.dy;
		}
		return getKeyValues(containerY, boxes, false);
	}

	/**
	 * Keep the coordinates which have room in a line, with a blank column between each: the ones with the highest priority first.
	 *
	 * @param values the coordinates, in the order of priority
	 * @param scale columns per unit
	 * @param centered true if a value is centered at its coordinate, false if it starts there
	 * @return the coordinates which are written
	 */
	private static int[] selectColumnValues(int[] values, double scale, boolean centered) {
		int[] selected = new int[values.length];
		int count = 0;
		for (int value : values) {
			int length = Integer.toString(value).length();
			int column = round(value * scale) - (centered ? length / 2 : 0);

			boolean fits = true;
			for (int i = 0; i < count && fits; i++) {
				int otherLength = Integer.toString(selected[i]).length();
				int otherColumn = round(selected[i] * scale) - (centered ? otherLength / 2 : 0);
				fits = column >= otherColumn + otherLength + 1 || otherColumn >= column + length + 1;
			}
			if (fits) {
				selected[count++] = value;
			}
		}
		return Arrays.copyOf(selected, count);
	}

	/**
	 * Keep one coordinate for each line: the one with the highest priority.
	 *
	 * @param values the coordinates, in the order of priority
	 * @param scale lines per unit
	 * @return the coordinates which are written
	 */
	private static int[] selectRowValues(int[] values, double scale) {
		int[] selected = new int[values.length];
		int count = 0;
		for (int value : values) {
			int row = round(value * scale);

			boolean fits = true;
			for (int i = 0; i < count && fits; i++) {
				fits = round(selected[i] * scale) != row;
			}
			if (fits) {
				selected[count++] = value;
			}
		}
		return Arrays.copyOf(selected, count);
	}

	private static int getHorizontalStart(View view, Item item) {
		return view == View.SIDE ? item.y : item.x;
	}

	private static int getHorizontalSize(View view, Item item) {
		return view == View.SIDE ? item.dy : item.dx;
	}

	private static int getVerticalStart(View view, Item item) {
		return view == View.TOP ? item.y : item.z;
	}

	private static int getVerticalSize(View view, Item item) {
		return view == View.TOP ? item.dy : item.dz;
	}

	private static int getDepth(View view, Item item) {
		switch (view) {
		case FRONT:
			// nearest is the lowest y
			return item.y;
		case TOP:
			// nearest is the highest z
			return -(item.z + item.dz);
		case SIDE:
			// nearest is the highest x
			return -(item.x + item.dx);
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * @return the size of the container in the horizontal direction of the view, in units
	 */
	private double getHorizontalExtent(View view) {
		switch (view) {
		case FRONT:
		case TOP:
			return extentX;
		case SIDE:
			return extentY;
		case OBLIQUE:
			// y is drawn at half the scale
			return extentX + extentY / 2.0;
		default:
			throw new IllegalArgumentException();
		}
	}

	private double getVerticalExtent(View view) {
		return view == View.TOP ? extentY : extentZ;
	}

	/**
	 * @return columns per unit horizontally and lines per unit vertically
	 */
	private double[] getScale(View view) {
		if (scaleX > 0.0) {
			return new double[] { scaleX, scaleZ };
		}
		if (width > 0) {
			double columnsPerUnit = (width - 1) / getHorizontalExtent(view);
			return new double[] { columnsPerUnit, columnsPerUnit / 2 };
		}
		return getLabelScale(view);
	}

	/**
	 * The smallest scale (at least 4 columns x 2 lines per unit) at which 90 % of the labels have a blank line above and below, and a space at each side.
	 * <p>
	 * Between the edges of a face which is w units wide, there are w * cx - 1 columns, so that a label with length l needs {@code w * cx - 1 >= l + 2}, and a face which is h units high
	 * has h * cz - 1 lines between its edges, of which three are needed.
	 */
	private double[] getLabelScale(View view) {
		int count = 0;
		int[] columns = new int[items.size()];
		int[] lines = new int[items.size()];
		for (Item item : items) {
			if (item.obstacle || item.label == null || item.label.isEmpty()) {
				continue;
			}
			int faceWidth;
			int faceHeight;
			switch (view) {
			case TOP:
				faceWidth = item.dx;
				faceHeight = item.dy;
				break;
			case SIDE:
				faceWidth = item.dy;
				faceHeight = item.dz;
				break;
			default:
				// the front, also in the oblique view
				faceWidth = item.dx;
				faceHeight = item.dz;
				break;
			}
			columns[count] = Math.max(MINIMUM_COLUMNS_PER_UNIT, ceilDivide(item.label.length() + 3, faceWidth));
			lines[count] = Math.max(MINIMUM_LINES_PER_UNIT, ceilDivide(4, faceHeight));
			count++;
		}
		if (count == 0) {
			return new double[] { MINIMUM_COLUMNS_PER_UNIT, MINIMUM_LINES_PER_UNIT };
		}
		int required = ceilDivide(count * LABEL_FIT_PERCENT, 100);

		// the cheapest scale which is large enough for the required number of labels, using the sizes needed by the labels as candidates
		long bestArea = Long.MAX_VALUE;
		int bestColumns = 0;
		int bestLines = 0;
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < count; j++) {
				long area = (long) columns[i] * lines[j];
				if (area > bestArea || (area == bestArea && columns[i] > bestColumns)) {
					continue;
				}
				int fits = 0;
				for (int k = 0; k < count; k++) {
					if (columns[k] <= columns[i] && lines[k] <= lines[j]) {
						fits++;
					}
				}
				if (fits >= required) {
					bestArea = area;
					bestColumns = columns[i];
					bestLines = lines[j];
				}
			}
		}
		return new double[] { bestColumns, bestLines };
	}

	private static int ceilDivide(int value, int divisor) {
		return (value + divisor - 1) / divisor;
	}

	private static int round(double value) {
		return (int) Math.round(value);
	}

	/**
	 * @return the number of cells between the edges of a face which has the given size in cells
	 */
	private static int interior(int size) {
		return Math.max(0, size - 1);
	}

	/**
	 * Write the labels on the visible part of the faces of each box. A label is put on the first face which has room for the whole of it,
	 * and which is not mostly hidden. Otherwise it is put on the face with the most room. The label is centered on the widest visible
	 * part of the face.
	 *
	 * @param bounds the columns and rows (first column, last column, first row, last row) of each item
	 * @param interiors the number of cells between the edges of each face of each item
	 * @param faces the number of faces per item
	 */
	private void drawLabels(AsciiCanvas canvas, int[] bounds, int[] interiors, int faces) {
		for (int i = 0; i < items.size(); i++) {
			Item item = items.get(i);
			if (item.obstacle || item.label == null || item.label.isEmpty()) {
				continue;
			}
			int[] chosen = null;
			for (int face = 0; face < faces; face++) {
				int[] run = findLabelRun(canvas, i * faces + face, item.label.length(), bounds[i * 4], bounds[i * 4 + 1], bounds[i * 4 + 2], bounds[i * 4 + 3]);
				if (run == null) {
					continue;
				}
				if (run[0] == item.label.length() && run[4] * 2 >= interiors[i * faces + face]) {
					chosen = run;
					break;
				}
				if (chosen == null || run[0] > chosen[0] || (run[0] == chosen[0] && run[4] > chosen[4])) {
					chosen = run;
				}
			}
			if (chosen == null) {
				continue;
			}
			int fit = chosen[0];
			int row = chosen[1];
			int start = chosen[2];
			int length = chosen[3];

			int offset = length >= 3 ? 1 : 0;
			int usable = length - 2 * offset;
			int column = start + offset + (usable - fit) / 2;
			for (int j = 0; j < fit; j++) {
				canvas.setCharacter(column + j, row, item.label.charAt(j));
			}
		}
	}

	/**
	 * Find the place for a label on a face: the runs of visible cells between the edges, with a space on each side (unless the run is short).
	 * The label goes on the longest of the runs where most of the label fits, in the middle of the lines with such runs.
	 *
	 * @return {number of label characters which fit, row, first column, length of the run, number of visible cells of the face}, or null if the face is not visible
	 */
	private static int[] findLabelRun(AsciiCanvas canvas, int owner, int labelLength, int firstColumn, int lastColumn, int firstRow, int lastRow) {
		// runs of cells: {row, first column, length}, from the top row down
		List<int[]> runs = new ArrayList<>();
		int visible = 0;
		for (int row = lastRow; row >= firstRow; row--) {
			int column = firstColumn;
			while (column <= lastColumn) {
				if (!canvas.isInterior(column, row, owner)) {
					column++;
					continue;
				}
				int start = column;
				while (column <= lastColumn && canvas.isInterior(column, row, owner)) {
					column++;
				}
				runs.add(new int[] { row, start, column - start });
				visible += column - start;
			}
		}
		if (runs.isEmpty()) {
			return null;
		}

		int bestFit = 0;
		for (int[] run : runs) {
			bestFit = Math.max(bestFit, getFit(run[2], labelLength));
		}
		int longest = 0;
		for (int[] run : runs) {
			if (getFit(run[2], labelLength) == bestFit) {
				longest = Math.max(longest, run[2]);
			}
		}
		// the middle of the lines with such runs, the upper one if there is no single middle
		int lowRow = Integer.MAX_VALUE;
		int highRow = Integer.MIN_VALUE;
		for (int[] run : runs) {
			if (getFit(run[2], labelLength) == bestFit && run[2] == longest) {
				lowRow = Math.min(lowRow, run[0]);
				highRow = Math.max(highRow, run[0]);
			}
		}
		int targetRow = highRow - (highRow - lowRow) / 2;

		int[] best = null;
		for (int[] run : runs) {
			if (getFit(run[2], labelLength) == bestFit && run[2] == longest) {
				if (best == null || Math.abs(run[0] - targetRow) < Math.abs(best[0] - targetRow)) {
					best = run;
				}
			}
		}
		return new int[] { bestFit, best[0], best[1], best[2], visible };
	}

	/**
	 * @return the number of characters of a label which fit in a run of cells, leaving a space on each side
	 */
	private static int getFit(int runLength, int labelLength) {
		int usable = runLength >= 3 ? runLength - 2 : runLength;
		return Math.min(labelLength, usable);
	}
}
