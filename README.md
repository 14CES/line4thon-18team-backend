# 🚇 dotlo (도틀로) - Backend

> 구글맵에 저장해 둔 장소로, AI가 실제로 다닐 수 있는 여행 일정을 만들어 주는 서비스

line4thon 18팀의 백엔드 레포지토리입니다.

- 서비스: https://dotlo-zeta.vercel.app
- API 서버: https://api.dotlo.cloud

## ✨ 주요 기능

| 기능 | 설명 |
| --- | --- |
| 구글 로그인 / 가입 | 구글 계정으로 로그인, 약관 동의와 닉네임, 캐릭터 설정 |
| 장소 가져오기 | Google Takeout(저장됨) CSV/zip 업로드, 구글맵 공유 리스트 링크로 저장 장소 가져오기 |
| 장소 보강 | 가져온 장소를 구글 장소 정보(좌표, 카테고리, 영업시간)와 매칭, 도시 밖/폐업 장소 구분 |
| AI 일정 생성 | 저장 장소와 취향, 영업시간을 함께 넘겨 AI가 날짜별로 배치하고, 서버가 이동 시간과 영업시간으로 검증 |
| 일정 수정 | 말로 수정 요청(AI), 직접 편집, 구간 이동 수단 선택, 버전 기록과 되돌리기 |
| 예약 정보 | 항공편(편명으로 조회), 숙소, 렌터카 |
| 동행 | 초대 링크, 구성원 권한, 채팅, AI 의견 조율, 투표, 좋아요/댓글, 알림 |
| 확정 / 공유 | 일정 확정, 로그인 없이 볼 수 있는 공유 링크 |
| 인기 코스 | 추천 코스 둘러보기, 찜, 내 일정으로 가져오기 |
| 장소 AI 답변 | 장소에 대해 궁금한 점을 AI에게 질문 |
| 마이 | 프로필 수정, 통계, 여행 나가기, 회원 탈퇴 |

## 🛠 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 4.1.1 (Web MVC, Data JPA, Validation, Security) |
| Auth | Google OAuth 2.0, JWT (access 30분 / refresh 14일) |
| Database | MySQL 8.4 |
| 외부 API | Google Places API, Google Routes API, OpenAI API, AeroDataBox (항공편) |
| Infra | AWS EC2, ECR, Route 53, Nginx (HTTPS), Docker |
| CI/CD | GitHub Actions (main 머지 시 자동 배포) |
| Build | Gradle |
| Etc | Lombok |

## 👥 역할 분담

| 파트 | 담당 | 주요 업무 |
| --- | --- | --- |
| BE1 | @14CES | 로그인/가입, 여행, 동행(초대/구성원/나가기), 장소 가져오기(Takeout, 공유 링크), 일정 저장/버전, 확정/공유, 마이, 회원 탈퇴, 배포 |
| BE2 | @Jadest03 | 장소 매칭/보강/검색/사진/상세, 이동 시간(Routes), 예약(항공/숙소/렌터카), 인기 코스/찜, 채팅, 좋아요/댓글, 알림 |
| BE3 | @psj-png | 취향 해석, AI 일정 생성/수정, 일정 검증, 장소 하나 넣기, 의견 조율/투표 후보, 장소 AI 답변 |

## 📁 패키지 구조

```
com.dotlo.line4thon_18team
├── auth          구글 로그인, JWT, 토큰 재발급
├── user          내 정보, 가입, 프로필, 통계, 회원 탈퇴
├── trip          여행 생성/조회/수정/삭제
├── member        초대, 구성원, 여행 나가기
├── place         저장 장소, Takeout/공유 링크 가져오기, 장소 매칭/검색/사진/상세, 이동 시간
├── placeai       장소 AI 답변
├── preference    여행 취향, 기본 취향
├── itinerary     AI 일정 생성/수정, 일정 검증, 일정 저장/버전
├── reservation   항공편, 숙소, 렌터카
├── share         일정 확정, 공유 링크
├── course        인기 코스, 찜, 내 일정으로 가져오기
├── chat          채팅, 의견 조율, 투표
├── interaction   좋아요, 댓글
├── notification  알림
├── city          지원 도시
└── global        설정, 에러 처리, AI 공통
```

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

| 변수 | 설명 | 로컬 예시 |
| --- | --- | --- |
| DB_HOST | DB 주소 | localhost |
| DB_PORT | DB 포트 | 3307 |
| DB_NAME | DB 이름 | dotlo |
| DB_USERNAME | DB 계정 | dotlo |
| DB_PASSWORD | DB 비밀번호 | (직접 정하기) |
| DB_ROOT_PASSWORD | MySQL 관리자 비밀번호 (로컬 Docker용) | (직접 정하기) |
| JWT_SECRET | JWT 서명 키 (base64) | `openssl rand -base64 32` 결과 |
| AUTH_DEV_HEADER_ENABLED | 로그인 없이 `X-User-Id` 헤더로 테스트 (**배포 서버는 반드시 false**) | true |
| CORS_ALLOWED_ORIGINS | 요청을 허용할 프론트 주소 (쉼표로 구분, 공백 없이) | http://localhost:5173 |
| GOOGLE_CLIENT_ID | 구글 로그인 클라이언트 ID | (팀원에게 따로 받기) |
| GOOGLE_CLIENT_SECRET | 구글 로그인 클라이언트 비밀번호 | (팀원에게 따로 받기) |
| GOOGLE_REDIRECT_URI | 구글 로그인 후 돌아갈 프론트 주소 (구글 콘솔에 등록된 값과 같아야 함) | http://localhost:5173/auth/callback/google |
| GOOGLE_MAPS_API_KEY | Google Places / Routes API 키 | (팀원에게 따로 받기) |
| OPENAI_API_KEY | OpenAI API 키 (비우면 AI 기능은 실패하거나 기본값으로 진행) | sk-... |
| OPENAI_MODEL | 사용할 OpenAI 모델 | gpt-5-mini |
| OPENAI_REASONING_EFFORT | AI가 생각하는 양 (minimal / low / medium / high). 비우면 모델 기본값. 낮을수록 빠름 | low |
| OPENAI_BASE_URL | OpenAI API 주소 (거의 안 바꿈) | https://api.openai.com/v1 |
| AERODATABOX_API_KEY | 항공편 조회 API 키 (비우면 항공편 찾기 실패) | (팀원에게 따로 받기) |
| JPA_SHOW_SQL | 실행되는 SQL 로그 (로컬만 true, 배포 서버는 넣지 않기) | true |
| TZ | 서버 시간대 (배포 서버만, 로컬은 안 넣어도 됨) | Asia/Seoul |

