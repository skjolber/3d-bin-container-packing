import * as THREE from "three";
import React, { Component } from "react";
import { Stats } from "stats-js";
import { Color } from "three";
import { TextGeometry } from 'three/examples/jsm/geometries/TextGeometry';
import { MemoryColorScheme, RandomColorScheme, StackableRenderer } from "./api";
import { StackPlacement, getLayoutExtent, getLayoutKey, getLayoutPositions, parsePackagings } from "./model";
import { http, computeLoads } from "./utils";
import { Font } from 'three/examples/jsm/loaders/FontLoader';
import SupportingPlacementsView from "./SupportingPlacementsView";
import ResultSummaryView from "./ResultSummaryView";
import { COLOR_MODES, ColorMode, getColor } from "./colorModes";

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
var mainGroup;
var boxesGroup;
var decorationsGroup; // grid, axes and labels, rebuilt on reload
var controls;
var delta = 0;
var visibleContainers;
var shouldAnimate = false

const pointer = new THREE.Vector2();
var raycaster;
var INTERSECTED;
var stepNumber = -1;
var pointNumber = -1;


var maxPointNumbers;
var maxStepNumber = 0;
var minStepNumber = 0;
var cameraInitialized = false;
var lastLayoutKey = null; // the containers' sizes when the camera was last fitted

var points = false;

var stackableRenderer = new StackableRenderer();
var memoryScheme = new MemoryColorScheme(new RandomColorScheme());
var groupColors = new Map(); // colours by group id, for the group colour mode

var gridXZ;

const font = new Font( helvetiker );

/**
 * Example temnplate of using Three with React
 */
