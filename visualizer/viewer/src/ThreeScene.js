import * as THREE from "three";
import React, { Component } from "react";
import { Stats } from "stats-js";
import { Color } from "three";
import { TextGeometry } from 'three/examples/jsm/geometries/TextGeometry';
import { MemoryColorScheme, RandomColorScheme, StackPlacement, Box, Container, Point, StackableRenderer } from "./api";
import { http } from "./utils";
import { Font } from 'three/examples/jsm/loaders/FontLoader';

import randomColor from "randomcolor";
import { thisExpression } from "@babel/types";

import { OrbitControls } from "three/examples/jsm/controls/OrbitControls"

const helvetiker = require( 'three/examples/fonts/droid/droid_sans_mono_regular.typeface.json');

const CONTAINERS = "./assets/containers.json";

//Textures
const ANGULAR_VELOCITY = 0.01;

const GRID_SPACING = 10;

var camera;
var orbit; // light orbit
var helvatiker;
var mainGroup;
var shouldAnimate;
var controls;
var delta = 0;
var visibleContainers;


const pointer = new THREE.Vector2();
var raycaster;
var INTERSECTED;
var stepNumber = -1;
var pointNumber = -1;


var maxPointNumbers;
var maxStepNumber = 0;
var minStepNumber = 0;

var points = false;

var stackableRenderer = new StackableRenderer();
var memoryScheme = new MemoryColorScheme(new RandomColorScheme());

var gridXZ;

const font = new Font( helvetiker );

/**
 * Example temnplate of using Three with React
 */
class ThreeScene extends Component {
  constructor(props) {
    super(props);
    this.state = { useWireFrame: false };
    visibleContainers = new Array();
  }

  animate = () => {
    //update Orbit Of Camera
    controls.update();

    //Animate rotation of light
    if (orbit) orbit.rotation.z += ANGULAR_VELOCITY;

    // Update Uniform of shader
    delta += 0.01;
    //Direct manipulation
    //shaderMaterial.uniforms.delta.value = 0.5 + Math.sin(delta) * 0.0005;
    //shaderMesh.material.uniforms.u_time.value = delta;

    this.handleIntersection();

    //Redraw scene
    this.renderScene();
    this.frameId = window.requestAnimationFrame(this.animate);
  };

  componentDidMount() {
    //Add Light & nCamera
    this.addScene();

    // // Add Box Mesh with shader as texture
    this.addModels();

    // Add Events
    window.addEventListener("resize", this.onWindowResize, false);
    document.addEventListener("keyup", this.onDocumentKeyUp, false);
    document.addEventListener("keydown", this.onDocumentKeyDown, false);
    document.addEventListener("mousemove", this.onDocumentMouseMove, false);

    //--------START ANIMATION-----------
    this.renderScene();
    this.start();
  }

  handleIntersection = () => {
    raycaster.setFromCamera( pointer, camera );
    
    var target = null;
    for(var i = 0; i < visibleContainers.length; i++) {
      for(var k = 0; k < visibleContainers[i].children.length; k++) {
        var intersects = raycaster.intersectObjects(visibleContainers[i].children[k].children );
        if ( intersects.length > 0 ) {
          target = intersects[ 0 ].object;
        }
      }
    }

    if(target) {
      if ( INTERSECTED != target) {
        if ( INTERSECTED ) {
          INTERSECTED.material.emissive = new Color("#000000");

        }
        INTERSECTED = target
        INTERSECTED.myColor = INTERSECTED.material.color;
        INTERSECTED.material.emissive = new Color("#FF0000") 
      }
    } else {
      if ( INTERSECTED ) {
        INTERSECTED.material.emissive = new Color("#000000") ;
        INTERSECTED = null;
      }
    }
  };

