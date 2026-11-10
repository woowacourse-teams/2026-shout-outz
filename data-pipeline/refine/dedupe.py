"""같은 레포 안의 사본(제외 규칙 duplicate, 09 문서 16장 R17).

팀 문서(R17-1): 같은 레포 안에서 정제 본문이 똑같거나 거의 같은(문자 5개 조각의 Jaccard 0.9 이상) 문서는 하나만 남긴다.
  남기는 것은 읽는 글자가 많은 쪽, 같으면 원천 순서(코드 마크다운, 위키, 디스커션), 그다음 키 순서.
PR(R17-2): 같은 레포 안에서 제목(이슈 번호를 빼고 글자와 숫자만)과 본문이 같으면 먼저 머지된 하나만 남긴다.
다른 레포끼리는 비교하지 않는다(R17-3). 1~15번 규칙에 걸린 문서와 사라진 문서는 비교하지 않는다(R17-6, R18-7).

거의 같은 문서 후보는 MinHash(64칸)로 찾고 Jaccard 를 정확히 세어 정한다. 같은 원본이면 늘 같은 결과가 나와야 하므로(4-6)
무작위 시드 대신 crc32 로 칸과 값을 정한다(한 번 순열 해싱). 서명은 정제 색인에 두어 문서를 열지 않고 후보를 찾는다.
"""

from __future__ import annotations

import hashlib
import re
import zlib
from collections import OrderedDict, defaultdict
from typing import Callable

from .config import TEAMDOC_SOURCES

SHINGLE = 5
BINS = 64
BANDS, ROWS = 16, 4
NEAR = 0.9
EMPTY_BIN = (1 << 26) - 1
TEXT_CACHE = 32  # 거의 같은 후보를 확인할 때 붙잡아 두는 글 수


def norm_text(texts: list[str]) -> str:
    return re.sub(r"\s+", " ", "\n".join(texts)).strip()


def shingles(t: str) -> set[str]:
    return {t[i:i + SHINGLE] for i in range(max(1, len(t) - SHINGLE + 1))}


def signature(t: str) -> str:
    """조각마다 crc32 를 구해 아래 6비트로 칸을 정하고 위 26비트의 가장 작은 값을 칸 값으로 둔다.
    칸 값 64개를 7자리 16진수로 이은 글자 하나로 돌려준다(정제 색인에 담기 쉽고, 정수 64개보다 메모리를 덜 쓴다).
    조각 집합을 만들지 않고 차례로 본다. 긴 문서마다 조각 수만 개짜리 집합을 만들면 1GB EC2 에서 메모리가 조각난다."""
    bins = [EMPTY_BIN] * BINS
    for i in range(max(1, len(t) - SHINGLE + 1)):
        h = zlib.crc32(t[i:i + SHINGLE].encode("utf-8"))
        b, v = h & (BINS - 1), h >> 6
        if v < bins[b]:
            bins[b] = v
    return "".join(f"{v:07x}" for v in bins)


def band(sig: str, b: int) -> str:
    return sig[b * ROWS * 7:(b + 1) * ROWS * 7]


def jaccard(a: set, b: set) -> float:
    return len(a & b) / len(a | b) if a or b else 1.0


def title_key(original_title: str) -> str:
    """PR 사본 비교의 제목: 이슈 번호((#12), #12)를 빼고 글자와 숫자만 소문자로(R17-2)."""
    t = re.sub(r"\(#\d+\)|#\d+", "", original_title or "")
    return re.sub(r"[\W_]+", "", t.lower())


def pr_dup_key(original_title: str, body_text: str) -> str | None:
    """제목과 본문(공백 뺌)이 모두 있을 때만 사본 비교 열쇠를 만든다."""
    t, b = title_key(original_title), re.sub(r"\s+", "", body_text or "")
    if not (original_title or "").strip() or not t or not b:
        return None
    return hashlib.sha1((t + "\0" + b).encode("utf-8")).hexdigest()


SOURCE_RANK = {s: i for i, s in enumerate(TEAMDOC_SOURCES)}


