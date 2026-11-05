"""테코톡 자막 주고받기(04 문서 5-4, 08 문서 7-3). yt-dlp, YouTube 는 부르지 않는다."""

import json
from datetime import timedelta
from pathlib import Path

from collector import lock, subtitles
from collector.config import Settings
from collector.index import Index
from collector.keys import raw_path
from collector.report import iso
from collector.subtitles_local import pick

from test_core import T0, clock_from
from test_tekotalk import FakeYouTube, run, video

SUB = {"lang": "ko", "kind": "manual", "format": "vtt", "fetched_at": "2026-11-06T10:00:00Z",
       "vtt": "WEBVTT\nKind: captions\nLanguage: ko\n\n00:00:06.400 --> 00:00:12.600\n안녕하세요"}


def world(n):
    """실제 영상 ID 처럼 11자(make_script 가 모양을 검사한다)."""
    ids = [f"v{i:010d}" for i in range(n)]
    return [(v, "public") for v in ids], {v: video(v) for v in ids}


def collected(tmp_path, n=3):
    store, _, _ = run(tmp_path, FakeYouTube(*world(n)))
    return store


def script_ids(text):
    compile(text, "subtitles_local.py", "exec")
    ns = {"__name__": "not_main"}
    exec(text, ns)
    return ns["VIDEO_IDS"]


def result_file(tmp_path, rows, skipped=()):
    p = tmp_path / "tekotalk-subtitles-20261106-100000.json"
    p.write_text(json.dumps({"kind": "tekotalk-subtitles", "made_at": "2026-11-06T09:59:00Z",
                             "results": rows, "skipped": list(skipped)}, ensure_ascii=False), encoding="utf-8")
    return str(p)


def do_import(tmp_path, store, path):
    settings = Settings(store_url=str(tmp_path), host="ec2")
    return subtitles.import_result(settings, store, path, clock=clock_from(T0 + timedelta(days=100)))


def test_make_script_all_missing_and_only(tmp_path):
    store = collected(tmp_path / "s", 3)
    text = subtitles.make_script(store, all_missing=True)
    assert script_ids(text) == ["v0000000000", "v0000000001", "v0000000002"]
    assert "VIDEO_IDS = []" not in text

    idx = Index(store, "youtube-tekotalk").load()
    idx.entries["yt:v0000000001"]["has_subtitle"] = True
    idx.entries["yt:v0000000002"]["gone"] = {"at": "x", "reason": "not_found"}
    idx.save()
    assert script_ids(subtitles.make_script(store, all_missing=True)) == ["v0000000000"]

    text = subtitles.make_script(store, only=["v0000000000", "yt:v0000000002", "v0000000001", "nope0000000"])
    assert script_ids(text) == ["v0000000000", "v0000000002"]
    assert "# 뺌(색인에 없음, 매일 수집이 아직 받지 않은 영상): nope0000000" in text
    assert "# 뺌(이미 자막을 받음): v0000000001" in text
    # 영상 ID 모양이 아니면(줄바꿈 등) 스크립트를 만들지 않는다
    for bad in ["v0000000000\nimport os", "short", "v000000000!"]:
        try:
            subtitles.make_script(store, only=[bad])
            assert False, bad
        except ValueError:
            pass


def test_import_unreadable_raw_does_not_stop_others(tmp_path):
    store = collected(tmp_path / "s", 2)
    store.write_bytes(raw_path("youtube-tekotalk", "yt:v0000000000"), b"{broken")
    rep = do_import(tmp_path, store, result_file(tmp_path, [{"video_id": "v0000000000", "subtitle": SUB},
                                                            {"video_id": "v0000000001", "subtitle": SUB}]))
    sr = rep.sources["youtube-tekotalk"]
    assert sr.counts["changed"] == 1 and sr.failure_reasons == {"raw_unreadable": 1}
    assert store.read_json(raw_path("youtube-tekotalk", "yt:v0000000001"))["subtitle"]["vtt"] == SUB["vtt"]


def test_template_runs_without_ids(capsys):
    from collector import subtitles_local
    assert subtitles_local.VIDEO_IDS == []
    assert subtitles_local.main() == 0  # ID 가 없으면 가상 환경도 만들지 않고 끝난다
    assert "받을 영상 ID 가 없습니다" in capsys.readouterr().out


