import React, { useState, useEffect } from 'react';
import { alpha } from '@mui/material/styles';
import {
    Box,
    Typography,
    Paper,
    TextField,
    Button,
    Grid,
    Container,
    Stepper,
    Step,
    StepLabel,
    Card,
    CardContent,
    Alert,
    Divider,
    InputAdornment
} from '@mui/material';
import { useParams, useNavigate } from 'react-router-dom';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import { useAppDispatch, useAppSelector } from '../../store';
import { validateBaggage, setStep } from '../../store/slices/checkinSlice';
import ScaleIcon from '@mui/icons-material/Scale';
import PaymentIcon from '@mui/icons-material/Payment';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';

const steps = ['Baggage Information', 'Payment', 'Confirmation'];

const CheckinPage: React.FC = () => {
    const { checkinId } = useParams<{ checkinId: string }>();
    const navigate = useNavigate();
    const dispatch = useAppDispatch();
    const { currentCheckin, step, loading, error } = useAppSelector(state => state.checkin);
    const { selectedSeat } = useAppSelector(state => state.seats);

    const activeStep = step === 'BAGGAGE' ? 0 : step === 'PAYMENT' ? 1 : 2;

    // When check-in completes (e.g. baggage ≤ 25kg, no payment), go to confirmation page
    useEffect(() => {
        if (step === 'CONFIRMATION') {
            navigate('/confirmation', { replace: true });
        }
    }, [step, navigate]);

    const baggageFormik = useFormik({
        initialValues: {
            weight: '' as any,
        },
        validationSchema: Yup.object({
            weight: Yup.number()
                .required('Weight is required')
                .min(0, 'Weight cannot be negative')
                .max(100, 'Weight seems too high'),
        }),
        onSubmit: async (values) => {
            if (checkinId) {
                await dispatch(validateBaggage({
                    checkinId: parseInt(checkinId),
                    weight: values.weight
                }));
            }
        },
    });

    const handleCompletePayment = () => {
        // Mock payment processing
        // In a real app, this would be a thunk call to Payment Service
        dispatch(setStep('CONFIRMATION'));
        navigate('/confirmation');
    };

    return (
        <Container maxWidth="md">
            <Box sx={{ mb: 6, mt: 4 }}>
                <Stepper activeStep={activeStep} alternativeLabel>
                    {steps.map((label) => (
                        <Step key={label}>
                            <StepLabel>{label}</StepLabel>
                        </Step>
                    ))}
                </Stepper>
            </Box>

            <Grid container spacing={4}>
                <Grid item xs={12} md={7}>
                    {activeStep === 0 && (
                        <Paper sx={{ p: 4, borderRadius: 4 }}>
                            <Typography variant="h5" gutterBottom>Baggage Information</Typography>
                            <Typography color="text.secondary" paragraph>
                                Please enter the total weight of your checked baggage. SkyHigh Airlines allows up to 25kg for free.
                            </Typography>

                            {error && (
                                <Alert severity="error" sx={{ mb: 3 }}>
                                    {error}
                                </Alert>
                            )}

                            <form onSubmit={baggageFormik.handleSubmit}>
                                <TextField
                                    fullWidth
                                    id="weight"
                                    name="weight"
                                    label="Baggage Weight"
                                    type="number"
                                    value={baggageFormik.values.weight}
                                    onChange={baggageFormik.handleChange}
                                    error={baggageFormik.touched.weight && Boolean(baggageFormik.errors.weight)}
                                    helperText={baggageFormik.touched.weight && baggageFormik.errors.weight}
                                    InputProps={{
                                        endAdornment: <InputAdornment position="end">kg</InputAdornment>,
                                        startAdornment: <InputAdornment position="start"><ScaleIcon /></InputAdornment>,
                                    }}
                                    sx={{ mb: 4, mt: 2 }}
                                />

                                <Button
                                    fullWidth
                                    size="large"
                                    type="submit"
                                    variant="contained"
                                    color="primary"
                                    disabled={loading}
                                >
                                    {loading ? 'Calculating...' : 'Calculate Fee & Continue'}
                                </Button>
                            </form>
                        </Paper>
                    )}

                    {activeStep === 1 && (
                        <Paper sx={{ p: 4, borderRadius: 4 }}>
                            <Typography variant="h5" gutterBottom>Payment Required</Typography>
                            <Alert severity="info" sx={{ mb: 3 }}>
                                Excess baggage fee detected: <strong>${currentCheckin?.excessBaggageFee}</strong>
                            </Alert>

                            <Box sx={{ mt: 4 }}>
                                <Typography variant="subtitle1" gutterBottom>Credit Card Details (Simulated)</Typography>
                                <Grid container spacing={2}>
                                    <Grid item xs={12}>
                                        <TextField fullWidth label="Card Number" placeholder="**** **** **** ****" />
                                    </Grid>
                                    <Grid item xs={6}>
                                        <TextField fullWidth label="Expiry" placeholder="MM/YY" />
                                    </Grid>
                                    <Grid item xs={6}>
                                        <TextField fullWidth label="CVC" placeholder="***" />
                                    </Grid>
                                </Grid>
                            </Box>

                            <Button
                                fullWidth
                                size="large"
                                variant="contained"
                                color="secondary"
                                onClick={handleCompletePayment}
                                startIcon={<PaymentIcon />}
                                sx={{ mt: 4 }}
                            >
                                Pay ${currentCheckin?.excessBaggageFee} & Complete Check-in
                            </Button>
                        </Paper>
                    )}

                    {activeStep === 2 && (
                        <Paper sx={{ p: 4, borderRadius: 4 }}>
                            <Typography variant="h5" gutterBottom>Check-in Complete</Typography>
                            <Alert severity="success" sx={{ mb: 3 }}>
                                Your baggage is within the free allowance. Check-in is complete.
                            </Alert>
                            <Typography color="text.secondary" paragraph>
                                You will be redirected to your boarding pass shortly. If not, click below.
                            </Typography>
                            <Button
                                fullWidth
                                size="large"
                                variant="contained"
                                color="primary"
                                onClick={() => navigate('/confirmation', { replace: true })}
                                startIcon={<CheckCircleIcon />}
                            >
                                View Boarding Pass
                            </Button>
                        </Paper>
                    )}
                </Grid>

                <Grid item xs={12} md={5}>
                    <Card sx={{ borderRadius: 4 }}>
                        <CardContent>
                            <Typography variant="h6" gutterBottom>Trip Summary</Typography>
                            <Divider sx={{ mb: 2 }} />

                            <Box display="flex" justifyContent="space-between" mb={1}>
                                <Typography color="text.secondary">Booking Reference:</Typography>
                                <Typography fontWeight={700}>{currentCheckin?.bookingReference || 'N/A'}</Typography>
                            </Box>
                            <Box display="flex" justifyContent="space-between" mb={1}>
                                <Typography color="text.secondary">Seat:</Typography>
                                <Typography fontWeight={700}>{selectedSeat?.seatNumber || 'N/A'}</Typography>
                            </Box>
                            <Box display="flex" justifyContent="space-between" mb={1}>
                                <Typography color="text.secondary">Status:</Typography>
                                <Typography color="primary">{currentCheckin?.status.replace('_', ' ')}</Typography>
                            </Box>

                            {currentCheckin?.excessBaggageFee ? (
                                <Box sx={{ mt: 2, pt: 2, borderTop: '1px dashed divider' }}>
                                    <Box display="flex" justifyContent="space-between">
                                        <Typography fontWeight={700}>Total Fee:</Typography>
                                        <Typography fontWeight={700} color="error">${currentCheckin.excessBaggageFee}</Typography>
                                    </Box>
                                </Box>
                            ) : null}
                        </CardContent>
                    </Card>

                    <Box sx={{ mt: 3, p: 2, bgcolor: alpha('#2e7d32', 0.05), borderRadius: 4, border: '1px solid', borderColor: alpha('#2e7d32', 0.2) }}>
                        <Box display="flex" alignItems="center">
                            <CheckCircleIcon color="success" sx={{ mr: 1 }} />
                            <Typography variant="subtitle2">Secure Check-in</Typography>
                        </Box>
                        <Typography variant="caption" color="text.secondary">
                            Your seat hold is secured while you complete these steps.
                        </Typography>
                    </Box>
                </Grid>
            </Grid>
        </Container>
    );
};

export default CheckinPage;
