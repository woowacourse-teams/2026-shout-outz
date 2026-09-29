import { useState } from 'react';
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';

import {
  adminBannersQuery,
  adminQueryKeys,
  createBannerMutation,
  deleteBannerMutation,
  updateBannerMutation,
} from '@/apis/admin';
import { uploadHomeBannerImage } from '@/api/media';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { Field } from '@/components/Field';
import { Image } from '@/components/Image';
import { Input } from '@/components/Input';
import { Select } from '@/components/Select';
import { ThumbnailField } from '@/components/projects/ThumbnailField';
import type { AdminHomeBanner, HomeBannerUpsertBody } from '@/types/admin';
import { getApiErrorMessage } from '@/utils/error';
import { resolveHomeBannerLink } from '@/utils/home-banner';

type DestinationType = HomeBannerUpsertBody['destinationType'];
type TargetType = NonNullable<HomeBannerUpsertBody['targetType']>;
type LinkType = NonNullable<HomeBannerUpsertBody['linkType']>;

const TARGET_TYPE_LABELS: Record<TargetType, string> = {
  NEWS: '소식',
  PROJECT: '프로젝트',
  FEED: '피드',
};

/** 수정 요청은 전체 교체라, 조회 응답에서 요청 필드만 골라 보낸다. */
const toUpsertBody = (banner: AdminHomeBanner): HomeBannerUpsertBody => ({
  mediaId: banner.mediaId,
  destinationType: banner.destinationType,
  targetType: banner.targetType ?? null,
  targetId: banner.targetId ?? null,
  linkType: banner.linkType ?? null,
  linkUrl: banner.linkUrl ?? null,
  displayOrder: banner.displayOrder,
  active: banner.active,
});

/** 홈 배너 목록과 추가. */
export function HomeBannerPanel() {
  return (
    <div className="flex flex-col gap-10">
      <BannerList />
      <BannerCreateForm />
    </div>
  );
}

function BannerList() {
  const { data: banners } = useSuspenseQuery(adminBannersQuery);
  const sorted = [...banners].sort((a, b) => a.displayOrder - b.displayOrder);

  return (
    <section className="flex flex-col gap-3">
      <h2 className="text-lg font-bold">등록된 배너 ({banners.length})</h2>
      {sorted.length === 0 ? (
        <p className="py-6 text-center text-sm text-gray-500">등록된 배너가 없습니다.</p>
      ) : (
        <ul className="flex flex-col divide-y divide-gray-200 rounded-xl border border-gray-200">
          {sorted.map((banner) => (
            <BannerRow key={banner.bannerId} banner={banner} />
          ))}
        </ul>
      )}
    </section>
  );
}

