"""테코톡 자막 받기 스크립트(집 컴퓨터에서 실행, 04 문서 5-4).

이 파일은 `python -m collector subtitles script ...` 가 영상 ID 를 넣어 내주는 본이다.
수집기 코드를 쓰지 않고 Python 3 표준 라이브러리만 쓴다(집 컴퓨터에는 수집기가 없다).

하는 일
1. 이 파일 옆에 임시 가상 환경을 만들고 최신 yt-dlp 를 설치한다(끝나면 지운다)
2. 영상마다 한국어 자막 1개를 받는다. 사람이 단 자막(manual) 우선, 없으면 자동 자막(auto)
   - 요청 사이 1초 쉼(--sleep-requests 1), User-Agent 는 yt-dlp 기본값 그대로(04 문서 8장)
   - "Sign in to confirm you're not a bot" 이 나오면 남은 영상은 시도하지 않고 멈춘다
3. 결과를 이 파일 옆의 JSON 파일 하나로 모은다. 이 파일을 관리 웹에 올리면 수집기가 저장한다

실행: python3 subtitles_local.py
"""

from __future__ import annotations

import json
import shutil
import subprocess
import sys
import tempfile
import venv
from datetime import datetime, timezone
from pathlib import Path

VIDEO_IDS = []

SUB_LANGS = "ko.*"  # ko, ko-KR, ko-orig(자동 자막 원문) 등 한국어 변형 모두
TIMEOUT_SEC = 300   # 영상 하나에 쓰는 가장 긴 시간
BOT_MARKERS = ("confirm you're not a bot", "confirm you’re not a bot", "confirm you are not a bot")


def now_iso() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def lang_rank(lang: str) -> tuple:
    """같은 종류 안에서 고르는 순서: ko-orig(자동 자막의 원문), ko, ko-KR, 그 밖의 ko 변형."""
    order = {"ko-orig": 0, "ko": 1, "ko-KR": 2}
    return (order.get(lang, 9), lang)


def pick(files: dict[str, Path], manual_langs: set[str]) -> tuple[str, str, Path] | None:
    """받은 자막 파일(언어 → 경로) 중 하나를 고른다. 사람 자막 우선, 없으면 자동 자막.

    yt-dlp 는 같은 언어에 사람 자막과 자동 자막이 다 있으면 사람 자막을 쓴다.
    그래서 언어가 info.json 의 subtitles(사람 자막)에 있으면 manual, 아니면 auto 다.
    """
    manual = sorted((l for l in files if l in manual_langs), key=lang_rank)
    if manual:
        return manual[0], "manual", files[manual[0]]
    auto = sorted(files, key=lang_rank)
    if auto:
        return auto[0], "auto", files[auto[0]]
    return None


def make_venv(base: Path) -> tuple[Path, Path]:
    tmp = Path(tempfile.mkdtemp(prefix=".tekotalk-subtitles-venv-", dir=base))
    print(f"임시 가상 환경 만드는 중: {tmp}", flush=True)
    venv.create(tmp, with_pip=True)
    py = tmp / ("Scripts/python.exe" if sys.platform == "win32" else "bin/python")
    print("최신 yt-dlp 설치 중...", flush=True)
    subprocess.run([str(py), "-m", "pip", "install", "--quiet", "--upgrade", "pip", "yt-dlp[default]"], check=True)
    out = subprocess.run([str(py), "-m", "yt_dlp", "--version"], capture_output=True, text=True)
    print(f"yt-dlp {out.stdout.strip()} 준비됨", flush=True)
    return tmp, py


def last_error(text: str) -> str:
    lines = [l.strip() for l in text.splitlines() if l.strip()]
    errors = [l for l in lines if l.startswith("ERROR")]
    if errors:
        return errors[-1][:300]
    return lines[-1][:300] if lines else "알 수 없는 오류"


