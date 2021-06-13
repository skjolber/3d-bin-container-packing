import React, { Component } from "react";
import * as THREE from "three";
import OrbitControls from "three-orbitcontrols";
import { Stats } from "stats-js";
import { Color, Font } from "three";

import { MemoryColorScheme, RandomColorScheme, StackPlacement, Box, Container, StackableRenderer } from "./api";
import { http } from "./utils";

import randomColor from "randomcolor";

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



  addModels = () => {

    // parent group to hold models
    mainGroup = new THREE.Object3D();
    this.scene.add(mainGroup);
    
    var latestData = null;

    var memoryScheme = new MemoryColorScheme(new RandomColorScheme());

    var load = function(packaging) {

      var data = JSON.stringify(packaging);
      if(latestData != null && data == latestData) {
        console.log("Wait for data..");
        return;
      }
      console.log("Update model");

      latestData = data;

      for(var i = 0; i < visibleContainers.length; i++) {
        mainGroup.remove(visibleContainers[i]);
      }

      var stackableRenderer = new StackableRenderer();

      var x = 0;
  
      for(var i = 0; i < packaging.containers.length; i++) {
        var containerJson = packaging.containers[i];
  
        var container = new Container(containerJson.name, containerJson.id, containerJson.dx, containerJson.dy, containerJson.dz, containerJson.loadDx, containerJson.loadDy, containerJson.loadDz);
    
        for(var j = 0; j < containerJson.stack.placements.length; j++) {
          var placement = containerJson.stack.placements[j];
          var stackable = placement.stackable;
          
          if(stackable.type == "box") {
            var box = new Box(stackable.name, stackable.id, stackable.dx, stackable.dy, stackable.dz);
            container.add(new StackPlacement(box, placement.x, placement.y, placement.z));
          } else {
  
          }
        }
        var visibleContainer = stackableRenderer.add(mainGroup, memoryScheme, new StackPlacement(container, x, 0, 0), 0, 0, 0);
        visibleContainers.push(visibleContainer);

        x += container.dx + GRID_SPACING;
        x = x - (x % GRID_SPACING);
      }
    };

    http(
      "/assets/containers.json"
    ).then(load);

    setInterval(function(){ 
      http(
        "/assets/containers.json"
      ).then(load);
    }, 500); //check 5 seconds


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
    if (this.renderer) this.renderer.render(this.scene, camera);
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
    // https://stackoverflow.com/questions/44630265/how-can-i-set-z-up-coordinate-system-in-three-js
    
    this.renderer.setSize(window.innerWidth, window.innerHeight);

  };

  onDocumentMouseMove = event => {
    event.preventDefault();

    if (event && typeof event !== undefined) {
      // update normalized Uniform
      let x = (event.clientX / window.innerWidth) * 2 - 1;
      let y = -(event.clientY / window.innerHeight) * 2 + 1;

      // update Uniform directly
      //shaderMaterial1.uniforms.u_mouse.value = new THREE.Vector2(x, y);
    }
  };

  onDocumentKeyDown = event => {
    shouldAnimate = false;
    var keyCode = event.which;
    switch (keyCode) {
      case 87: {
        // shaderMesh1.rotation.x += ROTATION_ANGLE; //W
        console.log("OnKeyPress W");
        mainGroup.rotation.y += 0.1;
        break;
      }
      case 83: {
        // shaderMesh1.rotation.x -= ROTATION_ANGLE; //S
        console.log("OnKeyPress S");
        mainGroup.rotation.y -= 0.1;
        break;
      }
      case 65: {
        //shaderMesh1.rotation.y -= ROTATION_ANGLE; //A
        console.log("OnKeyPress A");
        break;
      }
      case 68: {
        //shaderMesh1.rotation.y -= ROTATION_ANGLE; //D
        console.log("OnKeyPress D");
        break;
      }
      case 32: {
        console.log("OnKeyPress SPACE");
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
    this.renderer = new THREE.WebGLRenderer({ antialias: true });
    this.renderer.setClearColor("#263238");
    this.renderer.setSize(width, height);
    this.mount.appendChild(this.renderer.domElement);

    

    // -------Add CAMERA ------
    camera = new THREE.PerspectiveCamera(80, width / height, 0.1, 100000);
    camera.position.z = -50;
    camera.position.y = 50;
    camera.position.x = -50;
    camera.lookAt(new THREE.Vector3(0, 0, 0));

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

    var ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
    var directionalLight1 = new THREE.DirectionalLight(0xFFFFFF, 0.6);
    var directionalLight2 = new THREE.DirectionalLight(0xFFFFFF, 0.8);
    var directionalLight3 = new THREE.DirectionalLight(0xFFFFFF, 0.9);

    // set directionalLights to random places
    directionalLight1.position.set(3, 4, 5);
    directionalLight2.position.set(-3, 4, -5);
    directionalLight3.position.set(2, 5, 4);

    // (0, 0, 0) to target directionalLights at
    var origin = new THREE.Object3D();

    // target directionalLights to origin
    directionalLight1.target = origin;
    directionalLight2.target = origin;
    directionalLight3.target = origin;

    this.scene.add(ambientLight);
    this.scene.add(directionalLight1);
    this.scene.add(directionalLight2);
    this.scene.add(directionalLight3);

    this.addHelper();
  };
  //-------------HELPER------------------
  addHelper = () => {
    // Add Grid
    let gridXZ = new THREE.GridHelper(
      GRID_SPACING * 10,
      10,
      0x18ffff, //center line color
      0x42a5f5 //grid color,
    );
    this.scene.add(gridXZ);
    gridXZ.position.y = 0;

  };
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