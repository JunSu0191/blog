# Blog Backend

Spring Boot 기반으로 만든 블로그 백엔드입니다.  
게시글, 댓글, 첨부파일, 알림, 채팅까지 한 프로젝트에서 다루는 학습형 API 서버입니다.

## 한눈에 보기

| 항목 | 내용 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Build | Gradle |
| Database | MySQL |
| ORM | JPA (Hibernate) |
| Migration | Flyway |
| Realtime | STOMP / WebSocket |

## 주요 기능

- 게시글 CRUD + 페이지네이션 + 키워드 검색
- 댓글 CRUD(대댓글 포함) + 소프트 삭제
- 첨부파일 메타데이터 관리 + 업로드 경로 제공
- 알림/채팅 실시간 이벤트(STOMP)
- JWT 기반 인증 처리

## 빠른 시작

### 1) 실행 환경

- Java 17+
- MySQL 8+

### 2) 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3) 테스트 실행

```bash
./gradlew test
```

## 설정 파일

- 메인 설정: `src/main/resources/application.yml`
- 테스트 설정: `src/test/resources/application.yml`
- 환경변수 예시: `.env.example`

주요 설정 키:

- `spring.datasource.*`
- `jwt.secret`, `jwt.expiration-ms`
- `app.cors.allowed-origins`
- `app.auth.allow-dev-fallback`, `app.auth.dev-user-id`

### 환경변수로 실행하기

```bash
cp .env.example .env
# .env 값 수정 후
export $(grep -v '^#' .env | xargs)
./gradlew bootRun
```

## API 스냅샷

### Posts

- `GET /api/posts` (검색: `keyword`, 페이지: `page`, `size`)
- `GET /api/posts/{id}`
- `POST /api/posts`
- `PUT /api/posts/{id}`
- `DELETE /api/posts/{id}`

### Comments

- `GET /api/comments/posts/{postId}`
- `POST /api/comments`
- `PUT /api/comments/{id}`
- `DELETE /api/comments/{id}`

### Attach Files

- `POST /api/attach-files`
- `GET /api/attach-files/{id}`
- `GET /api/attach-files/post/{postId}`
- `DELETE /api/attach-files/{id}`

## 프로젝트 구조

```text
src/main/java/com/study/blog
├── auth
├── security
├── post
├── comment
├── attach
├── notification
├── chat
└── realtime
```

주요 시작점:

- 애플리케이션 엔트리포인트: `src/main/java/com/study/blog/BlogApplication.java`
- DB 마이그레이션: `src/main/resources/db/migration`

## 배포 문서

- HTTPS/Nginx 참고: `docs/deployment/https-nginx.md`

## 참고 사항

- `upload/` 폴더는 로컬 파일 저장소로 사용되며 Git 추적 대상에서 제외됩니다.
- 현재 첨부파일은 메타데이터 중심 구조입니다. 실제 파일 스토리지(로컬/클라우드)는 요구사항에 맞게 확장할 수 있습니다.
