import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import {Visualisation2dComponent} from './2d/visualisation/visualisation.2d.component';
import {Visualisation3dComponent} from './3d/visualisation/visualisation.3d.component';


const routes: Routes = [
  {'path': '', redirectTo: '2d', pathMatch: 'full'},
  {path: '2d', component: Visualisation2dComponent},
  {path: '3d', component: Visualisation3dComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
