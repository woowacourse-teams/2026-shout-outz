-- 블로그 자료 카드 출처 아이콘. 링크 미리보기 수집 때 원문 페이지에서 뽑은 파비콘 주소다.
ALTER TABLE link_preview_cache
    ADD COLUMN favicon_url TEXT;

-- 이미 미리보기를 받은 블로그 자료는 파비콘이 없으므로 다시 수집한다.
-- tistory, velog, rilog 는 플랫폼 공식 아이콘을 쓰므로 다시 받지 않는다.
-- 상태는 READY 로 두고 수집 시각만 오래된 것으로 바꾼다. 수집기가 오래된 미리보기를 다시 받는 규칙을 그대로 타서,
-- 다시 받는 동안에도 지금 미리보기가 계속 보인다.
UPDATE link_preview_cache preview
SET fetched_at = TIMESTAMPTZ '2000-01-01 00:00:00+00'
WHERE preview.status = 'READY'
  AND preview.favicon_url IS NULL
  AND EXISTS (
      SELECT 1
      FROM resource_link_preview_refs ref
      JOIN resource_documents document ON document.id = ref.document_id
      WHERE ref.cache_id = preview.id
        AND document.source = 'blog-post'
        AND document.deleted_at IS NULL
        AND COALESCE(document.extra ->> 'platform', '') NOT IN ('tistory', 'velog', 'rilog')
  );