  handleStepNumber = () => {
      console.log("Show step number " + stepNumber);
      
      for(var i = 0; i < visibleContainers.length; i++) {
        var visibleContainer = visibleContainers[i];
        
        var visibleContainerUserData = visibleContainer.userData;
        visibleContainer.visible = visibleContainerUserData.step < stepNumber;

		// adding alle the points is too expensive
		// so add for a single step at a time 
        stackableRenderer.removePoints(visibleContainer);
        if(points) {
        	stackableRenderer.addPoints(visibleContainer, memoryScheme, stepNumber, pointNumber);
        }
        
        for(var k = 0; k < visibleContainers[i].children.length; k++) {

          var container = visibleContainers[i].children[k];
          var containerUserData = container.userData;
          
          container.visible = containerUserData.step < stepNumber;
          
          var stackables = container.children;
          for(var j = 0; j < stackables.length; j++) {
            var stackable = stackables[j];
            var userData = stackables[j].userData;
            
            if(userData.type == "box") {
                stackable.visible = userData.step < stepNumber;
            }
          }
        }          
    }
  };

  addModels = () => {

    // parent group to hold models
    mainGroup = new THREE.Object3D();
    this.scene.add(mainGroup);
    
    let scene = this.scene;
    
    var latestData = null;

    var load = function(packaging) {

      var data = JSON.stringify(packaging);
      if(latestData != null && data == latestData) {
        return;
      }
      console.log("Update model");

      latestData = data;

      for(var i = 0; i < visibleContainers.length; i++) {
        mainGroup.remove(visibleContainers[i]);
      }

      var x = 0;

      var minStep = -1;
      var maxStep = -1;
      
      var maxX = 0;
      var maxY = 0;
      var maxZ = 0;

      maxPointNumbers = new Array();
  
      for(var i = 0; i < packaging.containers.length; i++) {
        var containerJson = packaging.containers[i];
  
        var container = new Container(containerJson.name, containerJson.id, containerJson.step, containerJson.dx, containerJson.dy, containerJson.dz, containerJson.loadDx, containerJson.loadDy, containerJson.loadDz);
    
        if(container.step < minStep || minStep == -1) {
          minStep = container.step;
        }

        if(container.step > maxStep || maxStep == -1) {
          maxStep = container.step;
        }

        for(var j = 0; j < containerJson.stack.placements.length; j++) {
          var placement = containerJson.stack.placements[j];
          var stackable = placement.stackable;

          if(stackable.step < minStep || minStep == -1) {
            minStep = stackable.step;
          }
  
          if(stackable.step > maxStep || maxStep == -1) {
            maxStep = stackable.step;
          }
          
          var points = new Array();
          
          for(var l = 0; l < placement.points.length; l++) {
                var point = placement.points[l];
                
                points.push(new Point(point.x, point.y, point.z, point.dx, point.dy, point.dz));
          }

          if(maxPointNumbers[stackable.step] == null || maxPointNumbers[stackable.step] < points.length) {
            maxPointNumbers[stackable.step] = points.length;
          }


          if(stackable.type == "box") {
            var box = new Box(stackable.name, stackable.id, stackable.step, stackable.dx, stackable.dy, stackable.dz);
            
            container.add(new StackPlacement(box, placement.step, placement.x, placement.y, placement.z, points));
          } else {
            // TODO
          }
        }

        console.log(maxPointNumbers)

        maxStepNumber = maxStep + 1;
        minStepNumber = minStep;
        pointNumber = -1;
        stepNumber = maxStepNumber;

        // TODO return controls instead
        var visibleContainer = stackableRenderer.add(mainGroup, memoryScheme, new StackPlacement(container, 0, x, 0, 0), 0, 0, 0);
        visibleContainers.push(visibleContainer);


        if(x + container.dx > maxX) {
          maxX = x + container.dx;
        }
        if(container.dy > maxY) {
          maxY = container.dy;
        }
        if(container.dz > maxZ) {
          maxZ = container.dz;
        }

        x += container.dx + GRID_SPACING;
        x = x - (x % GRID_SPACING);
      }
      
      camera.position.z = maxY * 2;
      camera.position.y = maxZ * 1.25;
      camera.position.x = maxX * 2;
      
	  // Add grid corresponding to containers
      var size = Math.max(maxY, maxX) + GRID_SPACING + GRID_SPACING + GRID_SPACING;
      let gridXZ = new THREE.GridHelper(
		      size,
		      size / GRID_SPACING,
		      0x42a5f5, // center line color
		      0x42a5f5 // grid color,
	    );
       scene.add(gridXZ);
       gridXZ.position.y = 0;
       gridXZ.position.x = size / 2 - GRID_SPACING;
       gridXZ.position.z = size / 2 - GRID_SPACING;

       const dir = new THREE.Vector3( 1, 2, 0 );

      //normalize the direction vector (convert to vector of length 1)
      dir.normalize();

      const origin = new THREE.Vector3( -GRID_SPACING - 1, 0, -GRID_SPACING - 1 );
      const length = maxY + GRID_SPACING;
      const hex = 0xffffff;
      const yAxis = new THREE.ArrowHelper( new THREE.Vector3( 1, 0, 0 ), origin, maxY + GRID_SPACING, hex, 1, 1);
      scene.add( yAxis );

      const xAxis = new THREE.ArrowHelper( new THREE.Vector3( 0, 0, 1 ), origin, maxX + GRID_SPACING, hex, 1, 1);
      scene.add( xAxis );

      const textMaterial = new THREE.MeshPhongMaterial( { color: 0xffffff } );

      const yLabelTextGeometry = new TextGeometry( 'Y', {
        font: font,
        size: GRID_SPACING / 2,
        depth: 0,
        curveSegments: 1,
        bevelEnabled: true,
        bevelThickness: 0,
        bevelSize: 0,
        bevelOffset: 0,
        bevelSegments: 1
      } );

      const yLabelMesh = new THREE.Mesh( yLabelTextGeometry, textMaterial );
      yLabelMesh.position.set( maxY - GRID_SPACING / 2, 0, -GRID_SPACING - GRID_SPACING / 4  );
      yLabelMesh.rotation.x = Math.PI / 2;
      yLabelMesh.rotation.z = -Math.PI / 2;
      scene.add( yLabelMesh );

      const xLabelTextGeometry = new TextGeometry( 'X', {
        font: font,
        size: GRID_SPACING / 2,
        depth: 0,
        curveSegments: 1,
        bevelEnabled: true,
        bevelThickness: 0,
        bevelSize: 0,
        bevelOffset: 0,
        bevelSegments: 1
      } );

      const xLabelMesh = new THREE.Mesh( xLabelTextGeometry, textMaterial );
      xLabelMesh.position.set(-GRID_SPACING - GRID_SPACING / 2 - GRID_SPACING / 4, 0, maxX - GRID_SPACING / 2);
      xLabelMesh.rotation.x = Math.PI / 2;
      scene.add( xLabelMesh );
    };

    http(
      "/assets/containers.json"
    ).then(load);

    setInterval(function(){ 
      http(
        "/assets/containers.json"
      ).then(load);
    }, 500);
  };

