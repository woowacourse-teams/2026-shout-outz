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

`PIPELINE_STORE` 는 S3 주소(`s3://techcourse-project-2026/shout-outz/data-pipeline`) 또는 로컬 폴더다. 둘 다 같은 모양이다.

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

```bash
sudo apt install -y python3-venv git
git clone https://github.com/woowacourse-teams/2026-shout-outz.git ~/2026-shout-outz
cd ~/2026-shout-outz && git switch <브랜치> && cd data-pipeline
curl -LsSf https://astral.sh/uv/install.sh | sh && ~/.local/bin/uv sync --no-dev
sudo mkdir -p /etc/shout-outz && sudo cp deploy/data-pipeline.env.example /etc/shout-outz/data-pipeline.env
sudo chmod 600 /etc/shout-outz/data-pipeline.env && sudo nano /etc/shout-outz/data-pipeline.env
sudo cp deploy/data-pipeline.service deploy/data-pipeline.timer /etc/systemd/system/
sudo systemctl daemon-reload && sudo systemctl enable --now data-pipeline.timer
systemctl list-timers data-pipeline.timer        # 다음 실행 시각
journalctl -u data-pipeline.service -f           # 실행 로그
```
