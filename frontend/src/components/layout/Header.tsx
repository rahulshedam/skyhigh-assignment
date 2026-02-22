import React from 'react';
import { AppBar, Toolbar, Typography, Container, Box, Button } from '@mui/material';
import FlightTakeoffIcon from '@mui/icons-material/FlightTakeoff';
import { useNavigate, useSearchParams } from 'react-router-dom';

const Header: React.FC = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const passengerId = searchParams.get('user');
    const waitlistPath = passengerId ? `/waitlist?user=${encodeURIComponent(passengerId)}` : '/waitlist';

    return (
        <AppBar position="static" color="primary" elevation={0}>
            <Container maxWidth="lg">
                <Toolbar disableGutters>
                    <Box
                        display="flex"
                        alignItems="center"
                        sx={{ cursor: 'pointer' }}
                        onClick={() => navigate('/')}
                    >
                        <FlightTakeoffIcon sx={{ fontSize: 32, mr: 1, color: 'secondary.main' }} />
                        <Typography
                            variant="h6"
                            noWrap
                            component="div"
                            sx={{
                                fontWeight: 700,
                                letterSpacing: '.1rem',
                                color: 'inherit',
                                textDecoration: 'none',
                            }}
                        >
                            SkyHigh Core
                        </Typography>
                    </Box>

                    <Box sx={{ flexGrow: 1 }} />

                    <Box sx={{ display: 'flex' }}>
                        <Button color="inherit" onClick={() => navigate('/')}>Home</Button>
                        <Button color="inherit" onClick={() => navigate(waitlistPath)}>My Waitlist</Button>
                    </Box>
                </Toolbar>
            </Container>
        </AppBar>
    );
};

export default Header;
