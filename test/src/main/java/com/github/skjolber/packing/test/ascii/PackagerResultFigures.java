package com.github.skjolber.packing.test.ascii;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;

/**
 * Figures of packager results in the source code of tests: call {@linkplain #figure(PackagerResult)} with the result in a test, and run the tests with
 * the system property {@code figures.record=true} to write the figure into the comment above the call.
 */
public class PackagerResultFigures {

	/** The most columns of an overview in a single row, as in the overview of the figure utility: more and it is drawn in two rows */
	private static final int MAX_COLUMNS = 160;

	/** The blank columns between the views of an overview, as in the overview of the figure utility */
	private static final int COLUMN_GAP = 3;

	/** The widest view when even two rows of two views do not fit at the default scale: two views and a gap are at most 160 columns */
	private static final int VIEW_COLUMNS = (MAX_COLUMNS - COLUMN_GAP) / 2;

	/** The highest view when the views are drawn narrower than at the default scale: as high as at the default scale */
	private static final int VIEW_LINES = 40;

	/** The most lines of a figure of a result (all of its containers): a result which needs more is not recorded, as such a figure is hard to read */
	private static final int MAX_LINES = 100;

	private PackagerResultFigures() {
		// utility class
	}

	/**
	 * Draw the containers of a result as a figure in the source code of the test, if the system property figures.record is true: the figure is
	 * written into the comment above the call of this method in the test. Each container is drawn as an overview (3D, front, top and side views),
	 * below each other, with a caption if there is more than one. A result whose figure is more than {@linkplain #MAX_LINES} lines high is not
	 * recorded: such a figure is hard to read, so the test has none (and a figure which is already there is not updated, so remove the call).
	 *
	 * @param result the result, with the containers to draw
	 */
	public static void figure(PackagerResult result) {
		if (!FigureRecorder.isEnabled() || result == null) {
			return;
		}
		List<Container> containers = result.getContainers();
		if (containers.isEmpty()) {
			return;
		}

		List<Figure> figures = new ArrayList<>(containers.size());
		for (int i = 0; i < containers.size(); i++) {
			Container container = containers.get(i);

			Figure overview = drawOverview(container);
			if (containers.size() > 1) {
				String name = container.getDescription() != null ? container.getDescription() : container.getId();
				String caption = "container " + (i + 1) + " of " + containers.size() + (name != null ? ": " + name : "");
				overview = Figures.vertical(0, Figure.of(caption), overview);
			}
			figures.add(overview);
		}
		Figure figure = Figures.vertical(1, figures.toArray(new Figure[0]));
		if (figure.getHeight() > MAX_LINES) {
			return;
		}
		FigureRecorder.record(figure);
	}

	/**
	 * The overview in the ASCII style at the default scale, which makes the labels fit: in one row if the four views are at most 160 columns wide, otherwise in
	 * two rows of two views. Only if even a row of two views is wider than that (about 78 columns per view, which happens for containers with large boxes), the
	 * views are drawn narrower, all at the same scale.
	 */
	private static Figure drawOverview(Container container) {
		// ASCII: the figures are read in editors where the box drawing characters do not join across lines
		Figure overview = ContainerAsciiArt.newBuilder().withContainer(container).withStyle(Style.ASCII).build().overview();
		if (overview.getWidth() > MAX_COLUMNS) {
			overview = ContainerAsciiArt.newBuilder()
					.withContainer(container)
					.withStyle(Style.ASCII)
					.withWidth(VIEW_COLUMNS)
					.withHeight(VIEW_LINES)
					.build()
					.overview();
		}
		return overview;
	}
}
