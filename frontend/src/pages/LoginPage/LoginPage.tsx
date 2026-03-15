import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch } from '../../store';
import { login } from '../../store/slices/authSlice';
import { AuthService } from '../../api/services';
import {
    Box,
    Button,
    CircularProgress,
    Container,
    TextField,
    Typography,
    Alert,
    Paper,
    Stepper,
    Step,
    StepLabel,
} from '@mui/material';
import FlightTakeoffIcon from '@mui/icons-material/FlightTakeoff';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import MarkEmailReadIcon from '@mui/icons-material/MarkEmailRead';

const steps = ['Enter Email', 'Verify OTP'];

const LoginPage: React.FC = () => {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const [activeStep, setActiveStep] = useState(0);
    const [email, setEmail] = useState('');
    const [otp, setOtp] = useState('');
    const [passengerName, setPassengerName] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleEmailSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            // Step 1: verify email exists in booking system
            const validateResult = await AuthService.validateEmail(email.trim().toLowerCase());
            if (!validateResult.valid) {
                setError(validateResult.message || 'Email not found in our booking system.');
                setLoading(false);
                return;
            }

            setPassengerName(validateResult.passengerName || '');

            // Step 2: send OTP via notification service
            const otpResult = await AuthService.sendOtp(email.trim().toLowerCase());
            if (!otpResult.success) {
                setError('Failed to send OTP. Please try again.');
                setLoading(false);
                return;
            }

            setActiveStep(1);
        } catch (err: any) {
            setError(err?.response?.data?.message || 'Something went wrong. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const handleOtpSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const result = await AuthService.verifyOtp(email.trim().toLowerCase(), otp.trim());
            if (result.success) {
                dispatch(login(email.trim().toLowerCase()));
                navigate('/', { replace: true });
            } else {
                setError(result.message || 'Invalid OTP. Please try again.');
            }
        } catch (err: any) {
            const msg = err?.response?.data?.message || 'Invalid or expired OTP.';
            setError(msg);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box
            sx={{
                minHeight: '100vh',
                background: 'linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
            }}
        >
            <Container maxWidth="sm">
                {/* Branding */}
                <Box sx={{ textAlign: 'center', mb: 4 }}>
                    <FlightTakeoffIcon sx={{ fontSize: 56, color: '#4fc3f7', mb: 1 }} />
                    <Typography variant="h4" fontWeight={700} color="white" letterSpacing={1}>
                        SkyHigh Airlines
                    </Typography>
                    <Typography variant="body2" color="rgba(255,255,255,0.6)" mt={0.5}>
                        Online Check-in System
                    </Typography>
                </Box>

                <Paper
                    elevation={12}
                    sx={{
                        p: 4,
                        borderRadius: 3,
                        backdropFilter: 'blur(10px)',
                        background: 'rgba(255, 255, 255, 0.97)',
                    }}
                >
                    <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                        <LockOutlinedIcon sx={{ color: '#1565c0', mr: 1.5 }} />
                        <Typography variant="h6" fontWeight={600} color="text.primary">
                            Passenger Login
                        </Typography>
                    </Box>

                    <Stepper activeStep={activeStep} sx={{ mb: 4 }}>
                        {steps.map((label) => (
                            <Step key={label}>
                                <StepLabel>{label}</StepLabel>
                            </Step>
                        ))}
                    </Stepper>

                    {error && (
                        <Alert severity="error" sx={{ mb: 3 }}>
                            {error}
                        </Alert>
                    )}

                    {activeStep === 0 && (
                        <Box component="form" onSubmit={handleEmailSubmit}>
                            <Typography variant="body2" color="text.secondary" mb={2}>
                                Enter the email address associated with your booking.
                            </Typography>
                            <TextField
                                label="Email Address"
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                fullWidth
                                required
                                autoFocus
                                variant="outlined"
                                placeholder="e.g. passenger@example.com"
                                sx={{ mb: 3 }}
                                inputProps={{ id: 'login-email-input' }}
                            />
                            <Button
                                type="submit"
                                variant="contained"
                                size="large"
                                fullWidth
                                disabled={loading || !email.trim()}
                                id="send-otp-btn"
                                sx={{
                                    py: 1.5,
                                    fontWeight: 600,
                                    background: 'linear-gradient(90deg, #1565c0, #4fc3f7)',
                                    '&:hover': { background: 'linear-gradient(90deg, #0d47a1, #29b6f6)' },
                                }}
                            >
                                {loading ? <CircularProgress size={22} color="inherit" /> : 'Send OTP'}
                            </Button>
                        </Box>
                    )}

                    {activeStep === 1 && (
                        <Box component="form" onSubmit={handleOtpSubmit}>
                            <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                                <MarkEmailReadIcon sx={{ color: '#2e7d32', mr: 1 }} />
                                <Typography variant="body2" color="text.secondary">
                                    OTP sent to <strong>{email}</strong>
                                    {passengerName && (
                                        <span> &nbsp;–&nbsp; Welcome, <strong>{passengerName}</strong>!</span>
                                    )}
                                </Typography>
                            </Box>
                            <Alert severity="info" sx={{ mb: 2 }}>
                                For this assignment, the OTP is always <strong>123123</strong>. Check the notification-service logs for the mock email.
                            </Alert>
                            <TextField
                                label="Enter OTP"
                                value={otp}
                                onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                                fullWidth
                                required
                                autoFocus
                                variant="outlined"
                                placeholder="123456"
                                inputProps={{ maxLength: 6, id: 'otp-input', inputMode: 'numeric' }}
                                sx={{ mb: 3 }}
                            />
                            <Button
                                type="submit"
                                variant="contained"
                                size="large"
                                fullWidth
                                disabled={loading || otp.length < 6}
                                id="verify-otp-btn"
                                sx={{
                                    py: 1.5,
                                    fontWeight: 600,
                                    background: 'linear-gradient(90deg, #1b5e20, #43a047)',
                                    '&:hover': { background: 'linear-gradient(90deg, #1b5e20, #2e7d32)' },
                                }}
                            >
                                {loading ? <CircularProgress size={22} color="inherit" /> : 'Verify & Login'}
                            </Button>
                            <Button
                                variant="text"
                                fullWidth
                                size="small"
                                onClick={() => { setActiveStep(0); setOtp(''); setError(''); }}
                                sx={{ mt: 1, color: 'text.secondary' }}
                                id="back-to-email-btn"
                            >
                                ← Use a different email
                            </Button>
                        </Box>
                    )}
                </Paper>
            </Container>
        </Box>
    );
};

export default LoginPage;
