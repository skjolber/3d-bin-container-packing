package com.github.skjolber.packing.test.ascii;

/**
 * A grid of cells where every cell remembers how far away the thing drawn into it is: the nearest wins, so that
 * parts of boxes which are hidden behind other boxes are not drawn. Row 0 is the bottom row.
 * <p>
 * A cell holds either a character (the inside of a face, a label, ...) or the directions of the edges which meet there
 * (horizontal, vertical and diagonal). The character of an edge is chosen when the lines are written, from the directions
 * of the edges in the cell and in the cells next to it: so that straight lines are joined with corners and junctions,
 * and run right up to diagonal lines.
 * <p>
 * At equal depth, edges win over the inside of a face, and the directions of edges are added.
 */
final class AsciiCanvas {

	/** Nothing drawn */
	static final int EMPTY = Integer.MAX_VALUE;
	/** The outline of the container: behind all boxes */
	static final int OUTLINE = Integer.MAX_VALUE - 1;
	/** Axes: in front of the outline, behind all boxes */
	static final int AXIS = Integer.MAX_VALUE - 2;
	/** Axis labels: in front of everything */
	static final int LABEL = Integer.MIN_VALUE;

	static final int HORIZONTAL = 1;
	static final int VERTICAL = 2;
	static final int DIAGONAL = 4;

	static final int NO_OWNER = -1;

	/** The most cells of a canvas: guards against huge drawings, for example from dimensions in millimeters */
	private static final long MAX_CELLS = 1_000_000L;

	private final int width;
	private final int height;

	/** The character of the cells which have no edges */
	private final char[] characters;
	private final int[] depths;
	/** The directions of the edges which meet in the cell, or zero */
	private final byte[] directions;
	/** Which face is drawn in the cell (for the inside of a face) */
	private final int[] owners;

	AsciiCanvas(int width, int height) {
		if ((long) width * (long) height > MAX_CELLS) {
			throw new IllegalStateException("The drawing would be " + width + " x " + height + " characters, use withWidth(columns) or a smaller withScale(cx, cz)");
		}
		this.width = width;
		this.height = height;

		int size = width * height;
		this.characters = new char[size];
		this.depths = new int[size];
		this.directions = new byte[size];
		this.owners = new int[size];

		for (int i = 0; i < size; i++) {
			characters[i] = ' ';
			depths[i] = EMPTY;
			owners[i] = NO_OWNER;
		}
	}

	/**
	 * Draw an edge, unless something nearer is already there. Cells outside the canvas are ignored.
	 *
	 * @param edgeDirections a combination of {@linkplain #HORIZONTAL}, {@linkplain #VERTICAL} and {@linkplain #DIAGONAL}
	 */
	void putEdge(int column, int row, int edgeDirections, int depth) {
		if (column < 0 || column >= width || row < 0 || row >= height) {
			return;
		}
		int index = row * width + column;
		int existing = depths[index];
		if (depth > existing) {
			return;
		}
		if (depth < existing || directions[index] == 0) {
			directions[index] = (byte) edgeDirections;
			depths[index] = depth;
			owners[index] = NO_OWNER;
		} else {
			directions[index] |= (byte) edgeDirections;
		}
	}

	/**
	 * Draw a character, unless something nearer is already there. Cells outside the canvas are ignored.
	 */
	void putCharacter(int column, int row, char c, int depth, int owner) {
		if (column < 0 || column >= width || row < 0 || row >= height) {
			return;
		}
		int index = row * width + column;
		if (depth < depths[index]) {
			characters[index] = c;
			directions[index] = 0;
			depths[index] = depth;
			owners[index] = owner;
		}
	}

	/**
	 * Change the character of a cell, keeping everything else (for labels).
	 */
	void setCharacter(int column, int row, char c) {
		characters[row * width + column] = c;
	}

	/**
	 * @return true if the cell shows the inside of the face with the given owner
	 */
	boolean isInterior(int column, int row, int owner) {
		if (column < 0 || column >= width || row < 0 || row >= height) {
			return false;
		}
		int index = row * width + column;
		return owners[index] == owner && directions[index] == 0;
	}

	private boolean hasDirection(int column, int row, int direction) {
		if (column < 0 || column >= width || row < 0 || row >= height) {
			return false;
		}
		return (directions[row * width + column] & direction) != 0;
	}

	/**
	 * @param style the characters of the edges
	 * @param oblique true for the oblique view, which has diagonal edges
	 * @return the lines from the top row down, without trailing blanks
	 */
	String[] toLines(Style style, boolean oblique) {
		String[] lines = new String[height];
		char[] line = new char[width];
		for (int row = 0; row < height; row++) {
			int end = 0;
			for (int column = 0; column < width; column++) {
				line[column] = getCharacter(style, oblique, column, row);
				if (line[column] != ' ') {
					end = column + 1;
				}
			}
			lines[height - 1 - row] = new String(line, 0, end);
		}
		return lines;
	}

	private char getCharacter(Style style, boolean oblique, int column, int row) {
		int index = row * width + column;
		int edges = directions[index];
		if (edges == 0) {
			return characters[index];
		}
		if (edges == DIAGONAL) {
			return style.diagonal;
		}
		boolean horizontal = (edges & HORIZONTAL) != 0;
		boolean vertical = (edges & VERTICAL) != 0;

		if (style == Style.ASCII && oblique) {
			// a vertical line is hidden except for its end point where it meets other edges: draw the other edges
			if (vertical && edges != VERTICAL && !hasDirection(column, row - 1, VERTICAL) && !hasDirection(column, row + 1, VERTICAL)) {
				vertical = false;
			}
			if (vertical) {
				return style.vertical;
			}
			return (edges & DIAGONAL) != 0 ? style.diagonal : style.horizontal;
		}

		if ((edges & DIAGONAL) != 0) {
			if (horizontal && !vertical) {
				// a diagonal line ends on a horizontal line
				boolean right = hasDirection(column + 1, row, HORIZONTAL);
				boolean left = hasDirection(column - 1, row, HORIZONTAL);
				if (left && right) {
					// the end point is on a line which continues at both sides, for example the edge of another box
					return style.teeDown;
				}
				// the corner of a face if the line continues to the right, otherwise the line runs right up to the diagonal
				return right ? style.topLeft : style.horizontal;
			}
			if (vertical && !horizontal) {
				return style.vertical;
			}
		}
		// in the oblique view, the edges of the cell itself decide which lines can continue; in the other views, the edge of a box which is
		// nearer than the edge of another box, which meets it, is also joined with it
		boolean up = (vertical || !oblique) && hasDirection(column, row + 1, VERTICAL);
		boolean down = (vertical || !oblique) && hasDirection(column, row - 1, VERTICAL);
		boolean left = (horizontal || !oblique) && hasDirection(column - 1, row, HORIZONTAL);
		boolean right = (horizontal || !oblique) && hasDirection(column + 1, row, HORIZONTAL);

		if ((left || right) && (up || down)) {
			return style.junction(up, down, left, right);
		}
		if (left || right) {
			return style.horizontal;
		}
		if (up || down) {
			return style.vertical;
		}
		return horizontal ? style.horizontal : style.vertical;
	}
}
