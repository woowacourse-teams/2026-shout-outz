"""테코톡 원천(04 문서). 네트워크 없이 가짜 YouTube Data API 로 시험한다."""

from datetime import timedelta

from collector.config import Settings
from collector.index import Index
from collector.keys import raw_path
from collector.runner import collect
from collector.sources.tekotalk import PLAYLIST_ID, TekotalkSource
from collector.store import LocalStore

from test_core import T0, clock_from

PAGE = 50


class Resp:
    def __init__(self, status, body):
        self.status_code, self._body = status, body
        self.text = str(body)

    def json(self):
        return self._body


def video(vid, privacy="public"):
    return {
        "id": vid, "etag": "버리는 칸",
        "snippet": {"title": f"[10분 테코톡] {vid}", "description": "설명", "publishedAt": "2021-06-03T02:43:22Z",
                    "channelId": "UC-mOekGSesms0agFntnQang", "channelTitle": "우아한테크", "tags": ["버리는 칸"],
                    "thumbnails": {"default": {"url": "d"}, "high": {"url": f"https://i.ytimg.com/vi/{vid}/hqdefault.jpg"}}},
        "contentDetails": {"duration": "PT23M54S", "caption": "true", "definition": "hd"},
        "statistics": {"viewCount": "28469", "likeCount": "727", "commentCount": "3"},
        "status": {"privacyStatus": privacy, "embeddable": True},
    }


class FakeYouTube:
    """playlist: [(영상 ID, 재생목록 항목의 privacyStatus)], videos: {영상 ID: videos.list 가 줄 영상}."""

    def __init__(self, playlist, videos):
        self.playlist, self.videos = playlist, videos
        self.calls = []
        self.fail_page = None  # 이 쪽 번호(0부터)에서 500

    def get(self, url, params=None, headers=None, **kw):
        assert headers["X-Goog-Api-Key"] == "k" and "key" not in params  # 키는 머리글로만
        name = url.rsplit("/", 1)[-1]
        self.calls.append((name, dict(params)))
        if name == "playlistItems":
            assert params["playlistId"] == PLAYLIST_ID
            page = int(params.get("pageToken") or 0)
            if page == self.fail_page:
                return Resp(500, {"error": {"message": "backend error"}})
            rows = self.playlist[page * PAGE:(page + 1) * PAGE]
            body = {"items": [{"contentDetails": {"videoId": v}, "status": {"privacyStatus": p}} for v, p in rows]}
            if (page + 1) * PAGE < len(self.playlist):
                body["nextPageToken"] = str(page + 1)
            return Resp(200, body)
        if name == "videos":
            ids = params["id"].split(",")
            assert len(ids) <= 50
            return Resp(200, {"items": [self.videos[i] for i in ids if i in self.videos]})
        raise AssertionError(url)

    def count(self, name):
        return sum(1 for n, _ in self.calls if n == name)


_RUNS = [0]


def run(tmp_path, yt, key="k", **kw):
    _RUNS[0] += 1
    store = LocalStore(str(tmp_path))
    settings = Settings(store_url=str(tmp_path), youtube_api_key=key)
    rep = collect(settings, ["youtube-tekotalk"], store=store, make_source=lambda n: TekotalkSource(http=yt),
                  clock=clock_from(T0 + timedelta(days=_RUNS[0])), **kw)
    return store, rep, rep.sources.get("youtube-tekotalk")


def world(n):
    ids = [f"v{i:03d}" for i in range(n)]
    return [(v, "public") for v in ids], {v: video(v) for v in ids}


def test_first_run_pages_batches_and_skips_private(tmp_path):
    playlist, videos = world(119)
    playlist.insert(10, ("secret1", "private"))  # 재생목록에서 비공개로 보이는 영상
    playlist.append(("vanish", "public"))        # 목록에는 공개인데 videos.list 가 안 주는 영상
    yt = FakeYouTube(playlist, videos)
    store, rep, sr = run(tmp_path, yt)

    assert rep.status == "success"
    assert sr.counts == {"new": 119, "changed": 0, "unchanged": 0, "gone": 0, "failed": 0}
    assert sr.failures == [] and sr.errors == []
    assert yt.count("playlistItems") == 3  # 121편, 50편씩
    sizes = [len(p["id"].split(",")) for n, p in yt.calls if n == "videos"]
    assert sizes == [50, 50, 20]           # 비공개 뺀 120편을 50편씩
    assert sr.extra["api_units"] == 6 and sr.extra["playlist_total"] == 121 and sr.extra["without_subtitle"] == 119

    idx = Index(store, "youtube-tekotalk").load()
    assert idx.get("yt:secret1") is None and idx.get("yt:vanish") is None  # 기록 없이 건너뜀(T7)
    e = idx.get("yt:v000")
    assert e["has_subtitle"] is False and e["version"] == 1 and e["gone"] is None

    rec = store.read_json(raw_path("youtube-tekotalk", "yt:v000"))
    assert rec["found_in"] == {"playlist_id": PLAYLIST_ID} and rec["subtitle"] is None
    assert rec["video"] == {
        "id": "v000",
        "snippet": {"title": "[10분 테코톡] v000", "description": "설명", "publishedAt": "2021-06-03T02:43:22Z",
                    "channelId": "UC-mOekGSesms0agFntnQang", "channelTitle": "우아한테크",
                    "thumbnails": {"high": {"url": "https://i.ytimg.com/vi/v000/hqdefault.jpg"}}},
        "contentDetails": {"duration": "PT23M54S", "caption": "true"},
        "statistics": {"viewCount": "28469", "likeCount": "727"},
        "status": {"privacyStatus": "public"},
    }
    data = store.read_json(f"reports/{rep.run_id}.json")["sources"]["youtube-tekotalk"]
    assert data["api_units"] == 6 and data["playlist_total"] == 121


