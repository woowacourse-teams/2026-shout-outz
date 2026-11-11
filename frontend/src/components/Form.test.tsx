import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { Button } from '@/components/Button';
import { FormController, FormInput, FormTextarea, FormValue } from '@/components/Form';
import { useFormStore } from '@/hooks/useFormStore';

function TestForm({ onSubmit }: { onSubmit: (values: unknown) => void }) {
  const form = useFormStore({ title: '', content: '', isAnonymous: false });

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit(form.getValues());
      }}
    >
      <FormInput form={form} name="title" aria-label="제목" />
      <FormTextarea form={form} name="content" aria-label="본문" />
      <FormController form={form} name="isAnonymous">
        {({ value, onChange }) => (
          <input
            type="checkbox"
            aria-label="익명"
            checked={value}
            onChange={(event) => onChange(event.target.checked)}
          />
        )}
      </FormController>
      <FormValue form={form} select={(values) => !values.title.trim()}>
        {(empty) => (
          <Button type="submit" disabled={empty}>
            등록
          </Button>
        )}
      </FormValue>
    </form>
  );
}

describe('Form', () => {
  it('입력한 값이 화면에 반영되고 제출할 때 모든 값을 읽을 수 있다', async () => {
    const user = userEvent.setup();
    const onSubmit = jest.fn();
    render(<TestForm onSubmit={onSubmit} />);

    await user.type(screen.getByRole('textbox', { name: '제목' }), '루프');
    await user.type(screen.getByRole('textbox', { name: '본문' }), '회고 도구');
    await user.click(screen.getByRole('checkbox', { name: '익명' }));

    expect(screen.getByRole('textbox', { name: '제목' })).toHaveValue('루프');
    expect(screen.getByRole('checkbox', { name: '익명' })).toBeChecked();

    await user.click(screen.getByRole('button', { name: '등록' }));

    expect(onSubmit).toHaveBeenCalledWith({
      title: '루프',
      content: '회고 도구',
      isAnonymous: true,
    });
  });

  it('값에서 계산한 UI는 그 값이 바뀌면 함께 바뀐다', async () => {
    const user = userEvent.setup();
    render(<TestForm onSubmit={jest.fn()} />);
    const submit = screen.getByRole('button', { name: '등록' });

    expect(submit).toBeDisabled();

    await user.type(screen.getByRole('textbox', { name: '제목' }), '루프');
    expect(submit).toBeEnabled();

    await user.clear(screen.getByRole('textbox', { name: '제목' }));
    expect(submit).toBeDisabled();
  });
});
