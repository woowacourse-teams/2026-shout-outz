-- 자료 카드 출처 아이콘(faviconUrl)을 블로그가 아닌 자료에도 내려준다(#659).
-- PR, 팀 문서, 위키, 디스커션 자료 미리보기 중 favicon_url 칸이 생기기 전에 받은 것은 파비콘이 비어 있다.
-- github.com 페이지는 종류와 관계없이 같은 파비콘을 쓰므로(<link rel="icon" type="image/svg+xml">), 페이지를 다시 받지 않고 그 주소를 바로 넣는다.
-- 수집기가 같은 페이지에서 뽑는 값과 같다(FaviconSelector 는 SVG 를 크기 표시 없는 PNG 보다 먼저 고른다).
-- 상태는 바꾸지 않는다. 실패했거나 받는 중인 미리보기도 파비콘은 사이트 것이라 넣어 두고, 다음에 받으면 수집한 값으로 덮인다.
UPDATE link_preview_cache preview
SET favicon_url = 'https://github.githubassets.com/favicons/favicon.svg'
WHERE preview.favicon_url IS NULL
  AND preview.url LIKE 'https://github.com/%'
  AND EXISTS (
      SELECT 1
      FROM resource_link_preview_refs ref
      JOIN resource_documents document ON document.id = ref.document_id
      WHERE ref.cache_id = preview.id
        AND document.source IN ('github-pr', 'github-teamdoc', 'github-wiki', 'github-discussion')
        AND document.deleted_at IS NULL
  );
