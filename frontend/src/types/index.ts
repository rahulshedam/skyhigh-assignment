export type SeatStatus = 'AVAILABLE' | 'HELD' | 'CONFIRMED' | 'CANCELLED';
export type WaitlistStatus = 'WAITING' | 'ASSIGNED' | 'EXPIRED' | 'CANCELLED';
export type CheckinStatus = 'IN_PROGRESS' | 'WAITING_FOR_PAYMENT' | 'COMPLETED' | 'CANCELLED';

export interface Seat {
    id: number;
    seatNumber: string;
    flightId: number;
    seatClass: string;
    status: SeatStatus;
    passengerId?: string;
    bookingReference?: string;
}

export interface Flight {
    id: number;
    flightNumber: string;
    departureTime: string;
    origin: string;
    destination: string;
    aircraftType: string;
    status: string;
}

export interface Checkin {
    id: number;
    checkinReference: string;
    passengerId: string;
    bookingReference: string;
    flightId: number;
    seatId: number;
    status: CheckinStatus;
    baggageWeightKg?: number;
    excessBaggageFee?: number;
    paymentId?: string;
    startedAt: string;
    completedAt?: string;
}

export interface Waitlist {
    id: number;
    seatId: number;
    passengerId: string;
    bookingReference: string;
    status: WaitlistStatus;
    joinedAt: string;
    assignedAt?: string;
}

export interface ApiResponse<T> {
    success: boolean;
    data: T;
    message?: string;
    errorCode?: string;
}
