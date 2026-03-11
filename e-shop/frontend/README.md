# E-Shop React Frontend

Simple React frontend for the E-Shopping Case Study API. Uses Vite, React Router, and a minimal auth context.

## Run locally

1. Install dependencies:
   ```bash
   cd frontend
   npm install
   ```

2. Start the backend (Eureka, gateway, and services) so the API is available at `http://localhost:9090`.

3. Start the frontend:
   ```bash
   npm run dev
   ```
   Opens at `http://localhost:3000`. API calls go to `/api` and are proxied to the gateway.

## Scripts

- `npm run dev` – development server (port 3000)
- `npm run build` – production build
- `npm run preview` – preview production build

## Design

- **Home**: Hero with soft beige background and subtle floating animation (inspired by product hero layouts).
- **Shop**: Left sidebar filters (category, price range, in stock) and product grid with active filter pills and sort (inspired by shop filter UIs).

See **REACT-IMPLEMENTATION-GUIDE.md** in the project root for a learning-focused walkthrough of how the React app is built.
