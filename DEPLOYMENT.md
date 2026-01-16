### Deployment to Railway

This project is prepared for deployment to [Railway](https://railway.app/) using Docker. Railway will automatically detect the `Dockerfile` in each service directory.

#### Step 1: Create a Railway Project
1. Go to [Railway.app](https://railway.app/) and create a new project.
2. Select **"Deploy from GitHub repo"** and connect your repository.

#### Step 2: Add PostgreSQL
1. In your Railway project, click **"New"** -> **"Database"** -> **"Add PostgreSQL"**.
2. Railway will automatically provide connection variables.

#### Step 3: Configure Environment Variables
To simplify the setup, you can use the `railway-env.json` file located in the root directory to bulk import variables.
1. Copy the contents of `railway-env.json`.
2. In your Railway service (Backend or Frontend), go to the **Variables** tab.
3. Click on **"Bulk Import"** or **"Raw Editor"**.
4. Paste the JSON and update the values for `FITBIT_CLIENT_ID`, `FITBIT_CLIENT_SECRET`, and `FITBIT_REDIRECT_URI`.

#### Step 4: Deploy Backend
1. Click **"New"** -> **"GitHub Repo"** -> Select your repo.
2. Go to the service **Settings**:
   - **Dockerfiles**: Railway should detect `backend/Dockerfile`. If not, specify it.
   - **Public Networking**: Enable it to get a public URL (e.g., `https://backend-production.up.railway.app`).
3. Go to the service **Variables**:
   - Use the variables mentioned in `railway-env.json`.

#### Step 5: Deploy Frontend
1. Click **"New"** -> **"GitHub Repo"** -> Select your repo again.
2. Go to the service **Settings**:
   - **Dockerfiles**: Railway should detect `frontend/Dockerfile`. If not, specify it.
   - **Public Networking**: Enable it.
3. Go to the service **Variables**:
   - `BACKEND_URL`: `http://backend.railway.internal:8080` (Railway's internal networking).

#### Step 6: Update Fitbit Application Settings
1. Go to the [Fitbit Developer Portal](https://dev.fitbit.com/apps).
2. Update the **"Callback URL"** to match your backend's public URL: `https://your-backend-url.railway.app/oauth/fitbit/callback`.

#### Local Testing with Docker

To test the Docker images locally (from the project root):

```bash
# Backend
docker build -f backend/Dockerfile -t fitdata-backend .
docker run -p 8080:8080 fitdata-backend

# Frontend
docker build -f frontend/Dockerfile -t fitdata-frontend .
docker run -p 80:80 -e BACKEND_URL=http://host.docker.internal:8080 fitdata-frontend
```
