import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { Layout } from './components/Layout'
import { ProfileGuideModal } from './components/ProfileGuideModal'
import { AppDetailPage } from './pages/AppDetailPage'
import { EditAppPage } from './pages/EditAppPage'
import { BookmarksPage } from './pages/BookmarksPage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { MakerPage } from './pages/MakerPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { SubmitPage } from './pages/SubmitPage'
import type { AppItem, Maker } from './types'
import { isAuthConfigured, supabase, toAuthUser, type AuthUser } from './utils/auth'
import { createRemoteApp, fetchRemoteState, recordRemotePlay, setRemoteBookmark, toggleRemoteLike, updateRemoteApp, upsertRemoteProfile, verifyRemoteCrewAccessCode } from './utils/supabaseData'

const SUPABASE_REQUIRED_MESSAGE = 'Supabase 연결이 필요합니다. .env.local의 설정을 확인한 뒤 개발 서버를 다시 시작해주세요.'
const PROFILE_GUIDE_HIDE_KEY = 'dropit:profile-guide-hide-date'

function localDateKey() {
  const today = new Date()
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
}

function isProfileGuideHiddenToday() {
  try {
    return window.localStorage.getItem(PROFILE_GUIDE_HIDE_KEY) === localDateKey()
  } catch {
    return false
  }
}

function makerFromAuthUser(user: AuthUser): Maker {
  const initials = user.name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase() || user.name.slice(0, 2).toUpperCase()

  return {
    id: user.id,
    name: user.name,
    initials: initials || '나',
    avatarUrl: user.avatarUrl,
    role: 'GitHub 사용자',
    bio: '프로필을 설정하면 이곳에 소개가 표시됩니다.',
    tone: '#d9e6ff',
  }
}

function AuthLoadingPage() {
  return <div className="auth-loading page-pad">로그인 상태를 확인하고 있습니다.</div>
}

function DataStatusPage({ message }: { message: string }) {
  return <div className="auth-loading page-pad">{message}</div>
}

function safeRedirectPath(value: string) {
  try {
    const requested = new URL(value, window.location.origin)
    if (requested.origin !== window.location.origin) return '/submit'
    if (requested.pathname === '/' || requested.pathname === '/bookmarks' || requested.pathname === '/submit' || requested.pathname === '/makers/me' || /^\/apps\/[^/]+$/.test(requested.pathname) || /^\/apps\/[^/]+\/edit$/.test(requested.pathname)) return requested.pathname
  } catch {
    // Fall back to the app registration page for malformed redirect values.
  }
  return '/submit'
}