> 키 값은 깃허브, 노션, 단톡방에 올리지 말고 개인 메시지로만 주고받습니다.

### 3. MySQL 실행
```bash
docker compose up -d
```
- 처음 실행할 때 비밀번호가 정해집니다. 이후에 `.env`의 DB 비밀번호를 바꾸려면 `docker compose down -v`로 DB를 초기화해야 합니다.
- 프로젝트 이름은 `docker-compose.yml`에 `line4thon_18team`으로 고정되어 있습니다. 예전에 이름 없이 띄운 적이 있으면 DB가 새로 만들어져서 비어 보일 수 있어요.
- 컴퓨터를 껐다 켜면 **Docker Desktop을 먼저 실행**하세요.

### 4. 서버 실행
IntelliJ에서 `Line4thon18teamApplication` 실행 또는
```bash
./gradlew bootRun
```
→ `http://localhost:8080/api/health` 에서 `{"status":"UP"}`이 나오면 정상입니다.

### 5. 로그인 없이 API 테스트하기 (로컬 전용)
`.env`에 `AUTH_DEV_HEADER_ENABLED=true`면 토큰 대신 `X-User-Id` 헤더로 사용자를 지정할 수 있습니다.
```bash
curl -H "X-User-Id: 1" http://localhost:8080/api/users/me
```
배포 서버에서는 꺼져 있어서 반드시 `Authorization: Bearer {accessToken}`이 필요합니다.

## 🧪 테스트

```bash
./gradlew test
```
- DB가 필요한 테스트가 많아서 **MySQL(3번)을 먼저 켜야** 합니다.
- 실제 구글/항공편 API를 부르는 테스트는 기본으로 건너뜁니다. 직접 돌리려면 환경변수로 키를 넣고 실행합니다 (요금이 나갈 수 있어요).
```bash
GOOGLE_MAPS_API_KEY=... ./gradlew test --tests "*TravelTimeTest"
```
PowerShell에서는 이렇게 씁니다.
```powershell
$env:GOOGLE_MAPS_API_KEY="..."; ./gradlew test --tests "*TravelTimeTest"
```

## 🌐 배포

```
main 머지 → GitHub Actions → Docker 이미지 빌드 → AWS ECR 업로드 → EC2에서 새 컨테이너 실행
```
- 이미지는 `latest`와 커밋 번호 태그로 올라가서, 문제가 생기면 이전 커밋 이미지로 되돌릴 수 있습니다.
- EC2의 Nginx가 HTTPS(`api.dotlo.cloud`)를 받아 같은 서버의 컨테이너(127.0.0.1:8080)로 넘깁니다.
- 배포 서버의 환경변수는 EC2의 `/home/ubuntu/app.env`에 있습니다 (위 표의 변수 + `TZ=Asia/Seoul`, `JPA_SHOW_SQL`은 넣지 않음). 바꾼 뒤에는 컨테이너를 다시 만들어야 적용됩니다.
- 배포 확인: https://api.dotlo.cloud/api/health

## 📄 API 명세

- 전체 API 명세는 팀 노션에 정리되어 있습니다.
- 에러 응답은 모두 같은 모양입니다.
```json
{ "status": 404, "code": "TRIP_NOT_FOUND", "message": "여행을 찾을 수 없습니다." }
```
- 에러 코드 목록: `global/error/ErrorCode.java`

## 🤝 협업 규칙

### 작업 흐름
1. **Issue 생성**: 할 일을 이슈로 먼저 등록
2. **최신 코드 받기**: `git switch main` → `git pull upstream main`
3. **브랜치 생성**: `git switch -c feat/기능이름`
4. **작업 후 내 포크에 push**: `git push origin 브랜치이름`
5. **PR 생성**: 팀 레포 `main`으로, 설명에 `Closes #이슈번호`
6. **리뷰**: 팀원 **1명 승인** 후 머지. 새로 push하면 기존 승인은 다시 받아야 합니다
7. **배포 확인**: 머지 후 Actions `Deploy`가 초록인지 확인하고 다음 PR을 머지

### 브랜치 이름
| 접두어 | 용도 | 예시 |
| --- | --- | --- |
| feat/ | 새 기능 | feat/takeout-upload |
| fix/ | 버그 수정 | fix/csv-header |
| refactor/ | 코드 개선 | refactor/place-service |
| docs/ | 문서 | docs/readme |
| chore/ | 설정, 기타 | chore/init-project |

### 커밋 메시지 (권장)
```
feat: Takeout CSV 업로드 API 추가
fix: 한국어 헤더 CSV 인식 안 되는 문제 수정
docs: README 실행 방법 추가
```
`feat`, `fix`, `refactor`, `docs`, `chore`, `test` 중 하나로 시작합니다.

### 이슈 / PR 제목 (권장)
`[Feat] 기능 설명`, `[Fix] 버그 설명`처럼 대괄호로 종류를 표시합니다.
