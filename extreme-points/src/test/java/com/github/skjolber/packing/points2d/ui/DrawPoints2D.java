package com.github.skjolber.packing.points2d.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.ep.Point2D;
import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.points.DefaultExtremePoints2D;
import com.github.skjolber.packing.points.DefaultExtremePoints3D;

public class DrawPoints2D {

	public static void show(DefaultExtremePoints2D p) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				
				init(p.getValues(), p.getPlacements(), p.getWidth(), p.getDepth());
			}
		});
	}

	public static void show3D(List<? extends Point2D> points, List<? extends Placement2D> placements, int width, int depth) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				
				init(points, placements, width, depth);
			}
		});
	}
	
	public static void show(DefaultExtremePoints3D p) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				
				List<Point3D<Placement3D>> values = p.getValues();
				
				init(values, p.getPlacements(), p.getWidth(), p.getDepth());
			}
		});
	}
	
	private static void init(List<? extends Point2D> points, List<? extends Placement2D> placements, int width, int depth) {
		DrawingArea drawingArea = new DrawingArea(width, depth);

		System.out.println("Show " + width + "x" + depth);
		
		for (Point2D extremePoint : points) {
			if (extremePoint.getMaxX() != width || extremePoint.getMaxY() != depth) {
				Color c;
				if (extremePoint.getMaxX() != width && extremePoint.getMaxY() != depth) {
					c = Color.red;
				} else if (extremePoint.getMaxX() != width) {
					c = Color.blue;
				} else {
					c = Color.yellow;
				}
				drawingArea.fillRect(extremePoint.getMinX(), extremePoint.getMinY(), extremePoint.getMaxX(),
						extremePoint.getMaxY(), c);
				System.out.println("Paint constrained " + extremePoint.getMinX() + "x" + extremePoint.getMinY() + " "
						+ extremePoint.getMaxX() + "x" + extremePoint.getMaxY());
			}
		}

		for (Point2D extremePoint : points) {
			if (extremePoint.getMaxX() == width && extremePoint.getMaxY() == depth) {
				System.out.println("Paint white " + extremePoint.getMinX() + "x" + extremePoint.getMinY() + " "
						+ extremePoint.getMaxX() + "x" + extremePoint.getMaxY());
				drawingArea.fillRect(extremePoint.getMinX(), extremePoint.getMinY(), extremePoint.getMaxX(),
						extremePoint.getMaxY(), Color.white);
			}
		}

		for (Placement2D extremePoint : placements) {
			drawingArea.addRectangle(extremePoint.getAbsoluteX(), extremePoint.getAbsoluteY(),
					extremePoint.getAbsoluteEndX(), extremePoint.getAbsoluteEndY(), Color.green);
			drawingArea.addDashedLine(extremePoint.getAbsoluteX(), extremePoint.getAbsoluteY(),
					extremePoint.getAbsoluteEndX(), extremePoint.getAbsoluteEndY(), Color.red);
			
			System.out.println(" " + extremePoint.getAbsoluteX() + "x" + extremePoint.getAbsoluteY());

		}

		for (int i = 0; i < points.size(); i++) {
			Point2D point2d = points.get(i);
			drawingArea.addCircle(point2d.getMinX(), point2d.getMinY(), Color.black, i);
		}

		drawingArea.addGuide(0, -5, width, -5, Color.blue);

		JFrame.setDefaultLookAndFeelDecorated(true);
		JFrame frame = new JFrame("Draw On extreme points");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		BufferedImage image = drawingArea.getImage();
		MainPanel mainPanel = new MainPanel(image);
		mainPanel.setBounds(0, 0, image.getWidth(), image.getHeight());
		mainPanel.setBorder(BorderFactory.createLineBorder(Color.black));

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		frame.getContentPane().add(mainPanel);

		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		frame.setBounds(0, 0, image.getWidth(), image.getHeight());

	}

	private static class DrawingArea extends JPanel {

		private static final long serialVersionUID = 1L;
		
		private int width;
		private int depth;

		private BufferedImage image;
		private Rectangle shape;

		public DrawingArea(int width, int depth) {
			this.width = width;
			this.depth = depth;

			image = new BufferedImage(width + 50, depth + 50, BufferedImage.TYPE_INT_ARGB);

			setBackground(Color.WHITE);

			MyMouseListener ml = new MyMouseListener();
			addMouseListener(ml);
			addMouseMotionListener(ml);

			addRectangle(new Rectangle(0, 0, width, depth), Color.black);

		}

		public BufferedImage getImage() {
			return image;
		}

		public void fillRect(int x, int y, int xx, int yy, Color color) {
			Graphics2D g2d = (Graphics2D) image.getGraphics();
			g2d.setColor(color);
			g2d.fillRect(x, depth - yy, xx - x + 1, (yy - y + 1));
			repaint();

		}

		@Override
		public Dimension getPreferredSize() {
			return isPreferredSizeSet() ? super.getPreferredSize() : new Dimension(image.getWidth(), image.getHeight());
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			// Custom code to support painting from the BufferedImage
			if (image != null) {
				g.drawImage(image, 0, 0, null);
			}

			// Paint the Rectangle as the mouse is being dragged
			if (shape != null) {
				Graphics2D g2d = (Graphics2D) g;
				g2d.draw(shape);
			}
		}

		public void addRectangle(int x1, int y1, int x2, int y2, Color color) {
			addRectangle(new Rectangle(x1, depth - y2, x2 - x1, y2 - y1), color);
		}

		public void addRectangle(Rectangle rectangle, Color color) {
			// Draw the Rectangle onto the BufferedImage

			Graphics2D g2d = (Graphics2D) image.getGraphics();
			g2d.setColor(color);
			g2d.draw(rectangle);
			repaint();
		}

		public void addCircle(int x1, int y1, Color color, int index) {
			Graphics2D g2d = (Graphics2D) image.getGraphics();
			g2d.setColor(color);
			
			int size = 10;
			
			g2d.drawOval(x1 - size / 2, depth - y1 - size / 2, size, size);
			g2d.drawLine(x1, depth - y1, x1, depth - y1);
			repaint();
		}

		public void addLine(int x1, int y1, int x2, int y2, Color color) {
			// Draw the Rectangle onto the BufferedImage

			Graphics2D g2d = (Graphics2D) image.getGraphics();
			g2d.setColor(color);
			g2d.drawLine(x1, depth - y1, x2, depth - y2);
			repaint();
		}

		public void addGuide(int x1, int y1, int x2, int y2, Color color) {
			// Draw the Rectangle onto the BufferedImage

			Graphics2D g2d = (Graphics2D) image.getGraphics();
			g2d.setColor(color);
			drawDashed(g2d, x1, depth - y1, x2, depth - y2, 1, 9);
			repaint();
		}

		public void addDashedLine(int x1, int y1, int x2, int y2, Color color) {
			// Draw the Rectangle onto the BufferedImage

			Graphics2D g2d = (Graphics2D) image.getGraphics();
			g2d.setColor(color);
			drawDashed(g2d, x1, depth - y1, x2, depth - y2, 5, 5);
			repaint();
		}

		class MyMouseListener extends MouseInputAdapter {
			private java.awt.Point startPoint;

			public void mousePressed(MouseEvent e) {
				startPoint = e.getPoint();
				shape = new Rectangle();
			}

			public void mouseDragged(MouseEvent e) {
				int x = Math.min(startPoint.x, e.getX());
				int y = Math.min(startPoint.y, e.getY());
				int width = Math.abs(startPoint.x - e.getX());
				int height = Math.abs(startPoint.y - e.getY());

				shape.setBounds(x, y, width, height);
				repaint();
			}

			public void mouseReleased(MouseEvent e) {
				if (shape.width != 0 || shape.height != 0) {
					addRectangle(shape, e.getComponent().getForeground());
				}

				shape = null;
			}
		}

		public static void drawDashed(Graphics g, int x1, int y1, int x2, int y2, int dashSize, int gapSize) {
			if (x2 < x1) {
				int temp = x1;
				x1 = x2;
				x2 = temp;
			}
			if (y2 < y1) {
				int temp = y1;
				y1 = y2;
				y2 = temp;
			}
			int totalDash = dashSize + gapSize;
			if (y1 == y2) {
				int virtualStartX = (x1 / totalDash) * totalDash;
				for (int x = virtualStartX; x < x2; x += totalDash) {
					int topX = x + dashSize;
					if (topX > x2) {
						topX = x2;
					}
					int firstX = x;
					if (firstX < x1) {
						firstX = x1;
					}
					if (firstX < topX) {
						g.drawLine(firstX, y1, topX, y1);
					}
				}
			} else if (x1 == x2) {
				int virtualStartY = (y1 / totalDash) * totalDash;
				for (int y = virtualStartY; y < y2; y += totalDash) {
					int topY = y + dashSize;
					if (topY > y2) {
						topY = y2;
					}
					int firstY = y;
					if (firstY < y1) {
						firstY = y1;
					}
					if (firstY < topY) {
						g.drawLine(x1, firstY, x1, topY);
					}
				}
			} else {
				// Not supported
				g.drawLine(x1, y1, x2, y2);
			}
		}
	}
}