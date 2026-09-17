import { useState, type ReactNode } from 'react';

/**
 * 프로젝트 카드. 응답 모양이 아니라 화면에 그릴 값만 받는다.
 *
 * 목록 화면과 프로필 프로젝트 탭이 같이 쓴다. 썸네일이 없으면 제목을 대신 보여준다.
 */
export interface ProjectCardProps {
  title: string;
  tagline: string;
  thumbnailUrl?: string | null;
  /** 제목 위에 놓을 한 줄. 예: "우아한테크코스 6기" */
  meta?: string;
  /** 소개 아래에 놓을 내용. 예: 기술 스택 배지 */
  children?: ReactNode;
}

export function ProjectCard({ title, tagline, thumbnailUrl, meta, children }: ProjectCardProps) {
  const [failedUrl, setFailedUrl] = useState<string | null>();
  const showImage = Boolean(thumbnailUrl) && thumbnailUrl !== failedUrl;

  return (
    <article className="min-w-0">
      <div className="flex aspect-video items-center justify-center overflow-hidden rounded-xl bg-gray-100">
        {showImage ? (
          <img
            src={thumbnailUrl!}
            alt=""
            loading="lazy"
            onError={() => setFailedUrl(thumbnailUrl)}
            className="size-full object-cover"
          />
        ) : (
          <span aria-hidden="true" className="px-4 text-center text-sm font-semibold text-gray-500">
            {title}
          </span>
        )}
      </div>
      {meta && <p className="mt-3 text-xs text-gray-500">{meta}</p>}
      <h2 className="mt-2 text-lg font-bold wrap-break-word text-gray-900">{title}</h2>
      <p className="mt-2 text-sm leading-relaxed wrap-break-word text-gray-600">{tagline}</p>
      {children && <div className="mt-3">{children}</div>}
    </article>
  );
}
