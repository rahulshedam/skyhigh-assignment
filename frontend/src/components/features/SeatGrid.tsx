import React from 'react';
import {
    Box,
    Grid,
    Paper,
    Typography,
    Tooltip,
    IconButton,
    alpha,
    useTheme
} from '@mui/material';
import EventSeatIcon from '@mui/icons-material/EventSeat';
import { Seat } from '../../types';

interface SeatGridProps {
    seats: Seat[];
    onSeatSelect: (seat: Seat) => void;
    selectedSeatId: number | null;
}

const SeatGrid: React.FC<SeatGridProps> = ({ seats, onSeatSelect, selectedSeatId }) => {
    const theme = useTheme();

    // Group seats by rows
    // Assuming seatNumber format like "12A", "1B", etc.
    const rows: { [key: string]: Seat[] } = {};
    seats.forEach(seat => {
        const rowNum = seat.seatNumber.replace(/[A-Z]/, '');
        if (!rows[rowNum]) rows[rowNum] = [];
        rows[rowNum].push(seat);
    });

    // Sort rows and seats within rows
    const sortedRowKeys = Object.keys(rows).sort((a, b) => parseInt(a) - parseInt(b));

    const getSeatColor = (seat: Seat) => {
        if (selectedSeatId === seat.id) return theme.palette.secondary.main;
        switch (seat.status) {
            case 'AVAILABLE': return theme.palette.success.light;
            case 'HELD': return theme.palette.warning.main;
            case 'CONFIRMED': return theme.palette.error.light;
            default: return theme.palette.grey[400];
        }
    };

    return (
        <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <Box sx={{
                width: '100%',
                maxWidth: 600,
                bgcolor: 'background.paper',
                borderRadius: '50px 50px 10px 10px',
                border: '3px solid',
                borderColor: 'divider',
                p: 4,
                position: 'relative',
                overflow: 'hidden'
            }}>
                {/* Cockpit area simulation */}
                <Box sx={{
                    height: 100,
                    width: '100%',
                    display: 'flex',
                    justifyContent: 'center',
                    alignItems: 'center',
                    mb: 4,
                    borderBottom: '1px dashed',
                    borderColor: 'divider'
                }}>
                    <Typography variant="h6" color="text.secondary">COCKPIT</Typography>
                </Box>

                {sortedRowKeys.map(rowKey => (
                    <Box key={rowKey} sx={{ display: 'flex', justifyContent: 'center', mb: 2, alignItems: 'center' }}>
                        <Typography variant="caption" sx={{ width: 30, textAlign: 'center', color: 'text.secondary', fontWeight: 700 }}>
                            {rowKey}
                        </Typography>

                        <Box sx={{ display: 'flex', gap: 1 }}>
                            {rows[rowKey].sort((a, b) => a.seatNumber.localeCompare(b.seatNumber)).map((seat, index) => (
                                <React.Fragment key={seat.id}>
                                    {/* Add an aisle after the middle seat (assuming 6-across for now) */}
                                    {index === 3 && <Box sx={{ width: 40 }} />}

                                    <Tooltip title={`${seat.seatNumber} - ${seat.status}`}>
                                        <IconButton
                                            disabled={seat.status !== 'AVAILABLE' && selectedSeatId !== seat.id}
                                            onClick={() => onSeatSelect(seat)}
                                            sx={{
                                                color: getSeatColor(seat),
                                                bgcolor: selectedSeatId === seat.id ? alpha(theme.palette.secondary.main, 0.1) : 'transparent',
                                                '&:hover': {
                                                    bgcolor: alpha(getSeatColor(seat), 0.1),
                                                }
                                            }}
                                        >
                                            <EventSeatIcon />
                                        </IconButton>
                                    </Tooltip>
                                </React.Fragment>
                            ))}
                        </Box>
                    </Box>
                ))}
            </Box>

            {/* Legend */}
            <Box sx={{ mt: 4, display: 'flex', gap: 3, flexWrap: 'wrap', justifyContent: 'center' }}>
                {[
                    { label: 'Available', color: theme.palette.success.light },
                    { label: 'Selected', color: theme.palette.secondary.main },
                    { label: 'Held/Occupied', color: theme.palette.warning.main },
                    { label: 'Confirmed', color: theme.palette.error.light },
                ].map(item => (
                    <Box key={item.label} sx={{ display: 'flex', alignItems: 'center gap: 1' }}>
                        <Box sx={{ width: 16, height: 16, bgcolor: item.color, borderRadius: '2px', mr: 1 }} />
                        <Typography variant="body2">{item.label}</Typography>
                    </Box>
                ))}
            </Box>
        </Box>
    );
};

export default SeatGrid;
