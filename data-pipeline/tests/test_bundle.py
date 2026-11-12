"""원본 묶음 넣기(bundle import): 같은 원본 모양으로 넣고, 키가 맞지 않거나 이미 있으면 넣지 않는다."""

import gzip
import json
from datetime import timedelta

from collector.config import Settings
from collector.bundle import import_bundle
from collector.index import Index
from collector.keys import blog_key, raw_path
from collector.store import LocalStore

from test_core import T0, clock_from

URL = "https://shinjungoh.tistory.com/entry/%EC%9A%B0%ED%85%8C%EC%BD%94-7%EA%B8%B0-%ED%8F%AC%EA%B2%8C%EB%8D%94%EA%B0%80-%EC%9D%BC%ED%95%98%EB%8A%94-%EB%B0%A9%EB%B2%95"


def record(url, key=None, **more):
    return {"source": "blog-post", "external_key": key or blog_key(url), "url": url,
            "blog_root": "https://shinjungoh.tistory.com", "platform": "tistory",
            "found_in": [{"archive_id": 1133, "project_title": "포게더"}], "body_format": "html", "body": "<html>본문</html>",
            "fetched_at": "2026-10-21T23:26:38Z", "origin": "archive-2026-10-22", **more}


def write_bundle(path, rows):
    with gzip.open(path, "wt", encoding="utf-8") as f:
        for r in rows:
            f.write((r if isinstance(r, str) else json.dumps(r, ensure_ascii=False)) + "\n")


def test_bundle_import_writes_same_shape_and_skips_known(tmp_path):
    store = LocalStore(str(tmp_path / "store"))
    settings = Settings(store_url=str(tmp_path / "store"))
    bundle = tmp_path / "b.jsonl.gz"
    write_bundle(bundle, [record(URL), record("https://a.tistory.com/1", key="post:" + "0" * 40), "not json",
                          {"source": "nope", "external_key": "x"}])
    rep = import_bundle(settings, store, str(bundle), clock=clock_from(T0))
    sr = rep.sources["blog-post"]
    assert sr.counts["new"] == 1 and sr.counts["failed"] == 3
    assert sr.failure_reasons == {"key_mismatch": 1, "bad_json": 1, "bad_record": 1}
    key = "post:8738139d847566747c42198b6b03dba5c4df2952"  # CLAUDE.md 예시 키와 같다
    rec = store.read_json(raw_path("blog-post", key))
    assert rec["version"] == 1 and rec["fetched_at"] == "2026-10-21T23:26:38Z" and rec["body"] == "<html>본문</html>"
    assert rec["origin"] == "archive-2026-10-22" and rec["found_in"][0]["project_title"] == "포게더"
    entry = Index(store, "blog-post").load().get(key)
    assert entry["url"] == URL and entry["blog_root"] == "https://shinjungoh.tistory.com" and entry["gone"] is None
    assert rep.scope["command"] == "bundle import" and store.read_bytes("state/lock.json") is None
    # 두 번째: 이미 아는 키는 건너뛴다(덮어쓰지 않음)
    write_bundle(bundle, [record(URL, body="<html>다른 본문</html>")])
    rep = import_bundle(settings, store, str(bundle), clock=clock_from(T0 + timedelta(days=1)))
    assert rep.sources["blog-post"].counts["unchanged"] == 1
    assert store.read_json(raw_path("blog-post", key))["body"] == "<html>본문</html>"


def test_velog_graphql_url_matches_rss_shape():
    from collector.sources.blog_platforms import velog_post_url
    assert velog_post_url("dev", "목록-(1)!") == "https://velog.io/@dev/%EB%AA%A9%EB%A1%9D-(1)!"
    assert blog_key(velog_post_url("dev", "목록")) == blog_key("https://velog.io/@dev/%EB%AA%A9%EB%A1%9D")
