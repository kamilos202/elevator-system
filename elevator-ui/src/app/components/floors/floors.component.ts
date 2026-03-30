import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ElevatorService, ElevatorStatus } from '../../services/elevator.service';
import { FloorPanelComponent } from '../floor-panel/floor-panel.component';

@Component({
  selector: 'app-floors',
  standalone: true,
  imports: [CommonModule, FloorPanelComponent],
  templateUrl: './floors.component.html',
  styleUrls: ['./floors.component.scss']
})
export class FloorsComponent {
  private elevatorService = inject(ElevatorService);

  @Input() floors: { [key: number]: any } = {};
  @Input() elevators: ElevatorStatus[] = [];
  @Input() numberOfFloors: number = 10;
  @Input() numberOfElevators: number = 3;

  /** Tracks which call buttons are in "waiting" (lit) state */
  private callActive: { [key: string]: boolean } = {};

  getFloorsReversed(): number[] {
    return Array.from({ length: this.numberOfFloors }, (_, i) => this.numberOfFloors - 1 - i);
  }

  callElevator(floor: number, direction: 'UP' | 'DOWN'): void {
    const key = `${floor}-${direction}`;
    this.callActive[key] = true;

    this.elevatorService.callElevator(floor, direction).subscribe({
      next: () => {
        console.log(`Elevator called to floor ${floor} direction ${direction}`);
        // Keep button lit until elevator arrives (cleared by WebSocket update / polling)
        // Auto-reset after 30s as fallback
        setTimeout(() => { delete this.callActive[key]; }, 30000);
      },
      error: (err) => {
        console.error('Error calling elevator:', err);
        delete this.callActive[key];
      }
    });
  }

  /** Called when WebSocket update shows elevator at this floor (call satisfied) */
  clearCallActive(floor: number, direction: 'UP' | 'DOWN'): void {
    delete this.callActive[`${floor}-${direction}`];
  }

  isCallButtonActive(floor: number, direction: 'UP' | 'DOWN'): boolean {
    return this.callActive[`${floor}-${direction}`] ?? false;
  }

  canCallUp(floor: number): boolean {
    return floor < this.numberOfFloors - 1;
  }

  canCallDown(floor: number): boolean {
    return floor > 0;
  }

  getElevatorsAtFloor(floor: number): ElevatorStatus[] {
    return this.elevators.filter(e => e.currentFloor === floor);
  }
}
