import { useState } from 'react';
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';

import {
  createCategoryMutation,
  deleteCategoryMutation,
  updateCategoryMutation,
} from '@/apis/admin';
import { categoriesQuery } from '@/apis/category';
import type { CategoryFindAllSuccessResponse, CategorySaveRequest } from '@/api/generated/schema';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { Field } from '@/components/Field';
import { Input } from '@/components/Input';
import { Select } from '@/components/Select';
import { Tab } from '@/components/Tab';
import type { FeedType } from '@/types/feed';
import { getApiErrorMessage } from '@/utils/error';

type Category = CategoryFindAllSuccessResponse['data'][number];

const FEED_TYPE_LABELS: Record<FeedType, string> = { POST: '글', QUESTION: '질문' };

/** 커뮤니티 카테고리 추가·수정·삭제. 글과 질문의 카테고리가 따로 있다. */
export function CategoryAdminPanel() {
  const [feedType, setFeedType] = useState<FeedType>('POST');

  return (
    <div className="flex flex-col gap-6">
      <Tab
        value={feedType}
        onChange={(value) => setFeedType(value as FeedType)}
        aria-label="피드 유형"
        className="w-full border-b border-gray-200"
      >
        {Object.entries(FEED_TYPE_LABELS).map(([value, label]) => (
          <Tab.Item key={value} value={value}>
            {label}
          </Tab.Item>
        ))}
      </Tab>
      <AsyncBoundary>
        <CategoryList feedType={feedType} />
      </AsyncBoundary>
      <CategoryCreateForm key={feedType} feedType={feedType} />
    </div>
  );
}

function CategoryList({ feedType }: { feedType: FeedType }) {
  const { data } = useSuspenseQuery(categoriesQuery(feedType));
  const categories = [...data].sort((a, b) => a.displayOrder - b.displayOrder);

  if (categories.length === 0) {
    return <p className="py-10 text-center text-sm text-gray-500">카테고리가 없습니다.</p>;
  }
  return (
    <ul className="flex flex-col divide-y divide-gray-200 rounded-xl border border-gray-200">
      {categories.map((category) => (
        <CategoryRow key={category.categoryId} category={category} />
      ))}
    </ul>
  );
}

function CategoryRow({ category }: { category: Category }) {
  const client = useQueryClient();
  const [displayName, setDisplayName] = useState(category.displayName);
  const [displayOrder, setDisplayOrder] = useState(String(category.displayOrder));
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const onSuccess = () => client.invalidateQueries({ queryKey: ['categories'] });
  const update = useMutation({ ...updateCategoryMutation, onSuccess });
  const remove = useMutation({ ...deleteCategoryMutation, onSuccess });
  const changed =
    displayName.trim() !== category.displayName || Number(displayOrder) !== category.displayOrder;
  const pending = update.isPending || remove.isPending;

  return (
    <li className="flex flex-col gap-2 p-4">
      <div className="flex flex-wrap items-center gap-2 text-sm">
        <span className="text-gray-500">{category.slug}</span>
        {category.type === 'EVENT' && <Badge tone="green">이벤트</Badge>}
      </div>
      <div className="flex flex-col gap-2 md:flex-row md:items-center">
        <Input
          size="sm"
          aria-label={`${category.displayName} 표시 이름`}
          value={displayName}
          onChange={(event) => setDisplayName(event.target.value)}
        />
        <div className="md:w-28">
          <Input
            size="sm"
            type="number"
            min={0}
            aria-label={`${category.displayName} 표시 순서`}
            value={displayOrder}
            onChange={(event) => setDisplayOrder(event.target.value)}
          />
        </div>
        <div className="flex gap-2">
          <Button
            size="sm"
            disabled={!changed || !displayName.trim() || pending}
            onClick={() =>
              update.mutate({
                categoryId: category.categoryId,
                input: { displayName: displayName.trim(), displayOrder: Number(displayOrder) },
              })
            }
          >
            저장
          </Button>
          {confirmingDelete ? (
            <>
              <Button
                size="sm"
                disabled={pending}
                onClick={() => remove.mutate(category.categoryId)}
              >
                삭제 확정
              </Button>
              <Button variant="ghost" size="sm" onClick={() => setConfirmingDelete(false)}>
                취소
              </Button>
            </>
          ) : (
            <Button
              variant="ghost"
              size="sm"
              disabled={pending}
              onClick={() => setConfirmingDelete(true)}
            >
              삭제
            </Button>
          )}
        </div>
      </div>
      {(update.error ?? remove.error) != null && (
        <p role="alert" className="text-xs text-red-600">
          {getApiErrorMessage(update.error ?? remove.error)}
        </p>
      )}
    </li>
  );
}

function CategoryCreateForm({ feedType }: { feedType: FeedType }) {
  const client = useQueryClient();
  const empty: CategorySaveRequest = {
    feedType,
    slug: '',
    displayName: '',
    displayOrder: 0,
    type: 'GENERAL',
  };
  const [values, setValues] = useState(empty);
  const create = useMutation({
    ...createCategoryMutation,
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ['categories'] });
      setValues(empty);
    },
  });
  const canSave = values.slug.trim() && values.displayName.trim();

  return (
    <form
      className="flex flex-col gap-3 rounded-xl border border-gray-200 p-4"
      onSubmit={(event) => {
        event.preventDefault();
        if (!canSave || create.isPending) return;
        create.mutate({
          ...values,
          slug: values.slug.trim(),
          displayName: values.displayName.trim(),
        });
      }}
    >
      <h2 className="font-bold">{FEED_TYPE_LABELS[feedType]} 카테고리 추가</h2>
      <div className="grid gap-3 md:grid-cols-4">
        <Field label="slug *">
          {(id) => (
            <Input
              id={id}
              value={values.slug}
              onChange={(event) => setValues((prev) => ({ ...prev, slug: event.target.value }))}
              placeholder="free-talk"
            />
          )}
        </Field>
        <Field label="표시 이름 *">
          {(id) => (
            <Input
              id={id}
              value={values.displayName}
              onChange={(event) =>
                setValues((prev) => ({ ...prev, displayName: event.target.value }))
              }
            />
          )}
        </Field>
        <Field label="표시 순서">
          {(id) => (
            <Input
              id={id}
              type="number"
              min={0}
              value={String(values.displayOrder)}
              onChange={(event) =>
                setValues((prev) => ({ ...prev, displayOrder: Number(event.target.value) }))
              }
            />
          )}
        </Field>
        <Field label="유형">
          {(id) => (
            <Select
              id={id}
              value={values.type}
              onValueChange={(value) =>
                setValues((prev) => ({ ...prev, type: value as CategorySaveRequest['type'] }))
              }
            >
              <Select.Item value="GENERAL">일반</Select.Item>
              <Select.Item value="EVENT">이벤트</Select.Item>
            </Select>
          )}
        </Field>
      </div>
      {create.error != null && (
        <p role="alert" className="text-xs text-red-600">
          {getApiErrorMessage(create.error)}
        </p>
      )}
      <Button type="submit" className="self-start" disabled={!canSave || create.isPending}>
        추가
      </Button>
    </form>
  );
}
