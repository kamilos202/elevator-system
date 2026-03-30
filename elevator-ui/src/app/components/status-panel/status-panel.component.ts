import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SystemStatus, SystemConfig, ElevatorStatus } from '../../services/elevator.service';

@Component({
  selector: 'app-status-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './status-panel.component.html',
  styleUrls: ['./status-panel.component.scss']
})
export class StatusPanelComponent {
  @Input() systemStatus: SystemStatus | null = null;
  @Input() systemConfig: SystemConfig | null = null;

  getActiveElevators(): number {
    if (!this.systemStatus) return 0;
    return this.systemStatus.elevators.filter(e => e.direction !== 'Idle').length;
  }

  getTotalPassengers(): number {
    if (!this.systemStatus) return 0;
    return this.systemStatus.elevators.reduce((sum, e) => sum + e.passengerCount, 0);
  }

  getTotalCapacity(): number {
    if (!this.systemStatus) return 0;
    return this.systemStatus.elevators.reduce((sum, e) => sum + e.capacity, 0);
  }

  getOccupancyPercentage(): number {
    const total = this.getTotalCapacity();
    if (total === 0) return 0;
    return Math.round((this.getTotalPassengers() / total) * 100);
  }

  getAllRequestedFloors(): number[] {
    if (!this.systemStatus) return [];
    const allFloors = new Set<number>();
    this.systemStatus.elevators.forEach(e => {
      e.requestedFloors.forEach(f => allFloors.add(f));
    });
    return Array.from(allFloors).sort((a, b) => a - b);
  }

  getElevatorsByStatus(): { [key: string]: ElevatorStatus[] } {
    if (!this.systemStatus) return {};

    const grouped: { [key: string]: ElevatorStatus[] } = {
      'Moving': [],
      'Idle': [],
      'At Floor': []
    };

    this.systemStatus.elevators.forEach(e => {
      if (e.direction === 'Idle' && e.doorState === 'Closed') {
        grouped['Idle'].push(e);
      } else if (e.doorState === 'Open' || e.doorState === 'Opening') {
        grouped['At Floor'].push(e);
      } else {
        grouped['Moving'].push(e);
      }
    });

    return grouped;
  }

  formatFloors(floors: number[]): string {
    return floors.map(f => f === 0 ? 'G' : String(f)).join(', ');
  }
}

