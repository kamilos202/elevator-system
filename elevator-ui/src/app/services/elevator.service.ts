import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {Observable, BehaviorSubject, interval, catchError, EMPTY} from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

export interface ElevatorStatus {
  id: number;
  currentFloor: number;
  direction: string;
  doorState: string;
  requestedFloors: number[];
  passengerCount: number;
  capacity: number;
}

export interface SystemStatus {
  elevators: ElevatorStatus[];
  floors: { [key: number]: FloorStatus };
  config: SystemConfig;
}

export interface FloorStatus {
  floorNumber: number;
  callUpButton: boolean;
  callDownButton: boolean;
  waitingPassengers: number;
}

export interface SystemConfig {
  numberOfElevators: number;
  numberOfFloors: number;
  elevatorCapacity: number;
  floorTransitionTimeMs: number;
  doorOpenCloseTimeMs: number;
  doorStayOpenTimeMs: number;
  elevatorTickIntervalMs: number;
}

@Injectable({
  providedIn: 'root'
})
export class ElevatorService {
  private apiUrl = 'http://localhost:8080/api/elevator';
  private statusSubject = new BehaviorSubject<SystemStatus | null>(null);
  public status$ = this.statusSubject.asObservable();

  constructor(private http: HttpClient) {
    this.startStatusPolling();
  }

  /**
   * Start polling for elevator status updates
   */
  private startStatusPolling(): void {
    interval(500)
      .pipe(
        switchMap(() =>
          this.getStatus().pipe(
            catchError(err => {
              // Don't let a single failed HTTP call kill the whole interval.
              // Just log it and skip this tick.
              console.warn('Status poll failed, will retry:', err);
              return EMPTY;
            })
          )
        )
      )
      .subscribe({
        next: (status) => this.statusSubject.next(status)
      });
  }

  /**
   * Call an elevator to a specific floor
   */
  callElevator(floor: number, direction: 'UP' | 'DOWN'): Observable<any> {
    const payload = {
      floor: floor,
      direction: direction
    };
    return this.http.post(`${this.apiUrl}/call`, payload);
  }

  /**
   * Select a floor from inside an elevator
   */
  selectFloor(elevatorId: number, floor: number): Observable<any> {
    const payload = {
      elevatorId: elevatorId,
      floor: floor
    };
    return this.http.post(`${this.apiUrl}/${elevatorId}/select`, payload);
  }

  /**
   * Get current status of all elevators
   */
  getStatus(): Observable<SystemStatus> {
    return this.http.get<SystemStatus>(`${this.apiUrl}/status`);
  }

  /**
   * Get status of a specific elevator
   */
  getElevatorStatus(elevatorId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${elevatorId}`);
  }

  /**
   * Get system configuration
   */
  getSystemConfig(): Observable<SystemConfig> {
    return this.http.get<SystemConfig>(`${this.apiUrl}/config`);
  }

  /**
   * Health check
   */
  healthCheck(): Observable<any> {
    return this.http.get(`${this.apiUrl}/health`);
  }
}

