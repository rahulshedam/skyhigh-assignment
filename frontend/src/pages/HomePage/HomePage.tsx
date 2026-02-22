import React, { useState } from 'react';
import {
    Box,
    Typography,
    Paper,
    TextField,
    Button,
    Grid,
    Container,
    Card,
    CardContent,
    Alert,
    CircularProgress,
    alpha
} from '@mui/material';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import { useNavigate } from 'react-router-dom';
import SearchIcon from '@mui/icons-material/Search';
import FlightIcon from '@mui/icons-material/Flight';
import { BookingService } from '../../api/services';

const validationSchema = Yup.object({
    bookingReference: Yup.string()
        .required('Booking reference is required')
        .matches(/^[A-Z0-9]{6}$/, 'Must be 6 alphanumeric characters'),
    passengerId: Yup.string()
        .required('Passenger ID (Last Name or Email) is required'),
});

const HomePage: React.FC = () => {
    const navigate = useNavigate();
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    const formik = useFormik({
        initialValues: {
            bookingReference: '',
            passengerId: '',
            // Initialize with empty string to avoid "uncontrolled to controlled" warning
        },
        validationSchema: validationSchema,
        onSubmit: async (values) => {
            setError(null);
            setLoading(true);
            
            try {
                console.log('Verifying booking:', values);
                
                // Call the backend to verify booking and get flight details
                const bookingInfo = await BookingService.verifyBooking(
                    values.bookingReference,
                    values.passengerId
                );
                
                if (!bookingInfo.valid) {
                    setError('This booking has been cancelled or is no longer active.');
                    setLoading(false);
                    return;
                }
                
                console.log('Booking verified, flight ID:', bookingInfo.flightId);
                
                // Navigate to the seat selection page with the correct flight ID
                navigate(`/seats/${bookingInfo.flightId}?ref=${values.bookingReference}&user=${values.passengerId}`);
            } catch (err: any) {
                console.error('Booking verification failed:', err);
                setError(
                    err.response?.data?.message || 
                    'Unable to find booking. Please check your booking reference and passenger ID.'
                );
                setLoading(false);
            }
        },
    });

    return (
        <Container maxWidth="md">
            <Box sx={{ mt: 8, mb: 6, textAlign: 'center' }}>
                <Typography variant="h1" gutterBottom color="primary">
                    Ready for your next adventure?
                </Typography>
                <Typography variant="h5" color="text.secondary" paragraph>
                    Check-in now and prepare for takeoff with SkyHigh Airlines.
                </Typography>
            </Box>

            <Paper
                elevation={0}
                sx={{
                    p: 4,
                    borderRadius: 4,
                    border: '1px solid',
                    borderColor: 'divider',
                    boxShadow: '0 10px 30px rgba(0,0,0,0.05)'
                }}
            >
                {error && (
                    <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
                        {error}
                    </Alert>
                )}
                
                <form onSubmit={formik.handleSubmit}>
                    <Grid container spacing={3}>
                        <Grid item xs={12} md={5}>
                            <TextField
                                fullWidth
                                id="bookingReference"
                                name="bookingReference"
                                label="Booking Reference (PNR)"
                                placeholder="e.g. SKY123"
                                value={formik.values.bookingReference}
                                onChange={formik.handleChange}
                                error={formik.touched.bookingReference && Boolean(formik.errors.bookingReference)}
                                helperText={formik.touched.bookingReference && formik.errors.bookingReference}
                                variant="outlined"
                                disabled={loading}
                            />
                        </Grid>
                        <Grid item xs={12} md={5}>
                            <TextField
                                fullWidth
                                id="passengerId"
                                name="passengerId"
                                label="Passenger Last Name / ID"
                                placeholder="e.g. Smith"
                                value={formik.values.passengerId}
                                onChange={formik.handleChange}
                                error={formik.touched.passengerId && Boolean(formik.errors.passengerId)}
                                helperText={formik.touched.passengerId && formik.errors.passengerId}
                                variant="outlined"
                                disabled={loading}
                            />
                        </Grid>
                        <Grid item xs={12} md={2}>
                            <Button
                                fullWidth
                                size="large"
                                type="submit"
                                variant="contained"
                                color="secondary"
                                sx={{ height: '56px' }}
                                startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <SearchIcon />}
                                disabled={loading}
                            >
                                {loading ? 'Searching...' : 'Search'}
                            </Button>
                        </Grid>
                    </Grid>
                </form>
            </Paper>

            <Grid container spacing={4} sx={{ mt: 8 }}>
                {[
                    { icon: <FlightIcon color="primary" />, title: 'Online Check-in', desc: 'Secure your favorite seat 24h before departure.' },
                    { icon: <FlightIcon color="primary" />, title: 'Manage Baggage', desc: 'Add bags and pay fees seamlessly online.' },
                    { icon: <FlightIcon color="primary" />, title: 'Live Updates', desc: 'Get real-time notifications about your flight.' },
                ].map((feature, index) => (
                    <Grid item xs={12} sm={4} key={index}>
                        <Card sx={{ height: '100%', bgcolor: alpha('#1a237e', 0.02) }}>
                            <CardContent sx={{ textAlign: 'center' }}>
                                <Box sx={{ mb: 2 }}>{feature.icon}</Box>
                                <Typography variant="h6" gutterBottom>{feature.title}</Typography>
                                <Typography variant="body2" color="text.secondary">
                                    {feature.desc}
                                </Typography>
                            </CardContent>
                        </Card>
                    </Grid>
                ))}
            </Grid>
        </Container>
    );
};

export default HomePage;
