import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { Seat } from '../../types';
import { SeatService } from '../../api/services';

interface SeatState {
    seats: Seat[];
    selectedSeat: Seat | null;
    loading: boolean;
    error: string | null;
    holdExpiryTime: string | null;
}

const initialState: SeatState = {
    seats: [],
    selectedSeat: null,
    loading: false,
    error: null,
    holdExpiryTime: null,
};

export const fetchSeatMap = createAsyncThunk(
    'seats/fetchSeatMap',
    async (flightId: number) => {
        return await SeatService.getSeatMap(flightId);
    }
);

export const holdSeat = createAsyncThunk(
    'seats/holdSeat',
    async ({ seatId, passengerId, bookingReference }: { seatId: number, passengerId: string, bookingReference: string }) => {
        return await SeatService.holdSeat(seatId, passengerId, bookingReference);
    }
);

const seatSlice = createSlice({
    name: 'seats',
    initialState,
    reducers: {
        selectSeat: (state, action: PayloadAction<Seat | null>) => {
            state.selectedSeat = action.payload;
        },
        clearError: (state) => {
            state.error = null;
        },
    },
    extraReducers: (builder) => {
        builder
            .addCase(fetchSeatMap.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(fetchSeatMap.fulfilled, (state, action) => {
                state.loading = false;
                state.seats = action.payload;
            })
            .addCase(fetchSeatMap.rejected, (state, action) => {
                state.loading = false;
                state.error = action.error.message || 'Failed to fetch seat map';
            })
            .addCase(holdSeat.fulfilled, (state, action) => {
                state.selectedSeat = action.payload;
                // Assuming the response includes the expiry time
                state.holdExpiryTime = new Date(Date.now() + 120000).toISOString(); // Default 120s
            })
            .addCase(holdSeat.rejected, (state, action) => {
                state.error = action.error.message || 'Failed to hold seat';
            });
    },
});

export const { selectSeat, clearError } = seatSlice.actions;
export default seatSlice.reducer;
