# 🚀 Complete Microservices Deployment Guide (Oracle Cloud Always Free VM)

This guide walks you through deploying the complete Success Academy School Management System microservices stack using **Docker Compose** on an **Oracle Cloud Always Free VM** (or any Ubuntu/Debian Linux VPS) with zero monthly cost ($0/month).

---

## 🏗️ 1. Architecture Overview

```
                        User Browser / Mobile
                                 │
                                 ▼
                     ┌───────────────────────┐
                     │   Vercel Frontend     │
                     │  (React 18 + Vite)    │
                     └───────────┬───────────┘
                                 │ HTTPS (VITE_API_URL)
                                 ▼
┌────────────────────────────────────────────────────────────────────────┐
│ Oracle Cloud Always Free VM (4 OCPUs, 24 GB RAM, 200 GB Storage)       │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │                      Spring Cloud Gateway                      │   │
│   │                    (Port 9000, Ingress Proxy)                  │   │
│   └──────┬─────────────────────┬────────────────────┬──────────────┘   │
│          │                     │                    │                  │
│          ▼                     ▼                    ▼                  │
│   ┌──────────────┐      ┌──────────────┐     ┌──────────────┐          │
│   │ auth-service │      │student-service│    │ fee-service  │   ...    │
│   │  (Port 8084) │      │  (Port 8085) │     │  (Port 8087) │          │
│   └──────┬───────┘      └──────┬───────┘     └──────┬───────┘          │
│          │                     │                    │                  │
│          └─────────────────────┼────────────────────┘                  │
│                                │                                       │
│          ┌─────────────────────┴─────────────────────┐                 │
│          │                                           │                 │
│          ▼                                           ▼                 │
│   ┌──────────────┐                            ┌──────────────┐         │
│   │eureka-server │                            │ MySQL 8.0 DB │         │
│   │ (Port 8761)  │                            │ (Port 3306)  │         │
│   └──────────────┘                            └──────────────┘         │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 2. Prerequisites

1. **Oracle Cloud Infrastructure (OCI)** account ([cloud.oracle.com](https://cloud.oracle.com)).
2. An **Always Free Ampere A1 Compute Instance** created with:
   - **OS**: Ubuntu 22.04 LTS (aarch64 / arm64) or Oracle Linux 8/9.
   - **Shape**: `VM.Standard.A1.Flex` with 4 OCPUs, 24 GB RAM (or 2 OCPUs, 12 GB RAM).
   - **Boot Volume**: 50 GB to 200 GB.
   - **Public IP**: Assigned automatically.
3. SSH Key pair for logging into the VM.

---

## 🛠️ 3. Initial VM Setup (One-Time Setup)

SSH into your Oracle Cloud VM:

```bash
ssh -i /path/to/your-private-key.key ubuntu@<YOUR_VM_PUBLIC_IP>
```

### Step 3.1: Update System & Install Docker + Docker Compose

```bash
# Update packages
sudo apt update && sudo apt upgrade -y

