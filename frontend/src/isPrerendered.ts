// entry-ssg.tsx가 임베드하는 TanStack Router의 $_TSR 하이드레이션 페이로드가 있는지로
// "이 문서가 SSG로 미리 렌더링됐는지"를 판단합니다. 서버(Node)에는 window 자체가 없고,
// CSR 셸(Document만 렌더링, App 없음)에는 라우터 매치가 없어 $_TSR도 없습니다.
export function isPrerendered(): boolean {
    return typeof window !== 'undefined' && typeof (window as unknown as { $_TSR?: unknown }).$_TSR !== 'undefined';
}
