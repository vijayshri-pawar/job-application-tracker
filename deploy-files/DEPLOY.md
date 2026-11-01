# Deploying Job Application Tracker

The app runs on **Render** (free web service, Docker) with a **Neon** PostgreSQL database (free, does not expire).

## 1. Create the database on Neon

1. Sign up at https://neon.com (you can use your GitHub account; no credit card needed).
2. Create a project:
   - Name: `job-tracker`
   - Region: **Asia Pacific (Singapore)** (closest to India, and the same region as the app on Render)
3. On the project dashboard, click **Connect**.
4. **Turn off "Connection pooling"**, so the host name has no `-pooler` in it.
5. Note down these values:
   - **Host**, for example `ep-cool-name-123456.ap-southeast-1.aws.neon.tech`
   - **Database**, usually `neondb`
   - **Role** (user name), for example `neondb_owner`
   - **Password**

## 2. Create a JWT secret

In Windows PowerShell, run:

```powershell
$b = New-Object byte[] 48; [Security.Cryptography.RNGCryptoServiceProvider]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

Copy the 64-character result. Never put it in the code or on GitHub.

## 3. Create the web service on Render

1. Sign up at https://render.com with your GitHub account.
2. Click **New → Web Service** and select the `job-application-tracker` repository.
3. Settings:
   - **Name:** `job-application-tracker` (this becomes part of the URL)
   - **Language:** Docker
   - **Branch:** main
   - **Region:** Singapore
   - **Instance type:** Free
4. Under **Environment Variables**, add:

| Key | Value |
|---|---|
| `DB_URL` | `jdbc:postgresql://YOUR-NEON-HOST/neondb?sslmode=require` |
| `DB_USERNAME` | your Neon role, e.g. `neondb_owner` |
| `DB_PASSWORD` | your Neon password |
| `JWT_SECRET` | the secret from step 2 |

   Note: `DB_URL` must start with `jdbc:postgresql://`, not just `postgresql://`.

5. Click **Deploy Web Service**. The first build takes about 5–10 minutes.
6. When the log shows `Started JobTrackerBackendApplication`, open the URL Render shows,
   e.g. `https://job-application-tracker.onrender.com`.

The live database starts empty, so sign up with a new account there.

## Good to know

- **Sleep on the free plan:** the app sleeps after 15 minutes without visitors. The next visit wakes it up,
  which takes about a minute. Open the link yourself a minute before you show it to someone.
- **Automatic deploys:** every `git push` to `main` redeploys the app.
- **Logs:** Render dashboard → your service → **Logs**. Check here first if something goes wrong.

## Troubleshooting

| Log message | Fix |
|---|---|
| `password authentication failed` | Check `DB_USERNAME` and `DB_PASSWORD` against the Neon dashboard |
| `Connection refused` / `UnknownHostException` | Check `DB_URL`: it must start with `jdbc:postgresql://` and contain the right host |
| `app.jwt.secret ... at least 32 characters` | Set `JWT_SECRET` to the 64-character value from step 2 |
| `No open ports detected` | Wait for the build to finish; the app takes about a minute to start |
