"""원본 저장소. S3 와 로컬 폴더가 같은 폴더 모양을 쓴다(08 문서 1장, C13, 7-6).

    raw/{source}/{external_key 의 sha1}.json   문서 한 건(C1, C2). 다시 받으면 덮어쓴다(C3)
    index/{source}.jsonl                      아는 키 색인(C4)
    state/{source}.json                       수집기 상태(C5)
    state/lock.json                           잠금(C9)
    reports/{run_id}.json                     실행 보고서(C7)
"""

from __future__ import annotations

import json
import os
from pathlib import Path
from typing import Any


class Store:
    label = ""

    def read_bytes(self, path: str) -> bytes | None: raise NotImplementedError
    def write_bytes(self, path: str, data: bytes, content_type: str = "application/json") -> None: raise NotImplementedError
    def delete(self, path: str) -> None: raise NotImplementedError
    def create_exclusive(self, path: str, data: bytes) -> bool:
        """없을 때만 만든다. 이미 있으면 False(잠금, C9)."""
        raise NotImplementedError
    def list(self, prefix: str) -> list[str]: raise NotImplementedError

    def read_json(self, path: str) -> Any:
        b = self.read_bytes(path)
        return None if b is None else json.loads(b.decode("utf-8"))

    def write_json(self, path: str, value: Any) -> None:
        self.write_bytes(path, json.dumps(value, ensure_ascii=False, indent=1).encode("utf-8"))


class LocalStore(Store):
    def __init__(self, root: str) -> None:
        self.root = Path(root).expanduser().resolve()
        self.label = str(self.root)

    def _p(self, path: str) -> Path:
        return self.root / path

    def read_bytes(self, path: str) -> bytes | None:
        p = self._p(path)
        return p.read_bytes() if p.exists() else None

    def write_bytes(self, path: str, data: bytes, content_type: str = "application/json") -> None:
        p = self._p(path)
        p.parent.mkdir(parents=True, exist_ok=True)
        tmp = p.with_name(p.name + ".tmp")
        tmp.write_bytes(data)
        os.replace(tmp, p)  # 쓰다 멈춰도 반쯤 쓴 파일이 남지 않게

    def delete(self, path: str) -> None:
        self._p(path).unlink(missing_ok=True)

    def create_exclusive(self, path: str, data: bytes) -> bool:
        p = self._p(path)
        p.parent.mkdir(parents=True, exist_ok=True)
        # 임시 파일에 다 쓴 뒤 하드 링크로 등록한다. 링크는 대상이 있으면 실패하므로 "없을 때만" 이 원자적이고,
        # 쓰다 멈춰도 빈 잠금 파일이 남지 않는다
        tmp = p.with_name(f"{p.name}.{os.getpid()}.tmp")
        try:
            tmp.write_bytes(data)
            os.link(tmp, p)
            return True
        except FileExistsError:
            return False
        finally:
            tmp.unlink(missing_ok=True)

    def list(self, prefix: str) -> list[str]:
        base = self._p(prefix)
        if not base.exists():
            return []
        return sorted(str(p.relative_to(self.root)) for p in base.rglob("*") if p.is_file() and not p.name.endswith(".tmp"))


class S3Store(Store):
    def __init__(self, url: str, client=None) -> None:
        rest = url[len("s3://"):]
        self.bucket, _, prefix = rest.partition("/")
        self.prefix = prefix.strip("/") + "/" if prefix.strip("/") else ""
        self.label = url
        if client is None:
            import boto3
            client = boto3.client("s3", region_name=os.environ.get("AWS_REGION") or "ap-northeast-2")
        self.s3 = client

    def _k(self, path: str) -> str:
        return self.prefix + path

    def read_bytes(self, path: str) -> bytes | None:
        try:
            return self.s3.get_object(Bucket=self.bucket, Key=self._k(path))["Body"].read()
        except self.s3.exceptions.NoSuchKey:
            return None

    def write_bytes(self, path: str, data: bytes, content_type: str = "application/json") -> None:
        self.s3.put_object(Bucket=self.bucket, Key=self._k(path), Body=data, ContentType=content_type)

    def delete(self, path: str) -> None:
        self.s3.delete_object(Bucket=self.bucket, Key=self._k(path))

    def create_exclusive(self, path: str, data: bytes) -> bool:
        from botocore.exceptions import ClientError
        try:
            self.s3.put_object(Bucket=self.bucket, Key=self._k(path), Body=data, IfNoneMatch="*")
            return True
        except ClientError as e:
            status = e.response.get("ResponseMetadata", {}).get("HTTPStatusCode")
            code = e.response.get("Error", {}).get("Code")
            if status in (409, 412) or code in ("PreconditionFailed", "ConditionalRequestConflict"):
                return False
            raise

    def list(self, prefix: str) -> list[str]:
        out: list[str] = []
        for page in self.s3.get_paginator("list_objects_v2").paginate(Bucket=self.bucket, Prefix=self._k(prefix)):
            out += [o["Key"][len(self.prefix):] for o in page.get("Contents", [])]
        return sorted(out)


def open_store(url: str) -> Store:
    return S3Store(url) if url.startswith("s3://") else LocalStore(url)
