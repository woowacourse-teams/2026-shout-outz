import { createFileRoute } from '@tanstack/react-router';

import { SignupPage } from '@/pages/SignupPage';

export const Route = createFileRoute('/signup')({ component: SignupRoute });

function SignupRoute() {
  const navigate = Route.useNavigate();

  return <SignupPage onComplete={() => void navigate({ to: '/' })} />;
}
