import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

declare var SockJS: any;
declare var Stomp: any;

export interface ElevatorEvent {
  type: string;
  elevatorStatus: any;
  timestamp: string;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private stompClient: any;
  private eventSubject = new BehaviorSubject<ElevatorEvent | null>(null);
  public events$ = this.eventSubject.asObservable();
  private connectionStatusSubject = new BehaviorSubject<boolean>(false);
  public connectionStatus$ = this.connectionStatusSubject.asObservable();
  private isConnected = false;

  constructor() {
    this.connect();
  }

  /**
   * Connect to WebSocket server
   */
  connect(): void {
    const socket = new SockJS('http://localhost:8080/ws/elevator');
    this.stompClient = Stomp.over(socket);
    this.stompClient.debug = null; // suppress debug logs

    this.stompClient.connect({}, (frame: any) => {
      console.log('WebSocket connected:', frame);
      this.isConnected = true;
      this.connectionStatusSubject.next(true);
      this.subscribe();
    }, (error: any) => {
      console.error('WebSocket connection error:', error);
      this.isConnected = false;
      this.connectionStatusSubject.next(false);
      // Retry connection after 5 seconds
      setTimeout(() => this.connect(), 5000);
    });
  }

  /**
   * Subscribe to elevator events
   */
  private subscribe(): void {
    if (this.stompClient && this.isConnected) {
      this.stompClient.subscribe('/topic/elevators', (message: any) => {
        try {
          const event: ElevatorEvent = JSON.parse(message.body);
          this.eventSubject.next(event);
        } catch (e) {
          console.error('Error parsing WebSocket message:', e);
        }
      });
    }
  }

  /**
   * Check if WebSocket is connected
   */
  getConnectionStatus(): boolean {
    return this.isConnected;
  }

  /**
   * Disconnect from WebSocket
   */
  disconnect(): void {
    if (this.stompClient) {
      this.stompClient.disconnect(() => {
        console.log('WebSocket disconnected');
        this.isConnected = false;
        this.connectionStatusSubject.next(false);
      });
    }
  }

  /**
   * Send a message to the server
   */
  sendMessage(destination: string, message: any): void {
    if (this.stompClient && this.isConnected) {
      this.stompClient.send(destination, {}, JSON.stringify(message));
    }
  }
}
