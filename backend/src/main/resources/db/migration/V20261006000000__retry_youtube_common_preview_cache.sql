UPDATE link_preview_cache
SET status = 'PENDING',
    title = NULL,
    description = NULL,
    image_url = NULL,
    site_name = NULL,
    attempts = 0,
    fetched_at = NULL,
    next_attempt_at = now(),
    lease_until = NULL
WHERE status = 'READY'
  AND title = '- YouTube'
  AND site_name = 'www.youtube.com';