def test_pick_prefers_manual_then_auto():
    f = {"ko": Path("a.ko.vtt"), "ko-orig": Path("a.ko-orig.vtt")}
    assert pick(f, {"ko"})[:2] == ("ko", "manual")
    assert pick(f, set())[:2] == ("ko-orig", "auto")
    assert pick({"ko-KR": Path("b")}, {"ko-KR"})[:2] == ("ko-KR", "manual")
    assert pick({}, {"ko"}) is None


def test_import_fills_subtitle_and_skips(tmp_path):
    store = collected(tmp_path / "s", 3)
    path = result_file(tmp_path, [
        {"video_id": "v0000000000", "subtitle": SUB},
        {"video_id": "unknown", "subtitle": SUB},
        {"video_id": "v0000000001", "subtitle": {"lang": "ko", "kind": "auto"}},  # vtt 없음
    ], skipped=[{"video_id": "v0000000002", "reason": "no_korean_subtitle"}])
    rep = do_import(tmp_path, store, path)

    assert rep.status == "success"
    assert rep.scope == {"command": "subtitles import", "file": Path(path).name}
    sr = rep.sources["youtube-tekotalk"]
    assert sr.counts == {"new": 0, "changed": 1, "unchanged": 0, "gone": 0, "failed": 0}
    assert sr.extra["skipped"] == [{"external_key": "yt:unknown", "reason": "not_in_index"},
                                   {"external_key": "yt:v0000000001", "reason": "invalid_subtitle"}]
    assert sr.extra["script_skipped"] == [{"video_id": "v0000000002", "reason": "no_korean_subtitle"}]
    assert sr.extra["without_subtitle"] == 2

    rec = store.read_json(raw_path("youtube-tekotalk", "yt:v0000000000"))
    assert rec["version"] == 2 and rec["subtitle"] == SUB
    assert rec["fetched_at"] == iso(T0 + timedelta(days=100, seconds=2))  # import 한 시각(잠금 다음 시계)
    assert rec["video"]["id"] == "v0000000000"  # 영상 정보는 그대로
    e = Index(store, "youtube-tekotalk").load().get("yt:v0000000000")
    assert e["has_subtitle"] is True and e["version"] == 2 and e["fetched_at"] == rec["fetched_at"]

    data = store.read_json(f"reports/{rep.run_id}.json")
    assert data["scope"]["command"] == "subtitles import" and data["status"] == "success"
    assert data["sources"]["youtube-tekotalk"]["changed"] == 1
    assert store.read_bytes("state/lock.json") is None

    # 같은 파일을 다시 올리면 이미 자막이 있어 건너뛴다
    rep = do_import(tmp_path, store, path)
    sr = rep.sources["youtube-tekotalk"]
    assert sr.counts["changed"] == 0
    assert {"external_key": "yt:v0000000000", "reason": "already_has_subtitle"} in sr.extra["skipped"]
    assert store.read_json(raw_path("youtube-tekotalk", "yt:v0000000000"))["version"] == 2
    # 자막을 받은 영상은 스크립트에서 빠진다
    assert script_ids(subtitles.make_script(store, all_missing=True)) == ["v0000000001", "v0000000002"]


def test_import_respects_lock(tmp_path):
    store = collected(tmp_path / "s", 1)
    assert lock.acquire(store, "daily", "ec2", T0 + timedelta(days=100))[0]
    rep = do_import(tmp_path, store, result_file(tmp_path, [{"video_id": "v0000000000", "subtitle": SUB}]))
    assert rep.status == "skipped" and rep.scope["lock_held_by"]["run_id"] == "daily"
    assert store.read_json(raw_path("youtube-tekotalk", "yt:v0000000000"))["subtitle"] is None
    assert store.read_json("state/lock.json")["run_id"] == "daily"  # 남의 잠금은 풀지 않는다


def test_import_bad_file_is_error(tmp_path):
    store = collected(tmp_path / "s", 1)
    p = tmp_path / "x.json"
    p.write_text(json.dumps({"kind": "other"}), encoding="utf-8")
    rep = do_import(tmp_path, store, str(p))
    assert rep.status == "failure"
    assert rep.sources["youtube-tekotalk"].errors[0]["type"] == "bad_file"
    assert store.read_bytes("state/lock.json") is None
