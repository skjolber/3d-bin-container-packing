package com.github.skjolber.packing;

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.Level;
import com.github.skjolber.packing.Placement;
import com.github.skjolber.packing.Space;
import com.indvd00m.ascii.render.Render;
import com.indvd00m.ascii.render.api.ICanvas;
import com.indvd00m.ascii.render.api.IContextBuilder;
import com.indvd00m.ascii.render.api.IRender;
import com.indvd00m.ascii.render.elements.Dot;
import com.indvd00m.ascii.render.elements.Rectangle;

/**
 * Simple ASCII style visualizer for level layout.
 * 
 */

class Visualizer {

	public static String visualize(Container container, int size, double horizontalScaling) {
		StringBuilder b = new StringBuilder();
		
		for(Level level : container.getLevels()) {
			double factor = (double)size / container.getWidth();
					
			IRender render = new Render();
			IContextBuilder builder = render.newBuilder();
			
			int w = (int)(size * horizontalScaling);
			int d = ((size * container.getDepth()) / container.getWidth());
			
			builder.width(w).height(d);

			for(Placement placement : level) {
				Space space = placement.getSpace();
				Box box = placement.getBox();

				builder.element(new Rectangle((int)Math.round(space.getX() * factor * horizontalScaling), (int)Math.round(space.getY() * factor), (int)(box.getWidth()  * factor * horizontalScaling), (int)(box.getDepth()  * factor)));
				if(box.getWidth() > 1 && box.getDepth() > 1) {
					builder.element(new Dot((int)Math.round(placement.getCenterX() * factor * horizontalScaling), (int)Math.round(placement.getCenterY() * factor)));
				}
			}

			builder.element(new Rectangle(0, 0, (int)(container.getWidth()  * factor * horizontalScaling), (int)(container.getDepth()  * factor)));

			ICanvas canvas = render.render(builder.build());
			
			b.append(canvas.getText());
			b.append("\n");
		}
		return b.toString();
	}
	
}
