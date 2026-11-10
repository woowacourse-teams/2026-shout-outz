# data-pipeline

샤라웃 매일 데이터 파이프라인. ① 원본 수집기(`collector/`)와 ② 정제(`refine/`)가 있다.

- 설계 문서(작업 폴더): `01-pipeline-overview.md`(전체 단계), `03`(블로그), `04`(테코톡), `06`(팀 문서), `07`(PR), `08-collect-component.md`(수집 컴포넌트), `09-refine.md`(정제)
- 하나의 수집기로 첫 실행과 매일 실행을 함께 맡는다. 원본 저장소가 비어 있으면 모든 문서가 새 문서가 되어 전체를 받고, 그 뒤에는 새 문서와 바뀐 문서만 받는다.

## 저장소 모양

```
{PIPELINE_STORE}/
├─ raw/{source}/{external_key 의 sha1}.json   문서 한 건, 다시 받으면 덮어쓴다
├─ index/{source}.jsonl                      아는 키 색인(키마다 한 줄, 바뀜 표시, 사라짐)
├─ state/{source}.json                       수집기 상태(블로그, 위키)
├─ state/lock.json                           실행 중 잠금
└─ reports/{run_id}.json                     실행 보고서
```

`PIPELINE_STORE` 는 S3 주소(`s3://<버킷>/shout-outz/data-pipeline`) 또는 로컬 폴더다. 둘 다 같은 모양이다. 꼭 적어야 한다(없으면 실행하지 않는다. 기본 폴더에 조용히 쓰고 성공으로 끝나는 일을 막으려고).

## 명령

```bash
uv sync                                                    # 처음 한 번
uv run python -m collector collect                         # 모든 원천(테코톡 → 블로그 → 팀 문서 → PR)
uv run python -m collector collect --source github-pr      # 원천 하나
uv run python -m collector collect --source blog-post --only https://team.tistory.com/12          # 그 건만 다시 받기
uv run python -m collector collect --source github-pr --repos 2026-Chalkak                       # 레포 몇 개로 시험
uv run python -m collector subtitles script --all-missing > subtitles_local.py                   # 집 컴퓨터용 자막 스크립트
uv run python -m collector subtitles import subtitles-result.json                                # 자막 결과 저장
uv run python -m collector bundle import blog-1_7-bundle.jsonl.gz                               # 원본 묶음 넣기(1~7기 블로그 등)
```

- 원천 이름: `youtube-tekotalk`, `blog-post`, `github-teamdoc`, `github-wiki`, `github-discussion`, `github-pr`
- 테코톡 `--only` 는 영상 ID 나 `yt:` 키를 받는다. 영상 ID 는 `-` 로 시작할 수 있어(`-gbqPuHzLj0`) 그대로 적으면 명령줄 옵션으로 읽히므로 `yt:-gbqPuHzLj0` 처럼 키로 적는다
- 환경 변수: `deploy/data-pipeline.env.example`
- 끝나면 표준 출력에 실행 id, 상태, 원천별 숫자가 나오고, 자세한 것은 `reports/{run_id}.json` 에 남는다.

## 정제(②)

원본 저장소의 원본을 읽어 원천과 상관없이 같은 모양의 정제 문서로 바꾸고, 서비스에 쓸 가치가 없는 문서를 LLM 전에 규칙으로
거르며, 문서마다 내용 해시를 붙인다(설계: 작업 폴더 `09-refine.md`, 결정 항목 R1~R21). 수집기와 같은 `PIPELINE_STORE`,
같은 잠금(`state/lock.json`)을 쓴다. 원본은 읽기만 한다.

```
{PIPELINE_STORE}/refine/
├─ docs/{source}/{키의 sha1}.json   정제 문서 한 건. 다시 정제하면 덮어쓰고 이력(history)은 문서 안에 쌓는다
├─ index/{source}.jsonl            정제 색인(키마다 한 줄: 원본 버전, 규칙 버전, 내용 해시, 대표 제외 규칙, 실패, 사라짐)
├─ changes/{run_id}.jsonl          실행마다 변경 기록(새 문서, 바뀐 문서와 그 전후, 무시한 변경, 사라짐, 돌아옴, 옮겨짐, 새로 제외, 제외 풀림)
├─ reports/{run_id}.json           정제 실행 보고서(15초마다 저장)
└─ previews/{source}-{시각}.json    규칙을 고치기 전 미리 보기
```

