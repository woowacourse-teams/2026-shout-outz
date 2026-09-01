-- 완료 요청자가 업로드를 시작한 사용자와 동일한지 확인하기 위한 소유자 정보다.

alter table media_metadata
    add column uploaded_by bigint references users(id);

create index media_metadata_uploaded_by_idx
    on media_metadata (uploaded_by);
