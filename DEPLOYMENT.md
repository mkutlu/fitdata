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
   - **BACKEND_URL**: `http://backend-service-name.railway.internal:8080`
   - **VITE_API_BASE_URL** (Optional): `https://your-backend-public-url.up.railway.app`
   - **Important Difference**:
     - `BACKEND_URL` is used **internally** by Nginx (Server-to-Server). It should use the `.internal` HTTP address.
     - `VITE_API_BASE_URL` is used **by the browser** (Client-to-Server). If your site is HTTPS, this **MUST** be the public HTTPS address. 
     - **CRITICAL**: Do NOT include a port number like `:8080` in `VITE_API_BASE_URL` if you are using the public Railway HTTPS domain. Standard HTTPS uses port 443, and Railway handles this automatically.
     - **Incorrect**: `https://backend-production.up.railway.app:8080` (Will timeout)
     - **Correct**: `https://backend-production.up.railway.app`
   - If `VITE_API_BASE_URL` is not set, the app will use the Nginx proxy (safer default).

#### Step 4: Deploy Backend
1. Click **"New"** -> **"GitHub Repo"** -> Select your repo.
2. Go to the service **Settings**:
   - **Dockerfiles**: Railway should detect `backend/Dockerfile`. If not, specify it.
   - **Start Command**: Leave empty. Railway will use the `ENTRYPOINT` defined in the `Dockerfile`.
   - **Public Networking**: Enable it to get a public URL (e.g., `https://backend-production.up.railway.app`).
3. Go to the service **Variables**:
   - Ensure the variables from Step 3 are set.

#### Step 5: Deploy Frontend
1. Click **"New"** -> **"GitHub Repo"** -> Select your repo again.
2. Go to the service **Settings**:
   - **Dockerfiles**: Railway should detect `frontend/Dockerfile`. If not, specify it.
   - **Start Command**: Leave empty. Railway will use the `CMD` defined in the `Dockerfile`.
   - **Public Networking**: Enable it.
3. Go to the service **Variables**:
   - `BACKEND_URL`: `http://backend.railway.internal:8080` (Railway's internal networking for the backend service).
   - **Note**: Ensure the backend service has a domain/public URL if you are using it for `FITBIT_REDIRECT_URI`.

#### Troubleshooting 502 Errors

If you see a 502 Bad Gateway error on the frontend:
1. **Check Nginx Logs**: In Railway, check the logs for the frontend service. Look for any "emerg" or "error" messages.
2. **Verify `BACKEND_URL`**: Ensure `BACKEND_URL` is set correctly in the Frontend service variables. It should be `http://<backend-service-name>.railway.internal:8080`. **Do not include a trailing slash.**
3. **Check `PORT` Variable**: Railway automatically provides a `PORT` variable. Our Nginx configuration is set to use it. If you have manually set `PORT` to something else, ensure it doesn't conflict.
4. **Check Backend Status**: Ensure the backend service is running and healthy. If the backend is down, Nginx will return a 502 when trying to access `/api/` or `/oauth/`.
5. **Home Screen 502**: If the home screen itself (not an API call) returns 502, it usually means Nginx failed to start or Railway cannot reach it. Check if the frontend container is crashing.
6. **Internal DNS**: Railway sometimes takes a moment to propagate internal DNS. If the backend was just deployed, wait a minute and refresh.
7. **Private Network**: Ensure both services are in the same Railway project to use internal networking.

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
