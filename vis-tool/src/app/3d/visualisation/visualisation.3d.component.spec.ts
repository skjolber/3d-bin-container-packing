import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { Visualisation3dComponent } from './visualisation.3d.component';

describe('Visualisation2dComponent', () => {
  let component: Visualisation3dComponent;
  let fixture: ComponentFixture<Visualisation3dComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ Visualisation3dComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(Visualisation3dComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
