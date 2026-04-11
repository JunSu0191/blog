# 배포 실행 가이드

## 1. 배포 전 확인

배포 전에 아래를 확인합니다.

- 운영 DB 마이그레이션이 필요한 경우 먼저 수동으로 실행
- `/home/ubuntu/app/.env` 값이 최신 운영 값인지 확인
- `blog.service`가 이미 등록되어 있는지 확인
- `https://blog-pause.com` 도메인과 Nginx가 정상 동작하는지 확인

Flyway 수동 실행 예시:

```bash
./scripts/flyway/manual.sh info .env.prod
./scripts/flyway/manual.sh migrate .env.prod
```

## 2. GitHub Actions 배포

배포는 자동 실행이 아니라 GitHub Actions에서 수동으로 실행합니다.

1. GitHub 저장소의 `Actions` 탭으로 이동
2. `Deploy Backend` 워크플로 선택
3. `Run workflow` 클릭
4. 배포할 브랜치를 선택하고 실행

워크플로는 아래 순서로 진행됩니다.

1. 테스트 실행
2. JAR 빌드
3. EC2 `/home/ubuntu/app` 경로로 업로드
4. `/home/ubuntu/app/app.jar` 갱신
5. `blog.service` 재시작

## 3. 배포 직후 서버 확인

배포가 끝나면 서버에서 아래를 확인합니다.

```bash
sudo systemctl status blog.service --no-pager
sudo journalctl -u blog.service -n 200 --no-pager
ls -l /home/ubuntu/app/app.jar
```

정상 상태 기준:

- `blog.service`가 `active (running)`
- `app.jar` 파일이 최신 시간으로 갱신됨
- 로그에 Spring Boot 기동 완료 메시지가 보임

## 4. 배포 직후 서비스 확인

서버 또는 로컬에서 아래를 확인합니다.

```bash
curl -I https://blog-pause.com
curl -I https://blog-pause.com/swagger-ui/index.html
```

필요하면 브라우저에서 아래도 확인합니다.

- 메인 페이지 렌더링
- 로그인 API 동작
- OAuth 로그인 진입
- WebSocket 연결이 필요한 기능

OAuth 프록시 확인 예시:

```bash
curl -I https://blog-pause.com/oauth2/authorization/kakao
curl -I https://blog-pause.com/login/oauth2/code/kakao
```

정상 기대값:

- `/oauth2/authorization/kakao` 요청 시 `302` 응답과 `Location: https://kauth.kakao.com/...`
- `/login/oauth2/code/kakao` 는 직접 열면 보통 `4xx` 또는 앱 정의 에러가 나더라도, Nginx가 `index.html`을 반환하면 안 됨

## 5. 배포 후 설정 변경이 필요한 경우

운영 환경변수 변경 시:

1. `/home/ubuntu/app/.env` 수정
2. 서비스 재시작

```bash
sudo systemctl restart blog.service
sudo systemctl status blog.service --no-pager
```

Nginx 설정 변경 시:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## 6. 장애 확인 순서

배포 후 장애가 나면 아래 순서로 확인합니다.

1. `blog.service` 상태 확인
2. `journalctl -u blog.service` 로그 확인
3. `/home/ubuntu/app/.env` 값 확인
4. RDS 접속 가능 여부 확인
5. Nginx 설정과 HTTPS 상태 확인

## 7. 롤백 방법

이전 JAR이 별도로 보관되어 있지 않으면 즉시 롤백은 어렵습니다.

운영에서 롤백이 필요하면 최소한 아래 중 하나는 준비합니다.

- 이전 버전 JAR 보관
- 특정 커밋 기준으로 다시 빌드 후 배포
- 릴리즈 태그 기준 재배포

가장 단순한 롤백 방법은 이전 정상 커밋으로 돌아가 다시 배포하는 방식입니다.
