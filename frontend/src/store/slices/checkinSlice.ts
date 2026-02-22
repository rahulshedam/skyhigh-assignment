import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { Checkin, CheckinStatus } from '../../types';
import { CheckinService } from '../../api/services';

interface CheckinState {
    currentCheckin: Checkin | null;
    step: 'SEARCH' | 'SEAT' | 'BAGGAGE' | 'PAYMENT' | 'CONFIRMATION';
    loading: boolean;
    error: string | null;
}

const initialState: CheckinState = {
    currentCheckin: null,
    step: 'SEARCH',
    loading: false,
    error: null,
};

export const startCheckin = createAsyncThunk(
    'checkin/startCheckin',
    async (data: {
        passengerId: string,
        bookingReference: string,
        flightId: number,
        seatId: number
    }) => {
        return await CheckinService.startCheckin(data);
    }
);

export const validateBaggage = createAsyncThunk(
    'checkin/validateBaggage',
    async ({ checkinId, weight }: { checkinId: number, weight: number }) => {
        return await CheckinService.validateBaggage(checkinId, weight);
    }
);

const checkinSlice = createSlice({
    name: 'checkin',
    initialState,
    reducers: {
        setStep: (state, action: PayloadAction<CheckinState['step']>) => {
            state.step = action.payload;
        },
        resetCheckin: (state) => {
            state.currentCheckin = null;
            state.step = 'SEARCH';
        },
    },
    extraReducers: (builder) => {
        builder
            .addCase(startCheckin.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(startCheckin.fulfilled, (state, action) => {
                state.loading = false;
                state.currentCheckin = action.payload;
                state.step = 'BAGGAGE';
            })
            .addCase(startCheckin.rejected, (state, action) => {
                state.loading = false;
                state.error = action.error.message || 'Failed to start check-in';
            })
            .addCase(validateBaggage.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(validateBaggage.fulfilled, (state, action) => {
                state.loading = false;
                state.currentCheckin = action.payload;
                if (state.currentCheckin?.status === 'WAITING_FOR_PAYMENT') {
                    state.step = 'PAYMENT';
                } else {
                    state.step = 'CONFIRMATION';
                }
            })
            .addCase(validateBaggage.rejected, (state, action) => {
                state.loading = false;
                state.error = action.error.message || 'Failed to validate baggage';
            });
    },
});

export const { setStep, resetCheckin } = checkinSlice.actions;
export default checkinSlice.reducer;
