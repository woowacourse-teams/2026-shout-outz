import { createFileRoute, Link } from '@tanstack/react-router';
import { AboutPage, type AboutLinkProps } from '@/pages/AboutPage';

export const Route = createFileRoute('/about')({
  staticData: { prerender: true },
  component: AboutRoute,
});

function AboutRoute() {
  return <AboutPage LinkComponent={AboutLink} />;
}

// 라우터 의존은 routes에 두고 Page는 일반 링크 props만 사용합니다.
function AboutLink({ href = '/', ...props }: AboutLinkProps) {
  if (href === '/') return <Link {...props} to="/" />;
  if (href === '/projects') return <Link {...props} to="/projects" />;
  if (href.startsWith('/projects/'))
    return (
      <Link {...props} to="/projects/$slug" params={{ slug: href.slice('/projects/'.length) }} />
    );
  if (href === '/community' || href === '/community?type=POST') {
    return (
      <Link
        {...props}
        to="/community"
        search={{ type: href.includes('POST') ? 'POST' : 'QUESTION', sort: 'LATEST' }}
      />
    );
  }
  if (href === '/news') return <Link {...props} to="/news" />;
  return <a {...props} href={href} />;
}