function BannerRow({ banner }: { banner: AdminHomeBanner }) {
  const client = useQueryClient();
  const onSuccess = () => client.invalidateQueries({ queryKey: adminQueryKeys.banners });
  const update = useMutation({ ...updateBannerMutation, onSuccess });
  const remove = useMutation({ ...deleteBannerMutation, onSuccess });
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const link = resolveHomeBannerLink(banner);
  const pending = update.isPending || remove.isPending;
  const error = update.error ?? remove.error;

  return (
    <li className="flex flex-col gap-3 p-4 md:flex-row md:items-center">
      <Image
        src={banner.imageUrl}
        alt={`배너 ${banner.bannerId}`}
        className="aspect-video w-full rounded-lg bg-gray-100 md:w-48"
        fallback={<div className="aspect-video w-full rounded-lg bg-gray-100 md:w-48" />}
      />
      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-bold">#{banner.bannerId}</span>
          <Badge tone={banner.active ? 'green' : 'gray'}>
            {banner.active ? '노출 중' : '숨김'}
          </Badge>
          <span className="text-xs text-gray-500">순서 {banner.displayOrder}</span>
        </div>
        <p className="truncate text-sm text-gray-600">
          {link
            ? link.href
            : banner.destinationType === 'TARGET' && banner.targetType === 'PROJECT'
              ? '프로젝트 ID 링크는 사용할 수 없습니다. /projects/@slug 주소로 새 배너를 등록해 주세요.'
              : '이동할 곳이 올바르지 않습니다'}
        </p>
        {error != null && (
          <p role="alert" className="text-xs text-red-600">
            {getApiErrorMessage(error)}
          </p>
        )}
      </div>
      <div className="flex gap-2">
        <Button
          variant="outline"
          size="sm"
          disabled={pending}
          onClick={() =>
            update.mutate({
              bannerId: banner.bannerId,
              body: { ...toUpsertBody(banner), active: !banner.active },
            })
          }
        >
          {banner.active ? '숨기기' : '노출하기'}
        </Button>
        {confirmingDelete ? (
          <>
            <Button size="sm" disabled={pending} onClick={() => remove.mutate(banner.bannerId)}>
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
    </li>
  );
}

interface BannerFormValues {
  mediaId: number | null;
  destinationType: DestinationType;
  targetType: TargetType;
  targetId: string;
  linkType: LinkType;
  linkUrl: string;
  displayOrder: string;
  active: boolean;
}

const EMPTY_FORM: BannerFormValues = {
  mediaId: null,
  destinationType: 'URL',
  targetType: 'NEWS',
  targetId: '',
  linkType: 'INTERNAL_PATH',
  linkUrl: '',
  displayOrder: '0',
  active: true,
};

function validate(values: BannerFormValues) {
  const errors: Partial<Record<keyof BannerFormValues, string>> = {};
  if (values.mediaId === null) errors.mediaId = '배너 이미지를 올려 주세요.';
  // 요청 타입에서 targetId는 선택(null 허용)이다. 비워 두면 null로 보내고, 숫자가 아닌 값만 막는다.
  const targetId = values.targetId.trim();
  if (values.destinationType === 'TARGET' && targetId && !/^\d+$/.test(targetId)) {
    errors.targetId = '대상 ID는 숫자로 입력해 주세요.';
  }
  if (values.destinationType === 'URL') {
    const url = values.linkUrl.trim();
    if (values.linkType === 'INTERNAL_PATH' && !(url.startsWith('/') && !url.startsWith('//'))) {
      errors.linkUrl = '내부 경로는 /로 시작해야 합니다.';
    }
    if (values.linkType === 'EXTERNAL_URL' && !/^https:\/\//i.test(url)) {
      errors.linkUrl = '외부 주소는 https://로 시작해야 합니다.';
    }
  }
  if (!/^\d+$/.test(values.displayOrder.trim())) {
    errors.displayOrder = '0 이상의 숫자를 입력해 주세요.';
  }
  return errors;
}

function toCreateBody(values: BannerFormValues): HomeBannerUpsertBody {
  const target = values.destinationType === 'TARGET';
  return {
    mediaId: values.mediaId!,
    destinationType: values.destinationType,
    targetType: target ? values.targetType : null,
    targetId: target && values.targetId.trim() ? Number(values.targetId) : null,
    linkType: target ? null : values.linkType,
    linkUrl: target ? null : values.linkUrl.trim(),
    displayOrder: Number(values.displayOrder),
    active: values.active,
  };
}

function BannerCreateForm() {
  const client = useQueryClient();
  const [values, setValues] = useState<BannerFormValues>(EMPTY_FORM);
  const [errors, setErrors] = useState<ReturnType<typeof validate>>({});
  // 이미지 필드는 고른 파일을 스스로 들고 있어, 등록 후 key를 바꿔 새로 그린다.
  const [formKey, setFormKey] = useState(0);
  const create = useMutation({
    ...createBannerMutation,
    onSuccess: () => {
      client.invalidateQueries({ queryKey: adminQueryKeys.banners });
      client.invalidateQueries({ queryKey: ['home', 'banners'] });
      setValues(EMPTY_FORM);
      setFormKey((key) => key + 1);
    },
  });

  const setField = <Key extends keyof BannerFormValues>(field: Key, value: BannerFormValues[Key]) =>
    setValues((current) => ({ ...current, [field]: value }));

  return (
    <section className="flex max-w-2xl flex-col gap-5">
      <h2 className="text-lg font-bold">배너 추가</h2>
      <form
        key={formKey}
        className="flex flex-col gap-5"
        onSubmit={(event) => {
          event.preventDefault();
          if (create.isPending) return;
          const nextErrors = validate(values);
          setErrors(nextErrors);
          if (Object.keys(nextErrors).length > 0) return;
          create.mutate(toCreateBody(values));
        }}
      >
        <ThumbnailField
          label="배너 이미지 *"
          value={values.mediaId}
          onChange={(mediaId) => setField('mediaId', mediaId)}
          upload={uploadHomeBannerImage}
          error={errors.mediaId}
        />

        <Field label="이동 방식">
          {(id) => (
            <Select
              id={id}
              value={values.destinationType}
              onValueChange={(value) => setField('destinationType', value as DestinationType)}
            >
              <Select.Item value="TARGET">서비스 안의 글</Select.Item>
              <Select.Item value="URL">주소 직접 입력</Select.Item>
            </Select>
          )}
        </Field>

        {values.destinationType === 'TARGET' ? (
          <div className="grid gap-5 md:grid-cols-2">
            <Field label="대상 유형">
              {(id) => (
                <Select
                  id={id}
                  value={values.targetType}
                  onValueChange={(value) => setField('targetType', value as TargetType)}
                >
                  {Object.entries(TARGET_TYPE_LABELS)
                    .filter(([value]) => value !== 'PROJECT')
                    .map(([value, label]) => (
                      <Select.Item key={value} value={value}>
                        {label}
                      </Select.Item>
                    ))}
                </Select>
              )}
            </Field>
            <Field label="대상 ID" error={errors.targetId}>
              {(id) => (
                <Input
                  id={id}
                  inputMode="numeric"
                  value={values.targetId}
                  onChange={(event) => setField('targetId', event.target.value)}
                  placeholder="예: 20"
                  aria-invalid={Boolean(errors.targetId)}
                />
              )}
            </Field>
          </div>
        ) : (
          <div className="grid gap-5 md:grid-cols-2">
            <Field label="주소 유형">
              {(id) => (
                <Select
                  id={id}
                  value={values.linkType}
                  onValueChange={(value) => setField('linkType', value as LinkType)}
                >
                  <Select.Item value="INTERNAL_PATH">내부 경로</Select.Item>
                  <Select.Item value="EXTERNAL_URL">외부 주소 (새 탭)</Select.Item>
                </Select>
              )}
            </Field>
            <Field label="주소 *" error={errors.linkUrl}>
              {(id) => (
                <Input
                  id={id}
                  value={values.linkUrl}
                  onChange={(event) => setField('linkUrl', event.target.value)}
                  placeholder={
                    values.linkType === 'INTERNAL_PATH' ? '/projects/@slug' : 'https://…'
                  }
                  aria-invalid={Boolean(errors.linkUrl)}
                />
              )}
            </Field>
          </div>
        )}

        <Field label="표시 순서 *" error={errors.displayOrder}>
          {(id) => (
            <Input
              id={id}
              inputMode="numeric"
              value={values.displayOrder}
              onChange={(event) => setField('displayOrder', event.target.value)}
              aria-invalid={Boolean(errors.displayOrder)}
            />
          )}
        </Field>

        <label className="flex items-center gap-2 text-sm text-gray-900">
          <input
            type="checkbox"
            checked={values.active}
            onChange={(event) => setField('active', event.target.checked)}
            className="accent-primary-500 size-4"
          />
          등록하자마자 노출
        </label>

        {create.isError && (
          <p role="alert" className="text-sm text-red-600">
            {getApiErrorMessage(create.error)}
          </p>
        )}
        <Button type="submit" size="lg" disabled={create.isPending}>
          {create.isPending ? '등록 중…' : '배너 등록'}
        </Button>
      </form>
    </section>
  );
}
