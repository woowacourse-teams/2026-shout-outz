/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen } from '@testing-library/react';

import { renderRoute } from '@/test/renderRoute';

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

  it('없는 소식이면 서버가 404를 주고 에러 화면을 보여준다', async () => {
    renderRoute('/news/999');

    expect(await screen.findByText('소식을 불러오지 못했습니다.')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { level: 1 })).not.toBeInTheDocument();
  });
});
