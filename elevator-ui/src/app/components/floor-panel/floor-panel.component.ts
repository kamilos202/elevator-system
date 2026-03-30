import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ElevatorStatus } from '../../services/elevator.service';

@Component({
  selector: 'app-floor-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './floor-panel.component.html',
  styleUrls: ['./floor-panel.component.scss']
})
export class FloorPanelComponent {
  @Input() floor: number = 0;
  @Input() numberOfFloors: number = 10;
  @Input() canCallUp: boolean = true;
  @Input() canCallDown: boolean = true;
  @Input() isCallUpActive: boolean = false;
  @Input() isCallDownActive: boolean = false;
  @Input() elevatorsAtFloor: ElevatorStatus[] = [];

  @Output() callUp = new EventEmitter<void>();
  @Output() callDown = new EventEmitter<void>();

  getFloorLabel(): string {
    return this.floor === 0 ? 'Ground' : `Floor ${this.floor}`;
  }

  getElevatorSummary(): string {
    if (!this.elevatorsAtFloor || this.elevatorsAtFloor.length === 0) return '';
    const ids = this.elevatorsAtFloor.map(e => `E${e.id + 1}`).join(', ');
    return `${ids} here`;
  }

  hasElevator(): boolean {
    return this.elevatorsAtFloor && this.elevatorsAtFloor.length > 0;
  }
}
