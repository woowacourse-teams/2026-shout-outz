import { createFileRoute } from '@tanstack/react-router';
import { AboutPage } from '@/pages/AboutPage';
import { AppGnb } from '@/components/AppGnb';
import { Footer } from '@/components/Footer';

export const Route = createFileRoute('/about')({
  staticData: { prerender: true },
  component: AboutRoute,
});

function AboutRoute() {
  return (
    <>
      <AppGnb />
      <AboutPage />
      <Footer />
    </>
  );
}
