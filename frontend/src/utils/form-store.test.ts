import { createFormStore } from '@/utils/form-store';

describe('createFormStore', () => {
  it('값을 바꾸면 등록된 함수를 부르고, 바뀌지 않은 필드는 같은 참조로 남긴다', () => {
    const tags = ['React'];
    const store = createFormStore({ title: '', tags });
    const listener = jest.fn();
    store.subscribe(listener);

    store.setValue('title', '루프');

    expect(listener).toHaveBeenCalledTimes(1);
    expect(store.getValues()).toEqual({ title: '루프', tags });
    expect(store.getValues().tags).toBe(tags);
  });

  it('같은 값으로 바꾸면 부르지 않는다', () => {
    const store = createFormStore({ title: '루프' });
    const listener = jest.fn();
    store.subscribe(listener);

    store.setValue('title', '루프');

    expect(listener).not.toHaveBeenCalled();
  });

  it('등록을 풀면 더 이상 부르지 않는다', () => {
    const store = createFormStore({ title: '' });
    const listener = jest.fn();
    const unsubscribe = store.subscribe(listener);

    unsubscribe();
    store.setValue('title', '루프');

    expect(listener).not.toHaveBeenCalled();
  });
});