def teamdoc_duplicates(records: list[dict], load_text: Callable[[dict], str | None]) -> dict[str, dict]:
    """한 레포의 비교 대상 팀 문서 → {사본 키: {"of", "detail", "similarity", "kept_by"}}.

    records 칸: key, source, body_hash, minhash, readable. load_text(record) 는 거의 같은 후보를 확인할 때만 부른다."""
    def rank(r: dict) -> tuple:
        return (-int(r.get("readable") or 0), SOURCE_RANK.get(r["source"], 9), r["key"])

    def kept_by(win: dict, lose: dict) -> str:
        if int(win.get("readable") or 0) != int(lose.get("readable") or 0):
            return "longer"
        if win["source"] != lose["source"]:
            return "source_order"
        return "key_order"

    out: dict[str, dict] = {}
    # 똑같은 본문 묶음: 하나만 남긴다
    groups: dict[str, list[dict]] = defaultdict(list)
    for r in records:
        groups[r["body_hash"]].append(r)
    reps = []
    for g in groups.values():
        g = sorted(g, key=rank)
        reps.append(g[0])
        for r in g[1:]:
            out[r["key"]] = {"of": g[0]["key"], "detail": "same", "similarity": 1.0, "kept_by": kept_by(g[0], r)}
    # 거의 같은 쌍: 서명 띠(4칸씩 16개)가 하나라도 같은 대표끼리만 정확히 센다
    cands = set()
    for b in range(BANDS):
        buckets: dict[str, list[int]] = defaultdict(list)
        for i, r in enumerate(reps):
            sig = r.get("minhash")
            if isinstance(sig, str) and len(sig) == BINS * 7:
                buckets[band(sig, b)].append(i)
        for idx in buckets.values():
            for x in range(len(idx)):
                for y in range(x + 1, len(idx)):
                    cands.add((idx[x], idx[y]))
    # 글은 최근에 쓴 몇 개만 붙잡아 두고, 조각 집합은 한 쌍을 견줄 때만 만들고 버린다. 긴 문서의 조각 집합은 하나에
    # 수 MB 라 여러 개를 붙잡아 두면 1GB EC2 에서 메모리가 모자란다(R20-9)
    cache: OrderedDict[str, str | None] = OrderedDict()

    def text(r: dict) -> str | None:
        k = r["key"]
        if k in cache:
            cache.move_to_end(k)
            return cache[k]
        cache[k] = load_text(r)
        if len(cache) > TEXT_CACHE:
            cache.popitem(last=False)
        return cache[k]

    for x, y in sorted(cands):
        a, b = reps[x], reps[y]
        ta, tb = text(a), text(b)
        if ta is None or tb is None:
            continue
        j = jaccard(shingles(ta), shingles(tb))
        if j >= NEAR:
            win, lose = (a, b) if rank(a) <= rank(b) else (b, a)
            if lose["key"] not in out:
                out[lose["key"]] = {"of": win["key"], "detail": "near", "similarity": round(j, 3), "kept_by": kept_by(win, lose)}
    return out


def pr_duplicates(records: list[dict]) -> dict[str, dict]:
    """한 레포의 비교 대상 PR → {사본 키: {...}}. records 칸: key, dup_key, merged_at. 먼저 머지된 것을 남긴다."""
    groups: dict[str, list[dict]] = defaultdict(list)
    for r in records:
        if r.get("dup_key"):
            groups[r["dup_key"]].append(r)
    out = {}
    for g in groups.values():
        if len(g) < 2:
            continue
        g = sorted(g, key=lambda r: (r.get("merged_at") or "9999", _pr_number(r["key"])))
        for r in g[1:]:
            by = "merged_first" if (r.get("merged_at") or "") != (g[0].get("merged_at") or "") else "key_order"
            out[r["key"]] = {"of": g[0]["key"], "detail": "same", "similarity": 1.0, "kept_by": by}
    return out


def _pr_number(key: str) -> int:
    tail = key.rpartition("#")[2]
    return int(tail) if tail.isdigit() else 0
