import { createFileRoute } from '@tanstack/react-router';
import { AboutPage } from '@/pages/AboutPage';
import { AboutGnb } from '@/pages/about/AboutGnb';
import { Footer } from '@/components/Footer';

export const Route = createFileRoute('/about')({
  staticData: { prerender: true },
  component: AboutRoute,
});

function AboutRoute() {
  return (
    <>
      <AboutGnb />
      <AboutPage />
      <Footer />
    </>
  );
}
