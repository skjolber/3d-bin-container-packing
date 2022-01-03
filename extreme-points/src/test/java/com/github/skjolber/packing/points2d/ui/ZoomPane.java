package com.github.skjolber.packing.points2d.ui;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.KeyStroke;

public class ZoomPane extends JPanel implements MouseWheelListener {

	/**
	 * @author samkortchmar
	 * Mostly taken from various StackOverflow posts such as:
	 * http://stackoverflow.com/questions/13155382/jscrollpane-zoom-relative-to-mouse-position
	 * But I've modded it (clumsily) to support the pan-by-dragging and zoom-by-scrolling 
	 * behavior we want for the map. There are problems with the panning right now, I think it's
	 * redrawing too quickly which makes it jerky.
	 * 
	 * Supports:
	 * Zoom in/out to mouse pointer from mousewheel
	 * Click and drag to pan (not quite right)
	 * Zoom in/out via SHIFT-EQUALS or MINUS
	 */
	private Image background;
	private Image scaled;
	private float zoom = 1f;

	private Dimension scaledSize;

	public ZoomPane(Image image) {

		background = image;
		scaled = background;
		scaledSize = new Dimension(background.getWidth(this), background.getHeight(this));

		InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = getActionMap();

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0), "plus");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.SHIFT_DOWN_MASK), "plus");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "minus");

		//key binding for zoom in
		am.put("plus", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setZoom(getZoom() + 0.5f);
			}
		});
		
		//key binding for zoom out
		am.put("minus", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setZoom(getZoom() - 0.5f);
			}
		});
		
	    //key binding for zooming with scroll wheel
	    addMouseWheelListener(this);

		
		//key binding for panning with click-n-drag.
		//TODO fix the jerkiness.
	    MouseAdapter ma = new HandScrollListener();
	    addMouseMotionListener(ma);
	    addMouseListener(ma);
	    

		setFocusable(true);
		requestFocusInWindow();
	}

	static class HandScrollListener extends MouseAdapter {
		private final Point pp = new Point();
		@Override public void mouseDragged(MouseEvent e) {
			ZoomPane z = (ZoomPane) e.getSource();
			JViewport vport = (JViewport) z.getParent();
			JComponent img = (JComponent) vport.getView();
			Point cp = e.getPoint();
			Point vp = vport.getViewPosition();
			vp.translate(pp.x-cp.x, pp.y-cp.y);
			img.scrollRectToVisible(new Rectangle(vp, vport.getSize()));
			pp.setLocation(cp);
		}
		@Override public void mousePressed(MouseEvent e) {
			pp.setLocation(e.getPoint());
		}
	}

	@Override
	public void addNotify() {
		super.addNotify();
	}

	public float getZoom() {
		return zoom;
	}

	public void setZoom(float value) {
		//We need the mouse to be in the JViewport to do anything, since
		//all zoom actions are dependent on mouse location.
		Point mouse = getMousePosition();
		if (zoom != value && value > 0 && mouse != null) {
			
			
			JViewport parent = (JViewport) getParent();
			Point viewPort = parent.getViewPosition();
			Rectangle viewRect = parent.getViewRect();

			int width = (int) Math.floor(background.getWidth(this) * value);
			int height = (int) Math.floor(background.getHeight(this) * value);
			
			
			//Things stop working if the image is smaller than the jviewport.
			if (width < viewRect.width || height < viewRect.height) {
				return;
			}
			
			//The appropriately scaled version of the background image to be repainted.
			scaled = background.getScaledInstance(width, height, Image.SCALE_SMOOTH);
			scaledSize = new Dimension(width, height);
			
			//I think this is a little bit ungraceful - we need it because
			//the MousePosition() and JViewport are relative to the component
			//which is basically the image, which means they both already have
			//the existing zoom factored into them. So every time we scale, we
			//remove the existing scale factor and then multiply by the new one.
			//not sure if there is a better way to do it. 
			float scaleFactor = value / zoom;

			if (getParent() instanceof JViewport) {
				//Establishes the top left corner of the viewPort relative to the image. See: 
				//http://stackoverflow.com/questions/13155382/jscrollpane-zoom-relative-to-mouse-position
				//for a helpful description. 
				//					(Ix' component)											(Vx component)
				viewRect.x = (int) ((mouse.x - viewPort.x) * (scaleFactor - 1) + (scaleFactor) * viewPort.x);
				viewRect.y = (int) ((mouse.y - viewPort.y) * (scaleFactor - 1) + (scaleFactor) * viewPort.y);
				zoom = value;
				
				//scrollRectToVisible(viewRect);
			}
			invalidate();
			repaint();
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return scaledSize;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (scaled != null) {
			g.drawImage(scaled, 0, 0, this);
		}
	}

	protected void centerInViewport() {
		Container container = getParent();
		if (container instanceof JViewport) {

			JViewport port = (JViewport) container;
			Rectangle viewRect = port.getViewRect();

			int width = getWidth();
			int height = getHeight();

			viewRect.x = (width - viewRect.width) / 2;
			viewRect.y = (height - viewRect.height) / 2;

			scrollRectToVisible(viewRect);
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		int notches = e.getWheelRotation();
		float zoomAmount = 0.05f;
		setZoom(getZoom() - notches * zoomAmount);
	}
}