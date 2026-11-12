-- 매칭 작업이 역량을 가져갈 때마다 새로 만드는 값. 결과를 저장할 때 이 값이 같아야 반영한다.
-- 시도 횟수는 다시 분석할 때 0으로 돌아가 이전에 가져간 작업과 번호가 겹칠 수 있어서, 겹치지 않는 값을 따로 둔다.
ALTER TABLE job_requirements
    ADD COLUMN claim_token UUID;
