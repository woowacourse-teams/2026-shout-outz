"""설정값. 비밀값은 환경 변수로만 받는다(EC2 는 /etc/shout-outz/data-pipeline.env, 08 문서 C12)."""

from __future__ import annotations

import os
from dataclasses import dataclass, field

# 수집 순서(08 문서 C8): 짧은 것부터, GitHub 한도는 팀 문서(REST 위주) 다음 PR(GraphQL)
SOURCES = [
    "youtube-tekotalk",
    "blog-post",
    "github-teamdoc",
    "github-wiki",
    "github-discussion",
    "github-pr",
]
GITHUB_SOURCES = {"github-teamdoc", "github-wiki", "github-discussion", "github-pr"}

USER_AGENT = "shout-outz-collector"
ORG = "woowacourse-teams"
INDEX_CHECKPOINT = 200          # 색인 중간 저장 간격(C4)
LOCK_STALE_SECONDS = 12 * 3600  # 이보다 오래된 잠금은 멈춘 실행이 남긴 것으로 본다(C9)


@dataclass
class Settings:
    store_url: str
    host: str = "home"
    trigger: str = "cli"
    github_token: str | None = None
    youtube_api_key: str | None = None
    db: dict = field(default_factory=dict)

    @classmethod
    def from_env(cls) -> "Settings":
        """PIPELINE_STORE 는 꼭 있어야 한다. 기본값으로 로컬 폴더에 쓰면, EC2 의 .env 에서 값이 빠졌을 때
        S3 에는 아무것도 쌓이지 않는데 실행은 성공으로 끝난다(08 문서 6-1)."""
        e = os.environ.get
        store_url = (e("PIPELINE_STORE") or "").strip()
        if not store_url:
            raise RuntimeError("PIPELINE_STORE 가 없어요. 원본을 쌓을 곳(S3 주소 s3://버킷/앞머리 또는 로컬 폴더)을 환경 변수 파일에 적어 주세요")
        return cls(
            store_url=store_url,
            host=e("PIPELINE_HOST") or "home",
            trigger=e("PIPELINE_TRIGGER") or "cli",
            github_token=e("GITHUB_TOKEN") or None,
            youtube_api_key=e("YOUTUBE_API_KEY") or None,
            db={
                "host": e("SERVICE_DB_HOST"),
                "port": int(e("SERVICE_DB_PORT") or 5432),
                "dbname": e("SERVICE_DB_NAME"),
                "user": e("SERVICE_DB_USER"),
                "password": e("SERVICE_DB_PASSWORD"),  # 없으면 RDS IAM 인증 토큰을 쓴다
                "region": e("AWS_REGION") or "ap-northeast-2",
            },
        )
