import * as THREE from "three";
import { Color, Mesh, Object3D, Scene } from "three";
import randomColor from "randomcolor";
import { TextGeometry } from "three/examples/jsm/geometries/TextGeometry";
import { Font } from "three/examples/jsm/loaders/FontLoader";

const helvetiker = require( 'three/examples/fonts/droid/droid_sans_mono_regular.typeface.json');
const font = new Font( helvetiker );
const textMaterial = new THREE.MeshPhongMaterial( { color: 0xffffff } );

// Container styling constants
const CONTAINER_BOX_COLOR = 0x888888;
const CONTAINER_BOX_OPACITY = 0.15;
const CONTAINER_EDGE_COLOR = 0x444444;

export class Point {
    
    x : number;
    y : number;
    z : number;
    
    dx : number;
    dy : number;
    dz : number;

    constructor(x : number, y : number, z: number, dx : number, dy : number, dz: number) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }
}

export class Stackable {

    dx : number;
    dy : number;
    dz : number;

    name: string;
    id: string;

    step : number;
    
    constructor(name : string, id : string, step: number, dx : number, dy : number, dz: number) {
        this.name = name;
        this.id = id;
        this.step = step;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

}

export class Box extends Stackable {

    constructor(name : string, id : string, step: number, dx : number, dy : number, dz: number) {
        super(name, id, step, dx, dy, dz);
    }
    
}

export class Container extends Stackable {

    loadDx : number;
    loadDy : number;
    loadDz : number;
    
    stack : Stack;

    constructor(name : string, id : string, step: number, dx : number, dy : number, dz: number, loadDx : number, loadDy : number, loadDz: number) {
        super(name, id, step, dx, dy, dz);

        this.loadDx = loadDx;
        this.loadDy = loadDy;
        this.loadDz = loadDz;

        this.stack = new Stack(step);
    }
    
    add(stackPlacement : StackPlacement) {
        this.stack.add(stackPlacement);
    }
}

export class StackPlacement {

    stackable : Stackable;
    x : number;
    y : number;
    z : number;

    step : number;

    points : Array<Point>;

    constructor(stackable : Stackable, step : number, x : number, y : number, z: number, points: Array<Point>) {
        this.stackable = stackable;
        this.step = step;
        this.x = x;
        this.y = y;
        this.z = z;
        this.points = points;
    }

}

export class Stack {

    placements : Array<StackPlacement>;

    step : number;

    constructor(step : number) {
        this.step = step;
        this.placements = new Array();
    }

    add(placement : StackPlacement) {
        this.placements.push(placement);
    }

}

export class ContainerControls {

    parent : Object3D;
    child : Object3D;
    container : Container;

    constructor(parent : Object3D, child : Object3D, container : Container) {
        this.parent = parent;
        this.child = child;
        this.container = container;
    }
}

export interface ColorScheme {
    getPoint(point : Point) : Color;
    getStackable(stackable : Stackable) : Color;
    getColorScheme(container : Container) : ColorScheme;
}

export class RandomColorScheme implements ColorScheme {
    getPoint(point : Point) : Color {
        return new Color(randomColor());
    }
    getStackable(stackable : Stackable) : Color {
        return new Color(randomColor());
    }
    getColorScheme(container : Container) : ColorScheme {
        return this;
    }

}

export class MemoryColorScheme implements ColorScheme {

    delegate : ColorScheme;
    map : Map<string, Color>;

    constructor(delegate : ColorScheme) {
        this.delegate = delegate;
        this.map = new Map();
    }

    getStackable(stackable : Stackable) : Color {
        if(!stackable.id) {
            // use random
            return this.delegate.getStackable(stackable);
        }
        // use same as before, for the
        var color = this.map.get(stackable.id);
        if(!color) {
            color = this.delegate.getStackable(stackable);
            this.map.set(stackable.id, color);
        }
        return color;
    }

    getColorScheme(container : Container) : ColorScheme {
        return this;
    }
    
    getPoint(point : Point) : Color {
        // use same as before, for the
        var id = point.x + "x"+point.y + "x" + point.z + " " + point.dx+"x" + point.dy + "x" + point.dz;
        var color = this.map.get(id);
        if(!color) {
            color = this.delegate.getPoint(point);
            this.map.set(id, color);
        }
        return color;
    }

}

export class StackableRenderer {

