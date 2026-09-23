import { IconBrandGithub } from '@tabler/icons-react';

import { Modal } from '@/components/Modal';
import { Button, getButtonStyles } from '@/components/Button';
import { getGithubLoginUrl } from '@/utils/auth';
import { analytics, toPathPattern } from '@/utils/analytics';

export interface AuthSheetProps {
  onClose: () => void;
}

export function AuthSheet({ onClose }: AuthSheetProps) {
  return (
    <Modal onClose={onClose} className="md:w-110">
      <div aria-hidden="true" className="flex justify-center pt-4 pb-1 md:hidden">
        <span className="h-1 w-9 rounded-full bg-gray-300" />
      </div>

      <div className="flex flex-col items-center gap-4 px-7 pt-5 pb-8 text-center md:px-10 md:pt-8">
        <span
          aria-hidden="true"
          className="bg-primary-600 flex size-13 items-center justify-center rounded-3xl text-3xl font-bold text-white"
        >
          S
        </span>

        <h2 className="text-lg font-bold">우아한테크코스 크루 인증 & 로그인</h2>
        <p className="text-sm leading-relaxed text-gray-600">
          우아한테크코스 크루 인증을 완료하고 프로젝트 아카이빙과 피드 소통을 시작하세요.
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
