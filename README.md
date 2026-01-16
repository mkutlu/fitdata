# Fitdata Dashboard

Fitdata is a modern health data visualization dashboard designed to provide deep insights into your physical activity, sleep, and cardiovascular health. By connecting directly to your Fitbit account, it offers a personalized overview of your wellness metrics through an intuitive and responsive interface.

## Key Features

- **Daily Readiness Score**: Get an estimated readiness score based on your Resting Heart Rate (RHR), Sleep Quality, Activity Load, and Heart Rate Variability (HRV).
- **Weekly Exercise Tracking**: Monitor your consistency with a "Current Week" view of your exercise frequency.
- **Advanced Heart Rate Analytics**:
    - **Intraday Tracking**: High-resolution view of your heart rate throughout the day.
    - **Zone Analysis**: Detailed breakdown of time spent in Fat Burn, Cardio, and Peak zones.
    - **Energy Expenditure**: Track calories burned and activity intensity.
- **Sleep Quality Insights**: Visualize sleep stages and overall sleep efficiency.
- **Activity & Weight Metrics**: Comprehensive charts for steps and body weight trends with flexible time ranges.
- **Responsive Dashboard**: A fully customizable and responsive grid layout that adapts to any screen size.

## Tech Stack

### Backend
- **Java 23** & **Spring Boot 4**
- **Spring Security** with OAuth2 & PKCE for secure Fitbit authentication
- **PostgreSQL** for persistent storage (Production) / **H2** (Local Development)
- **Flyway** for database migrations
- **Spring WebFlux** for non-blocking API communication with Fitbit

### Frontend
- **React** with **TypeScript**
- **Vite** for optimized building
- **TailwindCSS** for a modern, dark-themed UI
- **Recharts** for high-performance data visualization
- **React Grid Layout** for a customizable dashboard experience
- **Nginx** for efficient production serving

## Getting Started

### Prerequisites
- Java 23
- Node.js 22+
- A Fitbit Developer Account (to get Client ID/Secret)

### Local Development

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-repo/fitdata.git
   ```

2. **Backend Setup**:
   - Navigate to `backend`
   - Set environment variables: `FITBIT_CLIENT_ID`, `FITBIT_CLIENT_SECRET`, `FITBIT_REDIRECT_URI` (http://localhost:8080/oauth/fitbit/callback)
   - Run `./mvnw spring-boot:run`

3. **Frontend Setup**:
   - Navigate to `frontend`
   - Run `npm install`
   - Run `npm run dev`

### Deployment
This project is fully prepared for deployment on **Railway** using the included `Dockerfile` and `railway-env.json`. See [DEPLOYMENT.md](DEPLOYMENT.md) for detailed instructions.

## License
This project is for personal use and data visualization. See [PRIVACY.md](PRIVACY.md) and [TERMS.md](TERMS.md) for more details.
