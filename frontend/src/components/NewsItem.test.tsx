import { render, screen } from '@testing-library/react';

import { NewsItem, type NewsItemProps } from '@/components/NewsItem';

const NOTICE: NewsItemProps = {
  type: 'NOTICE',
  title: '우아한테크코스 6기 최종 프로젝트 데모데이 일정 및 참관 안내',
  summary: '6기 크루들이 준비한 최종 프로젝트 데모데이가 오는 9월 진행됩니다.',
  publishedAt: '2026-08-25T10:00:00+09:00',
};

const EVENT: NewsItemProps = {
  type: 'EVENT',
  title: '6기 프로젝트 아카이빙 챌린지 - 등록 크루 전원 굿즈팩 증정',
  summary: '지금 팀 프로젝트를 등록하면 우테코 공식 굿즈팩을 선물로 드립니다.',
  publishedAt: '2026-08-20T10:00:00+09:00',
};

const classMatching = (element: Element, matcher: RegExp) =>
  element.className.split(' ').filter((className) => matcher.test(className));

describe('NewsItem', () => {
  it('소식 한 건의 분류·발행일·제목·요약을 보여준다', () => {
    render(<NewsItem {...NOTICE} />);

    expect(screen.getByText('공지사항')).toBeInTheDocument();
    expect(screen.getByText('2026.08.25')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: NOTICE.title })).toBeInTheDocument();
    expect(screen.getByText(NOTICE.summary)).toBeInTheDocument();
  });

  describe('분류', () => {
    it('NOTICE와 EVENT를 각각의 한글 라벨로 옮긴다', () => {
      const { unmount } = render(<NewsItem {...NOTICE} />);
      expect(screen.getByText('공지사항')).toBeInTheDocument();
      expect(screen.queryByText('이벤트')).not.toBeInTheDocument();
      unmount();

      render(<NewsItem {...EVENT} />);
      expect(screen.getByText('이벤트')).toBeInTheDocument();
      expect(screen.queryByText('공지사항')).not.toBeInTheDocument();
    });

    it('두 분류를 서로 다른 색으로 구분한다', () => {
      render(
        <>
          <NewsItem {...NOTICE} />
          <NewsItem {...EVENT} />
        </>,
      );

      const backgroundOf = (label: string) => classMatching(screen.getByText(label), /^bg-/);

      expect(backgroundOf('공지사항')).toHaveLength(1);
      expect(backgroundOf('이벤트')).toHaveLength(1);
      expect(backgroundOf('공지사항')).not.toEqual(backgroundOf('이벤트'));
    });
  });

  it('목록에서의 위치에 따라 달라지는 스타일을 스스로 갖지 않는다', () => {
    render(<NewsItem {...NOTICE} />);

    const { className } = screen.getByRole('article');

    expect(className).not.toMatch(/first:|last:|odd:|even:|nth-/);
    expect(classMatching(screen.getByRole('article'), /^border|^divide|^m[btxy]?-/)).toEqual([]);
  });
});
