import '@tanstack/react-router';

declare module '@tanstack/react-router' {
  interface StaticDataRouteOption {
    prerender?: boolean;
    generateStaticParams?: () => Promise<Array<Record<string, string>>>;
  }
}
