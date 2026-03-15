import React from 'react';
import { AppBar, Toolbar, Typography, Container, Box, Button, Avatar, Tooltip } from '@mui/material';
import FlightTakeoffIcon from '@mui/icons-material/FlightTakeoff';
import LogoutIcon from '@mui/icons-material/Logout';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../store';
import { logout } from '../../store/slices/authSlice';

const Header: React.FC = () => {
    const navigate = useNavigate();
    const dispatch = useAppDispatch();
    const { email, isAuthenticated } = useAppSelector((state) => state.auth);

    const handleLogout = () => {
        dispatch(logout());
        navigate('/login');
    };

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

                    {isAuthenticated && (
                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                            <Button color="inherit" onClick={() => navigate('/')}>Home</Button>
                            <Button color="inherit" onClick={() => navigate('/waitlist')}>My Waitlist</Button>
                            
                            <Box sx={{ ml: 2, display: 'flex', alignItems: 'center', borderLeft: '1px solid rgba(255,255,255,0.3)', pl: 2 }}>
                                <Tooltip title={email || 'User'}>
                                    <Avatar sx={{ width: 32, height: 32, bgcolor: 'secondary.main', fontSize: '0.875rem', mr: 1 }}>
                                        {email ? email[0].toUpperCase() : 'U'}
                                    </Avatar>
                                </Tooltip>
                                <Button 
                                    color="inherit" 
                                    onClick={handleLogout}
                                    startIcon={<LogoutIcon />}
                                    size="small"
                                >
                                    Logout
                                </Button>
                            </Box>
                        </Box>
                    )}
                </Toolbar>
            </Container>
        </AppBar>
    );
};

export default Header;
