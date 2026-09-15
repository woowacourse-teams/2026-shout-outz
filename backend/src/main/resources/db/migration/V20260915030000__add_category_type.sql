-- 카테고리 유형 구분을 위한 컬럼과 제약 조건
alter table categories
    add column category_type varchar(20) not null default 'GENERAL';

alter table categories
    add constraint categories_category_type_check
        check (category_type in ('GENERAL', 'EVENT'));