def fetch_one(py: Path, vid: str, work: Path) -> tuple[dict | None, str | None, bool]:
    """영상 하나의 자막. (subtitle, 건너뛴 이유, 봇 확인에 막혔나)."""
    d = work / vid
    d.mkdir(parents=True, exist_ok=True)
    cmd = [str(py), "-m", "yt_dlp", "--skip-download", "--write-subs", "--write-auto-subs",
           "--sub-langs", SUB_LANGS, "--sub-format", "vtt", "--write-info-json", "--no-playlist",
           "--sleep-requests", "1", "-o", str(d / "%(id)s.%(ext)s"),
           "--", f"https://www.youtube.com/watch?v={vid}"]  # ID 가 - 로 시작해도 옵션으로 읽히지 않게
    try:
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=TIMEOUT_SEC)
    except subprocess.TimeoutExpired:
        return None, "timeout", False
    log = (r.stdout or "") + "\n" + (r.stderr or "")
    if any(m in log.lower() for m in BOT_MARKERS):
        return None, "bot_check", True
    files: dict[str, Path] = {}
    for p in d.glob(f"{vid}.*.vtt"):
        files[p.name[len(vid) + 1:-len(".vtt")]] = p
    manual_langs: set[str] = set()
    info_path = d / f"{vid}.info.json"
    if info_path.exists():
        try:
            manual_langs = set((json.loads(info_path.read_text(encoding="utf-8")).get("subtitles") or {}).keys())
        except ValueError:
            pass
    chosen = pick(files, manual_langs)
    if chosen is None:
        if r.returncode != 0:
            return None, f"yt_dlp_error: {last_error(log)}", False
        return None, "no_korean_subtitle", False
    lang, kind, path = chosen
    vtt = path.read_text(encoding="utf-8")
    return {"lang": lang, "kind": kind, "format": "vtt", "fetched_at": now_iso(), "vtt": vtt}, None, False


def save(out: Path, made_at: str, results: list, skipped: list) -> None:
    body = {"kind": "tekotalk-subtitles", "made_at": made_at, "results": results, "skipped": skipped}
    tmp = out.with_name(out.name + ".tmp")
    tmp.write_text(json.dumps(body, ensure_ascii=False, indent=1), encoding="utf-8")
    tmp.replace(out)


def main() -> int:
    if sys.version_info < (3, 10):
        print("Python 3.10 이상이 필요합니다(yt-dlp 가 요구).", file=sys.stderr)
        return 1
    ids = list(dict.fromkeys(VIDEO_IDS))
    if not ids:
        print("받을 영상 ID 가 없습니다. 관리 웹에서 스크립트를 다시 받아 주세요.")
        return 0
    base = Path(__file__).resolve().parent
    made_at = now_iso()
    out = base / f"tekotalk-subtitles-{datetime.now(timezone.utc).strftime('%Y%m%d-%H%M%S')}.json"
    results: list[dict] = []
    skipped: list[dict] = []
    print(f"영상 {len(ids)}편의 한국어 자막을 받습니다. 결과 파일: {out}", flush=True)

    venv_dir, py = make_venv(base)
    work = Path(tempfile.mkdtemp(prefix=".tekotalk-subtitles-work-", dir=base))
    try:
        for n, vid in enumerate(ids, 1):
            print(f"[{n}/{len(ids)}] {vid} 받는 중...", flush=True)
            sub, reason, bot = fetch_one(py, vid, work)
            if bot:
                rest = ids[n - 1:]
                skipped.append({"video_id": vid, "reason": "bot_check"})
                skipped += [{"video_id": v, "reason": "not_tried_after_bot_check"} for v in rest[1:]]
                print(f"  봇 확인(Sign in to confirm you're not a bot)에 막혔습니다. 남은 {len(rest)}편은 시도하지 않고 멈춥니다.", flush=True)
                print("  시간이 지난 뒤 관리 웹에서 스크립트를 다시 받아 실행해 주세요.", flush=True)
                save(out, made_at, results, skipped)
                break
            if sub:
                results.append({"video_id": vid, "subtitle": sub})
                kind = "사람 자막" if sub["kind"] == "manual" else "자동 자막"
                print(f"  {kind}({sub['lang']}) 받음", flush=True)
            else:
                skipped.append({"video_id": vid, "reason": reason})
                print(f"  건너뜀: {reason}", flush=True)
            save(out, made_at, results, skipped)  # 중간에 멈춰도 받은 데까지 남도록 매번 쓴다
    finally:
        shutil.rmtree(work, ignore_errors=True)
        shutil.rmtree(venv_dir, ignore_errors=True)

    print("", flush=True)
    print(f"끝: 받음 {len(results)}편, 건너뜀 {len(skipped)}편", flush=True)
    print(f"결과 파일: {out}", flush=True)
    print("이 파일을 관리 웹의 자막 결과 올리기에 올려 주세요.", flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