# Install Docker
sudo apt install -y ca-certificates curl gnupg lsb-release
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Allow current user to run Docker without sudo
sudo usermod -aG docker $USER
newgrp docker
```

### Step 3.2: Open Firewall Ports on Oracle Cloud & Ubuntu

Oracle Cloud VM requires opening port 9000 (API Gateway) and optionally 80/443:

```bash
# Open ports in Ubuntu iptables/ufw
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 9000 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save || true
```

> **Important**: Also add an **Ingress Rule** in your OCI Console:
> - Go to: **Networking** $\rightarrow$ **Virtual Cloud Networks** $\rightarrow$ Click your VCN $\rightarrow$ **Security Lists** $\rightarrow$ **Default Security List**.
> - Click **Add Ingress Rules**:
>   - **Source CIDR**: `0.0.0.0/0`
>   - **IP Protocol**: `TCP`
>   - **Destination Port Range**: `9000, 80, 443`

---

## 🚀 4. Deployment Steps

### Step 4.1: Clone the Backend Repository

```bash
git clone https://github.com/Farukhkhan106/school-microservices.git
cd school-microservices
```

### Step 4.2: Configure Environment Variables

Create your `.env` file from `.env.example`:

```bash
cp .env.example .env
nano .env
```

Set your production secrets inside `.env`:
```env
DB_ROOT_PASSWORD=YourStrongDatabasePassword123!
DB_NAME=school_db
JWT_SECRET=YourSuperLongAndSecureRandomJwtSecretKey32CharsMin!
CORS_ORIGINS=https://success-academy-frontend.vercel.app,http://localhost:3000
```

### Step 4.3: Start the Complete Microservices Stack

```bash
docker compose up -d --build
```

Docker will build the multi-stage images and start all 12 services in sequence:
- `school-mysql`
- `school-eureka`
- `school-auth-service`
- `school-student-service`
- `school-faculty-service`
- `school-fee-service`
- `school-attendance-service`
- `school-gallery-service`
- `school-notice-service`
- `school-event-service`
- `school-contact-service`
- `school-api-gateway`

---

## 🔒 5. Setting up Free HTTPS (SSL) for API Gateway

You can set up free SSL in one of two easy ways:

### Option A: Cloudflare Tunnel (Zero-config SSL, no ports needed)
1. Install `cloudflared` on the VM:
   ```bash
   curl -L --output cloudflared.deb https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64.deb
   sudo dpkg -i cloudflared.deb
   ```
2. Run tunnel pointing to API Gateway:
   ```bash
   cloudflared tunnel --url http://localhost:9000
   ```
   *(Cloudflare will output a public HTTPS URL like `https://xxx.trycloudflare.com` or your own custom domain with free edge SSL).*

### Option B: Caddy / Nginx Reverse Proxy with Let's Encrypt
If pointing your own domain (e.g. `api.successacademy.edu.in`):
```bash
sudo apt install -y caddy
```
Edit `/etc/caddy/Caddyfile`:
```
api.yourdomain.com {
    reverse_proxy localhost:9000
}
```
Reload Caddy: `sudo systemctl restart caddy` (automatically provisions free SSL).

---

## 🌐 6. Update Frontend Environment on Vercel

1. Go to your **Vercel Dashboard** $\rightarrow$ Project **`success-academy-frontend`** $\rightarrow$ **Settings** $\rightarrow$ **Environment Variables**.
2. Add or update:
   - **Key**: `VITE_API_URL`
   - **Value**: `http://<YOUR_VM_PUBLIC_IP>:9000` (or `https://api.yourdomain.com` if using SSL)
3. Go to **Deployments** $\rightarrow$ Click the latest deployment $\rightarrow$ Click **Redeploy**.

---

## 🔍 7. Monitoring, Logs & Useful Commands

| Action | Command |
| :--- | :--- |
| **Check running containers** | `docker compose ps` |
| **View all live logs** | `docker compose logs -f` |
| **View logs for specific service** | `docker compose logs -f api-gateway` (or `auth-service`, `student-service`, etc.) |
| **Restart a single service** | `docker compose restart api-gateway` |
| **Stop entire stack** | `docker compose down` |
| **Rebuild and restart all** | `docker compose up -d --build` |
| **Check RAM / CPU usage** | `docker stats` |

---

## 🔄 8. How to Update Backend on Code Changes

When you push new backend commits to GitHub:

```bash
cd school-microservices
git pull origin main
docker compose up -d --build
```
Docker will detect only the modified services, recompile them, and restart their containers with zero downtime for unchanged services.

---

## 🧪 9. Verification & Health Check

Test that all components are healthy from your terminal:

```bash
# Check Eureka Registry
curl -s http://<YOUR_VM_PUBLIC_IP>:8761/eureka/apps

# Check Public Notices through API Gateway
curl -s http://<YOUR_VM_PUBLIC_IP>:9000/notice-service/notice/public

# Check Public Fee Structures through API Gateway
curl -s http://<YOUR_VM_PUBLIC_IP>:9000/fee-service/fees/structure

# Check Auth Login through API Gateway
curl -X POST http://<YOUR_VM_PUBLIC_IP>:9000/auth-service/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"YourPassword"}'
```
