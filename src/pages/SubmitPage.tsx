import { ArrowLeft, ArrowRight, Check, CircleHelp, Info, Upload } from 'lucide-react'
import type { ChangeEvent, FormEvent, ReactNode } from 'react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import type { AppFormValues, AppItem, Category, Maker } from '../types'
import { CATEGORIES } from '../types'
import { AppCard } from '../components/AppCard'
import { MarkdownContent } from '../components/MarkdownContent'
import { isValidUrl, slugify } from '../utils/format'
import { clearAppDraft, readAppDraft, writeAppDraft } from '../utils/draftStorage'

interface SubmitPageProps {
  onAddApp?: (app: AppItem) => void
  onUpdateApp?: (app: AppItem) => void
  onVerifyCrewCode?: (code: string) => Promise<boolean>
  maker: Maker
  editingApp?: AppItem
}

const initialValues: AppFormValues = {
  name: '', tagline: '', description: '', appUrl: '', githubUrl: '', category: '', techTags: '', thumbnailUrl: '',
}
const MAX_THUMBNAIL_SIZE = 2 * 1024 * 1024
const EMBEDDED_IMAGE_PATTERN = /^data:image\/(?:png|jpeg|webp|gif);base64,/

function formValuesFromApp(app: AppItem): AppFormValues {
  return {
    name: app.name,
    tagline: app.tagline,
    description: app.description,
    appUrl: app.appUrl,
    githubUrl: app.githubUrl ?? '',
    category: app.category,
    techTags: app.techTags.join(', '),
    thumbnailUrl: app.thumbnailUrl ?? '',
  }
}

