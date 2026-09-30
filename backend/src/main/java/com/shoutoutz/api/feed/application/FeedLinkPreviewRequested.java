package com.shoutoutz.api.feed.application;

/** 링크 참조가 커밋된 뒤 수집 작업을 깨운다. 실제 대기 작업은 DB가 보관한다. */
public record FeedLinkPreviewRequested() {
}