class ThreeScene extends Component {
  constructor(props) {
    super(props);
    this.state = { useWireFrame: false, selectedBox: null, hoveredData: null, packaging: null, packagings: [], resultIndex: 0, colorMode: ColorMode.BOX_ITEM };
    visibleContainers = new Array();
    // Raw mouse position in client coordinates (updated on every mousemove)
    this.mouseX = 0;
    this.mouseY = 0;
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

  // Helper function to fit camera to an object
  fitCameraToObject = (camera, controls, object, offset = 1.0) => {
    this.fitCameraToBox(camera, controls, new THREE.Box3().setFromObject(object), offset);
  };

  fitCameraToBox = (camera, controls, box, offset = 1.0) => {
    const size = box.getSize(new THREE.Vector3());
    const center = box.getCenter(new THREE.Vector3());

    const maxDim = Math.max(size.x, size.y, size.z);
    const fov = camera.fov * (Math.PI / 180);
    let cameraZ = Math.abs(maxDim / 2 / Math.tan(fov / 2));
    cameraZ *= offset;

    camera.position.set(center.x - cameraZ, center.y + cameraZ * 0.6, center.z - cameraZ);
    camera.lookAt(center);
    controls.target.copy(center);
    controls.update();
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
    document.addEventListener("click", this.onDocumentClick, false);
    document.addEventListener("contextmenu", this.onDocumentRightClick, false);

    //--------START ANIMATION-----------
    this.renderScene();
    this.start();
  }

  handleIntersection = () => {
    raycaster.setFromCamera( pointer, camera );

    // Collect all intersections across every container, then sort by distance
    // so we can find the closest one.  We cannot simply take the last hit from
    // the loop because a farther invisible box would overwrite a closer visible
    // one, causing the post-loop visibility check to discard the valid hit.
    var allIntersects = [];
    for(var i = 0; i < visibleContainers.length; i++) {
      for(var k = 0; k < visibleContainers[i].children.length; k++) {
        var hits = raycaster.intersectObjects(visibleContainers[i].children[k].children);
        for(var j = 0; j < hits.length; j++) {
          allIntersects.push(hits[j]);
        }
      }
    }
    allIntersects.sort((a, b) => a.distance - b.distance);

    // Three.js v0.180 raycaster does not check object.visible, so walk the
    // ancestor chain and pick the closest hit whose full chain is visible.
    var target = null;
    for(var ii = 0; ii < allIntersects.length; ii++) {
      var candidate = allIntersects[ii].object;
      if (candidate.userData && (candidate.userData.type === "cog" || candidate.userData.type === "opening")) {
        continue;
      }
      if (candidate.userData && candidate.userData.type === "invalid") {
        // the red outline of an invalid box: hover the box
        candidate = candidate.parent;
      }
      var visible = true;
      var obj = candidate;
      while (obj) {
        if (!obj.visible) { visible = false; break; }
        obj = obj.parent;
      }
      if (visible) {
        target = candidate;
        break;
      }
    }

    if(target) {
      if ( INTERSECTED != target) {
        if ( INTERSECTED ) {
          INTERSECTED.material.emissive = new Color("#000000");

        }
        INTERSECTED = target
        INTERSECTED.myColor = INTERSECTED.material.color;
        INTERSECTED.material.emissive = new Color("#FF0000");

        // Update supporting-placements popup for box meshes
        if (INTERSECTED.userData && INTERSECTED.userData.type === "box") {
          this.updateHoveredBoxData(INTERSECTED);
        } else {
          this.setState({ hoveredData: null });
        }
      }
    } else {
      if ( INTERSECTED ) {
        INTERSECTED.material.emissive = new Color("#000000") ;
        INTERSECTED = null;
        this.setState({ hoveredData: null });
      }
    }
  };

  /**
   * Collect all box placements in the same container as the hovered mesh,
   * then push the data needed by SupportingPlacementsView into React state.
   */
  updateHoveredBoxData = (mesh) => {
    try {
      // Mesh hierarchy: box → containerLoad (LineSegments) → containerGroup (Group)
      const containerLoad = mesh.parent;
      const containerGroup = containerLoad && containerLoad.parent;
      const container = containerGroup && containerGroup.userData && containerGroup.userData.source;

      if (!container) {
        this.setState({ hoveredData: null });
        return;
      }

      // Collect all box meshes that live in the same containerLoad and are
      // currently visible (i.e. their step is within the current stepNumber).
      const allBoxPlacements = [];
      for (let i = 0; i < containerLoad.children.length; i++) {
        const child = containerLoad.children[i];
        if (child.userData && child.userData.type === "box" && child.visible) {
          const sp = child.userData.source; // StackPlacement
          // Convert the mesh color from linear to sRGB, blending in the emissive so
          // the popup color matches what Three.js actually renders (base + emissive, clamped).
          const base = child.material.color;
          const emissive = child.material.emissive;
          const blended = new THREE.Color(
            Math.min(1, base.r + emissive.r),
            Math.min(1, base.g + emissive.g),
            Math.min(1, base.b + emissive.b),
          );
          const colorHex = '#' + blended.convertLinearToSRGB().getHexString();
          allBoxPlacements.push({
            placement: sp,           // StackPlacement (has .x .y .z)
            stackable: sp.stackable, // Box (has .dx .dy .dz .name .id .step)
            color: colorHex,
            isHovered: child === mesh,
          });
        }
      }

      const loadInfos = computeLoads(allBoxPlacements);

      this.setState({
        hoveredData: {
          source: mesh.userData.source, // StackPlacement for the hovered box
          container: container,
          allBoxPlacements: allBoxPlacements,
          loadInfos: allBoxPlacements.map(bp => loadInfos.get(bp)),
          currentStep: stepNumber,
        },
      });
    } catch (e) {
      console.error("Error building hover data", e);
      this.setState({ hoveredData: null });
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

      // Refresh the supporting-placements popup to reflect the new set of
      // visible boxes.  If the currently hovered mesh is no longer effectively
      // visible (stepped past it), clear its highlight, release it, and hide the popup.
      if (INTERSECTED && INTERSECTED.userData && INTERSECTED.userData.type === "box") {
        var stillVisible = true;
        var obj = INTERSECTED;
        while (obj) {
          if (!obj.visible) { stillVisible = false; break; }
          obj = obj.parent;
        }
        if (stillVisible) {
          this.updateHoveredBoxData(INTERSECTED);
        } else {
          INTERSECTED.material.emissive = new Color("#000000");
          INTERSECTED = null;
          this.setState({ hoveredData: null });
        }
      }
  };

  addModels = () => {

    // parent group to hold models
    mainGroup = new THREE.Object3D();

    boxesGroup = new THREE.Group();
    mainGroup.add(boxesGroup);

    decorationsGroup = new THREE.Group();
    this.scene.add(decorationsGroup);

    this.scene.add(mainGroup);
    
    
    var latestData = null;
    const component = this;

    var load = function(json) {
      var data = JSON.stringify(json);
      if(latestData != null && data == latestData) {
        return;
      }
      latestData = data;

      var packagings = parsePackagings(json);
      console.log("Update model @ " + CONTAINERS + " — results: " + packagings.length);
      component.packagings = packagings;
      // keep showing the same result, if there still is one
      var resultIndex = Math.min(component.state.resultIndex, Math.max(0, packagings.length - 1));
      component.setState({ packagings, resultIndex });
      render(packagings[resultIndex]);
    };

    // show one result
    var render = function(parsed) {
      for(var i = 0; i < visibleContainers.length; i++) {
        boxesGroup.remove(visibleContainers[i]);
      }
      visibleContainers = [];
      decorationsGroup.clear();

      if(!parsed) {
        component.setState({ packaging: null });
        return;
      }
      component.setState({ packaging: parsed });
      maxPointNumbers = parsed.maxPointNumbers;
      maxStepNumber = parsed.maxStep + 1;
      minStepNumber = parsed.minStep;
      pointNumber = -1;
      stepNumber = maxStepNumber;

      var positions = getLayoutPositions(parsed.containers, GRID_SPACING);
      for(var i = 0; i < parsed.containers.length; i++) {
        var container = parsed.containers[i];
        var visibleContainer = stackableRenderer.add(boxesGroup, memoryScheme, new StackPlacement(container, 0, positions[i], 0, 0), 0, 0, 0);
        visibleContainers.push(visibleContainer);
      }

      // the grid, axes and camera cover the containers of every result in the file, so that switching results (R)
      // keeps the same reference
      var packagings = component.packagings && component.packagings.length > 0 ? component.packagings : [parsed];
      var extent = getLayoutExtent(packagings, GRID_SPACING);
      var maxX = extent.x;
      var maxY = extent.y;
      var maxZ = extent.z;

      // when the containers change (for example another scenario), fit the camera to them,
      // otherwise keep the camera where the user left it
      var layoutKey = getLayoutKey(packagings);
      if (!cameraInitialized) {
        camera.position.z = maxY * 2;
        camera.position.y = maxZ * 1.25;
        camera.position.x = maxX * 2;
        cameraInitialized = true;
      } else if (layoutKey !== lastLayoutKey) {
        // three.js x, y and z are the container y, z and x axes
        component.fitCameraToBox(camera, controls, new THREE.Box3(new THREE.Vector3(0, 0, 0), new THREE.Vector3(maxY, maxZ, maxX)), 1.5);
      }
      lastLayoutKey = layoutKey;
      
	  // Add grid corresponding to containers
      var size = Math.max(maxY, maxX) + GRID_SPACING + GRID_SPACING + GRID_SPACING;
      let gridXZ = new THREE.GridHelper(
		      size,
		      size / GRID_SPACING,
		      0x42a5f5, // center line color
		      0x42a5f5 // grid color,
	    );
       decorationsGroup.add(gridXZ);
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
      decorationsGroup.add( yAxis );

      const xAxis = new THREE.ArrowHelper( new THREE.Vector3( 0, 0, 1 ), origin, maxX + GRID_SPACING, hex, 1, 1);
      decorationsGroup.add( xAxis );

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
      decorationsGroup.add( yLabelMesh );

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
      decorationsGroup.add( xLabelMesh );

      component.applyColorMode(component.state.colorMode);
    };

    this.showResult = (index) => {
      if(this.packagings && this.packagings.length > 0) {
        this.setState({ resultIndex: index });
        render(this.packagings[index]);
      }
    };

    http(
      "/assets/containers.json"
    ).then(load).catch((err) => { console.warn("Failed to load containers data:", err.message); });

    setInterval(function(){ 
      http(
        "/assets/containers.json"
      ).then(load).catch((err) => { console.warn("Failed to load containers data:", err.message); });
    }, 500);
  };

  /**
   * Colour the boxes for a colour mode (see colorModes.ts).
   */
  applyColorMode = (colorMode) => {
    if (!visibleContainers) return;
    for (const visibleContainer of visibleContainers) {
      visibleContainer.traverse(obj => {
        if (!obj.userData || obj.userData.type !== "box") return;
        const color = getColor(colorMode, obj.userData.source, groupColors, randomColor);
        if (color === undefined) {
          obj.material.color.copy(obj.userData.baseColor);
        } else {
          obj.material.color.set(color);
          obj.material.color.convertSRGBToLinear();
        }
      });
    }
    this.renderScene();
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
    document.removeEventListener("click", this.onDocumentClick, false);
    document.removeEventListener("contextmenu", this.onDocumentRightClick, false);
    

    this.mount.removeChild(this.renderer.domElement);
  }

  onDocumentClick = event => {
    if (!this.mount || !boxesGroup) return;

    const rect = this.mount.getBoundingClientRect();
    pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
    pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;

    raycaster.setFromCamera(pointer, camera);
    const intersects = raycaster.intersectObjects(boxesGroup.children, true);
    if (intersects.length === 0) {
      this.setState({ selectedBox: null });
      return;
    }
    for(let i = 0; i < intersects.length; i++) {
      const mesh = intersects[i].object;
      const box = mesh.userData.box;
      if (!box) {
        continue;
      }
      if(stepNumber > mesh.userData.step) {
        this.setState({ selectedBox: box });
        return
      }
    }
     this.setState({ selectedBox: null });
  };

  onDocumentRightClick = event => {
      console.log("Right-click detected!");
      //event.preventDefault(); // Prevents the default context menu from appearing
      this.fitCameraToObject(camera, controls, mainGroup, 1.5);
  };

  onWindowResize = () => {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();

    this.renderer.setSize(window.innerWidth, window.innerHeight);
  };

  onDocumentMouseMove = event => {
    event.preventDefault();

    if (event && typeof event !== undefined) {
      this.mouseX = event.clientX;
      this.mouseY = event.clientY;
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
      case 32: {
        this.fitCameraToObject(camera, controls, mainGroup, 1.5);
        break
      }
      case 82: {
        // R: next result
        if (this.packagings && this.packagings.length > 1) {
          this.showResult((this.state.resultIndex + 1) % this.packagings.length);
        }
        break;
      }
      case 67: {
        // C: next colour mode
        const colorMode = COLOR_MODES[(COLOR_MODES.indexOf(this.state.colorMode) + 1) % COLOR_MODES.length];
        this.setState({ colorMode });
        this.applyColorMode(colorMode);
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

    const { selectedBox, hoveredData, packaging } = this.state;

    return (
      <div>
        <div
          style={{ width: window.innerWidth, height: window.innerHeight }}
          ref={mount => {
            this.mount = mount;
          }}
        />
      {/* Box info panel */}
      <div
        style={{
          position: "absolute",
          zIndex: 2,
          top: 0,
          right: 0,
          padding: "8px",
          maxWidth: "260px",
          background: "rgba(0, 0, 0, 0.5)",
          color: "#fff",
          textAlign: "left"
        }}
      >
          {selectedBox && (
            <div>
              {selectedBox.id && <div><b>{selectedBox.id}</b></div>}
              {selectedBox.name && <div>{selectedBox.name}</div>}
              <div>
                {selectedBox.location.x} × {selectedBox.location.y} × {selectedBox.location.z}
              </div>
              <div>
                {selectedBox.dimensions.dx} × {selectedBox.dimensions.dy} × {selectedBox.dimensions.dz}
              </div>
              <div>Step #{selectedBox.step}</div>
                {selectedBox.weight > 0 && <div>Weight: {selectedBox.weight}</div>}
                {selectedBox.maxLoadWeight != null && <div>Max load weight: {selectedBox.maxLoadWeight}</div>}
                {selectedBox.maxLoadPressure != null && <div>Max pressure: {selectedBox.maxLoadPressure}</div>}
                {selectedBox.maxLoadBoxCount != null && <div>Max stack count: {selectedBox.maxLoadBoxCount}</div>}
                {selectedBox.maxLoadIdenticalOnly === true && <div>Identical only</div>}
                <div>Supported: {selectedBox.supportedPercent} %</div>
                {selectedBox.loadWeight > 0 && <div>Load weight: {Math.round(selectedBox.loadWeight * 100) / 100}</div>}
                {selectedBox.reasons && selectedBox.reasons.map((reason, i) => (
                  <div key={i} style={{ color: "#ef5350" }}>{reason}</div>
                ))}
              </div>
            )}
        </div>
      {/* Result summary panel */}
      <ResultSummaryView packaging={packaging} packagings={this.state.packagings} resultIndex={this.state.resultIndex} colorMode={this.state.colorMode} onSelectResult={this.showResult} />
      {/* Supporting placements popup — shown in a separate floating window on hover */}
      <SupportingPlacementsView
        hoveredData={hoveredData}
      />
      </div>
    );
  }
}

export default ThreeScene;