def test_second_run_does_not_refetch_known(tmp_path):
    playlist, videos = world(60)
    run(tmp_path, FakeYouTube(playlist, videos))
    yt = FakeYouTube(playlist + [("new1", "public")], {**videos, "new1": video("new1")})
    yt.videos["v000"] = video("v000")  # 조회수가 바뀌어도 다시 받지 않는다(T5)
    store, rep, sr = run(tmp_path, yt)
    assert sr.counts == {"new": 1, "changed": 0, "unchanged": 60, "gone": 0, "failed": 0}
    assert [p["id"] for n, p in yt.calls if n == "videos"] == ["new1"]
    assert sr.extra["api_units"] == 2 + 1
    assert store.read_json(raw_path("youtube-tekotalk", "yt:v000"))["version"] == 1


def test_private_video_becomes_new_when_public_later(tmp_path):
    playlist, videos = world(3)
    run(tmp_path, FakeYouTube(playlist + [("later", "private")], videos))
    store, rep, sr = run(tmp_path, FakeYouTube(playlist + [("later", "public")], {**videos, "later": video("later")}))
    assert sr.counts["new"] == 1 and sr.counts["unchanged"] == 3


def test_gone_when_missing_and_api_says_absent_or_private(tmp_path):
    playlist, videos = world(5)
    run(tmp_path, FakeYouTube(playlist, videos))
    # v000 삭제(목록, API 모두 없음), v001 비공개로 바뀜(목록에 private 항목으로 남음), v002 재생목록에서만 빠짐
    today = [("v001", "private"), ("v003", "public"), ("v004", "public")]
    vids = {"v001": video("v001", "private"), "v002": video("v002"), "v003": videos["v003"], "v004": videos["v004"]}
    yt = FakeYouTube(today, vids)
    store, rep, sr = run(tmp_path, yt)
    assert sr.counts["gone"] == 2 and sorted(sr.gone_keys) == ["yt:v000", "yt:v001"]
    idx = Index(store, "youtube-tekotalk").load()
    assert idx.get("yt:v000")["gone"]["reason"] == "not_found"
    assert idx.get("yt:v001")["gone"]["reason"] == "private"
    assert idx.get("yt:v002")["gone"] is None
    assert sr.extra["api_units"] == 1 + 3  # 목록 1쪽, 사라짐 확인 3편
    assert sr.extra["without_subtitle"] == 3  # gone 은 세지 않는다


def test_page_failure_collects_what_it_got_and_skips_gone(tmp_path):
    playlist, videos = world(70)
    run(tmp_path, FakeYouTube(playlist[:60], videos))
    yt = FakeYouTube(playlist, videos)
    yt.fail_page = 1
    store, rep, sr = run(tmp_path, yt)
    assert sr.errors and sr.errors[0]["type"] == "list_incomplete"
    assert sr.counts["gone"] == 0 and sr.counts["unchanged"] == 50 and sr.counts["new"] == 0
    assert yt.count("videos") == 0  # 사라짐 확인을 하지 않는다
    assert rep.status == "failure"


def test_missing_api_key_is_source_error(tmp_path):
    yt = FakeYouTube(*world(3))
    store, rep, sr = run(tmp_path, yt, key=None)
    assert sr.errors[0]["type"] == "list_failed" and "YOUTUBE_API_KEY" in sr.errors[0]["message"]
    assert yt.calls == [] and rep.status == "failure"


def test_videos_api_error_counts_as_failed(tmp_path):
    class Quota(FakeYouTube):
        def get(self, url, params=None, headers=None, **kw):
            if url.endswith("/videos"):
                self.calls.append(("videos", dict(params)))
                return Resp(403, {"error": {"message": "quotaExceeded"}})
            return super().get(url, params, headers, **kw)

    store, rep, sr = run(tmp_path, Quota(*world(3)))
    assert sr.counts["failed"] == 3 and sr.failure_reasons == {"http_403": 3}
    assert Index(store, "youtube-tekotalk").load().entries == {}


def test_only_skips_known_and_fetches_unknown(tmp_path):
    playlist, videos = world(2)
    run(tmp_path, FakeYouTube(playlist, videos))
    yt = FakeYouTube([], {**videos, "x1": video("x1")})
    store, rep, sr = run(tmp_path, yt, only=["v000", "x1"])
    assert sr.counts["unchanged"] == 1 and sr.counts["new"] == 1
    assert [p["id"] for n, p in yt.calls if n == "videos"] == ["x1"]  # 아는 키는 다시 받지 않는다(자막을 덮어쓰지 않게)
    assert yt.count("playlistItems") == 0
