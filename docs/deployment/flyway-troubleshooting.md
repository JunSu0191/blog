# Flyway Troubleshooting Guide

Flyway 마이그레이션이 중단되거나 실패했을 때 빠르게 원인을 확인하고 복구하는 운영 가이드입니다.

## 1) 로그 확인

애플리케이션 로그는 파일로도 저장됩니다.

- `logs/application.log`
- `logs/tus-upload.log` (첨부 업로드 전용)

최근 로그 확인:

```bash
tail -n 200 logs/application.log
```

## 2) 진단 스크립트

프로젝트 루트에서 실행:

```bash
./scripts/flyway-troubleshoot.sh status
./scripts/flyway-troubleshoot.sh locks
```

- `status`: 현재 Flyway 버전과 실패 이력 확인
- `locks`: metadata lock / Flyway lock 대기 세션 확인

## 3) 자주 발생하는 장애와 복구

### A. 부팅이 중간에서 멈춤 (`Waiting for table metadata lock`)

증상:

- 로그에 Flyway 구문이 반복되고 다음 단계로 넘어가지 않음
- MySQL processlist에서 `Waiting for table metadata lock`

조치:

1. DB 클라이언트(DBeaver 등)에서 열린 트랜잭션 `COMMIT/ROLLBACK`
2. 필요 시 블로킹 세션 종료
3. 중단된 이전 앱 프로세스 종료 후 재기동

세션 종료 예시:

```bash
./scripts/flyway-troubleshoot.sh kill <mysql_process_id>
```

### B. `Duplicate column name` / `Table already exists`

증상:

- 예: `V20__...` 실행 중 `Duplicate column name 'nickname'`
- 부분 적용(스키마 변경은 반영, flyway 이력은 실패) 상태

조치 원칙:

1. 먼저 실제 스키마가 이미 반영됐는지 확인
2. 반영이 맞다면 실패 이력만 정리
3. 확신이 없으면 임의 수정하지 말고 확인 후 진행

복구 명령:

```bash
./scripts/flyway-troubleshoot.sh mark-success <version>
# 또는
./scripts/flyway-troubleshoot.sh delete-failed <version>
```

주의:

- `mark-success`는 해당 migration SQL이 실질적으로 적용된 상태일 때만 사용
- 잘못 처리하면 이후 migration 일관성이 깨질 수 있음

## 4) 운영 팁

- 기본 Flyway 로그 레벨은 `INFO` 권장
- 상세 추적이 필요할 때만 `APP_LOG_LEVEL_FLYWAY=DEBUG`로 일시 상향
- DB 클라이언트는 autocommit 활성화 권장 (긴 트랜잭션 방지)

