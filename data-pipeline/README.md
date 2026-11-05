# data-pipeline

샤라웃 매일 데이터 파이프라인. 지금은 ① 원본 수집기(`collector/`)만 있다.

- 설계 문서(작업 폴더): `01-pipeline-overview.md`(전체 단계), `03`(블로그), `04`(테코톡), `06`(팀 문서), `07`(PR), `08-collect-component.md`(수집 컴포넌트)
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

`PIPELINE_STORE` 는 S3 주소(`s3://techcourse-project-2026/shout-outz/data-pipeline`) 또는 로컬 폴더다. 둘 다 같은 모양이다. 꼭 적어야 한다(없으면 실행하지 않는다. 기본 폴더에 조용히 쓰고 성공으로 끝나는 일을 막으려고).

## 명령

```bash
uv sync                                                    # 처음 한 번
uv run python -m collector collect                         # 모든 원천(테코톡 → 블로그 → 팀 문서 → PR)
uv run python -m collector collect --source github-pr      # 원천 하나
uv run python -m collector collect --source blog-post --only https://team.tistory.com/12          # 그 건만 다시 받기
uv run python -m collector collect --source github-pr --repos 2026-Chalkak                       # 레포 몇 개로 시험
uv run python -m collector subtitles script --all-missing > subtitles_local.py                   # 집 컴퓨터용 자막 스크립트
uv run python -m collector subtitles import subtitles-result.json                                # 자막 결과 저장
```

- 원천 이름: `youtube-tekotalk`, `blog-post`, `github-teamdoc`, `github-wiki`, `github-discussion`, `github-pr`
- 환경 변수: `deploy/data-pipeline.env.example`
- 끝나면 표준 출력에 실행 id, 상태, 원천별 숫자가 나오고, 자세한 것은 `reports/{run_id}.json` 에 남는다.

## 로컬 시험

```bash
GITHUB_TOKEN=$(gh auth token) PIPELINE_STORE=./local-store \
  uv run python -m collector collect --source github-teamdoc --repos 2026-Harudle
uv run --group dev pytest -q
```

## EC2 에 올리기(사용자가 직접)

수집용 EC2 는 Amazon Linux 2023(ARM, t4g.micro), 사용자 `ec2-user` 다. 기본 Python 이 3.9 라서 uv 가 Python 3.12 를 따로 받아 `.venv` 를 만든다.

EC2 에는 원본 저장소 S3 폴더(`PIPELINE_STORE`)를 읽고 쓰고 지울 수 있는 IAM 역할이 붙어 있어야 한다(목록 보기, 읽기, 쓰기, 지우기). 수집기는 액세스 키를 쓰지 않고 이 역할로 S3 에 접근한다.

```bash
sudo dnf install -y git nano
git clone -b <브랜치> https://github.com/woowacourse-teams/2026-shout-outz.git ~/2026-shout-outz
cd ~/2026-shout-outz/data-pipeline
curl -LsSf https://astral.sh/uv/install.sh | sh && ~/.local/bin/uv sync --no-dev
sudo mkdir -p /etc/shout-outz && sudo cp deploy/data-pipeline.env.example /etc/shout-outz/data-pipeline.env
# 주인(ec2-user)만 읽기. systemd 는 root 로 읽고, 손으로 돌릴 때는 ec2-user 가 읽는다
sudo chown ec2-user:ec2-user /etc/shout-outz/data-pipeline.env && sudo chmod 600 /etc/shout-outz/data-pipeline.env
nano /etc/shout-outz/data-pipeline.env

# 손으로 한 번 돌려 보기(원천 하나씩). 매일 실행과 같은 환경 변수를 읽는다
set -a && . /etc/shout-outz/data-pipeline.env && set +a
PIPELINE_HOST=ec2 .venv/bin/python -m collector collect --source youtube-tekotalk

# 매일 실행 켜기
sudo cp deploy/data-pipeline.service deploy/data-pipeline.timer /etc/systemd/system/
sudo systemctl daemon-reload && sudo systemctl enable --now data-pipeline.timer
systemctl list-timers data-pipeline.timer        # 다음 실행 시각
journalctl -u data-pipeline.service -f           # 실행 로그
```
