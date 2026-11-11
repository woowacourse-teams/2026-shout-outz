import { IconBrandGithub, IconUser } from '@tabler/icons-react';
import { useCanGoBack, useRouter } from '@tanstack/react-router';
import type { ReactNode } from 'react';

import { GithubLoginButton } from '@/components/auth/GithubLoginButton';

interface LoginRequiredGuideProps {
  /** 안내 문구에 굵게 들어가는 화면 이름. 예: "피드 작성" */
  pageName: string;
  /** "다음에 할게요"를 눌렀을 때 돌아갈 화면이 없으면 갈 목록 주소 */
  fallbackTo: '/community' | '/projects';
  /** 로그인하러 떠나기 전에 할 일. false를 돌려주면 로그인하지 않는다. */
  onBeforeLogin?: () => boolean;
  /** 버튼 아래에 덧붙일 안내 */
  children?: ReactNode;
}

/** 로그인해야 쓸 수 있는 화면에 로그인하지 않고 들어왔을 때 보여 주는 공통 안내 */
export function LoginRequiredGuide({
  pageName,
  fallbackTo,
  onBeforeLogin,
  children,
}: LoginRequiredGuideProps) {
  const router = useRouter();
  const canGoBack = useCanGoBack();

  return (
    <section className="mx-auto flex w-full max-w-110 flex-col items-center px-6 pt-20 pb-28 text-center md:pt-30 md:pb-40">
      <span className="bg-primary-50 text-primary-600 flex size-13 items-center justify-center rounded-2xl">
        <IconUser className="size-6" aria-hidden="true" />
      </span>
      <h1 className="mt-5 text-2xl font-extrabold tracking-tight text-gray-900">
        로그인이 필요해요
      </h1>
      <p className="mt-2 text-base leading-relaxed break-keep text-gray-600">
        <b className="font-bold text-gray-900">{pageName}</b> 페이지는 로그인 후 이용할 수 있어요.
      </p>
      <GithubLoginButton
        className="focus-visible:outline-primary-600 mt-7 inline-flex h-12 w-full max-w-70 cursor-pointer items-center justify-center gap-2 rounded-lg bg-gray-900 text-[0.9375rem] font-bold text-white hover:bg-gray-700 focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-default disabled:opacity-60"
        onBeforeLogin={onBeforeLogin}
      >
        <IconBrandGithub className="size-4.5" aria-hidden="true" />
        GitHub로 로그인
      </GithubLoginButton>
      {/* ASSUMPTION: "다음에 할게요"는 이전 화면으로 돌아가고, 이전 화면이 없으면 목록으로 간다. */}
      <button
        type="button"
        onClick={() => {
          if (canGoBack) router.history.back();
          else void router.navigate({ to: fallbackTo });
        }}
        className="focus-visible:outline-primary-600 mt-3.5 cursor-pointer rounded-sm text-sm font-medium text-gray-500 hover:text-gray-900 focus-visible:outline-2"
      >
        다음에 할게요
      </button>
      {children}
    </section>
  );
}
