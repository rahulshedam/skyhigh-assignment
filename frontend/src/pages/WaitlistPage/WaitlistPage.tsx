import React, { useEffect, useState } from 'react';
import {
    Box,
    Typography,
    Paper,
    Button,
    Container,
    List,
    ListItem,
    ListItemText,
    ListItemIcon,
    Chip,
    Divider,
    TextField,
    CircularProgress,
    Alert
} from '@mui/material';
import { useNavigate, useSearchParams } from 'react-router-dom';
import HourglassEmptyIcon from '@mui/icons-material/HourglassEmpty';
import EventSeatIcon from '@mui/icons-material/EventSeat';
import { WaitlistService } from '../../api/services';
import { SeatService } from '../../api/services';
import { Waitlist } from '../../types';

type WaitlistEntryDisplay = {
    id: number;
    seatNumber: string;
    flightNumber: string;
    flightId: number;
    status: string;
    joinedAt: string;
};

const formatFlightNumber = (flightId: number) => `SK${String(flightId).padStart(4, '0')}`;

const WaitlistPage: React.FC = () => {
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const passengerIdParam = searchParams.get('user') || '';

    const [passengerIdInput, setPassengerIdInput] = useState(passengerIdParam);
    const [waitlistEntries, setWaitlistEntries] = useState<WaitlistEntryDisplay[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (passengerIdParam) {
            setPassengerIdInput(passengerIdParam);
            fetchWaitlist(passengerIdParam);
        } else {
            setWaitlistEntries([]);
        }
    }, [passengerIdParam]);

    const fetchWaitlist = async (passengerId: string) => {
        setLoading(true);
        setError(null);
        try {
            const waitlists: Waitlist[] = await WaitlistService.getPassengerWaitlist(passengerId);
            const enriched = await Promise.all(
                waitlists.map(async (w) => {
                    try {
                        const seatStatus = await SeatService.getSeatStatus(w.seatId);
                        return {
                            id: w.id,
                            seatNumber: seatStatus.seatNumber,
                            flightNumber: formatFlightNumber(seatStatus.flightId),
                            flightId: seatStatus.flightId,
                            status: w.status,
                            joinedAt: w.joinedAt,
                        } as WaitlistEntryDisplay;
                    } catch {
                        return {
                            id: w.id,
                            seatNumber: `Seat #${w.seatId}`,
                            flightNumber: '—',
                            flightId: 0,
                            status: w.status,
                            joinedAt: w.joinedAt,
                        } as WaitlistEntryDisplay;
                    }
                })
            );
            setWaitlistEntries(enriched);
        } catch (err: unknown) {
            const message = err && typeof err === 'object' && 'response' in err
                ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
                : 'Unable to load waitlist.';
            setError(message || 'Unable to load waitlist.');
            setWaitlistEntries([]);
        } finally {
            setLoading(false);
        }
    };

    const handleViewWaitlist = (e: React.FormEvent) => {
        e.preventDefault();
        const trimmed = passengerIdInput.trim();
        if (trimmed) {
            setSearchParams({ user: trimmed });
        }
    };

    return (
        <Container maxWidth="md">
            <Box sx={{ mt: 4, mb: 4 }}>
                <Typography variant="h4" gutterBottom>My Waitlist</Typography>
                <Typography color="text.secondary">
                    Track your waitlist status for preferred seats. You will be notified once a seat becomes available.
                </Typography>
            </Box>

            {!passengerIdParam ? (
                <Paper elevation={0} sx={{ p: 4, borderRadius: 4, border: '1px solid', borderColor: 'divider' }}>
                    <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
                        Enter your Passenger ID (email or last name) to view your waitlist entries.
                    </Typography>
                    <form onSubmit={handleViewWaitlist}>
                        <Box display="flex" gap={2} alignItems="flex-start" flexWrap="wrap">
                            <TextField
                                fullWidth
                                label="Passenger ID"
                                placeholder="e.g. john@email.com or Smith"
                                value={passengerIdInput}
                                onChange={(e) => setPassengerIdInput(e.target.value)}
                                variant="outlined"
                                sx={{ flex: { xs: 1, sm: 2 }, minWidth: 200 }}
                            />
                            <Button type="submit" variant="contained" disabled={!passengerIdInput.trim()}>
                                View Waitlist
                            </Button>
                        </Box>
                    </form>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                        You can also add <code>?user=your-passenger-id</code> to the URL to view your waitlist directly.
                    </Typography>
                </Paper>
            ) : (
                <>
                    <Paper elevation={0} sx={{ borderRadius: 4, border: '1px solid divider', overflow: 'hidden', mb: 2 }}>
                        <Box sx={{ px: 4, py: 2, borderBottom: '1px solid', borderColor: 'divider', display: 'flex', alignItems: 'center', gap: 2 }}>
                            <Typography variant="body2" color="text.secondary">
                                Viewing waitlist for: <strong>{passengerIdParam}</strong>
                            </Typography>
                            <Button
                                size="small"
                                variant="outlined"
                                onClick={() => {
                                    setSearchParams({});
                                    setPassengerIdInput('');
                                    setWaitlistEntries([]);
                                }}
                            >
                                Change passenger
                            </Button>
                        </Box>
                        {loading ? (
                            <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
                                <CircularProgress />
                            </Box>
                        ) : error ? (
                            <Box sx={{ p: 4 }}>
                                <Alert severity="error" onClose={() => setError(null)}>{error}</Alert>
                            </Box>
                        ) : (
                            <List disablePadding>
                                {waitlistEntries.map((entry, index) => (
                                    <React.Fragment key={entry.id}>
                                        <ListItem
                                            sx={{
                                                py: 3,
                                                px: 4,
                                                '&:hover': { bgcolor: 'action.hover' }
                                            }}
                                        >
                                            <ListItemIcon>
                                                <EventSeatIcon color="primary" />
                                            </ListItemIcon>
                                            <ListItemText
                                                primary={
                                                    <Box display="flex" alignItems="center" gap={1}>
                                                        <Typography variant="h6">Seat {entry.seatNumber}</Typography>
                                                        <Typography color="text.secondary" variant="body2">• {entry.flightNumber}</Typography>
                                                    </Box>
                                                }
                                                secondary={`Joined: ${new Date(entry.joinedAt).toLocaleString()}`}
                                            />
                                            <Box>
                                                <Chip
                                                    label={entry.status}
                                                    color={entry.status === 'ASSIGNED' ? 'success' : 'warning'}
                                                    icon={entry.status === 'WAITING' ? <HourglassEmptyIcon /> : undefined}
                                                    sx={{ fontWeight: 600 }}
                                                />
                                                {entry.status === 'ASSIGNED' && entry.flightId > 0 && (
                                                    <Button
                                                        variant="contained"
                                                        size="small"
                                                        sx={{ ml: 2 }}
                                                        onClick={() => navigate(`/seats/${entry.flightId}`)}
                                                    >
                                                        Check-in Now
                                                    </Button>
                                                )}
                                            </Box>
                                        </ListItem>
                                        {index < waitlistEntries.length - 1 && <Divider />}
                                    </React.Fragment>
                                ))}
                            </List>
                        )}
                    </Paper>

                    {!loading && !error && waitlistEntries.length === 0 && passengerIdParam && (
                        <Box sx={{ textAlign: 'center', py: 8 }}>
                            <Typography color="text.secondary">You are not currently on any waitlists.</Typography>
                            <Button variant="outlined" sx={{ mt: 2 }} onClick={() => navigate('/')}>
                                Search Flights
                            </Button>
                        </Box>
                    )}
                </>
            )}
        </Container>
    );
};

export default WaitlistPage;
