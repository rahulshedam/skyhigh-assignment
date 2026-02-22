# Frontend Application

## Overview
React-based frontend application for SkyHigh Airlines digital check-in system, providing a seamless user interface for passengers to check-in, select seats, and manage their bookings.

## Technology Stack
- **Framework**: React 18
- **Language**: TypeScript 5
- **State Management**: Redux Toolkit
- **UI Library**: Material-UI (MUI) 5
- **HTTP Client**: Axios
- **Build Tool**: Vite
- **Testing**: Jest + React Testing Library

## Port
3000

## Key Features
- ✅ Interactive seat map visualization
- ✅ Real-time seat availability updates
- ✅ Multi-step check-in flow
- ✅ Baggage and payment forms
- ✅ Responsive design
- ✅ Error handling with toast notifications

## Project Structure
```
src/
├── components/       # Reusable UI components
├── pages/           # Page components
├── store/           # Redux store and slices
├── api/             # API client services
├── hooks/           # Custom React hooks
├── types/           # TypeScript type definitions
├── utils/           # Utility functions
└── styles/          # Global styles
```

## Installation

### Prerequisites
- Node.js 18+
- npm or yarn

### Install Dependencies
```bash
npm install
```

## Running the Application

### Development Mode
```bash
npm run dev
```
Application will be available at `http://localhost:3000`

### Production Build
```bash
npm run build
npm run preview
```

### Docker
```bash
docker build -t frontend-app .
docker run -p 3000:3000 frontend-app
```

## Testing
```bash
# Run unit tests
npm test

# Run tests with coverage
npm run test:coverage

# Run E2E tests
npm run test:e2e
```

## Environment Variables
Create `.env.development` file:
```env
VITE_API_BASE_URL=http://localhost:8091
VITE_SEAT_SERVICE_URL=http://localhost:8091
VITE_CHECKIN_SERVICE_URL=http://localhost:8082
VITE_BAGGAGE_SERVICE_URL=http://localhost:8083
VITE_PAYMENT_SERVICE_URL=http://localhost:8084
```

## Key Pages
- **Home**: Landing page with flight search
- **Seat Selection**: Interactive seat map
- **Check-in**: Multi-step check-in wizard
- **Confirmation**: Booking confirmation
- **Waitlist**: Waitlist management

## API Integration
The frontend communicates with the following backend services:
- Seat Management Service (8091)
- Check-in Service (8082)
- Baggage Service (8083)
- Payment Service (8084)

## Code Standards
- **ESLint**: Code linting
- **Prettier**: Code formatting
- **TypeScript**: Strict type checking

## Available Scripts
- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm test` - Run tests
- `npm run lint` - Run ESLint
- `npm run format` - Format code with Prettier
