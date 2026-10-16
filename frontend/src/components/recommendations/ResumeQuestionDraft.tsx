import { useEffect, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useLocation, useNavigate } from '@tanstack/react-router';
import { sessionQuery } from '@/apis/session';
import { clearLoginQuestionDraft, readLoginQuestionDraft } from '@/utils/recommendation';

/** OAuth가 기본 완료 페이지로 돌아온 뒤, 사용자가 시작한 질문 작성을 이어갑니다. */
export function ResumeQuestionDraft() {
  const { data: session } = useQuery({
    ...sessionQuery,
    enabled: typeof window !== 'undefined',
    throwOnError: false,
  });
  const pathname = useLocation({ select: (location) => location.pathname });
  const navigate = useNavigate();
  const restoring = useRef(false);

  useEffect(() => {
    // 가입 직후 인증 신청/완료 안내가 남아 있을 수 있어 signup의 완료 콜백이 복귀를 맡습니다.
    if (session?.status !== 'AUTHENTICATED' || pathname === '/signup' || restoring.current) return;
    const draft = readLoginQuestionDraft();
    if (!draft) return;
    restoring.current = true;
    void navigate({
      to: '/community/new',
      search: { type: 'QUESTION' },
      state: { questionDraft: draft },
      replace: true,
    })
      .then(clearLoginQuestionDraft)
      .finally(() => {
        restoring.current = false;
      });
  }, [session?.status, pathname, navigate]);

  return null;
}
