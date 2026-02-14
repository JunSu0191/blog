# HTTPS and Nginx Deployment Guide

## 1. Spring app env

Set these environment variables for production:

```bash
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://<host>:3306/blog?useSSL=true&serverTimezone=Asia/Seoul
DB_USERNAME=<db-user>
DB_PASSWORD=<db-password>
APP_PUBLIC_BASE_URL=https://api.example.com
APP_CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com
```

`APP_PUBLIC_BASE_URL` is used by `PublicUrlBuilder` to generate public links that stay correct behind proxy/load balancer.

## 2. Nginx reverse proxy example

```nginx
upstream blog_backend {
    server 127.0.0.1:8080;
    keepalive 32;
}

server {
    listen 80;
    server_name api.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.example.com;

    ssl_certificate     /etc/letsencrypt/live/api.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.example.com/privkey.pem;
    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:10m;
    ssl_protocols TLSv1.2 TLSv1.3;

    client_max_body_size 20m;

    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host $host;

    location /api/ {
        proxy_pass http://blog_backend;
    }

    location /upload/ {
        proxy_pass http://blog_backend;
    }

    location /ws {
        proxy_pass http://blog_backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600;
    }

    location /ws-sockjs/ {
        proxy_pass http://blog_backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600;
    }
}
```

## 3. AWS/GCP notes

- AWS: ALB can terminate HTTPS and forward to Nginx or directly to Spring. Keep `X-Forwarded-*` headers and `server.forward-headers-strategy=framework`.
- GCP: HTTPS load balancer can do the same. Ensure WebSocket is enabled on backend service and idle timeout is high enough.
- For horizontal scale, keep app stateless and move file storage to S3/GCS instead of local `./upload`.

## 4. Notification scale strategy

- Current code now separates `NotificationService` and delivery channels.
- Add new channels by implementing `NotificationDeliveryChannel`:
  - email sender
  - mobile push sender
  - queue publisher (SQS/PubSub/Kafka)
- For high traffic, persist notification first, then publish async job to a queue worker.
