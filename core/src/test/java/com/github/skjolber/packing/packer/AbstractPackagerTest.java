package com.github.skjolber.packing.packer;

import static com.github.skjolber.packing.test.assertj.ContainerAssert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.impl.ValidatingStack;
import com.github.skjolber.packing.test.ascii.ContainerAsciiArt;
import com.github.skjolber.packing.test.ascii.Figure;
import com.github.skjolber.packing.test.ascii.FigureRecorder;
import com.github.skjolber.packing.test.ascii.Figures;
import com.github.skjolber.packing.test.ascii.Style;
import com.github.skjolber.packing.test.assertj.PackagerAssert;
import com.github.skjolber.packing.validator.DefaultValidator;

@SuppressWarnings("rawtypes")
public abstract class AbstractPackagerTest {

	protected final DefaultValidator validator = new DefaultValidator();

	@AfterEach
	void closeValidator() throws Exception {
		validator.close();
	}

	protected static BoxItem box(int l, int w, int h, int count) {
		return new BoxItem(Box.newBuilder().withRotate3D().withSize(l, w, h).withWeight(0).build(), count);
	}

	/** The most columns of an overview in a single row, as in the overview of the figure utility: more and it is drawn in two rows */
	private static final int FIGURE_MAX_COLUMNS = 160;

	/** The blank columns between the views of an overview, as in the overview of the figure utility */
	private static final int FIGURE_COLUMN_GAP = 3;

	/** The widest view when even two rows of two views do not fit at the default scale: two views and a gap are at most 160 columns */
	private static final int FIGURE_VIEW_COLUMNS = (FIGURE_MAX_COLUMNS - FIGURE_COLUMN_GAP) / 2;

	/** The highest view when the views are drawn narrower than at the default scale: as high as at the default scale */
	private static final int FIGURE_VIEW_LINES = 40;

	/** The most lines of a figure of a result (all of its containers): a result which needs more is not recorded, as such a figure is hard to read */
	private static final int FIGURE_MAX_LINES = 100;

	/**
	 * Draw the containers of a result as a figure in the source code of the test, if the system property figures.record is true: the figure is
	 * written into the comment above the call of this method in the test. Each container is drawn as an overview (3D, front, top and side views),
	 * below each other, with a caption if there is more than one. A result whose figure is more than {@linkplain #FIGURE_MAX_LINES} lines high is not
	 * recorded: such a figure is hard to read, so the test has none (and a figure which is already there is not updated, so remove the call).
	 *
	 * @param result the result, with the containers to draw
	 */
	protected static void figure(PackagerResult result) {
		if (!FigureRecorder.isEnabled()) {
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
		if (figure.getHeight() > FIGURE_MAX_LINES) {
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
		if (overview.getWidth() > FIGURE_MAX_COLUMNS) {
			overview = ContainerAsciiArt.newBuilder()
					.withContainer(container)
					.withStyle(Style.ASCII)
					.withWidth(FIGURE_VIEW_COLUMNS)
					.withHeight(FIGURE_VIEW_LINES)
					.build()
					.overview();
		}
		return overview;
	}

	protected void assertDeadlineRespected(Packager packager) {

		Container container = Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(1900, 1500, 4000)
				.withMaxLoadWeight(100).withStack(new ValidatingStack()).build();

		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(container, 1)
				.build();

		List<BoxItem> products = Arrays.asList(
				box(1000, 12, 12, 1),
				box(1000, 14, 14, 1),
				box(100, 250, 410, 2),
				box(104, 81, 46, 1),
				box(116, 94, 48, 1),
				box(120, 188, 368, 1),
				box(1250, 4, 600, 3),
				box(1270, 870, 45, 1),
				box(135, 189, 75, 2),
				box(13, 20, 2500, 4),
				box(154, 195, 255, 1),
				box(180, 170, 75, 1),
				box(180, 60, 50, 2),
				box(225, 120, 500, 2),
				box(2500, 1200, 13, 1),
				box(2500, 30, 600, 1),
				box(250, 150, 230, 1),
				box(28, 56, 1094, 3),
				box(28, 75, 145, 3),
				box(29, 71, 112, 1),
				box(30, 30, 2000, 2),
				box(30, 75, 150, 2),
				box(310, 130, 460, 10),
				box(313, 313, 16, 18),
				box(32, 105, 163, 2),
				box(32, 73, 150, 2),
				box(355, 370, 161, 1),
				box(36, 23, 23, 2),
				box(380, 380, 130, 1),
				box(385, 140, 55, 1),
				box(38, 38, 30, 6),
				box(397, 169, 133, 2),
				box(39, 38, 28, 2),
				box(39, 66, 206, 2),
				box(40, 40, 2000, 2),
				box(410, 410, 170, 1),
				box(419, 646, 784, 1),
				box(41, 29, 24, 12),
				box(42, 34, 19, 2),
				box(44, 35, 28, 6),
				box(467, 174, 135, 1),
				box(46, 41, 24, 12),
				box(47, 44, 29, 6),
				box(49, 36, 36, 2),
				box(49, 48, 23, 6),
				box(4, 4, 2500, 4),
				box(50, 39, 25, 2),
				box(50, 49, 21, 6),
				box(52, 51, 21, 6),
				box(55, 46, 45, 2),
				box(570, 310, 85, 29),
				box(58, 32, 32, 1),
				box(614, 824, 96, 1),
				box(61, 51, 26, 14),
				box(625, 500, 50, 6),
				box(640, 510, 1200, 1),
				box(640, 960, 220, 1),
				box(65, 48, 231, 2),
				box(65, 64, 38, 4),
				box(68, 66, 39, 4),
				box(700, 325, 90, 1),
				box(71, 42, 39, 4),
				box(73, 43, 40, 4),
				box(79, 78, 46, 4),
				box(82, 80, 47, 4),
				box(84, 67, 44, 4),
				box(88, 52, 47, 4),
				box(90, 800, 2040, 1),
				box(970, 790, 2200, 1));

		// XXXX
		PackagerAssert.assertThat(packager).respectsDeadline(containers, products, 30 * 1000);
	}
	

	public List<BoxItem> copy(List<BoxItem> products) {
		List<BoxItem> results = new ArrayList<>();
		
		for (BoxItem boxItem : products) {
			results.add(boxItem.copy());
		}
		
		return results;
	}

	public List<BoxItemGroup> copyGroups(List<BoxItemGroup> groups) {
		List<BoxItemGroup> results = new ArrayList<>();
		
		for (BoxItemGroup boxItemGroup : groups) {
			results.add(boxItemGroup.copy());
		}
		
		return results;
	}

}