export function SubmitPage({ onAddApp, onUpdateApp, onVerifyCrewCode, maker, editingApp }: SubmitPageProps) {
  const navigate = useNavigate()
  const isEditMode = Boolean(editingApp)
  const [values, setValues] = useState<AppFormValues>(() => editingApp ? formValuesFromApp(editingApp) : readAppDraft(maker.id)?.values ?? initialValues)
  const [errors, setErrors] = useState<Partial<Record<keyof AppFormValues, string>>>({})
  const [crewCode, setCrewCode] = useState('')
  const [crewCodeError, setCrewCodeError] = useState('')
  const [isVerifyingCrewCode, setIsVerifyingCrewCode] = useState(false)
  const [isDescriptionPreview, setIsDescriptionPreview] = useState(false)
  const [thumbnailFileName, setThumbnailFileName] = useState(() => editingApp ? '' : readAppDraft(maker.id)?.thumbnailFileName ?? '')
  const [thumbnailUploadError, setThumbnailUploadError] = useState('')
  const [submittedApp, setSubmittedApp] = useState<AppItem | null>(null)

  useEffect(() => {
    if (isEditMode) return
    const saveTimer = window.setTimeout(() => writeAppDraft(maker.id, { values, thumbnailFileName }), 450)
    return () => window.clearTimeout(saveTimer)
  }, [isEditMode, maker.id, thumbnailFileName, values])

  const previewApp = useMemo<AppItem>(() => ({
    id: editingApp?.id ?? 'preview', name: values.name || '서비스 이름', tagline: values.tagline || '서비스를 한 줄로 소개하세요.', description: values.description,
    category: (values.category || '실험') as Exclude<Category, '전체'>, thumbnailVariant: editingApp?.thumbnailVariant ?? 'new', thumbnailUrl: values.thumbnailUrl || undefined, appUrl: values.appUrl || 'https://example.com', githubUrl: values.githubUrl || undefined, maker, techTags: values.techTags.split(',').map((tag) => tag.trim()).filter(Boolean).slice(0, 5), plays: editingApp?.plays ?? 0, likes: editingApp?.likes ?? 0, createdAt: editingApp?.createdAt ?? new Date().toISOString(), source: 'submitted',
  }), [editingApp, maker, values])

  const update = (field: keyof AppFormValues, value: string) => {
    setValues((current) => ({ ...current, [field]: value }))
    if (field === 'thumbnailUrl') {
      setThumbnailFileName('')
      setThumbnailUploadError('')
    }
    if (errors[field]) setErrors((current) => ({ ...current, [field]: undefined }))
  }

  const handleThumbnailUpload = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    const supportedTypes = ['image/png', 'image/jpeg', 'image/webp', 'image/gif']
    if (!supportedTypes.includes(file.type)) {
      setThumbnailUploadError('PNG, JPG, WEBP, GIF 파일만 올릴 수 있습니다.')
      event.target.value = ''
      return
    }
    if (file.size > MAX_THUMBNAIL_SIZE) {
      setThumbnailUploadError('이미지 파일은 2MB 이하로 업로드해주세요.')
      event.target.value = ''
      return
    }

    const reader = new FileReader()
    reader.onload = () => {
      if (typeof reader.result !== 'string') {
        setThumbnailUploadError('이미지를 읽지 못했습니다. 다시 올려주세요.')
        return
      }
      setValues((current) => ({ ...current, thumbnailUrl: reader.result as string }))
      setThumbnailFileName(file.name)
      setThumbnailUploadError('')
      setErrors((current) => ({ ...current, thumbnailUrl: undefined }))
    }
    reader.onerror = () => setThumbnailUploadError('이미지를 읽지 못했습니다. 다시 올려주세요.')
    reader.readAsDataURL(file)
  }

  const clearThumbnail = () => {
    setValues((current) => ({ ...current, thumbnailUrl: '' }))
    setThumbnailFileName('')
    setThumbnailUploadError('')
  }

  const validate = () => {
    const nextErrors: Partial<Record<keyof AppFormValues, string>> = {}
    if (!values.name.trim()) nextErrors.name = '서비스 이름을 입력해주세요.'
    if (values.name.length > 40) nextErrors.name = '서비스 이름은 40자 이내로 입력해주세요.'
    if (!values.tagline.trim()) nextErrors.tagline = '한 줄 소개를 입력해주세요.'
    if (values.tagline.length > 80) nextErrors.tagline = '한 줄 소개는 80자 이내로 입력해주세요.'
    if (!values.appUrl.trim()) nextErrors.appUrl = '서비스 주소를 입력해주세요.'
    else if (!isValidUrl(values.appUrl)) nextErrors.appUrl = 'https://로 시작하는 주소를 입력해주세요.'
    if (values.githubUrl && !isValidUrl(values.githubUrl)) nextErrors.githubUrl = 'GitHub 주소를 확인해주세요.'
    if (!values.category) nextErrors.category = '카테고리를 선택해주세요.'
    if (values.description.length > 5000) nextErrors.description = '상세 설명은 5000자 이내로 입력해주세요.'
    if (values.thumbnailUrl && !isValidUrl(values.thumbnailUrl) && !EMBEDDED_IMAGE_PATTERN.test(values.thumbnailUrl)) nextErrors.thumbnailUrl = '이미지 주소를 확인해주세요.'
    setErrors(nextErrors)
    return Object.keys(nextErrors).length === 0
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (isVerifyingCrewCode) return
    if (!validate()) return

    if (!isEditMode) {
      if (!crewCode.trim()) {
        setCrewCodeError('크루 인증 코드를 입력해주세요.')
        return
      }
      if (!onVerifyCrewCode) {
        setCrewCodeError('인증 기능을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.')
        return
      }

      setIsVerifyingCrewCode(true)
      try {
        const isVerified = await onVerifyCrewCode(crewCode.trim())
        if (!isVerified) {
          setCrewCodeError('인증 코드가 맞지 않습니다.')
          return
        }
      } catch {
        setCrewCodeError('인증 코드를 확인하지 못했습니다. 잠시 후 다시 시도해주세요.')
        return
      } finally {
        setIsVerifyingCrewCode(false)
      }
    }

    const app: AppItem = editingApp
      ? { ...previewApp, id: editingApp.id, createdAt: editingApp.createdAt, plays: editingApp.plays, likes: editingApp.likes, ownerId: editingApp.ownerId ?? editingApp.maker.id, source: 'submitted' }
      : { ...previewApp, id: `${slugify(values.name)}-${Date.now()}`, createdAt: new Date().toISOString(), ownerId: maker.id, source: 'submitted' }
    if (editingApp) onUpdateApp?.(app)
    else {
      onAddApp?.(app)
      clearAppDraft(maker.id)
    }
    setSubmittedApp(app)
  }

  if (submittedApp) {
    return (
      <div className="submit-success page-pad">
        <div className="submit-success__icon"><Check size={24} /></div>
        <span className="section-kicker">{isEditMode ? '수정 완료' : '등록 완료'}</span>
        <h1>{submittedApp.name}<br />{isEditMode ? '수정했습니다.' : '등록했습니다.'}</h1>
        <p>{isEditMode ? '바꾼 내용이 반영됐습니다.' : '앱 목록에서 확인할 수 있습니다.'}</p>
        <div className="submit-success__actions"><button type="button" className="button button--primary" onClick={() => navigate(`/apps/${submittedApp.id}`)}>{isEditMode ? '수정한 앱 보기' : '등록한 앱 보기'} <ArrowRight size={16} /></button><Link to="/" className="button button--secondary">홈으로 돌아가기</Link></div>
      </div>
    )
  }

  return (
    <div className="submit-page">
      <div className="content-container">
        <Link to={editingApp ? `/apps/${editingApp.id}` : '/'} className="back-link"><ArrowLeft size={15} /> {editingApp ? '앱 상세' : '홈으로'}</Link>
        <div className="submit-page__heading"><span className="section-kicker">{isEditMode ? '앱 수정' : '새 서비스 공유하기'}</span><h1>{isEditMode ? <>앱 정보를<br /><em>수정해주세요.</em></> : <>직접 만든 서비스를<br /><em>소개해보세요.</em></>}</h1><p>{isEditMode ? '저장하면 바꾼 내용이 바로 반영됩니다.' : '어떤 서비스인지 간단히 소개해주세요.'}</p></div>
        <div className="submit-layout">
          <form className="submit-form" onSubmit={handleSubmit} noValidate>
            <FormSection title="기본 정보">
              <Field label="서비스 이름" htmlFor="app-name" error={errors.name} required hint="최대 40자">
                <input id="app-name" value={values.name} maxLength={40} onChange={(event) => update('name', event.target.value)} placeholder="예: 나만의 도구" />
              </Field>
              <Field label="한 줄 소개" htmlFor="app-tagline" error={errors.tagline} required hint={`${values.tagline.length}/80`}>
                <input id="app-tagline" value={values.tagline} maxLength={80} onChange={(event) => update('tagline', event.target.value)} placeholder="예: 매일 쓰는 메모장" />
              </Field>
              <Field label="상세 설명" htmlFor="app-description" error={errors.description} hint={`${values.description.length}/5000`}>
                <div className="markdown-editor__toolbar">
                  <span>마크다운</span>
                  <button type="button" className="markdown-editor__toggle" onClick={() => setIsDescriptionPreview((current) => !current)} aria-pressed={isDescriptionPreview}>
                    {isDescriptionPreview ? '편집하기' : '미리보기'}
                  </button>
                </div>
                {isDescriptionPreview ? (
                  <div className="markdown-preview" aria-live="polite">
                    {values.description.trim() ? <MarkdownContent content={values.description} /> : <p className="markdown-preview__empty">입력한 내용이 여기에 표시됩니다.</p>}
                  </div>
                ) : (
                  <textarea id="app-description" aria-describedby="app-description-guide" value={values.description} maxLength={5000} onChange={(event) => update('description', event.target.value)} placeholder="## 서비스 소개\n해결하는 문제와 사용 방법을 입력하세요.\n\n- 주요 기능\n- 사용 대상" rows={8} />
                )}
                <p id="app-description-guide" className="markdown-guide">마크다운을 지원해요. `## 제목`, `- 목록`, `**강조**`처럼 작성하면 서비스 페이지에서 보기 좋게 정리됩니다.</p>
              </Field>
            </FormSection>
            <FormSection title="서비스 주소">
              <Field label="서비스 주소" htmlFor="app-url" error={errors.appUrl} required hint="필수">
                <input id="app-url" type="url" value={values.appUrl} onChange={(event) => update('appUrl', event.target.value)} placeholder="https://your-app.com" />
              </Field>
              <Field label="GitHub 주소" htmlFor="github-url" error={errors.githubUrl} hint="선택">
                <input id="github-url" type="url" value={values.githubUrl} onChange={(event) => update('githubUrl', event.target.value)} placeholder="https://github.com/you/project" />
              </Field>
            </FormSection>
            <FormSection title="분류">
              <Field label="카테고리" htmlFor="app-category" error={errors.category} required>
                <select id="app-category" value={values.category} onChange={(event) => update('category', event.target.value)}><option value="">카테고리 선택</option>{CATEGORIES.filter((item): item is Exclude<Category, '전체'> => item !== '전체').map((item) => <option value={item} key={item}>{item}</option>)}</select>
              </Field>
                <Field label="기술 태그" htmlFor="app-tags" hint="쉼표로 구분, 최대 5개">
                <input id="app-tags" value={values.techTags} onChange={(event) => update('techTags', event.target.value)} placeholder="React, TypeScript, Vite" />
              </Field>
              <Field label="썸네일" htmlFor="app-thumbnail" error={errors.thumbnailUrl || thumbnailUploadError} hint="선택">
                <div className="input-with-icon"><Upload size={15} /><input id="app-thumbnail" type="url" value={EMBEDDED_IMAGE_PATTERN.test(values.thumbnailUrl) ? '' : values.thumbnailUrl} onChange={(event) => update('thumbnailUrl', event.target.value)} placeholder="이미지 주소 · https://..." /></div>
                <div className="thumbnail-upload-divider"><span>또는</span></div>
                <label className={`thumbnail-upload ${thumbnailFileName || EMBEDDED_IMAGE_PATTERN.test(values.thumbnailUrl) ? 'is-selected' : ''}`} htmlFor="app-thumbnail-file">
                  <Upload size={16} />
                  <span className="thumbnail-upload__name">{thumbnailFileName || (EMBEDDED_IMAGE_PATTERN.test(values.thumbnailUrl) ? '이미지가 등록되어 있습니다' : '이미지 파일을 선택하세요')}</span>
                  <span className="thumbnail-upload__button">파일 선택</span>
                </label>
                <input className="thumbnail-upload__input" id="app-thumbnail-file" type="file" accept="image/png,image/jpeg,image/webp,image/gif" onChange={handleThumbnailUpload} />
                {EMBEDDED_IMAGE_PATTERN.test(values.thumbnailUrl) ? <button type="button" className="thumbnail-upload__clear" onClick={clearThumbnail}>이미지 삭제</button> : null}
                <p className="thumbnail-guide">권장 크기: 가로 1200px × 세로 750px (16:10)<br />AI로 다듬을 때는 “1200 × 750px, 16:10 비율로 맞춰줘”라고 요청하세요.</p>
              </Field>
            </FormSection>
            {!isEditMode ? <FormSection title="크루 인증">
              <Field label="인증 코드" htmlFor="crew-code" error={crewCodeError} required hint="등록 권한 확인">
                <input id="crew-code" type="password" autoComplete="off" maxLength={80} value={crewCode} onChange={(event) => { setCrewCode(event.target.value); setCrewCodeError('') }} placeholder="크루에게 받은 인증 코드를 입력하세요" />
                <p className="crew-code-guide">외부인의 서비스 무단 등록을 방지하기 위해 필요한 크루 인증 코드입니다. 인증코드는 8기 샤를에게 DM 문의주세요! (update 07.21 - WOOWA8 입력하시면 됩니다.)</p>
              </Field>
            </FormSection> : null}
            <div className="form-note"><CircleHelp size={16} /><span>입력한 내용은 자동으로 임시 저장됩니다. 나중에 다시 이어서 작성할 수 있습니다.</span></div>
            <button type="submit" className="button button--primary button--submit" disabled={isVerifyingCrewCode}>{isVerifyingCrewCode ? '인증 확인 중' : isEditMode ? '수정 내용 저장' : '앱 등록하기'} <ArrowRight size={16} /></button>
          </form>
          <aside className="preview-panel">
            <AppCard app={previewApp} isBookmarked={false} onToggleBookmark={() => undefined} />
            <p className="preview-panel__hint"><Info size={14} /> 입력한 내용이 카드에 바로 표시됩니다.</p>
          </aside>
        </div>
      </div>
    </div>
  )
}

function FormSection({ title, children }: { title: string; children: ReactNode }) {
  return <section className="form-section"><h2>{title}</h2>{children}</section>
}

function Field({ label, htmlFor, error, required, hint, children }: { label: string; htmlFor: string; error?: string; required?: boolean; hint?: string; children: ReactNode }) {
  return <div className={`form-field ${error ? 'has-error' : ''}`}><div className="form-field__label"><label htmlFor={htmlFor}>{label}{required ? <span aria-hidden="true">*</span> : null}</label>{hint ? <span>{hint}</span> : null}</div>{children}{error ? <small className="form-error" role="alert">{error}</small> : null}</div>
}