  start = () => {
    if (!this.frameId) {
      this.frameId = requestAnimationFrame(this.animate);
    }
  };
  stop = () => {
    cancelAnimationFrame(this.frameId);
  };

  renderScene = () => {
    if (this.renderer) {
      this.renderer.render(this.scene, camera);
    }
  };

  componentWillUnmount() {
    this.stop();

    document.removeEventListener("mousemove", this.onDocumentMouseMove, false);
    window.removeEventListener("resize", this.onWindowResize, false);
    document.removeEventListener("keydown", this.onDocumentKeyDown, false);
    document.removeEventListener("keyup", this.onDocumentKeyUp, false);
    this.mount.removeChild(this.renderer.domElement);
  }

  onWindowResize = () => {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();

    this.renderer.setSize(window.innerWidth, window.innerHeight);
  };

  onDocumentMouseMove = event => {
    event.preventDefault();

    if (event && typeof event !== undefined) {
      pointer.x = ( event.clientX / window.innerWidth ) * 2 - 1;
      pointer.y = - ( event.clientY / window.innerHeight ) * 2 + 1;
    }
  };

  onDocumentKeyDown = event => {
    shouldAnimate = false;
    var keyCode = event.which;
    switch (keyCode) {
      case 49: {
        // shaderMesh1.rotation.x += ROTATION_ANGLE; //W
        mainGroup.rotation.y += 0.1;
        break;
      }
      case 50: {
        // shaderMesh1.rotation.x -= ROTATION_ANGLE; //S
        mainGroup.rotation.y -= 0.1;
        break;
      }
      case 65: {
        stepNumber++;
        if(stepNumber > maxStepNumber) {
          stepNumber = 0;
        }
        console.log("Shop step number " + stepNumber);
        this.handleStepNumber();

        pointNumber = -1;
        
        break;
      }
      case 68: {
        stepNumber--;
        if(stepNumber < minStepNumber) {
          stepNumber = maxStepNumber;
        }
        console.log("Shop step number " + stepNumber);
        this.handleStepNumber();
        
        break;
      }
      case 80: {
        points = !points;
        this.pointNumber = -1;
        if(points) {
          console.log("Show points");
        } else {
          console.log("Hide points");
        }
        
        this.handleStepNumber();
        this.renderScene();

        break;
      }
      case 87: {
        // 
        pointNumber++;
        if(pointNumber >= maxPointNumbers[stepNumber - 1]) {
          pointNumber = 0;
        }        
        console.log("Shop point number " + pointNumber + " of " + maxPointNumbers[stepNumber-1]);
        this.handleStepNumber();
        break;
      }
      case 83: {
        // 
        pointNumber--;
        if(pointNumber < 0) {
          pointNumber = maxPointNumbers[stepNumber - 1]-1;
        }
        console.log("Shop point number " + pointNumber + " of " + maxPointNumbers[stepNumber-1]);
        this.handleStepNumber();

        break;
      }
      
      default: {
        break;
      }
    }
  };
  onDocumentKeyUp = event => {
    var keyCode = event.which;
    shouldAnimate = true;
    console.log("onKey Up " + keyCode);
  };

