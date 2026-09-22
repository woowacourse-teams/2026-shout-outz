/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderRoute, server } from '@/test/renderRoute';

const navLink = async (label: string) =>
  screen.findByRole('link', { name: new RegExp(`^${label}`) });

describe('NewsDetailPage', () => {
  it('URL의 id에 해당하는 소식을 보여준다', async () => {
    renderRoute('/news/2');

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: '6기 프로젝트 아카이빙 챌린지 - 등록 크루 전원 굿즈팩 증정',
      }),
    ).toBeInTheDocument();
    expect(screen.getByText(/6기 프로젝트 아카이빙 챌린지'를 시작합니다/)).toBeInTheDocument();
    expect(screen.getByText('우아한테크코스 운영진', { exact: false })).toBeInTheDocument();
    expect(screen.getAllByRole('banner')).toHaveLength(1);
  });

  describe('이전·다음 소식', () => {
    it('이전글은 더 과거, 다음글은 더 최신 소식으로 간다', async () => {
      renderRoute('/news/2');

      expect(await navLink('이전글')).toHaveAttribute('href', '/news/3');
      expect(await navLink('다음글')).toHaveAttribute('href', '/news/1');
    });

    it('가장 최신 소식에는 다음글이 없다', async () => {
      renderRoute('/news/1');

      expect(await navLink('이전글')).toHaveAttribute('href', '/news/2');
      expect(screen.queryByRole('link', { name: /^다음글/ })).not.toBeInTheDocument();
    });

    it('가장 오래된 소식에는 이전글이 없다', async () => {
      renderRoute('/news/4');

      expect(await navLink('다음글')).toHaveAttribute('href', '/news/3');
      expect(screen.queryByRole('link', { name: /^이전글/ })).not.toBeInTheDocument();
    });
  });

  describe('CTA', () => {
    it('cta가 있으면 링크로 보여준다', async () => {
      renderRoute('/news/2');

      expect(
        await screen.findByRole('link', { name: '지금 프로젝트 등록하러 가기 ›' }),
      ).toHaveAttribute('href', '/projects/new');
    });

    it('cta가 없는 소식에는 그리지 않는다', async () => {
      renderRoute('/news/3');

      await screen.findByRole('heading', { level: 1 });
      expect(screen.queryByRole('link', { name: /등록하러 가기/ })).not.toBeInTheDocument();
    });
  });

  it('없는 소식이면 재시도할 수 없는 안내를 공통 레이아웃 안에 보여준다', async () => {
    const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});

    try {
      renderRoute('/news/999');

      expect(await screen.findByRole('alert')).toHaveTextContent(
        '소식이 없거나 접근할 수 없습니다.',
      );
      expect(screen.queryByRole('button', { name: '다시 시도' })).not.toBeInTheDocument();
      expect(screen.getByRole('banner', { name: '주요 헤더' })).toBeInTheDocument();
      expect(screen.getByRole('contentinfo')).toBeInTheDocument();
    } finally {
      consoleError.mockRestore();
    }
  });

  it('조회 실패 후 다시 시도하면 소식 상세를 보여준다', async () => {
    server.use(http.get('/api/v1/news/2', () => new HttpResponse(null, { status: 500 })));
    const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});

    try {
      renderRoute('/news/2');
      const retry = await screen.findByRole('button', { name: '다시 시도' }, { timeout: 3000 });

      expect(screen.getByRole('alert')).toHaveTextContent('소식을 불러오지 못했습니다.');
      expect(screen.getByRole('banner', { name: '주요 헤더' })).toBeInTheDocument();

      server.resetHandlers();
      await userEvent.click(retry);

      expect(
        await screen.findByRole('heading', {
          level: 1,
          name: '6기 프로젝트 아카이빙 챌린지 - 등록 크루 전원 굿즈팩 증정',
        }),
      ).toBeInTheDocument();
    } finally {
      consoleError.mockRestore();
    }
  });
});
