import { ArrowLeft, ArrowUpRight, Check, Play, RotateCcw } from 'lucide-react'
import type { FormEvent } from 'react'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import type { AppItem, Maker } from '../types'
import { AppCard } from '../components/AppCard'
import { Avatar } from '../components/Avatar'
import { EmptyState } from '../components/EmptyState'
import { formatNumber } from '../utils/format'

interface MakerPageProps {
  apps: AppItem[]
  deletedApps: AppItem[]
  profile: Maker | null
  currentMaker: Maker | null
  isOwnProfile?: boolean
  bookmarkedIds: string[]
  onSaveProfile: (maker: Maker) => void
  onToggleBookmark: (id: string) => void
  onRestoreApp: (id: string) => Promise<boolean>
}

export function MakerPage({ apps, deletedApps, profile, currentMaker, isOwnProfile = false, bookmarkedIds, onSaveProfile, onToggleBookmark, onRestoreApp }: MakerPageProps) {
  const { makerId } = useParams()
  const navigate = useNavigate()
  const [isEditing, setIsEditing] = useState(false)
  const [restoringAppId, setRestoringAppId] = useState<string | null>(null)
  const [restoreError, setRestoreError] = useState('')
  const isCurrentMakerPage = isOwnProfile || makerId === 'me' || currentMaker?.id === makerId
  const makerApps = isCurrentMakerPage && currentMaker
    ? apps.filter((app) => app.ownerId === currentMaker.id || app.maker.id === currentMaker.id)
    : apps.filter((app) => app.maker.id === makerId)
  const makerDeletedApps = isCurrentMakerPage && currentMaker
    ? deletedApps.filter((app) => app.ownerId === currentMaker.id || app.maker.id === currentMaker.id)
    : []
  const maker: Maker | undefined = isCurrentMakerPage ? currentMaker ?? makerApps[0]?.maker : makerApps[0]?.maker

  const handleRestore = async (appId: string) => {
    if (restoringAppId) return
    setRestoringAppId(appId)
    setRestoreError('')
    try {
      const didRestore = await onRestoreApp(appId)
      if (!didRestore) setRestoreError('서비스를 복구하지 못했습니다. 잠시 후 다시 시도해주세요.')
    } catch {
      setRestoreError('서비스를 복구하지 못했습니다. 잠시 후 다시 시도해주세요.')
    } finally {
      setRestoringAppId(null)
    }
  }

  if (isCurrentMakerPage && currentMaker && (isEditing || !profile)) {
    return <ProfileSetup profile={profile} avatarUrl={currentMaker.avatarUrl} onCancel={profile ? () => setIsEditing(false) : undefined} onSave={(nextProfile) => { onSaveProfile(nextProfile); setIsEditing(false) }} />
  }

  if (!maker) return <div className="page-pad"><EmptyState type="search" onReset={() => navigate('/')} /></div>

  const totalPlays = makerApps.reduce((total, app) => total + app.plays, 0)
  return (
    <div className="maker-page">
      <section className="maker-hero">
        <div className="content-container">
          <Link to="/" className="back-link"><ArrowLeft size={15} /> 전체 서비스</Link>
          <div className="maker-hero__profile"><Avatar maker={maker} size="large" /><div><span className="section-kicker">프로필</span><h1>{maker.name}</h1><p>{maker.role}</p></div>{isCurrentMakerPage && currentMaker ? <button type="button" className="button button--secondary button--small maker-hero__edit" onClick={() => setIsEditing(true)}>프로필 수정</button> : null}</div>
          <p className="maker-hero__bio">{maker.bio}</p>
          <div className="maker-stats"><span><strong>{makerApps.length}</strong><small>등록 앱</small></span><span><strong>{formatNumber(totalPlays)}</strong><small>누적 실행</small></span><span><strong>{makerApps.reduce((total, app) => total + app.likes, 0)}</strong><small>받은 좋아요</small></span></div>
        </div>
      </section>
      <section className="maker-apps"><div className="content-container"><div className="section-heading"><div><span className="section-kicker">만든 앱</span><h2>{maker.name}의 앱</h2></div><span className="section-heading__note"><Play size={14} fill="currentColor" /> {formatNumber(totalPlays)}회 실행</span></div>{makerApps.length > 0 ? <div className="app-grid">{makerApps.map((app) => <AppCard key={app.id} app={app} isBookmarked={bookmarkedIds.includes(app.id)} onToggleBookmark={onToggleBookmark} />)}</div> : <div className="empty-state empty-state--compact"><p>등록한 앱이 없습니다.</p><Link to="/submit" className="text-link">앱 등록하기 <ArrowUpRight size={14} /></Link></div>}
        {isCurrentMakerPage && makerDeletedApps.length > 0 ? <div className="maker-deleted-apps">
          <div className="section-heading maker-deleted-apps__heading"><div><span className="section-kicker">삭제한 앱</span><h2>복구할 수 있는 앱</h2></div><span className="section-heading__note">{makerDeletedApps.length}개</span></div>
          <p className="maker-deleted-apps__description">삭제한 서비스는 목록에서 숨겨져 있으며, 여기서 다시 복구할 수 있습니다.</p>
          <div className="deleted-app-list">{makerDeletedApps.map((app) => <div className="deleted-app-row" key={app.id}><div className="deleted-app-row__copy"><strong>{app.name}</strong><span>{app.tagline}</span></div><button type="button" className="button button--secondary button--small" onClick={() => void handleRestore(app.id)} disabled={Boolean(restoringAppId)}><RotateCcw size={14} /> {restoringAppId === app.id ? '복구 중...' : '복구하기'}</button></div>)}</div>
          {restoreError ? <p className="form-error" role="alert">{restoreError}</p> : null}
        </div> : null}
      </div></section>
    </div>
  )
}

