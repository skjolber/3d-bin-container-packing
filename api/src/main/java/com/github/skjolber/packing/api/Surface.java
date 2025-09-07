package com.github.skjolber.packing.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Surface {

	public static final Surface FRONT = new Surface(Label.FRONT);
	public static final Surface REAR = new Surface(Label.REAR);
	public static final Surface LEFT = new Surface(Label.LEFT);
	public static final Surface RIGHT = new Surface(Label.RIGHT);
	public static final Surface BOTTOM = new Surface(Label.BOTTOM);
	public static final Surface TOP = new Surface(Label.TOP);

	public static final List<Surface> DEFAULT_SURFACE = Arrays.asList(BOTTOM, TOP);
	//
	// Basically there is up to 6 alternative ways to place a box, i.e. 3 unique sides, each rotated 90 degrees. 
	//  
	//  
	// z              y
	// |             / 
	// |            / 
	// |         /-------/|
	// |        /       / |
	// |       /       /  |    
	// |      /  top  / t /
	// |     /       / h /  
    // |    /       / g / 
	// |   /       / i /
	// |  |-------| r /      
	// |  |       |  /
	// |  | front | /
	// |  |-------|/      
	// | /      
	// |/       
	// |------------------ x
	//
	// No rotation
	//          
	//                   |--------------|
	//                   |              |
	//                   |     top      |
	//                   |              |
	//  ║════════════════║══════════════║----------------|--------------|
	//  ║     left       ║     front    ║     right      |     rear     |
	//  ║     yz 0       ║     xz 0     ║                |              |
	//  ║════════════════║══════════════║----------------|--------------|
	//                   ║              ║
	//                   ║    bottom    ║
	//                   ║     xy 0     ║
	//                   ║══════════════║
	//
	// Rotated
	//
	//                    ║═══════║
	//                    ║       ║
	//                    ║       ║
	//                    ║       ║
	//                    ║  left ║
	//                    ║ yz 90 ║
	//                    ║       ║
	//                    ║       ║
	//           ║════════║═══════║--------|
	//           ║        ║       ║        |
	//           ║        ║       ║        |
	//           ║ bottom ║ front ║  top   |
	//           ║  xy 90 ║ xz 90 ║        |
	//           ║        ║       ║        |
	//           ║════════║═══════║--------|
	//                    |       |
	//                    |       |
	//                    |       |
	//                    | right |
	//                    |       |
	//                    |       |
	//                    |       |
	//                    |-------| 
	//                    |       |
	//                    |       |
	//                    |  rear |
	//                    |       |
	//                    |       |
	//                    |-------| 
	//
	
	public static enum Label {

		FRONT, REAR, TOP, BOTTOM, LEFT, RIGHT

	}

	protected Label label;
	protected Map<String, ?> attributes = null;

	public Surface(Label label) {
		this(label, Collections.emptyMap());
	}

	public Surface(Label label, Map<String, ?> attributes) {
		super();
		this.label = label;
		this.attributes = attributes;
	}

	public Map<String, ?> getAttributes() {
		return attributes;
	}

	public Label getLabel() {
		return label;
	}

	@Override
	public String toString() {
		if (attributes == null || attributes.isEmpty()) {
			return label.toString();
		}
		return "Surface [label=" + label + ", attributes=" + attributes + "]";
	}

}
