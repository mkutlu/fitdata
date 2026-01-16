### Deployment to Railway

This project is prepared for deployment to [Railway](https://railway.com/) using Docker.

#### Services

1.  **PostgreSQL**: Use the Railway PostgreSQL plugin.
2.  **Backend**: 
    - Build from `/backend` directory.
    - Environment Variables:
        - `DB_URL`: Railway PostgreSQL URL.
        - `DB_USER`: Railway PostgreSQL User.
        - `DB_PASS`: Railway PostgreSQL Password.
        - `FITBIT_CLIENT_ID`: Your Fitbit API Client ID.
        - `FITBIT_CLIENT_SECRET`: Your Fitbit API Client Secret.
        - `FITBIT_REDIRECT_URI`: `https://your-backend-url.railway.app/oauth/fitbit/callback`
3.  **Frontend**:
    - Build from `/frontend` directory.
    - Environment Variables:
        - `BACKEND_URL`: The internal or public URL of your backend service (e.g., `http://backend.railway.internal:8080`).

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