```bash
uv run python -m refine run                                    # 고른 문서만 정제. 한 번에 최대 1만 건
uv run python -m refine run --source github-pr --limit 500     # 원천 하나, 건수 제한(나눠 돌리기)
uv run python -m refine run --source github-pr --only 'woowacourse-teams/2026-Chalkak#912'   # 그 문서만 다시(아침 다시 돌리기)
uv run python -m refine run --after-collect                    # 가장 최근 수집 실행과 같은 run_id 로 묶기(수집 뒤 이어 돌릴 때 쓴다)
uv run python -m refine preview --source github-teamdoc        # 지금 규칙으로 정제만 해 본 결과. 정제 문서는 건드리지 않는다
```

- 고르는 것: 처음 보는 키, 원본 버전이 오른 키, 지난번 실패한 키, 규칙 버전이 바뀐 원천의 키. 원본 파일을 열지 않고 색인끼리 견준다.
  사라진 문서는 정제하지 않고 `gone` 만 고친다. 1~7기 블로그(원본 `origin` 이 `archive-`)는 처음 한 번 정제한 뒤 고정한다.
  자막을 아직 받지 않은 테코톡은 정제하지 않고 기다린다(보고서 `waiting`)
- 순서: 테코톡 → 블로그 → 디스커션 → 위키 → 코드 마크다운 → PR(작은 원천부터). 같은 레포 사본은 팀 문서 세 원천을 다 정제한 뒤와
  PR 다음에, 이번에 문서가 바뀐 레포만 다시 정한다
- `--only` 는 `--source` 하나와 함께 쓴다(코드 마크다운과 위키의 키 모양이 겹칠 수 있다)
- `--after-collect`: 12시간 안에 시작했고 아직 정제 보고서가 없는 가장 최근 수집 실행의 run_id 를 쓴다. 잠금 때문에 건너뛴 수집 실행은 넘기고,
  정제가 잠금 때문에 건너뛰면 건너뜀 보고서는 새 run_id 로 남긴다(잠금이 풀린 뒤의 정제가 그 수집 실행에 붙을 수 있게)
- 끝나면 표준 출력에 실행 id, 상태, 원천별 숫자가 나온다. `remaining` 은 1만 건 상한 때문에 다음 실행으로 넘긴 수다
- 규칙을 고칠 때: 고친 원천만 `refine/config.py` 의 `RULES_VERSION` 을 올린다. 올리기 전에 `preview` 로 새로 제외될 문서,
  제외가 풀릴 문서, 해시가 바뀔 문서를 본다. 라이브러리 판(`trafilatura`, `markdownify`, `lxml`)을 바꾸는 것도 같다
- 로컬 측정(2026-11-10, 받아 둔 공개 자료를 원본 모양으로 바꿔 넣음): 9,278건(테코톡 717, 디스커션 1,056, 위키 1,835,
  코드 마크다운 4,207, PR 1,463) 40초, 최대 메모리 109MB

## 로컬 시험

```bash
GITHUB_TOKEN=$(gh auth token) PIPELINE_STORE=./local-store \
  uv run python -m collector collect --source github-teamdoc --repos 2026-Harudle
PIPELINE_STORE=./local-store uv run python -m refine run --source github-teamdoc
uv run --group dev pytest -q
```

## EC2 처음 준비하기(사용자가 직접)

매일 수집을 돌리는 수집용 EC2 를 처음 준비하는 순서다. 2026-11-05 에 실제 EC2 에서 확인한 환경을 기준으로 적었다.
서버 주소, 키 파일, 버킷 이름, 토큰 같은 값은 이 문서에 적지 않는다. `<...>` 자리는 팀 안에서 따로 받은 값으로 바꾼다.

### 0. 전제

| 항목 | 값 | 확인 방법 |
|---|---|---|
| 인스턴스 | t4g.micro(ARM, 메모리 1GB), 디스크 20GB | AWS 콘솔 인스턴스 세부 정보 |
| OS, 사용자 | Amazon Linux 2023, `ec2-user` | `grep PRETTY_NAME /etc/os-release`, `whoami` |
| 미리 떠 있는 것 | 보안 에이전트(약 110MB), docker, codedeploy. 처음 볼 때 여유 메모리 약 430MB, 스왑 없음 | `free -h`, `ps aux --sort=-rss \| head` |
| 기본 Python | 3.9(수집기는 3.12 로 고정, `.python-version`. uv 가 따로 받는다) | `python3 --version` |
| IAM 역할 | 원본 저장소 S3 폴더를 목록 보기, 읽기, 쓰기, 지우기 할 수 있는 역할. 수집기는 액세스 키를 쓰지 않고 이 역할로 S3 에 접근한다 | 아래 2번 |
| 보안 그룹 | 22번 포트가 내 IP 에 열려 있어야 접속된다 | 접속이 멈추다 `Operation timed out` 이면 이것 |
| 바깥 연결 | GitHub, YouTube Data API, 블로그들, 서비스 DB 에 나갈 수 있어야 한다 | 아래 2번 |

