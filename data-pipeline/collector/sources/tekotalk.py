"""테코톡 `youtube-tekotalk`(04 문서).

- 목록: 공식 YouTube Data API v3 `playlistItems.list` 로 재생목록 전체 영상 ID 를 매일 받는다(50편씩, nextPageToken, T2).
- 영상 정보: 처음 보는 영상만 `videos.list`(part=snippet,contentDetails,statistics,status)로 50편씩 묶어 받는다(5-3).
  고른 항목만 API 이름, 값 그대로 저장하고 `subtitle` 은 null 이다(자막은 subtitles 명령, 5-4).
- 이미 받은 영상은 다시 받지 않는다(marker None, T5).
- 비공개, 삭제 영상은 기록 없이 건너뛴다(T7). 목록에서 비공개로 나오면 Item 을 만들지 않고,
  videos.list 가 안 주거나 공개가 아니라고 하면 fetch_many 가 아무것도 내주지 않는다.
  runner 는 내준 것만 세므로 새 영상, 실패 어느 쪽에도 잡히지 않고 색인에도 들어가지 않는다.
  다음 날 공개되어 있으면 처음 보는 키라 그때 새 영상으로 받는다.
- 사라짐: 아는 키가 오늘 목록(비공개 항목을 뺀 것)에 없고, videos.list 가 없다고 하거나 private 이라고 하면 gone(T6).
- API 키는 주소가 아니라 X-Goog-Api-Key 머리글로 보낸다. 요청 오류 메시지(주소 포함)가 보고서에 남아도 키가 새지 않게.
"""

from __future__ import annotations

import logging

from ..keys import tekotalk_key
from ..runner import Context, FetchError, Item, Listing, Source

log = logging.getLogger("collector.tekotalk")

PLAYLIST_ID = "PLgXGHBqgT2TvpJ_p9L_yZKPifgdBOzdVH"  # "#우아한테크코스 테코톡"(T1)
API = "https://www.googleapis.com/youtube/v3"
BATCH = 50  # playlistItems, videos 모두 한 번에 50까지
# 받을 수 있는 영상. unlisted(일부 공개)는 주소를 알면 볼 수 있으므로 받는다. private, 삭제 영상은 건너뛴다
VIEWABLE = {"public", "unlisted"}


class YouTubeError(Exception):
    def __init__(self, status: int, message: str) -> None:
        super().__init__(f"YouTube API {status}: {message}")
        self.status = status


def pick(video: dict) -> dict:
    """videos.list 한 건에서 고른 항목만 이름, 값 그대로 옮긴다(04 문서 6장)."""
    sn = video.get("snippet") or {}
    cd = video.get("contentDetails") or {}
    st = video.get("statistics") or {}
    ss = video.get("status") or {}
    high = ((sn.get("thumbnails") or {}).get("high") or {}).get("url")
    return {
        "id": video.get("id"),
        "snippet": {
            "title": sn.get("title"),
            "description": sn.get("description"),
            "publishedAt": sn.get("publishedAt"),
            "channelId": sn.get("channelId"),
            "channelTitle": sn.get("channelTitle"),
            "thumbnails": {"high": {"url": high}},
        },
        "contentDetails": {"duration": cd.get("duration"), "caption": cd.get("caption")},
        "statistics": {"viewCount": st.get("viewCount"), "likeCount": st.get("likeCount")},
        "status": {"privacyStatus": ss.get("privacyStatus")},
    }


