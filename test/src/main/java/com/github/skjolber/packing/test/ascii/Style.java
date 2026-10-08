package com.github.skjolber.packing.test.ascii;

/**
 * The characters used to draw the edges of the boxes. The edges of a box are drawn with straight lines, which are joined with corners and
 * junctions where they meet, and with diagonal lines in the oblique ("3D") view.
 */
public enum Style {

	/** Thin lines, with square corners. */
	LIGHT("─│╱", "┌┐└┘", "├┤┬┴┼", '░', '▒'),
	/** As {@linkplain #LIGHT}, but with round corners. */
	ROUNDED("─│╱", "╭╮╰╯", "├┤┬┴┼", '░', '▒'),
	/** Thick lines for all straight edges; there is no thick diagonal line. */
	HEAVY("━┃╱", "┏┓┗┛", "┣┫┳┻╋", '░', '▒'),
	/**
	 * Characters which are in ASCII only. In the front, top and side views, the corners and junctions are '+'. In the oblique view, the vertical
	 * edges are drawn through the corners and junctions, then the diagonal edges.
	 */
	ASCII("-|/", "++++", "+++++", '.', ':');

	final char horizontal;
	final char vertical;
	final char diagonal;

	final char topLeft;
	final char topRight;
	final char bottomLeft;
	final char bottomRight;

	final char teeRight;
	final char teeLeft;
	final char teeDown;
	final char teeUp;
	final char cross;

	/** Fill of the top faces when shading */
	final char topShade;
	/** Fill of the right faces when shading */
	final char rightShade;

	Style(String lines, String corners, String junctions, char topShade, char rightShade) {
		this.horizontal = lines.charAt(0);
		this.vertical = lines.charAt(1);
		this.diagonal = lines.charAt(2);

		this.topLeft = corners.charAt(0);
		this.topRight = corners.charAt(1);
		this.bottomLeft = corners.charAt(2);
		this.bottomRight = corners.charAt(3);

		this.teeRight = junctions.charAt(0);
		this.teeLeft = junctions.charAt(1);
		this.teeDown = junctions.charAt(2);
		this.teeUp = junctions.charAt(3);
		this.cross = junctions.charAt(4);

		this.topShade = topShade;
		this.rightShade = rightShade;
	}

	/**
	 * @param up a vertical line continues up
	 * @param down a vertical line continues down
	 * @param left a horizontal line continues to the left
	 * @param right a horizontal line continues to the right
	 * @return the corner or junction where at least one vertical and at least one horizontal line meet
	 */
	char junction(boolean up, boolean down, boolean left, boolean right) {
		if (up && down) {
			if (left && right) {
				return cross;
			}
			return left ? teeLeft : teeRight;
		}
		if (left && right) {
			return up ? teeUp : teeDown;
		}
		if (up) {
			return left ? bottomRight : bottomLeft;
		}
		return left ? topRight : topLeft;
	}
}
