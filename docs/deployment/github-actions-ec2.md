# GitHub Actions 기반 EC2 배포 가이드

## 1. EC2 디렉터리 구조

```bash
/home/ubuntu/app/
├── app.jar
├── .env
├── blog-0.0.1-SNAPSHOT.jar
├── blog/
└── logs/
```

`systemd` 서비스는 한 번만 설치하면 됩니다.

```bash
sudo cp deploy/blog.service.example /etc/systemd/system/blog.service
sudo systemctl daemon-reload
sudo systemctl enable blog.service
```

## 2. GitHub 저장소 시크릿

GitHub Actions에서 아래 시크릿을 등록합니다.

- `EC2_HOST`: `blog-pause.com` 또는 `43.203.85.214`
- `EC2_USER`: 보통 `ubuntu`
- `EC2_PORT`: 보통 `22`
- `EC2_SSH_KEY`: EC2 접속용 개인키
- `PROD_ENV_FILE`: 운영용 `.env` 전체 내용을 멀티라인 문자열로 저장한 값

## 3. 운영용 `.env` 준비 방법

운영용 환경변수 파일 경로는 `/home/ubuntu/app/.env` 입니다.

배포 흐름은 아래 둘 중 하나입니다.

1. GitHub Actions 사용:
   `PROD_ENV_FILE` 시크릿에 `.env` 전체 내용을 저장하고, 배포 시 `/home/ubuntu/app/.env`로 기록합니다.
2. 서버 수동 관리:
   운영 서버에서 직접 `/home/ubuntu/app/.env` 파일을 생성하고 관리합니다.

## 4. AWS 인증 정보

운영에서는 가능하면 EC2 인스턴스에 IAM Role을 붙이는 쪽이 맞습니다.

- 권한 예시: `s3:PutObject`, `s3:GetObject`, 필요하면 `s3:DeleteObject`
- 리소스 범위: `arn:aws:s3:::blog-upload-prod/*`

IAM Role을 쓰지 않으면 EC2에 AWS 자격 증명이 별도로 있어야 합니다. 현재 코드는 AWS SDK 기본 자격 증명 체인을 사용하므로, 환경변수 또는 인스턴스 메타데이터에서 값을 읽습니다.

## 5. OAuth 리다이렉트 URI

백엔드와 프론트가 같은 도메인 `https://blog-pause.com` 아래에 있으면 각 OAuth 제공자에 아래 URI를 등록합니다.

- `https://blog-pause.com/login/oauth2/code/google`
- `https://blog-pause.com/login/oauth2/code/kakao`
- `https://blog-pause.com/login/oauth2/code/naver`

프론트 콜백 주소:

- `https://blog-pause.com/auth/callback`

## 6. 배포 워크플로우 동작

`.github/workflows/deploy-backend.yml`은 아래 순서로 동작합니다.

1. `./gradlew test` 실행
2. `bootJar` 빌드
3. JAR와 재시작 스크립트를 `/home/ubuntu/app` 으로 복사
4. 운영용 `.env` 파일을 `/home/ubuntu/app/.env` 로 생성 또는 덮어쓰기
5. `blog.service` 재시작
