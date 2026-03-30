import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ElevatorStatus } from '../../services/elevator.service';

@Component({
  selector: 'app-elevator-car',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './elevator-car.component.html',
  styleUrls: ['./elevator-car.component.scss']
})
export class ElevatorCarComponent {
  @Input() elevator: ElevatorStatus | null = null;
  @Input() numberOfFloors: number = 10;
  @Input() position: string = '0%';

  getDirectionIcon(): string {
    if (!this.elevator) return '⏸';
    switch (this.elevator.direction) {
      case 'Up': return '▲';
      case 'Down': return '▼';
      default: return '●';
    }
  }
}
