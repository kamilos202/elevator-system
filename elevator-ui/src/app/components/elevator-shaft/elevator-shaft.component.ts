import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ElevatorService, ElevatorStatus } from '../../services/elevator.service';
import { ElevatorCarComponent } from '../elevator-car/elevator-car.component';

@Component({
  selector: 'app-elevator-shaft',
  standalone: true,
  imports: [CommonModule, ElevatorCarComponent],
  templateUrl: './elevator-shaft.component.html',
  styleUrls: ['./elevator-shaft.component.scss']
})
export class ElevatorShaftComponent {
  private elevatorService = inject(ElevatorService);

  @Input() elevators: ElevatorStatus[] = [];
  @Input() numberOfFloors: number = 10;
  @Input() floors: number[] = [];

  /** Returns floors top-to-bottom for visual rendering: [9,8,...,0] */
  getFloorsReversed(): number[] {
    return Array.from({ length: this.numberOfFloors }, (_, i) => this.numberOfFloors - 1 - i);
  }

  /** Returns all floor numbers bottom-to-top: [0,1,...,9] */
  getFloorNumbers(): number[] {
    return Array.from({ length: this.numberOfFloors }, (_, i) => i);
  }

  /**
   * Returns the bottom CSS percentage for the elevator car.
   * Floor 0 -> bottom: 0%, Floor 9 (of 10) -> bottom: 90%
   */
  getElevatorPosition(currentFloor: number): string {
    const percentage = (currentFloor / this.numberOfFloors) * 100;
    return `${percentage}%`;
  }

  getFloorLabel(floor: number): string {
    return floor === 0 ? 'G' : String(floor);
  }

  isFloorPending(elevator: ElevatorStatus, floor: number): boolean {
    return elevator.requestedFloors?.includes(floor) ?? false;
  }

  onSelectFloor(elevatorId: number, floor: number): void {
    this.elevatorService.selectFloor(elevatorId, floor).subscribe({
      next: () => console.log(`Floor ${floor} selected in elevator ${elevatorId + 1}`),
      error: (err: any) => console.error('Error selecting floor:', err)
    });
  }
}
