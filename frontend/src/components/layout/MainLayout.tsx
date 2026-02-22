import React from 'react';
import { Box, Container, CssBaseline, ThemeProvider } from '@mui/material';
import { Outlet } from 'react-router-dom';
import Header from './Header';
import theme from '../../styles/theme';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

const MainLayout: React.FC = () => {
    return (
        <ThemeProvider theme={theme}>
            <CssBaseline />
            <Box
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    minHeight: '100vh',
                    bgcolor: 'background.default'
                }}
            >
                <Header />
                <Container
                    component="main"
                    maxWidth="lg"
                    sx={{
                        flexGrow: 1,
                        py: 4,
                        display: 'flex',
                        flexDirection: 'column'
                    }}
                >
                    <Outlet />
                </Container>
                <Box
                    component="footer"
                    sx={{
                        py: 3,
                        px: 2,
                        mt: 'auto',
                        textAlign: 'center',
                        bgcolor: 'primary.dark',
                        color: 'primary.contrastText'
                    }}
                >
                    <Typography variant="body2">
                        © {new Date().getFullYear()} SkyHigh Airlines. All rights reserved.
                    </Typography>
                </Box>
            </Box>
            <ToastContainer position="bottom-right" />
        </ThemeProvider>
    );
};

// Internal Typography import fix for the footer
import { Typography } from '@mui/material';

export default MainLayout;
