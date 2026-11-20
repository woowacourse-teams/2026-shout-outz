import { createFileRoute } from '@tanstack/react-router';
import { AboutPage } from '@/pages/AboutPage';
import { AppGnb } from '@/components/AppGnb';
import { Footer } from '@/components/Footer';

export const Route = createFileRoute('/about')({
  staticData: { prerender: true },
  component: AboutRoute,
});

// 소개 페이지에서도 서비스 GNB와 푸터를 그대로 써서 다른 화면으로 돌아갈 수 있게 한다.
// 본문의 예시 링크는 아직 프로토타입 화면으로 연결한다.
function AboutRoute() {
  return (
    <>
      <AppGnb />
      <AboutPage />
      <Footer />
    </>
  );
}
