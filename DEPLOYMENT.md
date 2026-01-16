### Deployment to Railway

This project is prepared for deployment to [Railway](https://railway.app/) using Docker. Railway will automatically detect the `Dockerfile` in each service directory.

#### Step 1: Create a Railway Project
1. Go to [Railway.app](https://railway.app/) and create a new project.
2. Select **"Deploy from GitHub repo"** and connect your repository.

#### Step 2: Add PostgreSQL
1. In your Railway project, click **"New"** -> **"Database"** -> **"Add PostgreSQL"**.
2. Railway will automatically provide connection variables.

#### Step 3: Deploy Backend
1. Click **"New"** -> **"GitHub Repo"** -> Select your repo.
2. Go to the service **Settings**:
   - **Root Directory**: Set to `/backend`.
   - **Public Networking**: Enable it to get a public URL (e.g., `https://backend-production.up.railway.app`).
3. Go to the service **Variables**:
   - `DB_URL`: `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}`
   - `DB_USER`: `${{Postgres.PGUSER}}`
   - `DB_PASS`: `${{Postgres.PGPASSWORD}}`
   - `FITBIT_CLIENT_ID`: (Your Fitbit API Client ID)
   - `FITBIT_CLIENT_SECRET`: (Your Fitbit API Client Secret)
   - `FITBIT_REDIRECT_URI`: `https://your-backend-url.railway.app/oauth/fitbit/callback` (Update this after you have the backend URL)
   - `SPRING_PROFILES_ACTIVE`: `prod` (optional)

#### Step 4: Deploy Frontend
1. Click **"New"** -> **"GitHub Repo"** -> Select your repo again.
2. Go to the service **Settings**:
   - **Root Directory**: Set to `/frontend`.
   - **Public Networking**: Enable it.
3. Go to the service **Variables**:
   - `BACKEND_URL`: `http://backend.railway.internal:8080` (Railway's internal networking is faster and more secure). 
     *Note: If you use the public URL, use `https://...`*

#### Step 5: Update Fitbit Application Settings
1. Go to the [Fitbit Developer Portal](https://dev.fitbit.com/apps).
2. Update the **"Callback URL"** to match your backend's public URL: `https://your-backend-url.railway.app/oauth/fitbit/callback`.

#### Local Testing with Docker

To test the Docker images locally:

```bash
# Backend
cd backend
docker build -t fitdata-backend .
docker run -p 8080:8080 fitdata-backend

# Frontend
cd frontend
docker build -t fitdata-frontend .
docker run -p 80:80 -e BACKEND_URL=http://host.docker.internal:8080 fitdata-frontend
```
