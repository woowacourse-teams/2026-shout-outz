import { IconBrandGithub, IconX } from '@tabler/icons-react';

import { Modal } from '@/components/Modal';
import { Button, getButtonStyles } from '@/components/Button';
import { getGithubLoginUrl } from '@/utils/auth';
import { analytics, toPathPattern } from '@/utils/analytics';

export interface AuthSheetProps {
  onClose: () => void;
}

export function AuthSheet({ onClose }: AuthSheetProps) {
  return (
    <Modal onClose={onClose} className="relative md:w-110">
      <div aria-hidden="true" className="flex justify-center pt-4 pb-1 md:hidden">
        <span className="h-1 w-9 rounded-full bg-gray-300" />
      </div>

      <Button
        variant="ghost"
        size="sm"
        aria-label="닫기"
        onClick={onClose}
        className="absolute top-4 right-4 size-7.5 rounded-full bg-gray-100 p-0 md:bg-transparent"
      >
        <IconX className="size-4" aria-hidden="true" />
      </Button>

      <div className="flex flex-col items-center gap-4 px-7 pt-5 pb-8 text-center md:px-10 md:pt-8">
        <img
          src="/favicon/android-chrome-192x192.png"
          alt=""
          width={52}
          height={52}
          className="size-13"
        />

        <h2 className="text-xl font-bold">
          <span className="text-primary-600 font-extrabold">샤라웃</span>에 오신 것을 환영해요!
        </h2>
        <p className="-mt-2 text-sm leading-relaxed text-balance break-keep text-gray-600">
          궁금한 건 편하게 묻고, 서로의 이야기에 응원을 보내 보세요.
        </p>

        <a
          href={getGithubLoginUrl()}
          onClick={() =>
            analytics.track({
              name: 'login_started',
              from: toPathPattern(window.location.pathname),
            })
          }
          className={getButtonStyles({
            size: 'lg',
            className: 'mt-2 w-full gap-2 rounded-2xl bg-gray-900 hover:bg-gray-800',
          })}
        >
          <IconBrandGithub className="size-5" aria-hidden="true" />
          GitHub 계정으로 시작하기
        </a>

        <Button variant="ghost" size="sm" onClick={onClose} className="text-gray-500">
          로그인 없이 둘러보기
        </Button>
      </div>
    </Modal>
  );
}
