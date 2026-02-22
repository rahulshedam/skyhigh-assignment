import React, { lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from './components/layout/MainLayout';
import { CircularProgress, Box } from '@mui/material';

// Lazy load pages for better performance
const HomePage = lazy(() => import('./pages/HomePage/HomePage'));
const SeatSelectionPage = lazy(() => import('./pages/SeatSelectionPage/SeatSelectionPage'));
const CheckinPage = lazy(() => import('./pages/CheckinPage/CheckinPage'));
const WaitlistPage = lazy(() => import('./pages/WaitlistPage/WaitlistPage'));
const ConfirmationPage = lazy(() => import('./pages/ConfirmationPage/ConfirmationPage'));
const NotFoundPage = lazy(() => import('./pages/NotFoundPage/NotFoundPage'));

const PageLoader = () => (
    <Box display="flex" justifyContent="center" alignItems="center" height="50vh">
        <CircularProgress />
    </Box>
);

const App: React.FC = () => {
    return (
        <Suspense fallback={<PageLoader />}>
            <Routes>
                <Route path="/" element={<MainLayout />}>
                    <Route index element={<HomePage />} />
                    <Route path="seats/:flightId" element={<SeatSelectionPage />} />
                    <Route path="checkin/:checkinId" element={<CheckinPage />} />
                    <Route path="waitlist" element={<WaitlistPage />} />
                    <Route path="confirmation" element={<ConfirmationPage />} />
                    <Route path="404" element={<NotFoundPage />} />
                    <Route path="*" element={<Navigate to="/404" replace />} />
                </Route>
            </Routes>
        </Suspense>
    );
};

export default App;
