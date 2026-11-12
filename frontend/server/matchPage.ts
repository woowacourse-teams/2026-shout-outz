import { createPathRouter } from '../rendering/pathRouter';

const router = createPathRouter();

function countPathSegments(pathname: string): number {
  return pathname.split('/').filter(Boolean).length;
}

export function matchPage(pathname: string) {
  const matches = router.matchRoutes(pathname);
  const deepest = matches.at(-1);

  if (matches.length <= 1 || !deepest) return null;
  if (countPathSegments(deepest.pathname) !== countPathSegments(pathname)) return null;
  return matches;
}
