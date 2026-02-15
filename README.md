# Blog Backend

학습용을 넘어 실제 서비스 개발 흐름(인증, CRUD, 실시간, 마이그레이션, 테스트)을 모두 담은 Spring Boot 백엔드 프로젝트입니다.

- 게시글/댓글/첨부파일
- 게시글 좋아요 + 댓글 좋아요/싫어요
- 마이페이지 요약/프로필/내 활동 조회
- 채팅방/메시지/읽음/나가기 + 미읽음 실시간 동기화
- 알림 실시간 전파
- JWT 인증

## Tech Stack

| 구분 | 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5 |
| Build | Gradle |
| DB | MySQL 8 |
| ORM | Spring Data JPA (Hibernate) |
| Migration | Flyway |
| Realtime | WebSocket + STOMP |
| API Docs | springdoc-openapi |

## 시작하기

### 1. 저장소 클론

```bash
git clone https://github.com/JunSu0191/blog.git
cd blog
```

### 2. 실행 전 요구사항

- Java 17+
- MySQL 8+

### 3. MySQL DB 준비

```sql
CREATE DATABASE blog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

필요하면 사용자도 생성하세요.

```sql
CREATE USER 'blog'@'%' IDENTIFIED BY 'change-me';
GRANT ALL PRIVILEGES ON blog.* TO 'blog'@'%';
FLUSH PRIVILEGES;
```

### 4. 환경변수 설정

```bash
cp .env.example .env
```

`.env` 주요 항목:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET`, `JWT_EXPIRATION_MS`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_AUTH_ALLOW_DEV_FALLBACK`, `APP_AUTH_DEV_USER_ID`

셸에 로드:

```bash
export $(grep -v '^#' .env | xargs)
```

### 5. 서버 실행

```bash
./gradlew bootRun
```

기본 서버 주소: `http://localhost:8080`

### 6. API 문서 확인

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## 테스트

```bash
./gradlew test
```

도메인별 빠른 테스트 예시:

```bash
./gradlew test --tests 'com.study.blog.auth.AuthControllerTest'
./gradlew test --tests 'com.study.blog.chat.ChatServiceTest'
./gradlew test --tests 'com.study.blog.like.*'
```

## 인증 빠른 흐름

### 1) 회원가입

`POST /api/auth/register`

```json
{
  "username": "junsu",
  "password": "1234",
  "name": "준수"
}
```

### 2) 로그인

`POST /api/auth/login`

```json
{
  "username": "junsu",
  "password": "1234"
}
```

응답(data) 예시:

```json
{
  "token": "<JWT>",
  "user": {
    "id": 1,
    "username": "junsu",
    "name": "준수"
  }
}
```

### 3) 내 정보 조회

`GET /api/auth/me`

헤더:

```http
Authorization: Bearer <JWT>
```

### Name 정책

프론트에서 `user.name`만 써도 되도록 백엔드에서 보장합니다.

- `name`이 null/blank면 `username`으로 fallback
- 회원 생성/수정 시점에도 동일 정책 적용
- 기존 데이터는 Flyway(`V17`)로 보정

## 공통 응답 포맷

대부분의 REST 응답은 아래 래퍼를 사용합니다.

```json
{
  "status": "OK",
  "success": true,
  "message": "",
  "data": {}
}
```

## API 명세 (핵심)

### Auth

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/auth/register` | 회원가입 | X |
| POST | `/api/auth/login` | 로그인(JWT 발급) | X |
| GET | `/api/auth/me` | 내 계정 조회 | O |

### Posts

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/posts` | 게시글 목록(`mode=cursor/page`, `keyword`) | O |
| GET | `/api/posts/{id}` | 게시글 상세 | O |
| POST | `/api/posts` | 게시글 작성 | O |
| PUT | `/api/posts/{id}` | 게시글 수정 | O |
| DELETE | `/api/posts/{id}` | 게시글 삭제(soft) | O |
| GET | `/api/posts/user/{userId}` | 특정 사용자 게시글 목록 | O |

### Comments

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/comments/posts/{postId}` | 게시글 댓글 목록(대댓글 포함) | O |
| POST | `/api/comments` | 댓글 작성 | O |
| PUT | `/api/comments/{id}` | 댓글 수정 | O |
| DELETE | `/api/comments/{id}` | 댓글 삭제(soft) | O |

### Likes / Reactions

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/posts/{postId}/likes` | 게시글 좋아요 | O |
| DELETE | `/api/posts/{postId}/likes` | 게시글 좋아요 취소 | O |
| GET | `/api/posts/{postId}/likes/me` | 내 좋아요 상태 | O |
| PUT | `/api/comments/{commentId}/reaction` | 댓글 반응(LIKE/DISLIKE/NONE) | O |
| GET | `/api/comments/{commentId}/reaction/me` | 내 댓글 반응 상태 | O |

