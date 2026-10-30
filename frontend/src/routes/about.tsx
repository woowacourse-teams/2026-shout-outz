import { createFileRoute } from '@tanstack/react-router';
import { AboutPage } from '@/pages/AboutPage';
import { AboutGnb } from '@/pages/about/AboutGnb';
import { Footer } from '@/components/Footer';
import { PROTOTYPE_URL } from '@/pages/about/links';

export const Route = createFileRoute('/about')({
  staticData: { prerender: true },
  component: AboutRoute,
});

function AboutRoute() {
  return (
    <>
      <AboutGnb />
      <AboutPage />
      <Footer linksHref={PROTOTYPE_URL} />
    </>
  );
}
