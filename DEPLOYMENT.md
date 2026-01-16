### Deployment to Railway

This project is prepared for deployment to [Railway](https://railway.app/) using Docker. Railway will automatically detect the `Dockerfile` in each service directory.

#### Step 1: Create a Railway Project
1. Go to [Railway.app](https://railway.app/) and create a new project.
2. Select **"Deploy from GitHub repo"** and connect your repository.

#### Step 2: Add PostgreSQL
1. In your Railway project, click **"New"** -> **"Database"** -> **"Add PostgreSQL"**.
2. Railway will automatically provide connection variables.

#### Step 3: Configure Environment Variables

Each service (Backend and Frontend) requires specific environment variables. You can use the `railway-env.json` file as a reference.

**For the Backend Service:**
1. Go to the **Variables** tab of your Backend service.
2. Use **"Bulk Import"** or **"Raw Editor"** and paste the relevant variables from `railway-env.json`:
   - `FITBIT_CLIENT_ID`
   - `FITBIT_CLIENT_SECRET`
   - `FITBIT_REDIRECT_URI` (Use your backend's public URL)
   - `DB_URL`, `DB_USER`, `DB_PASS` (Using Railway's Postgres variables)
   - `SPRING_PROFILES_ACTIVE`: `prod`

**For the Frontend Service:**
1. Go to the **Variables** tab of your Frontend service.
2. Add the following variable:
   - `BACKEND_URL`: `http://backend-service-name.railway.internal:8080`
   - **Important**: Do NOT include a trailing slash in the URL (e.g., use `...:8080`, not `...:8080/`).
   - Replace `backend-service-name` with your actual backend service name in Railway.

#### Step 4: Deploy Backend
1. Click **"New"** -> **"GitHub Repo"** -> Select your repo.
2. Go to the service **Settings**:
   - **Dockerfiles**: Railway should detect `backend/Dockerfile`. If not, specify it.
   - **Public Networking**: Enable it to get a public URL (e.g., `https://backend-production.up.railway.app`).
3. Go to the service **Variables**:
   - Ensure the variables from Step 3 are set.

#### Step 5: Deploy Frontend
1. Click **"New"** -> **"GitHub Repo"** -> Select your repo again.
2. Go to the service **Settings**:
   - **Dockerfiles**: Railway should detect `frontend/Dockerfile`. If not, specify it.
   - **Public Networking**: Enable it.
3. Go to the service **Variables**:
   - `BACKEND_URL`: `http://backend.railway.internal:8080` (Railway's internal networking for the backend service).
   - **Note**: Ensure the backend service has a domain/public URL if you are using it for `FITBIT_REDIRECT_URI`.

#### Troubleshooting 502 Errors

If you see a 502 Bad Gateway error on the frontend:
1. **Check Backend Status**: Ensure the backend service is running and healthy. Check its logs for any startup errors (e.g., database connection issues).
2. **Verify `BACKEND_URL`**: Double-check that the `BACKEND_URL` in the Frontend service exactly matches the internal URL of your backend. It should be `http://<backend-service-name>.railway.internal:8080`.
3. **Internal DNS**: Railway sometimes takes a moment to propagate internal DNS. If the backend was just deployed, wait a minute and refresh.
4. **Port Mapping**: Ensure your backend is listening on the port specified in `BACKEND_URL` (default is 8080).
5. **Private Network**: Ensure both services are in the same Railway project to use internal networking.
6. **Health Check**: You can verify if the frontend container is healthy by checking the `/health` endpoint on its domain.

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