    add(parent: Object3D, colorScheme : ColorScheme, stackPlacement : StackPlacement, x: number, y:number, z: number): Object3D | undefined {

        var stackable = stackPlacement.stackable;
        
        if(stackable instanceof Container) {
            var containerStackable : Container = stackable;

            var color = colorScheme.getStackable(containerStackable);
            
            // Create a group for the container (will hold transparent box + edges)
            var containerGroup = new THREE.Group();
            
            // Create transparent box mesh for container volume
            var containerBoxMaterial = new THREE.MeshPhongMaterial({
                color: CONTAINER_BOX_COLOR,
                opacity: CONTAINER_BOX_OPACITY,
                transparent: true,
                depthWrite: false
            });
            var containerBoxGeometry = new THREE.BoxGeometry(containerStackable.dy, containerStackable.dz, containerStackable.dx);
            var containerBox = new THREE.Mesh(containerBoxGeometry, containerBoxMaterial);
            containerBox.userData = { type: "container" };
            
            // Create edges for container
            var containerEdgesGeometry = new THREE.EdgesGeometry(containerBoxGeometry);
            var containerEdgesMaterial = new THREE.LineBasicMaterial({ color: CONTAINER_EDGE_COLOR });
            var containerEdges = new THREE.LineSegments(containerEdgesGeometry, containerEdgesMaterial);
            containerEdges.userData = { type: "container" };
            
            // Add both to container group
            containerGroup.add(containerBox);
            containerGroup.add(containerEdges);
            
            // Create load area edges (lighter color)
            var containerLoadGeometry = new THREE.EdgesGeometry(new THREE.BoxGeometry(containerStackable.loadDy, containerStackable.loadDz, containerStackable.loadDx));
            var containerLoadMaterial = new THREE.LineBasicMaterial({ color: color });
            var containerLoad = new THREE.LineSegments(containerLoadGeometry, containerLoadMaterial);

            containerGroup.position.x = stackPlacement.y + containerStackable.dy / 2 + x;
            containerGroup.position.y = stackPlacement.z + containerStackable.dz / 2 + y;
            containerGroup.position.z = stackPlacement.x + containerStackable.dx / 2 + z;

            var offsetX = - containerStackable.dy / 2;
            var offsetY = - containerStackable.dz / 2;
            var offsetZ = - containerStackable.dx / 2;

            console.log("Add container " + containerStackable.name + " size " + containerStackable.dx + "x" + containerStackable.dy + "x" + containerStackable.dz + " with load " + containerStackable.loadDx + "x" + containerStackable.loadDy + "x" + containerStackable.loadDz + " at " + stackPlacement.x + "x" + stackPlacement.y + "x" + stackPlacement.z) ;

            containerGroup.name = containerStackable.name;
            containerGroup.userData = {
                step: 0,
                type: "container",
                source: containerStackable,
                id: containerStackable.id
            };
            
            containerLoad.name = containerStackable.name;
            containerLoad.userData = {
                step: 0,
                type: "containerLoad",
                offsetX : offsetX,
                offsetY : offsetY,
                offsetZ : offsetZ
            };

            parent.add(containerGroup);
            containerGroup.add(containerLoad);

            var nextColorScheme = colorScheme.getColorScheme(containerStackable);
            for (let s of containerStackable.stack.placements) {
                this.add(containerLoad, nextColorScheme, s, offsetX, offsetY, offsetZ);
            }

            return containerGroup;
        } else if(stackable instanceof Box) {
            var boxStackable : Box = stackable;

            console.log("Add box " + boxStackable.name + " size " + boxStackable.dx + "x" + boxStackable.dy + "x" + boxStackable.dz + " at " + stackPlacement.x + "x" + stackPlacement.y + "x" + stackPlacement.z);

            var sColor = colorScheme.getStackable(boxStackable);

            var material = new THREE.MeshStandardMaterial({
                color: sColor,
                opacity: 0.7,
                metalness: 0.2,
                roughness: 1,
                transparent: true,
                polygonOffset: true,
                polygonOffsetFactor: 1,
                polygonOffsetUnits: 1
            });
            material.color.convertSRGBToLinear();
            var geometry = new THREE.BoxGeometry(boxStackable.dy, boxStackable.dz, boxStackable.dx);
            var box = new THREE.Mesh(geometry, material);

            box.name = boxStackable.name;

            box.position.x = stackPlacement.y + boxStackable.dy / 2 + x;
            box.position.y = stackPlacement.z + boxStackable.dz / 2 + y;
            box.position.z = stackPlacement.x + boxStackable.dx / 2 + z;

            // Store metadata for picking
            box.userData = {
                step: boxStackable.step,
                type: "box",
                source: stackPlacement,
                box: {
                    id: boxStackable.id,
                    name: boxStackable.name,
                    dimensions: {
                        dx: boxStackable.dx,
                        dy: boxStackable.dy,
                        dz: boxStackable.dz
                    },
                    location: {
                        x: stackPlacement.x,
                        y: stackPlacement.y,
                        z: stackPlacement.z
                    },
                    step: boxStackable.step
                }
            };
    
            if(boxStackable.name) {
                const yLabelTextGeometry = new TextGeometry( boxStackable.name, {
                font: font,
                size: 1,
                depth: 0,
                curveSegments: 1,
                bevelEnabled: true,
                bevelThickness: 0,
                bevelSize: 0,
                bevelOffset: 0,
                bevelSegments: 1
                } );
    
                const yLabelMesh = new THREE.Mesh( yLabelTextGeometry, textMaterial );
                yLabelMesh.rotation.x = -Math.PI / 2;
                yLabelMesh.rotation.z = -Math.PI / 2;
                yLabelMesh.position.set( -yLabelMesh.scale.x / 2, 0, -yLabelMesh.scale.y / 2);
                box.add( yLabelMesh );
            }

            parent.add(box);

            return box;
        }
        return undefined;
    }
    