### 1. 접속

```bash
chmod 400 <키 파일 경로>.pem
ssh -i <키 파일 경로>.pem ec2-user@<EC2 공개 DNS 또는 IP>
```

- 사용자 이름은 `ec2-user` 다. `ubuntu`, `root` 로 들어가면 키가 거절되고 서버가 비밀번호(`Password:`)를 묻는다. 비밀번호를 넣지 말고 `Ctrl+C` 로 나온 뒤 사용자 이름을 고친다.
- 키 파일은 레포에 넣지 않는다.

### 2. 상태 확인(읽기만 한다)

```bash
whoami; grep PRETTY_NAME /etc/os-release; uname -m
free -h; df -h /
curl -s -o /dev/null -w "github %{http_code}\n" https://api.github.com
# 이 EC2 에 붙은 IAM 역할 이름(비밀값은 나오지 않는다). 아무것도 안 나오면 역할이 없어 S3 에 쓸 수 없다
TOKEN=$(curl -sX PUT http://169.254.169.254/latest/api/token -H "X-aws-ec2-metadata-token-ttl-seconds: 60")
curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/iam/security-credentials/; echo
```

### 3. 스왑 1GB 만들기

여유 메모리가 400MB 남짓이고 스왑이 없어서, 메모리가 모자라면 수집기나 다른 프로그램이 바로 죽는다. 안전장치로 1GB 스왑을 둔다. 수집기 자체는 실제 시험에서 최대 약 45MB 를 썼다.

```bash
sudo dd if=/dev/zero of=/swapfile bs=1M count=1024 && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab   # 다시 켜도 유지
free -h                                                            # Swap 1.0Gi 확인
```

### 4. 도구 설치와 코드 받기

```bash
sudo dnf install -y git nano
curl -LsSf https://astral.sh/uv/install.sh | sh                    # ~/.local/bin/uv
git clone -b main https://github.com/woowacourse-teams/2026-shout-outz.git ~/2026-shout-outz
cd ~/2026-shout-outz/data-pipeline
~/.local/bin/uv sync --no-dev                                      # Python 3.12(.python-version)와 라이브러리를 .venv 에
.venv/bin/python -c "import sys, boto3, requests, psycopg; print(sys.version.split()[0], 'ok')"   # 3.12.x ok
```

- 배포된 코드(`main`)를 받는다. 매일 실행도 `main` 의 코드로 돈다.
- 레포는 공개 레포라 토큰 없이 받는다.

### 5. 환경 변수 파일 만들기

비밀값은 이 파일에만 둔다(08 문서 C12). 레포, 채팅, 명령줄에 값을 적지 않는다.

```bash
sudo mkdir -p /etc/shout-outz
sudo cp deploy/data-pipeline.env.example /etc/shout-outz/data-pipeline.env
# 주인(ec2-user)만 읽고 쓰기. systemd 는 root 로 읽고, 손으로 돌릴 때는 ec2-user 가 읽는다
sudo chown ec2-user:ec2-user /etc/shout-outz/data-pipeline.env && sudo chmod 600 /etc/shout-outz/data-pipeline.env
nano /etc/shout-outz/data-pipeline.env                             # 저장 Ctrl+O 엔터, 나가기 Ctrl+X
ls -l /etc/shout-outz/data-pipeline.env                            # -rw------- ec2-user 확인
```

