import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppComponent } from './app.component';
import { Visualisation2dComponent } from './2d/visualisation/visualisation.2d.component';
import { HeaderComponent } from './header/header.component';
import {AppRoutingModule} from './app.routing.module';
import {Visualisation3dComponent} from './3d/visualisation/visualisation.3d.component';

@NgModule({
  declarations: [
    AppComponent,
    Visualisation2dComponent,
    Visualisation3dComponent,
    HeaderComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