### MyPage

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/mypage` | 내 요약(프로필/통계) | O |
| PUT | `/api/mypage/profile` | 프로필 수정 | O |
| GET | `/api/mypage/posts` | 내가 쓴 게시글 | O |
| GET | `/api/mypage/comments` | 내가 쓴 댓글 | O |

### Chat (REST)

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/chat/conversations` | 대화방 생성(DIRECT/GROUP) | JWT 권장* |
| POST | `/api/chat/conversations/direct/{otherUserId}` | 1:1 대화방 생성/재사용 | JWT 권장* |
| GET | `/api/chat/users` | 채팅 대상 사용자 목록 | JWT 권장* |
| GET | `/api/chat/conversations` | 내 대화방 목록(unread 포함) | JWT 권장* |
| GET | `/api/chat/conversations/{id}/messages` | 메시지 목록(커서) | JWT 권장* |
| POST | `/api/chat/conversations/{id}/read` | 읽음 처리 | JWT 권장* |
| DELETE | `/api/chat/conversations/{id}` | 대화방 나가기 | JWT 권장* |

### Notifications

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/notifications` | 알림 목록(커서) | JWT 권장* |
| POST | `/api/notifications/{id}/read` | 단건 읽음 | JWT 권장* |
| POST | `/api/notifications/read-all` | 전체 읽음 | JWT 권장* |

### Attach Files

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/attach-files` | 첨부 메타 생성 | O |
| GET | `/api/attach-files/{id}` | 첨부 조회 | O |
| GET | `/api/attach-files/post/{postId}` | 게시글 첨부 목록 | O |
| DELETE | `/api/attach-files/{id}` | 첨부 삭제 | O |
| POST | `/api/attach-files/complete` | TUS 업로드 완료 처리 | JWT 권장* |
| GET | `/api/attach-files/uploads/{uploadId}/info` | 업로드 상태 조회 | JWT 권장* |
| GET | `/api/attach-files/uploads/{uploadId}/download` | 업로드 파일 다운로드 | JWT 권장* |

`*` 현재 보안 설정에서 `/api/chat/**`, `/api/notifications/**`, `/api/attach-files/complete`, `/api/attach-files/uploads/**`는 `permitAll`이지만, 실제 사용자 식별은 JWT 또는 개발용 fallback(`X-User-Id`, `APP_AUTH_ALLOW_DEV_FALLBACK`)에 의해 결정됩니다.

## 실시간(WebSocket/STOMP)

### 연결 엔드포인트

- WebSocket: `/ws`
- SockJS: `/ws-sockjs`

### 클라이언트 Send Prefix

- `/app`

### Subscribe Prefix

- `/topic`
- `/queue`
- `/user`

### 주요 STOMP 라우팅

| 구분 | Destination | 설명 |
|---|---|---|
| SEND | `/app/conversations/{id}/send` | 메시지 전송 |
| SUBSCRIBE | `/topic/conversations/{id}` | 대화방 메시지 수신 |
| SUBSCRIBE | `/user/queue/conversations/{id}/acks` | 내 ACK 수신 |
| SUBSCRIBE | `/topic/conversations/{id}/acks/{userId}` | 유저별 ACK |
| SUBSCRIBE | `/topic/chat/unreads/{userId}` | 미읽음 수 실시간 갱신 |
| SUBSCRIBE | `/user/queue/notifications` | 내 알림 수신 |
| SUBSCRIBE | `/topic/notifications/{userId}` | 알림 토픽 |

CONNECT 시 권장 헤더:

```http
Authorization: Bearer <JWT>
```

## DB 마이그레이션

Flyway로 `src/main/resources/db/migration`의 스크립트를 순차 적용합니다.

- `V16`: 좋아요/댓글 반응 확장
- `V17`: 사용자 `name` null/blank 데이터 보정

## 프로젝트 구조

```text
src/main/java/com/study/blog
├── auth
├── security
├── post
├── comment
├── like
├── mypage
├── attach
├── notification
├── chat
├── realtime
└── core
```

## 운영/배포 참고

- HTTPS/Nginx 가이드: `docs/deployment/https-nginx.md`
- CORS는 `APP_CORS_ALLOWED_ORIGINS`로 제어
- `upload/`는 로컬 저장소 경로로 사용

---

문의나 개선 제안은 Issue/PR로 언제든 환영합니다.
