# 🚇 line4thon 18team - Backend

서비스 소개: 추후 기재

## 🛠 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 4.1.1 (Web MVC, Data JPA, Validation) |
| Database | MySQL 8.4 (Docker) |
| Build | Gradle |
| Etc | Lombok |

## 👥 역할 분담

| 파트 | 담당 | 주요 업무 |
| --- | --- | --- |
| BE1 | @14CES | 서버·데이터·파일 수집 (Takeout 업로드, 공통 데이터 모델, 일정 저장, 배포) |
| BE2 |  | 장소·경로 API (Places 매칭, Routes 이동시간) |
| BE3 |  | AI 일정 생성·수정·검증 |

## 🚀 로컬 실행 방법

### 0. 준비
- JDK 17 이상
- Docker Desktop
- IntelliJ (권장)

### 1. 레포 받기
본인 계정으로 **Fork** 한 뒤 clone 합니다.
```bash
git clone https://github.com/<내 아이디>/<포크한 레포 이름>.git
cd <포크한 레포 이름>
git remote add upstream https://github.com/line4thon-18team/backend.git
```

### 2. 환경변수 파일 만들기
`.env.example`을 복사해서 `.env`를 만들고 값을 채웁니다. (`.env`는 git에 올라가지 않습니다)
```bash
cp .env.example .env
```

| 변수 | 설명 | 예시 |
| --- | --- | --- |
| DB_HOST | DB 주소 | localhost |
| DB_PORT | DB 포트 | 3307 |
| DB_NAME | DB 이름 | dotlo |
| DB_USERNAME | DB 계정 | dotlo |
| DB_PASSWORD | DB 비밀번호 | (직접 정하기) |
| DB_ROOT_PASSWORD | MySQL 관리자 비밀번호 | (직접 정하기) |

### 3. MySQL 실행
```bash
docker compose up -d
```
- 처음 실행할 때 비밀번호가 정해집니다. 이후에 `.env`의 비밀번호를 바꾸려면 `docker compose down -v`로 DB를 초기화해야 합니다.
- 컴퓨터를 껐다 켜면 **Docker Desktop을 먼저 실행**하세요.

### 4. 서버 실행
IntelliJ에서 `Line4thon18teamApplication` 실행 또는
```bash
./gradlew bootRun
```
→ `http://localhost:8080`

## 🤝 협업 규칙

### 작업 흐름
1. **Issue 생성** — 할 일을 이슈로 먼저 등록
2. **최신 코드 받기** — `git switch main` → `git pull upstream main`
3. **브랜치 생성** — `git switch -c feat/기능이름`
4. **작업 후 내 포크에 push** — `git push origin 브랜치이름`
5. **PR 생성** — 팀 레포 `main`으로, 설명에 `Closes #이슈번호`
6. **리뷰** — 팀원 **1명 승인** 후 머지

### 브랜치 이름
| 접두어 | 용도 | 예시 |
| --- | --- | --- |
| feat/ | 새 기능 | feat/takeout-upload |
| fix/ | 버그 수정 | fix/csv-header |
| refactor/ | 코드 개선 | refactor/place-service |
| docs/ | 문서 | docs/readme |
| chore/ | 설정, 기타 | chore/init-project |

### 커밋 메시지
```
feat: Takeout CSV 업로드 API 추가
fix: 한국어 헤더 CSV 인식 안 되는 문제 수정
docs: README 실행 방법 추가
```
`feat`, `fix`, `refactor`, `docs`, `chore`, `test` 중 하나로 시작합니다.

### 이슈 / PR 제목
`[Feat] 기능 설명`, `[Fix] 버그 설명`처럼 대괄호로 종류를 표시합니다.