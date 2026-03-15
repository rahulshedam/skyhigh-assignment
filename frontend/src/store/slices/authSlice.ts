import { createSlice, PayloadAction } from '@reduxjs/toolkit';

const EMAIL_KEY = 'skyhigh_user_email';

interface AuthState {
    email: string | null;
    isAuthenticated: boolean;
}

const initialState: AuthState = {
    email: localStorage.getItem(EMAIL_KEY),
    isAuthenticated: !!localStorage.getItem(EMAIL_KEY),
};

const authSlice = createSlice({
    name: 'auth',
    initialState,
    reducers: {
        login: (state, action: PayloadAction<string>) => {
            state.email = action.payload;
            state.isAuthenticated = true;
            localStorage.setItem(EMAIL_KEY, action.payload);
        },
        logout: (state) => {
            state.email = null;
            state.isAuthenticated = false;
            localStorage.removeItem(EMAIL_KEY);
        },
    },
});

export const { login, logout } = authSlice.actions;
export default authSlice.reducer;