| 변수 | 넣을 값 | 없으면 |
|---|---|---|
| `PIPELINE_STORE` | `s3://<버킷>/shout-outz/data-pipeline`(버킷 이름은 08 문서 C13) | 실행하지 않는다 |
| `AWS_REGION` | `ap-northeast-2` | 기본값 `ap-northeast-2` |
| `GITHUB_TOKEN` | 팀 GitHub 계정의 fine-grained 토큰. Repository access 는 Public repositories, Permissions 는 고르지 않는다(06 문서 5-8). 만료일을 적어 두고 그 전에 바꾼다 | GitHub 원천 네 개가 오류 |
| `YOUTUBE_API_KEY` | YouTube Data API v3 키(04 문서 7-3) | 테코톡이 오류 |
| `SERVICE_DB_HOST`, `SERVICE_DB_PORT`, `SERVICE_DB_NAME`, `SERVICE_DB_USER` | 서비스 DB 읽기 전용 접속 정보(03 문서 2-2) | 블로그가 오류 |
| `SERVICE_DB_PASSWORD` | 읽기 전용 계정 비밀번호. 비우면 EC2 역할로 RDS IAM 인증 토큰을 만든다(아래 전제가 모두 갖춰져야 한다) | |

값이 아직 없는 원천은 비워 두고 다른 원천부터 시험해도 된다. 그 원천만 오류로 남고 나머지는 돈다.

- **비밀번호를 비워 RDS IAM 인증을 쓸 때의 전제:** RDS 인스턴스에서 IAM 데이터베이스 인증이 켜져 있어야 하고, DB 사용자에게 `rds_iam` 역할이 있어야 하며(PostgreSQL: `GRANT rds_iam TO <사용자>;`), EC2 역할에 그 DB 사용자로의 `rds-db:connect` 권한이 있어야 한다. 하나라도 없으면 블로그 대상 목록을 읽지 못한다. 셋을 갖추기 어려우면 비밀번호를 넣는다.
- **값 적는 법:** 이 파일은 systemd(`EnvironmentFile`) 와 셸(`set -a && . 파일`) 이 함께 읽는다. 값에 `$`, 공백, 따옴표, `#` 가 없게 한다. 셸은 `$` 를 변수로 풀어 쓰지만 systemd 는 그대로 읽어서, 손으로 돌릴 때와 매일 실행이 서로 다른 값을 쓸 수 있다. 비밀번호에 이런 글자가 꼭 들어가야 하면 두 쪽에서 같은 값으로 읽히는지 `echo` 와 `systemctl show data-pipeline -p Environment` 로 확인한다.

### 6. 손으로 원천 하나씩 돌려 보기

매일 실행과 같은 환경 변수를 읽어 짧은 것부터 확인한다. 끝나면 마지막 줄에 실행 id, 상태, 원천별 숫자가 나온다. `"status": "success"` 면 S3 저장까지 된 것이다.

```bash
cd ~/2026-shout-outz/data-pipeline
set -a && . /etc/shout-outz/data-pipeline.env && set +a
export PIPELINE_HOST=ec2

# 1) S3 쓰기 확인을 겸해 위키 한 레포(수 초)
.venv/bin/python -m collector collect --source github-wiki --repos <레포 이름>
# 2) 테코톡(영상 정보만, 자막은 집 컴퓨터에서 따로)
.venv/bin/python -m collector collect --source youtube-tekotalk
# 3) 블로그(서비스 DB 접속 정보가 있을 때)
.venv/bin/python -m collector collect --source blog-post
```

- 손으로 돌려 받은 문서도 진짜 원본이다. 시험용으로 따로 지울 필요가 없다. 다음 실행부터 아는 문서가 되어 다시 받지 않는다.
- 코드 마크다운(약 45분), PR(첫 실행 5~6시간)처럼 오래 걸리는 원천은 SSH 가 끊기면 멈춘다. 아래 7번처럼 systemd 로 돌린다.
- 결과 보고서 보기: `aws s3 ls "$PIPELINE_STORE/reports/"`, `aws s3 cp "$PIPELINE_STORE/reports/<run_id>.json" - | head -50`

### 7. 첫 전체 실행과 매일 실행 켜기

```bash
cd ~/2026-shout-outz/data-pipeline
sudo cp deploy/data-pipeline.service deploy/data-pipeline.timer /etc/systemd/system/
sudo systemctl daemon-reload

# 첫 전체 실행을 지금 바로(SSH 를 끊어도 계속 돈다). 끝나기를 기다리지 않고 바로 돌아온다
sudo systemctl start --no-block data-pipeline.service
systemctl status data-pipeline.service --no-pager                 # active (running) 이면 도는 중
journalctl -u data-pipeline.service -f                             # 실행 로그(나가기 Ctrl+C, 실행은 계속)

# 매일 새벽 3시(한국 시각) 실행 켜기
sudo systemctl enable --now data-pipeline.timer
systemctl list-timers data-pipeline.timer                          # 다음 실행 시각
```

