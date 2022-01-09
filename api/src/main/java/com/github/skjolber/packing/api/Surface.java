package com.github.skjolber.packing.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Surface {
	
	public static final List<Surface> DEFAULT_SURFACE = Arrays.asList(new Surface(Label.BOTTOM), new Surface(Label.TOP));
	
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
		
		FRONT,
		REAR,
		TOP,
		BOTTOM,
		LEFT,
		RIGHT
		
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
		if(attributes == null) {
			return label.toString();
		}
		return "Surface [label=" + label + ", attributes=" + attributes + "]";
	}
	
	
}
