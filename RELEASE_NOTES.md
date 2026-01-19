# Release Notes - Fitdata Dashboard

Welcome to the official release of **Fitdata**, a professional-grade dashboard for your Fitbit health data. This release marks a significant milestone in providing a performant, secure, and intuitive way to visualize your health metrics.

---

### 🚀 Key Features

#### 1. Professional Architecture (Nginx + Java/Spring Boot)
- **Decoupled Architecture**: Unlike simple Node.js apps, Fitdata uses **Nginx** for the frontend and **Java 23 (Spring Boot)** for the backend. This ensures high stability, low memory usage, and enterprise-grade security.
- **Internal Networking**: Optimized for cloud platforms like **Railway**, using internal DNS for fast and secure service-to-service communication.

#### 2. Advanced Health Insights
- **Intelligent Readiness Score**: A custom algorithm that estimates your daily readiness based on Resting Heart Rate (RHR) trends, Sleep Score, Activity Load, and Heart Rate Variability (HRV).
- **Dynamic Exercise Tracking**: New "Exercise Days" tracking that shows your activity progress for the **current week** (starting Monday).
- **Interactive Heart Rate UI**: A compact, interactive card that lets you toggle between a live intraday heart rate chart and a detailed heart rate zone distribution.
- **Comprehensive Metrics**: Integrated tracking for Steps, Weight, and Sleep patterns with historical range views.

#### 3. Modern Dashboard Experience
- **Responsive Layout**: A fully customizable and draggable grid system that adapts to your screen size.
- **Smart Date Management**: Seamlessly navigate through your health history with an intuitive date selector.
- **Live UI Updates**: All charts and metrics (including the circular Readiness gauge) are now fully responsive and scale beautifully as you resize your browser or dashboard cards.

#### 4. Security & Privacy
- **OAuth 2.0 Integration**: Secure connection to your Fitbit account using industry-standard PKCE (Proof Key for Code Exchange) flow.
- **Enhanced Session Handling**: Optimized cookie management for HTTPS/SSL environments, ensuring your session remains secure and stable across different domains.

---

### 🛠 Improvements & Bug Fixes

- **Resilient Proxying**: Updated Nginx configuration with automatic trailing-slash handling and runtime DNS resolution for maximum uptime.
- **Performance Optimization**: Increased proxy buffer sizes and timeouts to handle large data transfers from the Fitbit API smoothly.
- **Cookie Security**: Implementation of `SameSite=None` and `Secure` attributes for stable authentication in cloud-hosted environments.
- **Auto-Cleanup**: Added intelligent token management that automatically cleans up invalid or expired credentials, preventing authentication loops.

---

### 📦 Deployment Information

- **Dockerized**: Full multi-stage Docker support for both frontend and backend.
- **Cloud Ready**: Pre-configured environment templates (`railway-env.json`) and comprehensive `DEPLOYMENT.md` instructions for one-click deployment to Railway.
- **Local Development**: Enhanced fallback configurations for local `dev` profile support over HTTP.

---

*Thank you for using Fitdata. Your health, visualized.*