- 첫 실행이 다음 날 새벽 3시까지 이어지면 그날 실행은 잠금(`state/lock.json`)을 보고 `skipped` 로 끝난다. 겹쳐 돌지 않는다.
- 서비스 설정: 메모리 상한 700MB(`MemoryMax`), 최대 13시간(`TimeoutStartSec`), 낮은 우선순위(`Nice=10`). `deploy/data-pipeline.service` 참고.

### 7-1. 첫 정제

원본이 약 5만 3천 건이라 1만 건씩 몇 번 이어 돌린다(09 문서 R20-6). 작은 원천부터 하면 빨리 끝나는 원천의 결과를 먼저 본다.
수집이 도는 동안에는 잠금 때문에 `skipped` 로 끝나므로, 수집이 끝난 뒤에 돌린다.

```bash
cd ~/2026-shout-outz/data-pipeline
set -a && . /etc/shout-outz/data-pipeline.env && set +a
export PIPELINE_HOST=ec2
.venv/bin/python -m refine run --source youtube-tekotalk blog-post github-discussion github-wiki
.venv/bin/python -m refine run --source github-teamdoc
.venv/bin/python -m refine run --source github-pr     # 끝 줄의 remaining 이 0 이 될 때까지 되풀이한다
```

- 매일 실행(systemd)은 수집이 끝나면 `refine run --after-collect` 로 이어서 정제한다. 첫 정제를 매일 실행에 맡겨도 하루 1만 건씩 줄어든다.
- 정제 보고서 보기: `aws s3 ls "$PIPELINE_STORE/refine/reports/"`, `aws s3 cp "$PIPELINE_STORE/refine/reports/<run_id>.json" - | head -80`

### 8. 코드 고친 뒤 올리기

고친 코드가 `main` 에 배포된 뒤 EC2 에서 받는다.

```bash
cd ~/2026-shout-outz && git switch main && git pull && cd data-pipeline && ~/.local/bin/uv sync --no-dev
# .venv 의 Python 이 3.12 가 아니면 가상 환경을 다시 만든다
.venv/bin/python --version | grep -q ' 3\.12' || { rm -rf .venv && ~/.local/bin/uv sync --no-dev; }
# systemd 파일이 바뀌었을 때만(정제를 더한 판부터 매일 실행이 수집 뒤 정제까지 돈다)
sudo cp deploy/data-pipeline.service deploy/data-pipeline.timer /etc/systemd/system/ && sudo systemctl daemon-reload
```

### 9. 막힐 때

| 증상 | 원인 | 할 일 |
|---|---|---|
| SSH 가 `Password:` 를 묻는다 | 사용자 이름이 틀려 키가 거절됨 | `ec2-user@` 로 접속 |
| SSH 가 멈추다 `Operation timed out` | 보안 그룹 22번 포트가 내 IP 에 닫힘 | 보안 그룹 인바운드 규칙 확인 |
| `PIPELINE_STORE 가 없습니다` | 환경 변수 파일을 읽지 않았거나 값이 비었음 | `set -a && . /etc/shout-outz/data-pipeline.env && set +a` 다시, 파일 값 확인 |
| S3 에서 `AccessDenied` | EC2 에 IAM 역할이 없거나 권한이 모자람 | 2번의 역할 이름 확인 |
| 상태가 `skipped`, `lock_held_by` 가 나옴 | 다른 실행이 돌고 있음(`systemctl status data-pipeline.service`) | 끝나기를 기다린다. 멈춘 실행이 남긴 잠금은 12시간 뒤 풀린다 |
| 상태가 `skipped`, `lock_held_by` 에 `unreadable` | 잠금 파일이 깨짐 | 도는 실행이 없는지 확인한 뒤 `aws s3 rm "$PIPELINE_STORE/state/lock.json"` |
| `.venv/bin/python --version` 이 3.12 가 아님(오류 경로에 `python3.15` 등) | `.python-version` 이 없던 때 uv 가 가장 최신 Python 을 받음(2026-11-05 겪음). 시험은 3.12 에서만 했다 | `rm -rf .venv && ~/.local/bin/uv sync --no-dev` 뒤 `.venv/bin/python --version` 이 3.12 인지 확인 |
| `uv: command not found` | 새 셸에 PATH 가 아직 없음 | `~/.local/bin/uv` 로 부르거나 다시 접속 |
| 로그에 `Killed`, 서비스가 `oom-kill` | 메모리 부족 | 3번 스왑 확인(`free -h`), `journalctl -u data-pipeline.service` 확인 |
