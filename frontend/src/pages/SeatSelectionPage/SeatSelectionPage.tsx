import React, { useEffect, useState } from 'react';
import {
    Box,
    Typography,
    Button,
    Grid,
    Paper,
    Container,
    CircularProgress,
    Alert,
    Divider,
    LinearProgress
} from '@mui/material';
import { useParams, useSearchParams, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../store';
import { fetchSeatMap, holdSeat, selectSeat } from '../../store/slices/seatSlice';
import { startCheckin } from '../../store/slices/checkinSlice';
import SeatGrid from '../../components/features/SeatGrid';
import TimerIcon from '@mui/icons-material/Timer';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';

const SeatSelectionPage: React.FC = () => {
    const { flightId } = useParams<{ flightId: string }>();
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const dispatch = useAppDispatch();

    const bookingReference = searchParams.get('ref') || 'DUMMY1';
    const passengerId = searchParams.get('user') || 'GUEST';

    const { seats, selectedSeat, loading, error, holdExpiryTime } = useAppSelector(state => state.seats);
    const [timeLeft, setTimeLeft] = useState<number | null>(null);

    useEffect(() => {
        if (flightId) {
            dispatch(fetchSeatMap(parseInt(flightId)));
        }
    }, [flightId, dispatch]);

    useEffect(() => {
        let timer: NodeJS.Timeout;
        if (holdExpiryTime) {
            const calculateTimeLeft = () => {
                const difference = new Date(holdExpiryTime).getTime() - new Date().getTime();
                return Math.max(0, Math.floor(difference / 1000));
            };

            setTimeLeft(calculateTimeLeft());
            timer = setInterval(() => {
                const remaining = calculateTimeLeft();
                setTimeLeft(remaining);
                if (remaining <= 0) {
                    clearInterval(timer);
                    dispatch(selectSeat(null));
                }
            }, 1000);
        }
        return () => clearInterval(timer);
    }, [holdExpiryTime, dispatch]);

    const handleSeatClick = (seat: any) => {
        dispatch(selectSeat(seat));
    };

    const handleHoldSeat = async () => {
        if (selectedSeat && flightId) {
            await dispatch(holdSeat({
                seatId: selectedSeat.id,
                passengerId,
                bookingReference
            }));
        }
    };

    const handleContinue = async () => {
        if (selectedSeat && flightId) {
            const result = await dispatch(startCheckin({
                passengerId,
                bookingReference,
                flightId: parseInt(flightId),
                seatId: selectedSeat.id
            }));

            if (startCheckin.fulfilled.match(result)) {
                navigate(`/checkin/${result.payload.id}`);
            }
        }
    };

    if (loading && !seats.length) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 10 }}>
                <CircularProgress />
            </Box>
        );
    }

    return (
        <Container maxWidth="lg">
            <Grid container spacing={4}>
                <Grid item xs={12} md={8}>
                    <Typography variant="h4" gutterBottom>Select Your Seat</Typography>
                    <Typography color="text.secondary" paragraph>
                        Flight ID: {flightId} | Booking: {bookingReference}
                    </Typography>

                    {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

                    <Paper elevation={0} sx={{ border: '1px solid divider', borderRadius: 4, overflow: 'hidden' }}>
                        <SeatGrid
                            seats={seats}
                            onSeatSelect={handleSeatClick}
                            selectedSeatId={selectedSeat?.id || null}
                        />
                    </Paper>
                </Grid>

                <Grid item xs={12} md={4}>
                    <Box sx={{ position: 'sticky', top: 24 }}>
                        <Paper sx={{ p: 3, borderRadius: 4 }}>
                            <Typography variant="h6" gutterBottom>Seat Summary</Typography>
                            <Divider sx={{ mb: 2 }} />

                            {selectedSeat ? (
                                <Box>
                                    <Box display="flex" justifyContent="space-between" mb={1}>
                                        <Typography color="text.secondary">Seat Number:</Typography>
                                        <Typography fontWeight={700}>{selectedSeat.seatNumber}</Typography>
                                    </Box>
                                    <Box display="flex" justifyContent="space-between" mb={1}>
                                        <Typography color="text.secondary">Class:</Typography>
                                        <Typography>{selectedSeat.seatClass}</Typography>
                                    </Box>
                                    <Box display="flex" justifyContent="space-between" mb={3}>
                                        <Typography color="text.secondary">Status:</Typography>
                                        <Typography color={selectedSeat.status === 'AVAILABLE' ? 'success.main' : 'warning.main'}>
                                            {selectedSeat.status}
                                        </Typography>
                                    </Box>

                                    {timeLeft !== null && timeLeft > 0 ? (
                                        <Box sx={{ mb: 3, p: 2, bgcolor: 'warning.light', borderRadius: 2 }}>
                                            <Box display="flex" alignItems="center" mb={1}>
                                                <TimerIcon sx={{ mr: 1, fontSize: 20 }} />
                                                <Typography variant="subtitle2">Hold expires in:</Typography>
                                            </Box>
                                            <Typography variant="h4" sx={{ mb: 1 }}>{Math.floor(timeLeft / 60)}:{(timeLeft % 60).toString().padStart(2, '0')}</Typography>
                                            <LinearProgress
                                                variant="determinate"
                                                value={(timeLeft / 120) * 100}
                                                color="warning"
                                                sx={{ height: 8, borderRadius: 4 }}
                                            />
                                        </Box>
                                    ) : (
                                        <Button
                                            fullWidth
                                            variant="contained"
                                            color="primary"
                                            size="large"
                                            onClick={handleHoldSeat}
                                            disabled={selectedSeat.status !== 'AVAILABLE'}
                                            sx={{ mb: 2 }}
                                        >
                                            Hold Selected Seat
                                        </Button>
                                    )}

                                    <Button
                                        fullWidth
                                        variant="contained"
                                        color="secondary"
                                        size="large"
                                        disabled={!holdExpiryTime}
                                        onClick={handleContinue}
                                        startIcon={<ArrowForwardIcon />}
                                    >
                                        Continue to Baggage
                                    </Button>
                                </Box>
                            ) : (
                                <Box sx={{ py: 4, textAlign: 'center' }}>
                                    <Typography color="text.secondary italic">Please select a seat from the map to proceed.</Typography>
                                </Box>
                            )}
                        </Paper>

                        <Box sx={{ mt: 3, p: 2, bgcolor: 'background.paper', borderRadius: 4, border: '1px solid divider' }}>
                            <Typography variant="subtitle2" gutterBottom>Need Help?</Typography>
                            <Typography variant="body2" color="text.secondary">
                                If you cannot find a suitable seat, you can join the waitlist for your preferred one.
                            </Typography>
                            <Button color="primary" sx={{ mt: 1, p: 0 }}>View Waitlist Policy</Button>
                        </Box>
                    </Box>
                </Grid>
            </Grid>
        </Container>
    );
};

export default SeatSelectionPage;
