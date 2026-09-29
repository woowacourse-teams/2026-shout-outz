-- 기존 GitHub OAuth 회원과 이전 기수 프로젝트 참여자를 연결한다.
--
-- 애플리케이션은 신규 가입/로그인 시 같은 로직을 실행하지만,
-- Import 전에 이미 가입한 회원을 위한 1회성 백필용으로도 사용할 수 있다.

BEGIN;

UPDATE woowa_archived_project_members am
SET matched_user_id = oa.user_id
FROM oauth_accounts oa
JOIN users u ON u.id = oa.user_id
WHERE oa.provider = 'GITHUB'
  AND u.status = 'ACTIVE'
  AND am.matched_user_id IS NULL
  AND am.github_account_id = oa.provider_account_id;

INSERT INTO project_members (project_id, user_id, display_order)
SELECT am.project_id, am.matched_user_id, am.display_order
FROM woowa_archived_project_members am
JOIN users u ON u.id = am.matched_user_id
WHERE am.matched_user_id IS NOT NULL
  AND u.status = 'ACTIVE'
ON CONFLICT (project_id, user_id) DO NOTHING;

COMMIT;
