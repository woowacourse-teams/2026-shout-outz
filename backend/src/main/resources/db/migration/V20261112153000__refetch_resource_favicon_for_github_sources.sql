-- 자료 카드 출처 아이콘(faviconUrl)을 블로그가 아닌 자료에도 내려준다(#659).
-- PR, 팀 문서, 위키, 디스커션 자료의 미리보기는 대부분 favicon_url 칸이 생기기 전에 받아 파비콘이 없으므로 다시 수집한다.
-- 테코톡은 YouTube oEmbed 로 받아 파비콘이 나오지 않으므로 다시 받지 않는다.
-- V20261111170000 과 같은 방식이다. 상태는 READY 로 두고 수집 시각만 오래된 것으로 바꿔, 다시 받는 동안에도 지금 미리보기가 계속 보인다.
UPDATE link_preview_cache preview
SET fetched_at = TIMESTAMPTZ '2000-01-01 00:00:00+00'
WHERE preview.status = 'READY'
  AND preview.favicon_url IS NULL
  AND EXISTS (
      SELECT 1
      FROM resource_link_preview_refs ref
      JOIN resource_documents document ON document.id = ref.document_id
      WHERE ref.cache_id = preview.id
        AND document.source IN ('github-pr', 'github-teamdoc', 'github-wiki', 'github-discussion')
        AND document.deleted_at IS NULL
  );
