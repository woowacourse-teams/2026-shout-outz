/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { ThumbnailField } from '@/components/projects/ThumbnailField';
import { server } from '@/test/renderRoute';

const IMAGE = new File(['thumbnail'], 'loop.png', { type: 'image/png' });

const upload = async (user: ReturnType<typeof userEvent.setup>) => {
  await user.upload(screen.getByLabelText(/대표 썸네일 이미지/), IMAGE);
};

describe('ThumbnailField', () => {
  it('고른 이미지를 올리고 mediaId를 넘긴다', async () => {
    const user = userEvent.setup();
    const onChange = jest.fn();
    render(<ThumbnailField value={null} onChange={onChange} />);

    await upload(user);

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(12));
  });

  it('업로드에 실패하면 알리고 값을 바꾸지 않는다', async () => {
    server.use(http.post('/api/v1/media/uploads', () => new HttpResponse(null, { status: 400 })));
    const user = userEvent.setup();
    const onChange = jest.fn();
    render(<ThumbnailField value={null} onChange={onChange} />);

    await upload(user);

    expect(await screen.findByText('이미지 업로드에 실패했습니다.')).toBeInTheDocument();
    expect(onChange).not.toHaveBeenCalled();
  });

  it('오류 문구를 받으면 보여준다', () => {
    render(<ThumbnailField value={null} onChange={jest.fn()} error="이미지를 등록해 주세요." />);

    expect(screen.getByText('이미지를 등록해 주세요.')).toBeInTheDocument();
  });
});