function initialsFromName(value: string) {
  return value
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase() || '나'
}

export function ProfileSetup({ profile, avatarUrl, onCancel, onSave }: { profile: Maker | null; avatarUrl?: string | null; onCancel?: () => void; onSave: (maker: Maker) => void }) {
  const [name, setName] = useState(profile?.name ?? '')
  const [role, setRole] = useState(profile?.role ?? '')
  const [bio, setBio] = useState(profile?.bio ?? '')
  const [error, setError] = useState('')

  const isEditingProfile = Boolean(profile)
  const previewMaker: Maker = { id: profile?.id ?? 'me', name: name.trim() || '내 이름', initials: initialsFromName(name.trim() || profile?.name || ''), avatarUrl: profile?.avatarUrl ?? avatarUrl ?? null, role: role.trim() || '우테코 크루', bio: bio.trim() || '앱 소개가 여기에 표시됩니다.', tone: profile?.tone ?? '#d9e6ff' }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!name.trim()) {
      setError('이름을 입력해주세요.')
      return
    }
    if (!role.trim()) {
      setError('역할 또는 소속을 입력해주세요.')
      return
    }
    if (!bio.trim()) {
      setError('짧은 소개를 입력해주세요.')
      return
    }
    onSave({ ...previewMaker, name: name.trim(), role: role.trim(), bio: bio.trim() })
  }

  return (
    <div className="profile-setup page-pad">
      <div className="content-container">
        <Link to="/" className="back-link"><ArrowLeft size={15} /> 홈으로</Link>
        <div className="profile-setup__heading"><span className="section-kicker">{isEditingProfile ? '프로필 수정' : '프로필 등록'}</span><h1>{isEditingProfile ? <>프로필을<br /><em>수정해주세요.</em></> : <>내 프로필을<br /><em>먼저 만들어보세요.</em></>}</h1><p>{isEditingProfile ? '앱에 표시되는 정보를 수정합니다.' : '앱을 등록하기 전에 다른 사람에게 보일 정보를 설정해주세요.'}</p></div>
        <div className="profile-setup__card">
          <div className="profile-setup__preview"><span className="section-kicker section-kicker--on-dark"><i /> 미리보기</span><Avatar maker={previewMaker} size="large" /><h2>{previewMaker.name}</h2><p>{previewMaker.role}</p><span className="profile-setup__preview-line" /><small>{previewMaker.bio}</small></div>
          <form className="profile-form" onSubmit={handleSubmit} noValidate>
            <div className="form-field"><div className="form-field__label"><label htmlFor="profile-name">이름<span aria-hidden="true">*</span></label><span>최대 20자</span></div><input id="profile-name" value={name} maxLength={20} onChange={(event) => { setName(event.target.value); setError('') }} placeholder="예: 샤를" /></div>
            <div className="form-field"><div className="form-field__label"><label htmlFor="profile-role">역할 또는 소속<span aria-hidden="true">*</span></label><span>최대 40자</span></div><input id="profile-role" value={role} maxLength={40} onChange={(event) => { setRole(event.target.value); setError('') }} placeholder="예: 우아한테크코스 8기 BE" /></div>
            <div className="form-field"><div className="form-field__label"><label htmlFor="profile-bio">짧은 소개<span aria-hidden="true">*</span></label><span>최대 100자</span></div><textarea id="profile-bio" value={bio} maxLength={100} onChange={(event) => { setBio(event.target.value); setError('') }} placeholder="간단한 자기소개를 작성해주세요." rows={4} /></div>
            {error ? <p className="form-error" role="alert">{error}</p> : null}
            <div className="profile-form__actions"><button type="submit" className="button button--primary">프로필 저장하기 <Check size={16} /></button>{onCancel ? <button type="button" className="button button--secondary" onClick={onCancel}>취소</button> : null}</div>
          </form>
        </div>
      </div>
    </div>
  )
}
