import { render, screen } from '@testing-library/react';

import { ModalProvider } from '@/components/ModalProvider';
import { TechTagField } from '@/components/projects/TechTagField';

const REACT_TAG = { id: 1, displayName: 'React' };

const renderField = (props: Parameters<typeof TechTagField>[0]) =>
  render(
    <ModalProvider>
      <TechTagField {...props} />
    </ModalProvider>,
  );

describe('TechTagField', () => {
  it('고른 기술 스택을 칩으로 보여준다', () => {
    renderField({ value: [REACT_TAG], onChange: jest.fn() });

    expect(screen.getByText('React')).toBeInTheDocument();
  });

  it('아무것도 고르지 않았으면 추가 버튼만 보여준다', () => {
    renderField({ value: [], onChange: jest.fn() });

    expect(screen.getByRole('button', { name: '기술 스택 추가' })).toBeInTheDocument();
    expect(screen.queryByRole('list', { name: '선택한 기술 스택' })).not.toBeInTheDocument();
  });

  it('이미 고른 기술 스택이 있으면 변경 버튼으로 바뀐다', () => {
    renderField({ value: [REACT_TAG], onChange: jest.fn() });

    expect(screen.getByRole('button', { name: '기술 스택 변경' })).toBeInTheDocument();
  });

  it('오류 문구를 받으면 보여준다', () => {
    renderField({
      value: [],
      onChange: jest.fn(),
      error: '기술 스택을 1개 이상 선택해 주세요.',
    });

    expect(screen.getByText('기술 스택을 1개 이상 선택해 주세요.')).toBeInTheDocument();
  });
});