  /**
   * Boilder plate to add LIGHTS, Renderer, Axis, Grid,
   */
  addScene = () => {
    const width = this.mount.clientWidth;
    const height = this.mount.clientHeight;
    this.scene = new THREE.Scene();

    // ------- Add RENDERED ------
    this.renderer = new THREE.WebGLRenderer({ antialias: true, powerPreference: "high-performance" });
    this.renderer.setClearColor("#263238");
    this.renderer.setSize(width, height);
    this.mount.appendChild(this.renderer.domElement);

    // -------Add CAMERA ------
    camera = new THREE.PerspectiveCamera(80, width / height, 0.1, 100000);
    camera.position.z = -50;
    camera.position.y = 50;
    camera.position.x = -50;
//    camera.lookAt(new THREE.Vector3(19000, 0, 0));

    //------Add ORBIT CONTROLS--------
    controls = new OrbitControls(camera, this.renderer.domElement);
    controls.enableDamping = true;
    controls.dampingFactor = 0.25;
    controls.enableZoom = true;
    controls.autoRotate = false;
    controls.keys = {
      LEFT: 37, //left arrow
      UP: 38, // up arrow
      RIGHT: 39, // right arrow
      BOTTOM: 40 // down arrow
    };

    controls.addEventListener("change", () => {
      if (this.renderer) this.renderer.render(this.scene, camera);
    });

    raycaster = new THREE.Raycaster();

    var ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
    
    this.scene.add(ambientLight);
  };
  //-------------HELPER------------------
  render() {
    return (
      <div
        style={{ width: window.innerWidth, height: window.innerHeight }}
        ref={mount => {
          this.mount = mount;
        }}
      />
    );
  }
}

export default ThreeScene;
