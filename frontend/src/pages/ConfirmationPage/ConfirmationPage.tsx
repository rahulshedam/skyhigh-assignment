import React, { useEffect } from 'react';
import {
    Box,
    Typography,
    Paper,
    Button,
    Container,
    Card,
    CardContent,
    Divider,
    Stack
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import { useAppDispatch, useAppSelector } from '../../store';
import { resetCheckin } from '../../store/slices/checkinSlice';
import PrintIcon from '@mui/icons-material/Print';
import HomeIcon from '@mui/icons-material/Home';

const ConfirmationPage: React.FC = () => {
    const navigate = useNavigate();
    const dispatch = useAppDispatch();
    const { currentCheckin } = useAppSelector(state => state.checkin);
    const { selectedSeat } = useAppSelector(state => state.seats);

    // If no check-in data, redirect home after a short delay or show message
    useEffect(() => {
        return () => {
            // Optional: clear state on unmount if needed
        };
    }, []);

    const handleGoHome = () => {
        dispatch(resetCheckin());
        navigate('/');
    };

    return (
        <Container maxWidth="sm">
            <Box sx={{ mt: 8, mb: 4, textAlign: 'center' }}>
                <CheckCircleOutlineIcon color="success" sx={{ fontSize: 100, mb: 2 }} />
                <Typography variant="h3" gutterBottom>Check-in Successful!</Typography>
                <Typography variant="body1" color="text.secondary">
                    Your boarding pass has been sent to your registered email.
                </Typography>
            </Box>

            <Paper elevation={0} sx={{ p: 4, borderRadius: 4, border: '1px solid divider', bgcolor: '#fff' }}>
                <Typography variant="h6" gutterBottom>Boarding Pass Details</Typography>
                <Divider sx={{ mb: 3 }} />

                <Stack spacing={2}>
                    <Box display="flex" justifyContent="space-between">
                        <Typography color="text.secondary">Passenger:</Typography>
                        <Typography fontWeight={700}>{currentCheckin?.passengerId || 'John Doe'}</Typography>
                    </Box>
                    <Box display="flex" justifyContent="space-between">
                        <Typography color="text.secondary">Booking Reference:</Typography>
                        <Typography fontWeight={700}>{currentCheckin?.bookingReference || 'SKY777'}</Typography>
                    </Box>
                    <Box display="flex" justifyContent="space-between">
                        <Typography color="text.secondary">Seat Number:</Typography>
                        <Typography fontWeight={700} color="primary" variant="h5">
                            {selectedSeat?.seatNumber || '12A'}
                        </Typography>
                    </Box>
                    <Box display="flex" justifyContent="space-between">
                        <Typography color="text.secondary">Flight Number:</Typography>
                        <Typography fontWeight={700}>SH101</Typography>
                    </Box>
                </Stack>

                <Box sx={{ mt: 4, p: 2, bgcolor: 'background.default', borderRadius: 2, border: '1px dashed divider' }}>
                    <Typography variant="caption" color="text.secondary" display="block" textAlign="center">
                        Gate closes 20 minutes before departure. Please be at the airport on time.
                    </Typography>
                </Box>
            </Paper>

            <Box sx={{ mt: 4, display: 'flex', gap: 2 }}>
                <Button
                    fullWidth
                    variant="contained"
                    color="primary"
                    size="large"
                    startIcon={<PrintIcon />}
                >
                    Print Boarding Pass
                </Button>
                <Button
                    fullWidth
                    variant="outlined"
                    color="primary"
                    size="large"
                    onClick={handleGoHome}
                    startIcon={<HomeIcon />}
                >
                    Return Home
                </Button>
            </Box>
        </Container>
    );
};

export default ConfirmationPage;