function App() {
  const location = useLocation()
  const navigate = useNavigate()
  const [apps, setApps] = useState<AppItem[]>([])
  const [profile, setProfile] = useState<Maker | null>(null)
  const [authUser, setAuthUser] = useState<AuthUser | null>(null)
  const [authLoading, setAuthLoading] = useState(isAuthConfigured)
  const [authError, setAuthError] = useState('')
  const [dataLoading, setDataLoading] = useState(Boolean(supabase))
  const [dataError, setDataError] = useState(supabase ? '' : SUPABASE_REQUIRED_MESSAGE)
  const [bookmarkedIds, setBookmarkedIds] = useState<string[]>([])
  const [likedIds, setLikedIds] = useState<string[]>([])
  const [playCounts, setPlayCounts] = useState<Record<string, number>>({})
  const [profileGuideOpen, setProfileGuideOpen] = useState(false)
  const profileGuideShownFor = useRef<string | null>(null)

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
  }, [location.pathname, location.search])

  useEffect(() => {
    if (!supabase) {
      setAuthLoading(false)
      return
    }

    let active = true
    const applySession = (user: Parameters<typeof toAuthUser>[0] | null) => {
      if (!active) return
      setAuthUser(user ? toAuthUser(user) : null)
      setAuthLoading(false)
    }

    void supabase.auth.getSession().then(({ data, error }) => {
      if (error) setAuthError('로그인 상태를 확인하지 못했습니다.')
      applySession(data.session?.user ?? null)
    })

    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      applySession(session?.user ?? null)
    })

    return () => {
      active = false
      subscription.unsubscribe()
    }
  }, [])

  useEffect(() => {
    if (authLoading) return

    if (supabase) {
      let active = true
      setDataLoading(true)
      setDataError('')
      void fetchRemoteState(authUser?.id ?? null).then((state) => {
        if (!active) return
        const profileWithAvatar = state.profile && !state.profile.avatarUrl && authUser?.avatarUrl
          ? { ...state.profile, avatarUrl: authUser.avatarUrl }
          : state.profile
        const appsWithAvatar = authUser?.avatarUrl
          ? state.apps.map((app) => app.ownerId === authUser.id && !app.maker.avatarUrl ? { ...app, maker: { ...app.maker, avatarUrl: authUser.avatarUrl } } : app)
          : state.apps
        setApps(appsWithAvatar)
        setProfile(profileWithAvatar)
        setBookmarkedIds(state.bookmarkedIds)
        setLikedIds(state.likedIds)
        setPlayCounts({})
        setDataLoading(false)
      }).catch(() => {
        if (!active) return
        setDataError('Supabase 데이터를 불러오지 못했습니다. 최신 supabase/schema.sql을 실행했는지 확인해주세요.')
        setDataLoading(false)
      })

      return () => {
        active = false
      }
    }
    setDataLoading(false)
    setDataError(SUPABASE_REQUIRED_MESSAGE)
  }, [authLoading, authUser?.id])

  useEffect(() => {
    if (!authUser) {
      profileGuideShownFor.current = null
      setProfileGuideOpen(false)
      return
    }
    if (authLoading || dataLoading || dataError || isProfileGuideHiddenToday() || profileGuideShownFor.current === authUser.id) {
      return
    }
    if (profile) return
    profileGuideShownFor.current = authUser.id
    setProfileGuideOpen(true)
  }, [authLoading, authUser, dataError, dataLoading, profile])

  const currentMaker = useMemo(() => authUser ? profile ?? makerFromAuthUser(authUser) : null, [authUser, profile])
  const displayApps = useMemo(() => apps.map((app) => ({ ...app, likes: app.likes + (likedIds.includes(app.id) ? 1 : 0), plays: app.plays + (playCounts[app.id] ?? 0) })), [apps, likedIds, playCounts])

  const toggleBookmark = useCallback((id: string) => {
    if (!authUser) {
      const returnTo = `${window.location.pathname}${window.location.search}`
      navigate(`/login?returnTo=${encodeURIComponent(returnTo)}&notice=bookmark`)
      return
    }
    if (!supabase) {
      setDataError(SUPABASE_REQUIRED_MESSAGE)
      return
    }
    const wasBookmarked = bookmarkedIds.includes(id)
    const nextIds = wasBookmarked ? bookmarkedIds.filter((item) => item !== id) : [...bookmarkedIds, id]
    setBookmarkedIds(nextIds)
    void setRemoteBookmark(authUser.id, id, !wasBookmarked).catch(() => {
      setBookmarkedIds((current) => wasBookmarked ? (current.includes(id) ? current : [...current, id]) : current.filter((item) => item !== id))
      setDataError('앱 저장에 실패했습니다. Supabase의 app_bookmarks 테이블과 권한 설정을 확인해주세요.')
    })
  }, [authUser, bookmarkedIds, navigate])

  const toggleLike = useCallback((id: string) => {
    if (!supabase || !authUser) return
    const wasLiked = likedIds.includes(id)
    setLikedIds(wasLiked ? likedIds.filter((item) => item !== id) : [...likedIds, id])
    void toggleRemoteLike(id).then((isLiked) => {
      setLikedIds((current) => isLiked ? (current.includes(id) ? current : [...current, id]) : current.filter((item) => item !== id))
    }).catch(() => {
      setLikedIds((current) => wasLiked ? (current.includes(id) ? current : [...current, id]) : current.filter((item) => item !== id))
      setDataError('좋아요 상태를 반영하지 못했습니다. 잠시 후 다시 시도해주세요.')
    })
  }, [authUser, likedIds])

  const launchApp = useCallback((id: string) => {
    setPlayCounts((current) => ({ ...current, [id]: (current[id] ?? 0) + 1 }))
    if (supabase) {
      void recordRemotePlay(id).catch(() => setDataError('실행 수를 반영하지 못했습니다.'))
    }
  }, [])

  const addApp = useCallback((app: AppItem) => {
    if (!supabase || !authUser) return
    const ownedApp = currentMaker && authUser ? { ...app, ownerId: authUser.id, maker: currentMaker } : app
    setApps((current) => [ownedApp, ...current])
    void createRemoteApp(ownedApp).catch(() => setDataError('앱을 Supabase에 저장하지 못했습니다.'))
  }, [authUser, currentMaker])

  const verifyCrewCode = useCallback(async (code: string) => {
    if (!supabase || !authUser) return false
    return verifyRemoteCrewAccessCode(code)
  }, [authUser])

  const updateApp = useCallback((updatedApp: AppItem) => {
    if (!supabase || !authUser) return
    setApps((current) => current.map((app) => app.id === updatedApp.id ? updatedApp : app))
    void updateRemoteApp(updatedApp).catch(() => setDataError('앱 수정 내용을 Supabase에 저장하지 못했습니다.'))
  }, [authUser])

  const saveProfile = useCallback((maker: Maker) => {
    if (!authUser) return
    const nextProfile = { ...maker, id: authUser.id }
    const nextApps = apps.map((app) => {
      const ownsApp = app.ownerId === authUser.id || (!app.ownerId && app.maker.id === authUser.id)
      return ownsApp ? { ...app, ownerId: authUser.id, maker: nextProfile } : app
    })
    setProfile(nextProfile)
    setApps(nextApps)
    if (!supabase) return
    const ownedApps = nextApps.filter((app) => app.ownerId === authUser.id)
    void Promise.all([upsertRemoteProfile(nextProfile), ...ownedApps.map((app) => updateRemoteApp(app))]).catch(() => setDataError('프로필 정보를 Supabase에 저장하지 못했습니다.'))
  }, [apps, authUser])

  const loginWithGithub = useCallback((redirectTo: string) => {
    if (!supabase) {
      setAuthError('Supabase 로그인 설정이 필요합니다.')
      return
    }
    setAuthError('')
    const redirectUrl = new URL(safeRedirectPath(redirectTo), window.location.origin).toString()
    void supabase.auth.signInWithOAuth({ provider: 'github', options: { redirectTo: redirectUrl } }).then(({ error }) => {
      if (error) setAuthError('GitHub 로그인에 실패했습니다. 다시 시도해주세요.')
    })
  }, [])

  const logout = useCallback(() => {
    if (!supabase) {
      setAuthUser(null)
      return
    }
    void supabase.auth.signOut().then(({ error }) => {
      if (error) {
        setAuthError('로그아웃하지 못했습니다.')
        return
      }
      setAuthUser(null)
      setProfile(null)
    })
  }, [])

  const closeProfileGuide = useCallback(() => setProfileGuideOpen(false), [])
  const hideProfileGuideToday = useCallback(() => {
    try {
      window.localStorage.setItem(PROFILE_GUIDE_HIDE_KEY, localDateKey())
    } catch {
      // Storage가 차단된 경우에도 팝업은 닫습니다.
    }
    setProfileGuideOpen(false)
  }, [])
  const goToProfile = useCallback(() => {
    setProfileGuideOpen(false)
    navigate('/makers/me')
  }, [navigate])

  const loginParams = new URLSearchParams(location.search)
  const loginReturnTo = loginParams.get('returnTo') ?? '/submit'
  const loginNotice = loginParams.get('notice') === 'bookmark' ? '저장한 앱을 이용하려면 로그인해주세요. 로그인 후 다시 저장할 수 있습니다.' : undefined
  const loginPage = <LoginPage isConfigured={isAuthConfigured} isLoading={authLoading} error={authError} notice={loginNotice} returnTo={loginReturnTo} onLogin={loginWithGithub} />
  const dataStatusPage = dataError ? <DataStatusPage message={dataError} /> : dataLoading ? <DataStatusPage message="Supabase 데이터를 불러오고 있습니다." /> : null
  const homePage = dataStatusPage ?? <HomePage apps={displayApps} bookmarkedIds={bookmarkedIds} onToggleBookmark={toggleBookmark} />
  const detailPage = dataStatusPage ?? <AppDetailPage apps={displayApps} profile={currentMaker} bookmarkedIds={bookmarkedIds} likedIds={likedIds} onToggleBookmark={toggleBookmark} onToggleLike={toggleLike} onLaunch={launchApp} />
  const makerPage = dataStatusPage ?? <MakerPage apps={displayApps} profile={profile} currentMaker={currentMaker} bookmarkedIds={bookmarkedIds} onSaveProfile={saveProfile} onToggleBookmark={toggleBookmark} />
  const bookmarksPage = dataStatusPage ?? <BookmarksPage apps={displayApps} bookmarkedIds={bookmarkedIds} onToggleBookmark={toggleBookmark} />
  const submitPage = authLoading ? <AuthLoadingPage /> : authUser && currentMaker ? dataStatusPage ?? <SubmitPage maker={currentMaker} onAddApp={addApp} onVerifyCrewCode={verifyCrewCode} /> : loginPage
  const editPage = authLoading ? <AuthLoadingPage /> : authUser && currentMaker ? dataStatusPage ?? <EditAppPage apps={apps} currentUserId={authUser.id} maker={currentMaker} onUpdateApp={updateApp} /> : <LoginPage isConfigured={isAuthConfigured} isLoading={authLoading} error={authError} returnTo={window.location.pathname} onLogin={loginWithGithub} />
  const profilePage = authLoading ? <AuthLoadingPage /> : authUser && currentMaker ? dataStatusPage ?? <MakerPage apps={displayApps} profile={profile} currentMaker={currentMaker} isOwnProfile bookmarkedIds={bookmarkedIds} onSaveProfile={saveProfile} onToggleBookmark={toggleBookmark} /> : <LoginPage isConfigured={isAuthConfigured} isLoading={authLoading} error={authError} returnTo="/makers/me" onLogin={loginWithGithub} />

  return (
    <>
      <Routes>
      <Route element={<Layout authUser={authUser} onLogout={logout} />}>
        <Route path="/" element={homePage} />
        <Route path="/login" element={authLoading ? <AuthLoadingPage /> : authUser ? homePage : loginPage} />
        <Route path="/apps/:appId" element={detailPage} />
        <Route path="/apps/:appId/edit" element={editPage} />
        <Route path="/submit" element={submitPage} />
        <Route path="/makers/me" element={profilePage} />
        <Route path="/makers/:makerId" element={makerPage} />
        <Route path="/bookmarks" element={bookmarksPage} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
      </Routes>
      <ProfileGuideModal open={profileGuideOpen && location.pathname !== '/makers/me'} onClose={closeProfileGuide} onHideToday={hideProfileGuideToday} onGoToProfile={goToProfile} />
    </>
  )
}

export default App
