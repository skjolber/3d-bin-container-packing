import {Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {PackModel} from './model/pack.model';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  private packModel: PackModel;

  @ViewChild('myCanvas') canvasRef: ElementRef;
  ctx: CanvasRenderingContext2D;

  constructor() {
  }

  fileUpload(input: HTMLInputElement) {
    const files = input.files;
    if (files && files.length) {
      // We only support one file at the time (for now, at least)
      const fileToRead = files[0];

      const fileReader = new FileReader();

      const self = this;

      // When the file is loaded call the event handler
      // fileReader.onload = this.onFileLoad;
      fileReader.onload = function (fileLoadedEvent) {
        const target: FileReader = <FileReader> fileLoadedEvent.target;
        const textFromFileLoaded = target.result as string;
        self.packModel = JSON.parse(textFromFileLoaded);

        self.drawPackedContainer(self.packModel);
      };

      fileReader.readAsText(fileToRead, 'UTF-8');
    }
  }

  drawPackedContainer(packModel: PackModel) {
    this.ctx.canvas.width = this.packModel.width;
    this.ctx.canvas.height = this.packModel.length;
    this.ctx.clearRect(0, 0, this.ctx.canvas.width, this.ctx.canvas.height);
    const placementArray = packModel.placement;
    const placementSize = placementArray.length;

    for (let i = 0; i < placementSize; i++) {
      const placement = placementArray[i];

      this.ctx.lineWidth = 4;
      this.ctx.strokeStyle = this.generateRandomColor();
      this.ctx.fillStyle = this.generateRandomColor();

      this.drawBoxOnCanvas(this.ctx, placement.x, placement.y, placement.width, placement.length);
      this.ctx.font = '10px Georgia';
      this.ctx.textAlign = 'center';
      this.ctx.textBaseline = 'middle';
      this.ctx.fillStyle = this.generateRandomColor();
      this.ctx.fillText(placement.name, placement.x + (placement.width / 2), placement.y + (placement.length / 2));
    }
  }

  // Source: http://jsfiddle.net/vu7dZ/4/
  private drawBoxOnCanvas(ctx, x, y, width, height): void {
    const radius = 0;
    ctx.beginPath();
    ctx.moveTo(x + radius, y);
    ctx.lineTo(x + width - radius, y);
    ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
    ctx.lineTo(x + width, y + height - radius);
    ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
    ctx.lineTo(x + radius, y + height);
    ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
    ctx.lineTo(x, y + radius);
    ctx.quadraticCurveTo(x, y, x + radius, y);
    ctx.closePath();
    // ctx.stroke();
    ctx.fill();
  }

  private generateRandomColor() {
    const letters = '0123456789ABCDEF';
    let color = '#';
    for (let i = 0; i < 6; i++) {
      color += letters[Math.floor(Math.random() * 16)];
    }
    return color;
  }

  ngOnInit() {
    this.ctx = this.canvasRef.nativeElement.getContext('2d');
  }
}
