# 이전 기수 팀 프로젝트 이관 스크립트

우아한테크코스 이전 기수(2기~7기)의 팀 프로젝트를 GitHub 에서 수집해
DB 에 넣을 SQL 을 만든다.

> **8기부터는 크루가 서비스에서 직접 등록한다.** 이 스크립트는 이전 기수 전용이다.

## 무엇을 하려는지 먼저 고른다

| 상황 | 할 일 | 절 |
| --- | --- | --- |
| 로컬 DB 가 비어 있어 채우려고 한다 | 전체 SQL 을 만들어 넣는다 | [A](#a-로컬-db-를-처음-채울-때) |
| 빠진 프로젝트를 새로 넣으려고 한다 | 추가분만 잘라 SQL 을 만든다 | [B](#b-프로젝트를-새로-추가할-때) |
| 이미 들어간 값을 고치려고 한다 | 스크립트를 쓰지 않고 `UPDATE` 한다 | [C](#c-이미-들어간-값을-고칠-때) |

## 사전 준비

- Node.js 20 이상, Python 3.9 이상, `jq`
- `GITHUB_TOKEN` 환경변수 (public 저장소만 읽으므로 스코프 없는 토큰이면 된다)

```bash
export GITHUB_TOKEN=ghp_xxx    # VITE_ 접두사를 쓰면 안 된다. 번들에 실릴 수 있다.
```

아래 명령은 모두 `backend/` 에서 실행한다.

## A. 로컬 DB 를 처음 채울 때

```bash
# 1. GitHub 에서 수집한다. scripts/prev-crew.json 이 이미 있으면 건너뛴다.
#    저장소 60여 개를 조회하므로 몇 분 걸린다.
node scripts/generate-prev-crew.mjs

# 2. 전체 SQL 을 만든다.
python3 scripts/generate-archived-projects.py scripts/prev-crew.json > scripts/archived-projects.sql

# 3. 넣고 지운다.
psql -U shoutoutz -d shoutoutz -f scripts/archived-projects.sql
rm scripts/archived-projects.sql
```

### 다시 채우고 싶을 때

이미 데이터가 들어 있는 상태에서 위를 그대로 실행하면 중복이라 실패한다.
먼저 비우고 2~3번을 다시 실행한다.

```bash
psql -U shoutoutz -d shoutoutz -c "TRUNCATE projects RESTART IDENTITY CASCADE;"
```

`projects` 를 참조하는 테이블(`woowa_archived_project_members`, `project_tags`,
`project_members`, `project_comments`, `project_reactions`, `project_view_days`,
`project_approval_histories`)이 `CASCADE` 로 함께 비워지고, `RESTART IDENTITY` 가
`id` 시퀀스를 1부터 돌린다. 로그인 계정 같은 다른 데이터는 남는다.

스키마 자체가 꼬였을 때만 통째로 갈아엎는다. 이쪽은 **로컬 전용**이다.
`flyway_schema_history` 까지 지워져야 Flyway 가 처음부터 다시 적용한다.

```bash
psql -U shoutoutz -d shoutoutz -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
# 애플리케이션 재시작 -> Flyway 가 db/migration 적용 -> 위 2~3번 실행
```

## B. 프로젝트를 새로 추가할 때

이미 데이터가 있는 DB 에 넣는 경우다. **전체 SQL 을 쓰면 안 되고, 추가할 것만
잘라내야 한다.** (만들어지는 SQL은 INSERT 뿐이기 때문이다.)

```bash
# 1. 저장소 목록에 추가하고 커밋한다.
#    { "owner": "woowacourse-teams", "repo": "2021-abcd", "title": "푸", "year": 2021, "generation": "3기" }
vi scripts/prev-repo-list.json

# 2. 수집한다. 이미 있는 저장소는 재사용하고 새 것만 API 를 호출하므로 몇 초면 끝난다.
node scripts/generate-prev-crew.mjs

# 3. GitHub 에 없는 값(배포 URL, 운영 여부, 썸네일)이 있으면 적고 커밋한다.
#    "abcd": { "service_status": "OPERATING", "deployment_url": "https://..." }
vi scripts/overrides.json

# 4. 추가할 저장소만 잘라 SQL 을 만든다.
jq '[.[] | select(.repo == "2021-abcd")]' scripts/prev-crew.json \
  | python3 scripts/generate-archived-projects.py /dev/stdin > scripts/archived-2021-abcd.sql

# 5. 로컬 -> dev -> prod 순으로 같은 파일을 실행한다.
psql -U shoutoutz -d shoutoutz -f scripts/archived-2021-abcd.sql

# 6. 다 끝나면 지운다.
rm scripts/archived-2021-abcd.sql
```

여러 건이면 `select(.repo == "2021-abcd" or .repo == "2021-efg")`,
기수 통째로면 `select(.generation == "1기")` 로 조건을 바꾼다.

4번에서 `overrides.json 에 존재하지 않는 slug` 경고가 여러 개 나오는데,
입력을 잘라서 나머지 slug 가 대조 대상에서 빠졌을 뿐이라 무시해도 된다.
A 의 전체 SQL 을 만들 때 같은 경고가 나오면 그때는 실제 오타다.

다른 팀원은 파일을 받을 필요 없이, `git pull` 후 2번과 4~6번을 각자 돌리면 된다.

## C. 이미 들어간 값을 고칠 때

**스크립트를 쓰지 않는다.** `INSERT` 만 만들기 때문에 이미 있는 행은 못 고친다.
대신 두 곳을 같이 손본다.

```bash
# 1. overrides.json 을 고치고 커밋한다. 여기가 원본이고, 수정 이력도 여기 남는다.
#    "abcd": { "service_status": "CLOSED" }
vi scripts/overrides.json

# 2. DB 에는 UPDATE 를 직접 실행한다.
psql -U shoutoutz -d shoutoutz \
  -c "UPDATE projects SET service_status = 'CLOSED' WHERE slug = 'abcd';"
```

1번을 빠뜨리면 다음에 A 로 로컬 DB 를 다시 채울 때 옛날 값으로 돌아간다.

`overrides.json` 이 덮어쓸 수 있는 값은 `thumbnail_url`, `team_name`,
`service_status`, `approval_status`, `deployment_url` 다섯이다. readme 나 star 처럼
GitHub 에서 오는 값은 수집으로 갱신되므로 여기에 적지 않는다.
키는 slug(저장소 이름에서 연도 접두사를 뗀 것)를 쓴다.

---

# 참고 사항

## 각 파일이 하는 일

```
prev-repo-list.json   조사할 저장소 목록 (사람이 관리)
        |
        |  generate-prev-crew.mjs   GitHub API 호출
        v
prev-crew.json        수집 결과. readme, stars, 참여자          --+
                      재수집하면 통째로 새로 써진다.               |
                                                                  +--> generate-archived-projects.py
overrides.json        GitHub 이 모르는 값 (사람이 관리)          |     |
                      배포 URL, 운영 여부, 썸네일               --+     |
                                                                        v
                                                             archived-*.sql
```

| 파일 | 커밋 | 로컬 |
| --- | --- | --- |
| `generate-prev-crew.mjs` | O | |
| `generate-archived-projects.py` | O | |
| `prev-repo-list.json` | O | |
| `overrides.json` | O | |
| `prev-crew.json` | **X** | 남긴다 |
| `archived-*.sql` | **X** | 쓰고 지운다 |


`.gitignore` 에는 이렇게 들어 있다. 이름을 무엇으로 짓든 `archived-` 로 시작하면
커밋되지 않는다.