class TekotalkSource(Source):
    name = "youtube-tekotalk"

    def __init__(self, http=None) -> None:
        self._http = http  # 시험에서 가짜를 넣는다. 없으면 ctx.http

    # 요청 ---------------------------------------------------------------

    def _get(self, ctx: Context, path: str, params: dict) -> dict:
        key = ctx.settings.youtube_api_key
        if not key:
            raise RuntimeError("YOUTUBE_API_KEY 가 없습니다(08 문서 C12, 04 문서 7-3)")
        http = self._http or ctx.http
        ctx.report.extra["api_units"] = ctx.report.extra.get("api_units", 0) + 1  # list 호출은 1단위
        r = http.get(f"{API}/{path}", params=params, headers={"X-Goog-Api-Key": key})
        if r.status_code != 200:
            try:
                msg = r.json()["error"]["message"]
            except Exception:
                msg = (r.text or "")[:200]
            raise YouTubeError(r.status_code, msg)
        return r.json()

    def _videos(self, ctx: Context, ids: list[str]) -> dict[str, dict]:
        # id 로 물을 때는 maxResults 를 쓰지 않는다(API 가 id 와 함께 쓰는 것을 지원하지 않음). 한 번에 50개까지
        data = self._get(ctx, "videos", {"part": "snippet,contentDetails,statistics,status", "id": ",".join(ids)})
        return {v["id"]: v for v in data.get("items", []) if v.get("id")}

    # Source -------------------------------------------------------------

    def list_items(self, ctx: Context) -> Listing:
        items: list[Item] = []
        total = 0
        token = None
        params = {"part": "contentDetails,status", "playlistId": PLAYLIST_ID, "maxResults": BATCH,
                  "fields": "nextPageToken,items(contentDetails/videoId,status/privacyStatus)"}
        while True:
            try:
                data = self._get(ctx, "playlistItems", {**params, **({"pageToken": token} if token else {})})
            except Exception as exc:
                if not items and token is None:
                    raise  # 첫 쪽부터 막힘(키 없음 포함): 원천 전체 오류
                # 중간 쪽에서 막힘: 받은 데까지만 새 영상을 받고, 목록이 덜 찼으니 사라짐 확인은 하지 않는다
                log.exception("테코톡 재생목록 중간 쪽 받기 실패")
                ctx.report.error(exc, "list_incomplete", ctx.now)
                ctx.report.extra["playlist_total"] = total
                return Listing(items, set())
            for it in data.get("items", []):
                total += 1
                vid = (it.get("contentDetails") or {}).get("videoId")
                privacy = (it.get("status") or {}).get("privacyStatus")
                if not vid or privacy not in VIEWABLE:
                    continue  # 비공개, 삭제 영상: 기록 없이 건너뜀(T7)
                items.append(Item(tekotalk_key(vid), None, group=PLAYLIST_ID, data=vid))
            token = data.get("nextPageToken")
            if not token:
                break
        ctx.report.extra["playlist_total"] = total
        return Listing(items, {PLAYLIST_ID})

    def only_items(self, ctx: Context, values: list[str]) -> list[Item]:
        """--only 영상 ID(또는 yt: 키). 이미 받은 영상은 다시 받지 않는다(T5).

        다시 받으면 자막 없는 새 버전이 자막 있는 버전을 덮어쓰므로, 아는 키는 받지 않고 unchanged 로 센다.
        """
        if not ctx.settings.youtube_api_key:
            raise RuntimeError("YOUTUBE_API_KEY 가 없습니다(08 문서 C12, 04 문서 7-3)")
        out = []
        for v in values:
            vid = v[3:] if v.startswith("yt:") else v
            key = tekotalk_key(vid)
            if ctx.index.get(key):
                log.info("이미 받은 영상이라 다시 받지 않음: %s", key)
                ctx.report.add("unchanged")
                continue
            out.append(Item(key, None, group=PLAYLIST_ID, data=vid))
        return out

    def fetch_many(self, ctx: Context, items: list[Item]):
        for i in range(0, len(items), BATCH):
            chunk = items[i:i + BATCH]
            try:
                got = self._videos(ctx, [it.data for it in chunk])
            except YouTubeError as exc:
                for it in chunk:
                    yield it, FetchError(f"http_{exc.status}")
                continue
            except Exception as exc:
                for it in chunk:
                    yield it, exc
                continue
            for it in chunk:
                video = got.get(it.data)
                if not video or (video.get("status") or {}).get("privacyStatus") not in VIEWABLE:
                    log.info("받을 수 없는 영상이라 건너뜀(T7): %s", it.key)
                    continue  # 내주지 않으면 runner 는 세지도, 색인에 넣지도 않는다
                yield it, {"found_in": {"playlist_id": PLAYLIST_ID}, "video": pick(video), "subtitle": None}

    def index_fields(self, record: dict) -> dict:
        return {"has_subtitle": record.get("subtitle") is not None}

    def group_of(self, entry: dict) -> str | None:
        return PLAYLIST_ID

    def confirm_gone(self, ctx: Context, entry: dict) -> str | None:
        vid = entry["external_key"][len("yt:"):]
        video = self._videos(ctx, [vid]).get(vid)
        if video is None:
            return "not_found"
        if (video.get("status") or {}).get("privacyStatus") == "private":
            return "private"
        return None  # 재생목록에서만 빠졌고 영상은 그대로다

    def finish(self, ctx: Context) -> None:
        ctx.report.extra.setdefault("api_units", 0)
        ctx.report.extra["without_subtitle"] = sum(1 for e in ctx.index.alive() if not e.get("has_subtitle"))
