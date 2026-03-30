import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ElevatorService, SystemStatus, SystemConfig } from '../../services/elevator.service';
import { WebSocketService } from '../../services/websocket.service';
import { FloorsComponent } from '../floors/floors.component';
import { ElevatorShaftComponent } from '../elevator-shaft/elevator-shaft.component';
import { StatusPanelComponent } from '../status-panel/status-panel.component';

@Component({
  selector: 'app-building',
  standalone: true,
  imports: [CommonModule, FloorsComponent, ElevatorShaftComponent, StatusPanelComponent],
  templateUrl: './building.component.html',
  styleUrls: ['./building.component.scss']
})
export class BuildingComponent implements OnInit, OnDestroy {
  private elevatorService = inject(ElevatorService);
  private webSocketService = inject(WebSocketService);
  private cdr = inject(ChangeDetectorRef);   // <-- NEW

  systemStatus: SystemStatus | null = null;
  systemConfig: SystemConfig | null = null;
  loading = true;
  error: string | null = null;
  isConnected = false;

  ngOnInit(): void {
    this.initializeStatus();
    this.loading = false;

    this.webSocketService.connectionStatus$.subscribe((connected: boolean) => {
      this.isConnected = connected;
      this.cdr.detectChanges();
    });

    // HTTP polling: full system snapshot every 500 ms
    this.elevatorService.status$.subscribe({
      next: (status: SystemStatus | null) => {
        if (status) {
          this.systemStatus = status;
          this.systemConfig = status.config;
          this.cdr.detectChanges();   // <-- zoneless needs explicit notification
        }
      },
      error: (err: any) => console.warn('HTTP polling unavailable:', err)
    });

    // WebSocket: real-time elevator position updates
    this.webSocketService.events$.subscribe({
      next: (event: any) => {
        if (event && event.elevatorStatus && this.systemStatus) {
          const idx = this.systemStatus.elevators.findIndex(e => e.id === event.elevatorStatus.id);
          if (idx >= 0) {
            // Spread BOTH the outer object AND the array — a shallow spread would keep
            // the same array reference and child components wouldn't see new input.
            const updatedElevators = [...this.systemStatus.elevators];
            updatedElevators[idx] = event.elevatorStatus;
            this.systemStatus = { ...this.systemStatus, elevators: updatedElevators };
            this.cdr.detectChanges();   // <-- NEW
          }
        }
      }
    });
  }

  private initializeStatus(): void {
    this.systemStatus = {
      elevators: [
        {id: 0, currentFloor: 0, direction: 'Idle', doorState: 'Closed', requestedFloors: [], passengerCount: 0, capacity: 10},
        {id: 1, currentFloor: 0, direction: 'Idle', doorState: 'Closed', requestedFloors: [], passengerCount: 0, capacity: 10},
        {id: 2, currentFloor: 0, direction: 'Idle', doorState: 'Closed', requestedFloors: [], passengerCount: 0, capacity: 10}
      ],
      floors: this.createDefaultFloors(10),
      config: {
        numberOfElevators: 3,
        numberOfFloors: 10,
        elevatorCapacity: 10,
        floorTransitionTimeMs: 1000,
        doorOpenCloseTimeMs: 2000,
        doorStayOpenTimeMs: 3000,
        elevatorTickIntervalMs: 100
      }
    };
    this.systemConfig = this.systemStatus.config;
  }

  private createDefaultFloors(numberOfFloors: number): { [key: number]: any } {
    const floors: { [key: number]: any } = {};
    for (let i = 0; i < numberOfFloors; i++) {
      floors[i] = { floorNumber: i, callUpButton: false, callDownButton: false, waitingPassengers: 0 };
    }
    return floors;
  }

  ngOnDestroy(): void {
    this.webSocketService.disconnect();
  }

  getFloorsArray(): number[] {
    if (!this.systemConfig) return [];
    return Array.from({ length: this.systemConfig.numberOfFloors }, (_, i) => i);
  }
}
