# HTTPS 및 Nginx 배포 가이드

## 1. Spring 운영 환경변수

운영 기본값은 아래와 같습니다.

```bash
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://<host>:3306/blog?useSSL=true&serverTimezone=Asia/Seoul
DB_USERNAME=<db-user>
DB_PASSWORD=<db-password>
APP_PUBLIC_BASE_URL=https://blog-pause.com
APP_WEB_BASE_URL=https://blog-pause.com
APP_API_BASE_URL=https://blog-pause.com
APP_CORS_ALLOWED_ORIGINS=https://blog-pause.com
APP_STORAGE_TYPE=s3
APP_STORAGE_S3_BUCKET=blog-upload-prod
APP_STORAGE_S3_REGION=ap-northeast-2
APP_STORAGE_S3_PUBLIC_BASE_URL=https://blog-upload-prod.s3.ap-northeast-2.amazonaws.com
APP_TUS_STORAGE_PATH=./upload/tus
```

`APP_PUBLIC_BASE_URL`은 프록시 뒤에서 공용 URL 생성에 사용합니다.
`APP_TUS_STORAGE_PATH`는 `tus` 업로드 중간 파일 저장 경로입니다.

## 2. Nginx 설정

정적 프론트는 Nginx가 직접 서빙하고, API와 WebSocket은 Spring Boot(`127.0.0.1:8080`)로 프록시합니다.

운영 규칙:

- `location /api/`에서 `proxy_pass http://127.0.0.1:8080;` 처럼 뒤에 `/`를 붙이지 않는 것
- `tus` 업로드 경로에서는 `proxy_request_buffering off;`를 켜는 것

```nginx
server {
    listen 80;
    server_name blog-pause.com;

    root /var/www/blog-front;
    index index.html;

    access_log /var/log/nginx/access.log;
    error_log /var/log/nginx/error.log;

    client_max_body_size 200m;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
    }

    location /api/attach-files/uploads/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_request_buffering off;
        proxy_buffering off;
        proxy_read_timeout 3600;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
    }

    location /ws {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
    }

    location /ws-sockjs/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
    }
}
```

HTTPS 적용 시:

- `443 ssl http2` 서버 블록 추가
- `80`에서는 `https://$host$request_uri`로 리다이렉트
- 인증서 경로는 `ssl_certificate`, `ssl_certificate_key`에 설정

## 3. Nginx 적용 명령

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## 4. 프록시 헤더

- `X-Forwarded-*` 헤더를 유지합니다.
- Spring 설정은 `server.forward-headers-strategy=framework` 를 사용합니다.

## 5. 저장소 운영 기준

- 업로드 최종 저장소는 S3를 사용합니다.
- 로컬 `./upload` 경로는 `tus` 중간 파일 또는 개발 환경 용도로만 사용합니다.
- 수평 확장 환경에서는 로컬 디스크를 최종 저장소로 사용하지 않습니다.