    removePoints(container: Object3D) {
		  var children = container.children;
	      for(var j = 0; j < children.length; j++) {
	        var userData = children[j].userData;
	        
	        if(userData.type == "containerLoad") {
				var containerLoad = children[j];
				var containerLoadChildren = containerLoad.children;
				
				for (var i = containerLoadChildren.length - 1; i >= 0; i--) {
					var child = containerLoadChildren[i];
					var userData = child.userData;
					
	    		    if(userData.type == "point") {
						containerLoad.remove(child);
					}
				}
			}
	      }
	}
	
	addPoints(container: Object3D, colorScheme : ColorScheme, stepNumber: number, pointNumber: number) {
		
		var children = container.children;
	      for(var j = 0; j < children.length; j++) {
	        var userData = children[j].userData;
	        
	        if(userData.type == "containerLoad") {
				var containerLoad = children[j];
				var containerLoadChildren = containerLoad.children;
				
				for(var i = 0; i < containerLoadChildren.length; i++) { 
					var child = containerLoadChildren[i];

					var containerLoadChildUserData = child.userData;
					
	    		    if(containerLoadChildUserData.type == "box" && containerLoadChildUserData.step == stepNumber - 1) {


                        for (var k = 0; k < containerLoadChildUserData.source.points.length; k++) {
                            var p = containerLoadChildUserData.source.points[k]
                            if(pointNumber == -1 || pointNumber == k) {
                                
                                var color = colorScheme.getPoint(p);
                                
                                var pointMaterial = new THREE.LineBasicMaterial({ color: color});
                                pointMaterial.color.convertSRGBToLinear();
                                var containerGeometry = new THREE.EdgesGeometry(new THREE.BoxGeometry(p.dy, p.dz, p.dx));
                                var pp = new THREE.LineSegments(containerGeometry, pointMaterial);
                
                                pp.position.x = p.y + p.dy / 2 + userData.offsetX;
                                pp.position.y = p.z + p.dz / 2 + userData.offsetY;
                                pp.position.z = p.x + p.dx / 2 + userData.offsetZ;
                
                                pp.userData = {
                                    type: "point"
                                };
                                
                                pp.visible = true;
                    
                                containerLoad.add(pp);
                            }
			            }
						break;
					}
				}
			}
	      }

	}
}    

