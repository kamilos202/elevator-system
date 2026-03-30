import { Component } from '@angular/core';
import { BuildingComponent } from './components/building/building.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [BuildingComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
}
