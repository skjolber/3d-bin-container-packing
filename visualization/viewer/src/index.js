import React from "react";
import ReactDOM from "react-dom";
import ThreeScene from "./ThreeScene";
import "./styles.css";

function App() {
  return (
    <div className="App">
      <ThreeScene />
    </div>
  );
}

const rootElement = document.getElementById("root");
ReactDOM.render(<App />, rootElement);
