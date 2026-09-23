import { render, screen } from '@testing-library/react';

import { MemberField } from '@/components/projects/MemberField';
import { ModalProvider } from '@/components/ModalProvider';
import { type CrewSearchItem } from '@/types/project';

const crew = (handle: string, displayName: string): CrewSearchItem => ({
  handle,
  displayName,
  userType: 'WOOWACOURSE_CREW',
});

const renderField = (props: Parameters<typeof MemberField>[0]) =>
  render(
    <ModalProvider>
      <MemberField {...props} />
    </ModalProvider>,
  );

describe('MemberField', () => {
  it('고른 팀원을 이름으로 보여준다', () => {
    renderField({ value: [crew('zzaekkii', '재키')], onChange: jest.fn() });

    expect(screen.getByText('재키')).toBeInTheDocument();
  });

  it('아무도 고르지 않았으면 추가 버튼만 보여준다', () => {
    renderField({ value: [], onChange: jest.fn() });

    expect(screen.getByRole('button', { name: '참여 팀원 추가' })).toBeInTheDocument();
    expect(screen.queryByRole('list', { name: '선택한 참여 팀원' })).not.toBeInTheDocument();
  });

  it('이미 고른 팀원이 있으면 변경 버튼으로 바뀐다', () => {
    renderField({ value: [crew('zzaekkii', '재키')], onChange: jest.fn() });

    expect(screen.getByRole('button', { name: '참여 팀원 변경' })).toBeInTheDocument();
  });

  it('오류 문구를 받으면 보여준다', () => {
    renderField({
      value: [],
      onChange: jest.fn(),
      error: '참여 팀원을 1명 이상 선택해 주세요.',
    });

    expect(screen.getByText('참여 팀원을 1명 이상 선택해 주세요.')).toBeInTheDocument();
  });
});
