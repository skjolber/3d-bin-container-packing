import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { Visualisation2dComponent } from './visualisation.2d.component';

describe('Visualisation2dComponent', () => {
  let component: Visualisation2dComponent;
  let fixture: ComponentFixture<Visualisation2dComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ Visualisation2dComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(Visualisation2dComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
