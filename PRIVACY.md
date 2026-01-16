# Privacy Policy

Fitdata is committed to protecting your privacy. This policy explains how we handle your data.

### 1. Data Collection
Fitdata does not collect or store your personal data on any external servers under our control. All data is fetched directly from your Fitbit account using the official Fitbit API.

### 2. Data Usage
- **Authentication**: We use OAuth 2.0 with PKCE to securely authenticate with Fitbit. Your login credentials are never shared with or stored by Fitdata.
- **Health Metrics**: We access metrics such as heart rate, activity, sleep, and weight solely to display them on your dashboard.
- **Readiness Estimation**: We use your RHR, HRV, and sleep data to calculate an estimated readiness score. This calculation happens within the application environment.

### 3. Data Storage
- **Local Cache**: For performance, we may temporarily store session information (like access tokens) in your local environment's database (H2) or browser storage.
- **Production Database**: In production (e.g., Railway), encrypted tokens are stored in a private PostgreSQL database to maintain your session.
- **No Third-Party Sharing**: Your health data is never shared with, sold to, or accessible by any third parties.

### 4. User Control
You can revoke Fitdata's access to your data at any time through your Fitbit account settings or by using the "Logout" feature within the app, which deletes your stored session tokens.

### 5. Contact
For any privacy-related questions, please contact the repository owner.