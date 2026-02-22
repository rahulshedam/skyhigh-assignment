import api from './base';
import { Seat, Checkin, Waitlist } from '../types';

export const BookingService = {
    verifyBooking: async (bookingReference: string, passengerId: string): Promise<{
        flightId: number;
        flightNumber: string;
        origin: string;
        destination: string;
        bookingReference: string;
        passengerId: string;
        valid: boolean;
    }> => {
        const response = await api.post('/api/bookings/verify', {
            bookingReference,
            passengerId,
        });
        return response.data;
    },
};

export interface SeatStatusResponse {
    id: number;
    seatNumber: string;
    flightId: number;
    seatClass: string;
    status: string;
}

export const SeatService = {
    getSeatMap: async (flightId: number): Promise<Seat[]> => {
        const response = await api.get(`/api/seats/flights/${flightId}/seatmap`);
        return response.data.seats;
    },
    getSeatStatus: async (seatId: number): Promise<SeatStatusResponse> => {
        const response = await api.get(`/api/seats/${seatId}/status`);
        return response.data;
    },
    holdSeat: async (seatId: number, passengerId: string, bookingReference: string): Promise<Seat> => {
        const response = await api.post(`/api/seats/${seatId}/hold`, {
            passengerId,
            bookingReference,
        });
        return response.data;
    },
    releaseSeat: async (seatId: number): Promise<void> => {
        await api.post(`/api/seats/${seatId}/release`);
    },
};

export const CheckinService = {
    startCheckin: async (data: {
        passengerId: string;
        bookingReference: string;
        flightId: number;
        seatId: number;
        baggageWeightKg?: number;
    }): Promise<Checkin> => {
        const payload = {
            ...data,
            baggageWeight: data.baggageWeightKg ?? 0,
        };
        const response = await api.post('/api/checkin/start', payload);
        return response.data;
    },
    validateBaggage: async (checkinId: number, weight: number): Promise<Checkin> => {
        const response = await api.post(`/api/checkin/${checkinId}/baggage`, { weight });
        return response.data;
    },
    processPayment: async (checkinId: number, paymentDetails: any): Promise<Checkin> => {
        const response = await api.post(`/api/checkin/${checkinId}/payment`, paymentDetails);
        return response.data;
    },
};

export const WaitlistService = {
    joinWaitlist: async (data: {
        seatId: number;
        passengerId: string;
        bookingReference: string;
    }): Promise<Waitlist> => {
        const response = await api.post('/api/seats/waitlist', data);
        return response.data;
    },
    getPassengerWaitlist: async (passengerId: string): Promise<Waitlist[]> => {
        const response = await api.get(`/api/seats/waitlist/passenger/${passengerId}`);
        return response.data;
    },
};
