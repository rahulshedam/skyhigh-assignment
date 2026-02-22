import React from 'react';
import { Box, Typography, Button, Container } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import FlightLandIcon from '@mui/icons-material/FlightLand';

const NotFoundPage: React.FC = () => {
    const navigate = useNavigate();

    return (
        <Container maxWidth="sm">
            <Box
                sx={{
                    mt: 10,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    textAlign: 'center'
                }}
            >
                <FlightLandIcon sx={{ fontSize: 120, color: 'primary.light', mb: 4 }} />
                <Typography variant="h2" gutterBottom>404</Typography>
                <Typography variant="h5" color="text.secondary" paragraph>
                    Oops! It seems you've landed on a non-existent runway.
                </Typography>
                <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
                    The page you are looking for might have been moved or deleted.
                </Typography>
                <Button
                    variant="contained"
                    color="primary"
                    size="large"
                    onClick={() => navigate('/')}
                >
                    Back to Terminal (Home)
                </Button>
            </Box>
        </Container>
    );
};

export default NotFoundPage;
